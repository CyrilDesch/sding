package sding.agent

import dev.langchain4j.model.chat.request.json.*
import scala.compiletime.*
import scala.deriving.*

trait JsonSchemaOf[A]:
  def element: JsonSchemaElement
  def isOptional: Boolean = false

object JsonSchemaOf:

  given JsonSchemaOf[String] with
    def element = JsonStringSchema.builder().build()

  given JsonSchemaOf[Int] with
    def element = JsonIntegerSchema.builder().build()

  given JsonSchemaOf[Long] with
    def element = JsonIntegerSchema.builder().build()

  given JsonSchemaOf[Double] with
    def element = JsonNumberSchema.builder().build()

  given JsonSchemaOf[Float] with
    def element = JsonNumberSchema.builder().build()

  given JsonSchemaOf[Boolean] with
    def element = JsonBooleanSchema.builder().build()

  given [A](using sa: JsonSchemaOf[A]): JsonSchemaOf[List[A]] with
    def element = JsonArraySchema.builder().items(sa.element).build()

  given [A](using sa: JsonSchemaOf[A]): JsonSchemaOf[Option[A]] with
    def element             = sa.element
    override def isOptional = true

  given JsonSchemaOf[Map[String, String]] with
    def element = JsonObjectSchema.builder().build()

  private[agent] final class ProductJsonSchemaOf[A](schema: JsonObjectSchema) extends JsonSchemaOf[A]:
    def element = schema

  inline def derived[A](using m: Mirror.ProductOf[A]): JsonSchemaOf[A] =
    val labels    = elemLabels[m.MirroredElemLabels]
    val instances = summonSchemas[m.MirroredElemTypes]
    val required  = labels.zip(instances).collect { case (name, s) if !s.isOptional => name }
    val builder   = JsonObjectSchema.builder()
    labels.zip(instances).foreach { (name, s) => builder.addProperty(name, s.element) }
    if required.nonEmpty then builder.required(required*)
    new ProductJsonSchemaOf[A](builder.build())

  private inline def elemLabels[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t)   => constValue[h].asInstanceOf[String] :: elemLabels[t]

  private inline def summonSchemas[T <: Tuple]: List[JsonSchemaOf[?]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t)   => summonInline[JsonSchemaOf[h]] :: summonSchemas[t]
