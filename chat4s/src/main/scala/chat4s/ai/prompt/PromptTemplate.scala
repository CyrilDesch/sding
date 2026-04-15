package chat4s.ai.prompt

final case class PromptTemplate(name: String, template: String, version: Int):
  def render(vars: Map[String, String]): String =
    vars.foldLeft(template) { case (acc, (key, value)) =>
      acc.replace(s"{{ $key }}", value).replace(s"{{$key}}", value)
    }
