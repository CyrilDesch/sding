package sding.agent

final case class PromptTemplate(name: String, template: String):
  def render(vars: Map[String, String]): String =
    vars.foldLeft(template) { case (acc, (key, value)) =>
      acc.replace(s"{{ $key }}", value).replace(s"{{$key}}", value)
    }
