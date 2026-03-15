package sding.domain

import scala.util.control.NoStackTrace
import sding.protocol.WorkflowStep

sealed trait AppError extends NoStackTrace:
  def message: String
  override def getMessage: String = message

object AppError:

  sealed trait ConfigError extends AppError
  object ConfigError:
    final case class LoadFailed(message: String)          extends ConfigError
    final case class MissingEnvVariable(variable: String) extends ConfigError:
      val message: String = s"Missing required environment variable: $variable"

  sealed trait AuthError extends AppError
  object AuthError:
    final case class InvalidCredentials(message: String)      extends AuthError
    final case class InvalidToken(message: String)            extends AuthError
    final case class TokenExpired(message: String)            extends AuthError
    final case class InsufficientPermissions(message: String) extends AuthError

  sealed trait ChatError extends AppError
  object ChatError:
    final case class LlmNotConfigured(userId: UserId) extends ChatError:
      val message: String = s"No LLM configuration for user: ${userId.asString}"
    final case class ChatNotFound(id: ChatId) extends ChatError:
      val message: String = s"Chat not found: ${id.asString}"
    final case class NoActiveWorkflow(chatId: ChatId) extends ChatError:
      val message: String = s"No active workflow for chat: ${chatId.asString}"
    final case class NoInputRequest(chatId: ChatId) extends ChatError:
      val message: String = s"No pending input request for chat: ${chatId.asString}"
    final case class InputRequestMismatch(message: String) extends ChatError
    final case class InvalidInput(message: String)         extends ChatError

  sealed trait WorkflowError extends AppError
  object WorkflowError:
    final case class GraphCompilationFailed(message: String)          extends WorkflowError
    final case class NodeExecutionFailed(node: String, cause: String) extends WorkflowError:
      val message: String = s"Node '$node' failed: $cause"
    final case class WorkflowCreationFailed(stepType: String) extends WorkflowError:
      val message: String = s"Failed to create workflow for step type: $stepType"

  sealed trait AgentError extends AppError
  object AgentError:
    final case class LlmInvocationFailed(step: WorkflowStep, cause: String) extends AgentError:
      val message: String = s"Step '${step.friendlyName}' failed: $cause"
    final case class StructuredOutputFailed(step: WorkflowStep, cause: String) extends AgentError:
      val message: String = s"Structured output parsing failed in ${step.friendlyName}: $cause"
    final case class PromptLoadFailed(name: String, cause: String) extends AgentError:
      val message: String = s"Prompt '$name' failed: $cause"
    final case class QuotaExhausted(message: String) extends AgentError

  sealed trait RepositoryError extends AppError
  object RepositoryError:
    final case class NotFound(entity: String, id: String) extends RepositoryError:
      val message: String = s"$entity not found: $id"
    final case class DatabaseError(message: String) extends RepositoryError
