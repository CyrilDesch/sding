package chat4s.ai

/** Per-call configuration for an [[Agent]] invocation.
  *
  * These are runtime concerns that vary per task and must not be baked into the
  * agent at construction time.
  */
final case class CallConfig(
    maxToolCallRounds: Int  = 5,
    maxToolResultChars: Int = 2000
)

object CallConfig:
  val default: CallConfig = CallConfig()
