package chat4s.ai.prompt

import scala.util.control.NoStackTrace

final class PromptLoadError(val name: String, cause: String)
    extends RuntimeException(s"Prompt '$name' failed: $cause")
    with NoStackTrace
