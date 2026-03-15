package sding.domain

import cats.Show
import io.circe.Decoder
import io.circe.Encoder

enum UserRole:
  case Admin, User

object UserRole:
  given Show[UserRole]    = Show.fromToString
  given Encoder[UserRole] = Encoder.encodeString.contramap(_.productPrefix.toUpperCase)
  given Decoder[UserRole] = Decoder.decodeString.emap:
    case "ADMIN" => Right(Admin)
    case "USER"  => Right(User)
    case other   => Left(s"Unknown UserRole: $other")

enum ProjectStatus:
  case Draft, InProgress, Completed, Archived

object ProjectStatus:
  given Show[ProjectStatus]    = Show.fromToString
  given Encoder[ProjectStatus] = Encoder.encodeString.contramap:
    case Draft      => "draft"
    case InProgress => "in_progress"
    case Completed  => "completed"
    case Archived   => "archived"

  given Decoder[ProjectStatus] = Decoder.decodeString.emap:
    case "draft"       => Right(Draft)
    case "in_progress" => Right(InProgress)
    case "completed"   => Right(Completed)
    case "archived"    => Right(Archived)
    case other         => Left(s"Unknown ProjectStatus: $other")

enum SenderType:
  case User, System, Agent

object SenderType:
  given Show[SenderType]    = Show.fromToString
  given Encoder[SenderType] = Encoder.encodeString.contramap(_.productPrefix.toUpperCase)
  given Decoder[SenderType] = Decoder.decodeString.emap:
    case "USER"   => Right(User)
    case "SYSTEM" => Right(System)
    case "AGENT"  => Right(Agent)
    case other    => Left(s"Unknown SenderType: $other")

enum ContentType:
  case Text, Markdown, Html

object ContentType:
  given Show[ContentType]    = Show.fromToString
  given Encoder[ContentType] = Encoder.encodeString.contramap(_.productPrefix.toUpperCase)
  given Decoder[ContentType] = Decoder.decodeString.emap:
    case "TEXT"     => Right(Text)
    case "MARKDOWN" => Right(Markdown)
    case "HTML"     => Right(Html)
    case other      => Left(s"Unknown ContentType: $other")

enum MessageType:
  case Message, StateUpdate, InputRequest, InputSubmission, Error

object MessageType:
  given Show[MessageType]    = Show.fromToString
  given Encoder[MessageType] = Encoder.encodeString.contramap:
    case Message         => "message"
    case StateUpdate     => "state_update"
    case InputRequest    => "input_request"
    case InputSubmission => "input_submission"
    case Error           => "error"

  given Decoder[MessageType] = Decoder.decodeString.emap:
    case "message"          => Right(Message)
    case "state_update"     => Right(StateUpdate)
    case "input_request"    => Right(InputRequest)
    case "input_submission" => Right(InputSubmission)
    case "error"            => Right(Error)
    case other              => Left(s"Unknown MessageType: $other")

enum MessageFormat:
  case Text, Html, Markdown

object MessageFormat:
  given Show[MessageFormat]    = Show.fromToString
  given Encoder[MessageFormat] = Encoder.encodeString.contramap(_.productPrefix.toLowerCase)
  given Decoder[MessageFormat] = Decoder.decodeString.emap:
    case "text"     => Right(Text)
    case "html"     => Right(Html)
    case "markdown" => Right(Markdown)
    case other      => Left(s"Unknown MessageFormat: $other")

enum InputType:
  case Text, Choice, MultipleChoice, Number, File

object InputType:
  given Show[InputType]    = Show.fromToString
  given Encoder[InputType] = Encoder.encodeString.contramap:
    case Text           => "text"
    case Choice         => "choice"
    case MultipleChoice => "multiple_choice"
    case Number         => "number"
    case File           => "file"

  given Decoder[InputType] = Decoder.decodeString.emap:
    case "text"            => Right(Text)
    case "choice"          => Right(Choice)
    case "multiple_choice" => Right(MultipleChoice)
    case "number"          => Right(Number)
    case "file"            => Right(File)
    case other             => Left(s"Unknown InputType: $other")
