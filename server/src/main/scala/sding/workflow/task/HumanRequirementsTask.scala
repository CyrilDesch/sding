package sding.workflow.task

import cats.effect.Async
import cats.syntax.all.*
import chat4s.ai.JsonSchemaOf
import chat4s.io.ChatContext
import chat4s.io.UserInputRequest.*
import io.circe.Decoder
import io.circe.Encoder
import sding.protocol.WorkflowStep
import sding.workflow.TaskNode
import sding.workflow.state.ProjectContextState

final case class ProjectRequirements(
    projectType: String,
    projectSoftwareType: String,
    projectProblem: Option[String] = None,
    projectDomain: Option[String] = None,
    projectPromiseStatement: Option[String] = None
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf

final class HumanRequirementsTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = WorkflowStep.HumanRequirements

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      _            <- chatContext.sendMessage("Please provide your project requirements.")
      projectType  <- chatContext.requestInput(Choice("Market type?", List("B2B", "B2C", "C2C", "C2B")))
      softwareType <- chatContext.requestInput(
        Choice("Software type?", List("Web SaaS", "Software", "Mobile App", "Hardware", "Other"))
      )
      problem <- chatContext.requestInput(FreeText("Describe the problem you want to solve:"))
    yield state.copy(
      projectRequirements = Some(
        ProjectRequirements(
          projectType = projectType,
          projectSoftwareType = softwareType,
          projectProblem = Some(problem)
        )
      )
    )
