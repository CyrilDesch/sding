package chat4s.ai.llm

final case class AgentConfig(
    name: String,
    systemPromptName: String,
    modelName: String,
    temperature: Double,
    thinkingBudget: Option[Int]
)
