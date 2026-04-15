# chat4s

A Scala 3 library for building LLM-powered workflow graphs using a pure-functional, streaming architecture.

chat4s models your AI application as a **directed graph** of computation steps. Each step transforms an immutable state value, steps are wired together with static or conditional edges, and the engine executes the graph as a lazy `fs2.Stream`. The library ships with first-class support for multiple LLM providers, YAML-based prompt management, automatic checkpointing for crash-recovery, and built-in distributed tracing via OpenTelemetry.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Core Concepts](#core-concepts)
  - [State](#state)
  - [Step](#step)
  - [WorkflowDef](#workflowdef)
  - [WorkflowEngine](#workflowengine)
  - [WorkflowJournal](#workflowjournal)
- [LLM Integration](#llm-integration)
  - [Agent](#agent)
  - [AgentStep](#agentstep)
  - [AgentTool](#agenttool)
  - [JsonSchemaOf](#jsonschemaof)
  - [LLM Providers](#llm-providers)
- [Prompt Management](#prompt-management)
- [Chat Context](#chat-context)
- [Running a Workflow](#running-a-workflow)
  - [Start from scratch](#start-from-scratch)
  - [Resume after a crash](#resume-after-a-crash)
  - [Cancellation](#cancellation)
- [Conditional Branching](#conditional-branching)
- [Workflow Visualization](#workflow-visualization)
- [Distributed Tracing](#distributed-tracing)
- [Complete Example](#complete-example)
- [Testing](#testing)
- [Dependencies](#dependencies)

---

## Architecture Overview

```
                     WorkflowDef[F, S]
                   ┌──────────────────────────────────┐
 initial state ──► │  entry ──► step_a ──► step_b ──► │ ──► terminal
                   │              │                    │
                   │              ▼ (conditional)      │
                   │           step_c                  │
                   └──────────────────────────────────┘
                              │
                              ▼
                    WorkflowEngine.start(...)
                              │
                              ▼
                    Stream[F, StepResult[S]]
                    (lazy, cancellable, resumable)
```

**Layers:**

| Layer | Responsibility |
|---|---|
| **State `S`** | Immutable data passed through every step |
| **Step[F, S]** | Single node: `S => F[S]` |
| **WorkflowDef[F, S]** | Graph declaration: nodes + edges + entry point |
| **WorkflowEngine[F]** | Executes the graph, handles checkpointing and cancellation |
| **WorkflowJournal[F, S]** | Persists checkpoints for crash-recovery |
| **Agent[F]** | LLM invocation wrapper (structured output + tool calls) |
| **AgentStep[F, S]** | Step backed by an LLM agent |
| **ChatContext[F]** | Bidirectional communication channel with the user |

---

## Core Concepts

### State

State is a plain immutable case class you define. It flows through every node of the graph; each step receives the current state and returns a new state.

```scala
final case class PipelineState(
    userQuery:   String,
    searchResults: Option[List[String]],
    summary:     Option[String],
    approved:    Boolean
) derives io.circe.Decoder, io.circe.Encoder.AsObject
// Circe instances are required only if you use a persistent journal.
```

The state type is fully generic — `WorkflowDef[F, S]`, `Step[F, S]`, and `WorkflowEngine[F]` are all parameterised on `S`.

---

### Step

`Step[F[_], S]` is the base trait for every workflow node.

```scala
package chat4s.graph

trait Step[F[_], S]:
  /** Unique identifier within the workflow. */
  def id: String

  /** Pure transformation: receives the current state, returns the next state. */
  def execute(state: S): F[S]
```

**Minimal example**

```scala
import cats.effect.IO
import chat4s.graph.Step

final class SanitiseQueryStep extends Step[IO, PipelineState]:
  val id = "sanitise_query"

  def execute(state: PipelineState): IO[PipelineState] =
    IO.pure(state.copy(userQuery = state.userQuery.trim.toLowerCase))
```

Steps can be:

- **Pure computation** – wrap with `IO.pure` / `Sync[F].pure`.
- **Effectful** – database calls, HTTP requests, file I/O.
- **LLM-powered** – extend `AgentStep` (see [AgentStep](#agentstep)).
- **Human-in-the-loop** – use `ChatContext[F]` to request input from the user.

---

### WorkflowDef

`WorkflowDef[F, S]` is a pure data structure that describes the graph. Nothing is executed when you build it.

```scala
package chat4s.graph

final case class WorkflowDef[F[_], S](
    nodes:            Map[String, Step[F, S]],
    edges:            List[WorkflowEdge],
    conditionalEdges: List[ConditionalWorkflowEdge[S]],
    entryPoint:       String
)
```

| Field | Description |
|---|---|
| `nodes` | Map from step id to `Step` instance |
| `edges` | Static (unconditional) directed edges |
| `conditionalEdges` | State-dependent routing (see [Conditional Branching](#conditional-branching)) |
| `entryPoint` | Id of the first step to execute |

**Terminal nodes** are steps with no outgoing edge (neither static nor conditional). The engine stops when it reaches one.

```scala
import chat4s.graph.*

val wf = WorkflowDef[IO, PipelineState](
  nodes = Map(
    "sanitise"  -> SanitiseQueryStep(),
    "search"    -> SearchStep(searchClient),
    "summarise" -> SummariseStep(agent, promptLoader, chatCtx)
  ),
  edges = List(
    WorkflowEdge("sanitise",  "search"),
    WorkflowEdge("search",    "summarise")
  ),
  conditionalEdges = Nil,
  entryPoint = "sanitise"
)
```

**Static edge**

```scala
final case class WorkflowEdge(from: String, to: String)
```

**Conditional edge** — see [Conditional Branching](#conditional-branching).

---

### WorkflowEngine

`WorkflowEngine[F]` manages the execution lifecycle.

```scala
package chat4s.graph

trait WorkflowEngine[F[_]]:

  /** Start a brand-new execution. */
  def start[S](
      workflowId: WorkflowId,
      wf:         WorkflowDef[F, S],
      initialState: S,
      journal:    WorkflowJournal[F, S]
  ): F[WorkflowExecution[F, S]]

  /** Resume from the last saved checkpoint, if one exists. */
  def resume[S](
      workflowId: WorkflowId,
      wf:         WorkflowDef[F, S],
      journal:    WorkflowJournal[F, S]
  ): F[Option[WorkflowExecution[F, S]]]

object WorkflowEngine:
  /** Create the default (live) engine. Requires Async + Tracer. */
  def make[F[_]: Async: Tracer]: WorkflowEngine[F]
```

The engine returns a `WorkflowExecution[F, S]`:

```scala
trait WorkflowExecution[F[_], S]:
  def id:     WorkflowId
  def stream: fs2.Stream[F, StepResult[S]]  // lazy, pull-based
  def cancel: F[Unit]
```

`stream` emits one `StepResult[S]` after each step completes:

```scala
final case class StepResult[S](stepId: String, state: S)
```

The stream is **lazy** — steps only execute when you pull from it (e.g., `compile.drain`, `compile.toList`).

---

### WorkflowJournal

`WorkflowJournal[F, S]` persists checkpoints so a workflow can survive crashes.

```scala
package chat4s.graph

trait WorkflowJournal[F[_], S]:
  def save(workflowId: WorkflowId, checkpoint: WorkflowCheckpoint[S]): F[Unit]
  def load(workflowId: WorkflowId): F[Option[WorkflowCheckpoint[S]]]

final case class WorkflowCheckpoint[S](
    completedStep: Option[String],  // None = not started yet
    state:         S
)
```

The engine saves a checkpoint **after every step** completes. On resume, it skips all steps up to (and including) the last completed one and continues from the next node.

**Built-in implementations**

```scala
// No persistence — suitable for fire-and-forget workflows
val noop: WorkflowJournal[IO, S] = WorkflowJournal.noop[IO, S]

// In-memory Ref — suitable for single-run resumption within one JVM instance
val mem: IO[WorkflowJournal[IO, S]] = WorkflowJournal.inMemory[IO, S]
```

For durable persistence (survives JVM restarts), implement the trait and back it with a database or file store. The checkpoint state type `S` must have Circe `Encoder`/`Decoder` instances.

---

## LLM Integration

### Agent

`Agent[F]` is the primary interface for calling an LLM.

```scala
package chat4s.ai.agent

trait Agent[F[_]]:
  def name: String

  /** Single-turn call that returns a strongly-typed structured output. */
  def call[A: Decoder: JsonSchemaOf](
      prompt:     String,
      promptLink: PromptLink
  ): F[AgentResult[A]]

  /** Agentic loop: LLM may call tools zero or more times before returning. */
  def tooledCall[A: Decoder: JsonSchemaOf](
      prompt:     String,
      tools:      List[AgentTool[F]],
      promptLink: PromptLink,
      config:     CallConfig = CallConfig.default
  ): F[AgentResult[A]]
```

**`AgentResult`**

```scala
enum AgentResult[+A]:
  case Success(value: A, agentName: String)
  case Failure(errorMessage: String, agentName: String)
```

**`CallConfig`** — controls the agentic loop

```scala
final case class CallConfig(
    maxToolCallRounds: Int = 5,      // max LLM ↔ tool round-trips
    maxToolResultChars: Int = 2000   // truncate tool output to this length
)
```

**Create an agent**

```scala
import chat4s.ai.agent.AgentFactory
import chat4s.ai.llm.LlmProvider

val agent: IO[Agent[IO]] =
  AgentFactory.makeAgent[IO](LlmProvider.Anthropic, apiKey = sys.env("ANTHROPIC_KEY"), model = "claude-sonnet-4-5")
```

---

### AgentStep

`AgentStep[F, S]` is an abstract base class that wires an `Agent[F]` into the workflow graph. Extend it to create LLM-powered steps.

```scala
package chat4s.ai.agent

abstract class AgentStep[F[_]: Async, S](
    agent:        Agent[F],
    promptLoader: PromptLoader[F],
    chatContext:  ChatContext[F]
) extends Step[F, S]:

  /** Name of the task prompt to load from prompts.yaml. */
  protected def promptName: String

  /** Variables injected into the prompt template. */
  protected def templateVars(state: S): Map[String, String]

  /** Merge the LLM output back into the state. */
  protected def updateState[A](state: S, result: A): S

  /** Human-readable label shown to the user while the step runs. */
  protected def displayName: String = id

  /** Exception factory — called when the agent returns Failure. */
  protected def onFailure(msg: String): Throwable

  /** Optional pre-processing before the agent call. */
  protected def beforeRun(state: S): S = state

  /** Run the agent and update state — call this from your subclass. */
  protected def runAgent[A: Decoder: JsonSchemaOf](state: S): F[S]
```

**Example subclass**

```scala
import io.circe.Decoder
import io.circe.generic.semiauto.*

final case class SummaryOutput(summary: String) derives Decoder, JsonSchemaOf

final class SummariseStep[F[_]: Async](
    agent:        Agent[F],
    promptLoader: PromptLoader[F],
    chatCtx:      ChatContext[F]
) extends AgentStep[F, PipelineState](agent, promptLoader, chatCtx):

  val id          = "summarise"
  val promptName  = "summarise_results"      // matches key in prompts.yaml

  def templateVars(state: PipelineState) = Map(
    "query"   -> state.userQuery,
    "results" -> state.searchResults.getOrElse(Nil).mkString("\n")
  )

  def updateState[A](state: PipelineState, result: A): PipelineState =
    result match
      case out: SummaryOutput => state.copy(summary = Some(out.summary))
      case _                  => state

  def onFailure(msg: String): Throwable = RuntimeException(s"Summarise failed: $msg")

  def execute(state: PipelineState): F[PipelineState] =
    runAgent[SummaryOutput](state)
```

`AgentStep` automatically:

- Loads the system prompt (by convention: agent name) and task prompt (`promptName`) from the `PromptLoader`.
- Sends a `sendState(displayName)` notification to the user before executing.
- Handles `AgentResult.Failure` by raising `onFailure(msg)` as an exception.

---

### AgentTool

`AgentTool[F]` represents an external capability the LLM can invoke during a `tooledCall`.

```scala
package chat4s.ai.agent

trait AgentTool[F[_]]:
  def name:        String
  def description: String
  def inputSchema: Option[SchemaElement.JsObject] = None
  def execute(input: String): F[String]
```

**Example**

```scala
final class CalculatorTool[F[_]: Sync] extends AgentTool[F]:
  val name        = "calculator"
  val description = "Evaluates a mathematical expression and returns the numeric result."
  override val inputSchema = Some(
    SchemaElement.JsObject(
      properties = Map("expression" -> SchemaElement.JsString),
      required   = List("expression")
    )
  )

  def execute(rawInput: String): F[String] =
    Sync[F].delay:
      // parse and evaluate `rawInput` (JSON with key "expression")
      val expr = io.circe.parser.parse(rawInput).flatMap(_.hcursor.get[String]("expression"))
      expr.fold(_ => "error", evalExpression)
```

Use tools in a step:

```scala
agent.tooledCall[AnalysisOutput](
  prompt     = "Analyse the following data: ...",
  tools      = List(CalculatorTool[F](), WebSearchTool.stub[F]),
  promptLink = promptLink
)
```

**Built-in stubs**

```scala
// Stub web search tool — logs calls but does not make real HTTP requests
val stub: WebSearchTool[F] = WebSearchTool.stub[F]
```

---

### JsonSchemaOf

`JsonSchemaOf[A]` is a typeclass that generates a JSON schema description for type `A`, used by the LLM to produce structured output.

**Derivation** — add `derives JsonSchemaOf` to your case class:

```scala
final case class AnalysisOutput(
    title:   String,
    score:   Int,
    tags:    List[String],
    details: Option[String]
) derives io.circe.Decoder, JsonSchemaOf
```

**Built-in instances** are provided for: `String`, `Int`, `Long`, `Double`, `Float`, `Boolean`, `List[A]`, `Option[A]`, `Map[String, String]`.

**Manual instance**

```scala
given JsonSchemaOf[MyType] = JsonSchemaOf.fromSchema(
  SchemaElement.JsObject(
    properties = Map(
      "name"  -> SchemaElement.JsString,
      "count" -> SchemaElement.JsInteger
    ),
    required = List("name", "count")
  )
)
```

---

### LLM Providers

```scala
enum LlmProvider:
  case Gemini, OpenAI, Anthropic, DeepSeek, OpenRouter
```

| Provider | Default model | Env var (suggested) |
|---|---|---|
| `Anthropic` | `claude-sonnet-4-5` | `ANTHROPIC_API_KEY` |
| `OpenAI` | `gpt-4o` | `OPENAI_API_KEY` |
| `Gemini` | `gemini-2.0-flash` | `GEMINI_API_KEY` |
| `DeepSeek` | `deepseek-chat` | `DEEPSEEK_API_KEY` |
| `OpenRouter` | `deepseek/deepseek-v3.2` | `OPENROUTER_API_KEY` |

All providers route through langchain4j. The max output token limit defaults to **4096**.

**Rate limit handling** — on a `RateLimitException`, the agent retries automatically up to **3 times** with exponential back-off starting at 30 seconds.

---

## Prompt Management

### YAML format

Prompts live in a single YAML file, conventionally `src/main/resources/prompts.yaml`:

```yaml
system_prompts:
  SummariseAgent: |
    You are a concise technical summariser.
    Always respond in plain English.

task_prompts:
  summarise_results:
    version: 1
    template: |
      Query: {{ query }}

      Search results:
      {{ results }}

      Produce a 3-sentence summary of the results above.
```

`PromptLoader` resolves:

- `loadSystemPrompt("SummariseAgent")` → returns the system prompt string.
- `loadTaskPrompt("summarise_results")` → returns a `PromptTemplate` that you `.render(vars)`.

### PromptLoader

```scala
package chat4s.ai.prompt

trait PromptLoader[F[_]]:
  def loadSystemPrompt(name: String): F[String]
  def loadTaskPrompt(name: String):   F[PromptTemplate]

object LivePromptLoader:
  /** Load from an InputStream. */
  def make[F[_]: Sync](is: java.io.InputStream): F[PromptLoader[F]]

  /** Load from `prompts.yaml` on the classpath. */
  def makeFromClasspath[F[_]: Sync]: F[PromptLoader[F]]
```

### PromptTemplate

```scala
final case class PromptTemplate(name: String, template: String, version: Int):
  /** Substitute {{ key }} placeholders with the provided values. */
  def render(vars: Map[String, String]): String
```

```scala
val pt = PromptTemplate("greet", "Hello {{ name }}, welcome to {{ place }}!", version = 1)
pt.render(Map("name" -> "Alice", "place" -> "Wonderland"))
// → "Hello Alice, welcome to Wonderland!"
```

---

## Chat Context

`ChatContext[F]` is the communication channel between the workflow and the end user. Pass it to steps that need to display progress or collect input.

```scala
package chat4s.ai.context

trait ChatContext[F[_]]:
  def sessionId: String

  /** Send a message to the user. */
  def sendMessage(message: String, format: MessageFormat = MessageFormat.Text): F[Unit]

  /** Send a brief status update (e.g., "Running step X…"). */
  def sendState(message: String): F[Unit]

  /** Block until the user provides free-text or makes a choice. */
  def requestInput(request: UserInputRequest): F[String]

  /** Present a selectable list; block until the user picks one. */
  def requestSelection(title: String, items: List[SelectionItem], allowRetry: Boolean): F[String]

enum MessageFormat:
  case Text, Html, Markdown

enum UserInputRequest:
  case FreeText(prompt: String)
  case Choice(prompt: String, options: List[String])
```

Implement `ChatContext[F]` to suit your delivery channel (WebSocket, HTTP long-poll, CLI, etc.).

**Console stub for tests / CLI:**

```scala
val consoleChatContext: ChatContext[IO] = new ChatContext[IO]:
  val sessionId = java.util.UUID.randomUUID().toString
  def sendMessage(msg: String, fmt: MessageFormat)  = IO.println(s"[msg] $msg")
  def sendState(msg: String)                         = IO.println(s"[state] $msg")
  def requestInput(req: UserInputRequest)            = IO.print("> ") *> IO(scala.io.StdIn.readLine())
  def requestSelection(title, items, allowRetry)     =
    IO.println(title) *>
    items.zipWithIndex.traverse_ { case (i, n) => IO.println(s"  $n. ${i.label}") } *>
    IO(scala.io.StdIn.readLine())
```

---

## Running a Workflow

### Start from scratch

```scala
import cats.effect.IO
import chat4s.graph.*
import org.typelevel.otel4s.trace.Tracer

given Tracer[IO] = Tracer.noop  // replace with a real tracer in production

val program: IO[Unit] =
  for
    journal   <- WorkflowJournal.inMemory[IO, PipelineState]
    engine     = WorkflowEngine.make[IO]
    wfId       = WorkflowId.random
    state0     = PipelineState(userQuery = "What is Scala 3?", searchResults = None, summary = None, approved = false)
    execution <- engine.start(wfId, wf, state0, journal)
    results   <- execution.stream.compile.toList
    _         <- IO.println(s"Completed ${results.length} steps")
    _         <- IO.println(s"Final state: ${results.last.state}")
  yield ()
```

### Resume after a crash

```scala
// First run — crashes after step "search"
val firstRun: IO[Unit] =
  for
    execution <- engine.start(wfId, wf, state0, journal)
    // Simulate crash by cancelling mid-stream:
    _ <- execution.stream.take(1).compile.drain
  yield ()

// Second run — resumes from "summarise"
val secondRun: IO[Unit] =
  engine.resume(wfId, wf, journal).flatMap:
    case Some(execution) =>
      execution.stream
        .evalTap(r => IO.println(s"Resumed at step: ${r.stepId}"))
        .compile.drain
    case None =>
      IO.println("No checkpoint — starting fresh")
```

### Cancellation

```scala
for
  execution <- engine.start(wfId, wf, state0, journal)
  fiber     <- execution.stream.compile.drain.start
  _         <- IO.sleep(10.seconds)
  _         <- execution.cancel   // signals the stream to stop
  _         <- fiber.join
yield ()
```

The current step finishes cleanly before the stream terminates. The checkpoint written by the last completed step is still valid; you can resume from it.

---

## Conditional Branching

Use `ConditionalWorkflowEdge[S]` when the next step depends on the current state value.

```scala
final case class ConditionalWorkflowEdge[S](
    from:    String,
    router:  S => String,   // inspect state; return a routing key
    targets: Map[String, String]  // routing key → next node id
)
```

**Example — approval gate**

```scala
val conditionalEdges = List(
  ConditionalWorkflowEdge[PipelineState](
    from    = "approval_gate",
    router  = state => if state.approved then "publish" else "revise",
    targets = Map("publish" -> "publish_step", "revise" -> "revise_step")
  )
)

val wf = WorkflowDef[IO, PipelineState](
  nodes            = Map(/* ... */),
  edges            = List(/* static edges */),
  conditionalEdges = conditionalEdges,
  entryPoint       = "fetch"
)
```

When the engine reaches `approval_gate`, it calls `router(state)` to obtain a key, then follows the matching target. If no conditional edge matches a node, the engine falls back to the static edge list.

---

## Workflow Visualization

chat4s can render any workflow as a [Mermaid](https://mermaid.js.org/) flowchart.

```scala
import chat4s.graph.WorkflowMermaid

// From a WorkflowDef directly
val diagram: String = wf.toMermaid

// From a WorkflowBlueprint (F-free, S-free representation)
val blueprint = WorkflowBlueprint.from("my-pipeline", wf)
val diagram2  = WorkflowMermaid.render(blueprint)
```

**Example output**

```
flowchart TD
  sanitise --> search
  search --> summarise
  summarise --> approval_gate
  approval_gate -- publish --> publish_step
  approval_gate -- revise --> revise_step
```

Paste this into any Markdown renderer, the Mermaid Live Editor, or the `ExportMermaid` CLI to generate a PNG.

**Export as PNG** (requires `mmdc` on your `PATH`):

```bash
sbt "runMain chat4s.graph.ExportMermaid"
```

---

## Distributed Tracing

`WorkflowEngine.make[F]` requires a `Tracer[F]` from [otel4s](https://typelevel.org/otel4s/). Every step execution is automatically wrapped in an OpenTelemetry span.

**Span attributes recorded per step**

| Attribute | Value |
|---|---|
| `gen_ai.system` | `"openai"`, `"anthropic"`, `"google"`, … |
| `gen_ai.request.model` | Model name string |
| `gen_ai.usage.input_tokens` | Prompt token count |
| `gen_ai.usage.output_tokens` | Completion token count |
| `langfuse.session.id` | `chatContext.sessionId` |
| `langfuse.observation.input` | Serialised prompt(s) |
| `langfuse.observation.output` | LLM response JSON |
| `langfuse.observation.prompt.name` | Prompt template name |
| `langfuse.observation.prompt.version` | Prompt template version |

**Minimal Tracer setup (no-op, for tests)**

```scala
given Tracer[IO] = Tracer.noop[IO]
```

**Production setup (Langfuse via OTLP)**

Wire an OTLP exporter that points to your Langfuse endpoint — the spans are compatible out of the box.

---

## Complete Example

Below is a self-contained runnable example that searches, summarises, and conditionally requests user approval.

```scala
//> using scala 3.8.2
//> using dep "org.typelevel::cats-effect::3.7.0"
//> using dep "co.fs2::fs2-core::3.13.0"

import cats.effect.{IO, IOApp}
import chat4s.ai.agent.*
import chat4s.ai.context.*
import chat4s.ai.prompt.*
import chat4s.graph.*
import org.typelevel.otel4s.trace.Tracer

object MyApp extends IOApp.Simple:

  // --- State ---
  final case class MyState(query: String, answer: Option[String], approved: Boolean)

  // --- Prompt YAML (inline for the example) ---
  val promptsYaml =
    """
    |system_prompts:
    |  MyAgent: "You are a helpful assistant."
    |task_prompts:
    |  answer_query:
    |    version: 1
    |    template: "Answer this query concisely: {{ query }}"
    """.stripMargin

  // --- Output type ---
  final case class AnswerOutput(answer: String) derives io.circe.Decoder, JsonSchemaOf

  // --- Step: call the LLM ---
  final class AnswerStep[F[_]: cats.effect.Async](
      agent:        Agent[F],
      promptLoader: PromptLoader[F],
      chatCtx:      ChatContext[F]
  ) extends AgentStep[F, MyState](agent, promptLoader, chatCtx):
    val id        = "answer"
    val promptName = "answer_query"
    def templateVars(s: MyState)              = Map("query" -> s.query)
    def updateState[A](s: MyState, r: A)      = r match
      case out: AnswerOutput => s.copy(answer = Some(out.answer))
      case _                 => s
    def onFailure(msg: String)                = RuntimeException(msg)
    def execute(s: MyState): F[MyState]       = runAgent[AnswerOutput](s)

  // --- Step: ask the user for approval ---
  final class ApprovalStep[F[_]: cats.effect.Async](chatCtx: ChatContext[F]) extends Step[F, MyState]:
    val id = "approval"
    def execute(s: MyState): F[MyState] =
      chatCtx.requestSelection(
        title       = s"Accept this answer?\n\n${s.answer.getOrElse("")}",
        items       = List(SelectionItem("yes", "Yes"), SelectionItem("no", "No")),
        allowRetry  = false
      ).map(choice => s.copy(approved = choice == "yes"))

  given Tracer[IO] = Tracer.noop[IO]

  def run: IO[Unit] =
    for
      promptLoader <- LivePromptLoader.make[IO](
        java.io.ByteArrayInputStream(promptsYaml.getBytes)
      )
      agent        <- AgentFactory.makeAgent[IO](LlmProvider.Anthropic, sys.env("ANTHROPIC_KEY"), "claude-haiku-4-5")
      chatCtx       = /* your ChatContext implementation */
      answerStep    = AnswerStep[IO](agent, promptLoader, chatCtx)
      approvalStep  = ApprovalStep[IO](chatCtx)
      wf            = WorkflowDef[IO, MyState](
                        nodes = Map("answer" -> answerStep, "approval" -> approvalStep),
                        edges = List(WorkflowEdge("answer", "approval")),
                        conditionalEdges = Nil,
                        entryPoint = "answer"
                      )
      journal      <- WorkflowJournal.inMemory[IO, MyState]
      engine        = WorkflowEngine.make[IO]
      execution    <- engine.start(WorkflowId.random, wf, MyState("What is Scala 3?", None, false), journal)
      finalState   <- execution.stream.compile.last
      _            <- IO.println(s"Final state: $finalState")
    yield ()
```

---

## Testing

chat4s is designed to be testable without hitting real LLM APIs.

### Testing a workflow step

```scala
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import chat4s.graph.*

class SanitiseQueryStepSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  "SanitiseQueryStep" should:
    "trim and lowercase the query" in:
      val step  = SanitiseQueryStep()
      val state = PipelineState(userQuery = "  Hello World  ", searchResults = None, summary = None, approved = false)
      step.execute(state).asserting(_.userQuery shouldBe "hello world")
```

### Testing a full workflow

```scala
"WorkflowEngine" should:
  "execute all steps in order" in:
    val steps: Map[String, Step[IO, Int]] = Map(
      "a" -> new Step[IO, Int] { val id = "a"; def execute(s: Int) = IO.pure(s + 1) },
      "b" -> new Step[IO, Int] { val id = "b"; def execute(s: Int) = IO.pure(s + 10) }
    )
    val wf      = WorkflowDef[IO, Int](steps, List(WorkflowEdge("a", "b")), Nil, "a")
    val engine  = WorkflowEngine.make[IO]
    val journal = WorkflowJournal.noop[IO, Int]
    for
      execution <- engine.start(WorkflowId("test"), wf, 0, journal)
      results   <- execution.stream.compile.toList
    yield
      results.map(_.stepId) shouldBe List("a", "b")
      results.map(_.state)  shouldBe List(1, 11)
```

### Stubbing the LLM

Implement a `LlmClient[F]` stub that returns canned JSON responses, then construct a `LiveAgent` with it:

```scala
val stubClient = new LlmClient[IO]:
  def chatStructured(sys, user, schema, link) = IO.pure("""{"answer": "42"}""")
  def chatStep(sys, history, tools, link)     = ???
  def extractStructured(sys, hist, schema, l) = IO.pure("""{"answer": "42"}""")

val agent = LiveAgent.make[IO](
  agentConfig  = AgentConfig("stub", "TestSystem", "stub-model", 0.0, None),
  llmClient    = stubClient,
  systemPrompt = "You are a stub."
)
```

---

## Dependencies

```scala
libraryDependencies ++= Seq(
  "org.typelevel"   %% "cats-effect"                   % "3.7.0",
  "co.fs2"          %% "fs2-core"                      % "3.13.0",
  "org.typelevel"   %% "otel4s-sdk"                    % "0.17.0",
  "org.typelevel"   %% "otel4s-sdk-exporter"           % "0.17.0",
  "io.circe"        %% "circe-core"                    % "0.14.15",
  "io.circe"        %% "circe-parser"                  % "0.14.15",
  "dev.langchain4j"  % "langchain4j"                   % "1.12.2",
  "dev.langchain4j"  % "langchain4j-google-ai-gemini"  % "1.12.2",
  "dev.langchain4j"  % "langchain4j-open-ai"           % "1.12.2",
  "dev.langchain4j"  % "langchain4j-anthropic"         % "1.12.2",
  "org.snakeyaml"    % "snakeyaml-engine"              % "3.0.1",
  "com.outr"        %% "scribe"                        % "3.18.0",
  // Test
  "org.scalatest"   %% "scalatest"                     % "3.2.19"  % Test,
  "org.typelevel"   %% "cats-effect-testing-scalatest" % "1.8.0"   % Test,
  "org.typelevel"   %% "cats-effect-testkit"           % "3.7.0"   % Test
)
```

| Requirement | Minimum version |
|---|---|
| Scala | 3.8.2 |
| JVM | 21 |
| Cats Effect | 3.7.0 |
| FS2 | 3.13.0 |
| Circe | 0.14.15 |
| langchain4j | 1.12.2 |
| otel4s | 0.17.0 |
