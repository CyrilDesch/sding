package chat4s.ai

import scala.compiletime.*
import scala.deriving.*

/** LLM-agnostic JSON Schema representation. */
sealed trait SchemaElement

object SchemaElement:
  case object JsString                                                                      extends SchemaElement
  case object JsInteger                                                                     extends SchemaElement
  case object JsNumber                                                                      extends SchemaElement
  case object JsBoolean                                                                     extends SchemaElement
  final case class JsArray(items: SchemaElement)                                            extends SchemaElement
  final case class JsObject(properties: Map[String, SchemaElement], required: List[String]) extends SchemaElement

/** Typeclass that provides a JSON schema description for [[A]].
  * Used by [[Agent]] to communicate the expected output structure to the LLM.
  */
trait JsonSchemaOf[A]:
  def element: SchemaElement
  def isOptional: Boolean = false

object JsonSchemaOf:

  /** Non-inline factory: keeps the anonymous class body out of every `derives` call site. */
  def fromSchema[A](schema: SchemaElement): JsonSchemaOf[A] =
    new JsonSchemaOf[A]:
      def element = schema

  given JsonSchemaOf[String] with
    def element = SchemaElement.JsString

  given JsonSchemaOf[Int] with
    def element = SchemaElement.JsInteger

  given JsonSchemaOf[Long] with
    def element = SchemaElement.JsInteger

  given JsonSchemaOf[Double] with
    def element = SchemaElement.JsNumber

  given JsonSchemaOf[Float] with
    def element = SchemaElement.JsNumber

  given JsonSchemaOf[Boolean] with
    def element = SchemaElement.JsBoolean

  given [A](using sa: JsonSchemaOf[A]): JsonSchemaOf[List[A]] with
    def element = SchemaElement.JsArray(sa.element)

  given [A](using sa: JsonSchemaOf[A]): JsonSchemaOf[Option[A]] with
    def element             = sa.element
    override def isOptional = true

  given JsonSchemaOf[Map[String, String]] with
    def element = SchemaElement.JsObject(Map.empty, Nil)

  inline def derived[A](using m: Mirror.ProductOf[A]): JsonSchemaOf[A] =
    val labels    = elemLabels[m.MirroredElemLabels]
    val instances = summonSchemas[m.MirroredElemTypes]
    val required  = labels.zip(instances).collect { case (name, s) if !s.isOptional => name }
    val props     = labels.zip(instances).map { (name, s) => name -> s.element }.toMap
    val schema    = SchemaElement.JsObject(props, required)
    fromSchema[A](schema)

  private inline def elemLabels[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t)   => constValue[h].asInstanceOf[String] :: elemLabels[t]

  private inline def summonSchemas[T <: Tuple]: List[JsonSchemaOf[?]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t)   => summonInline[JsonSchemaOf[h]] :: summonSchemas[t]
