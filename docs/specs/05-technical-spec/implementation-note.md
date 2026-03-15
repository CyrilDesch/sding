# Implementation Note – Technology Stack

> **Role in the project:** Records the deliberate divergence between the original Python-centric specification and the actual implementation. Read this before using the LangGraph Pipeline Spec as a reference.

---

## Original Spec vs. Actual Implementation

The [`langgraph-pipeline-spec.md`](./langgraph-pipeline-spec.md) was written when the target stack was **Python + LangGraph + Pydantic**. The implementation diverged early and the spec was never rewritten. The core concepts (node sequence, state shape, backward flows) remain valid, but every technology reference is wrong.

| Concept | Spec (Python) | Implementation (Scala) |
|---|---|---|
| Workflow engine | LangGraph | Custom `WorkflowGraph[F]` in `sding.workflow.graph` |
| State object | Python dataclass / Pydantic model | `ProjectContextState` (Scala case class, Circe codecs) |
| Output schemas | Pydantic models with JSON annotations | Scala case classes in `sding.workflow.result.*` with `Decoder` / `JsonSchemaOf` |
| Agent abstraction | LangChain `ChatModel` | `Agent[F[_]]` trait in `sding.agent` |
| Agent call | `.invoke()` | `agent.call[A: Decoder: JsonSchemaOf](prompt)` |
| Tool use | LangChain tools | `AgentTool[F]` passed to `agent.tooledCall(...)` |
| Effect model | None (synchronous / async via asyncio) | Cats Effect `Async[F]`, `fs2.Stream` for SSE |
| Node definition | `@node` decorated Python function | `TaskNode[F]` trait with `execute`, `templateVars`, `updateState` |
| Graph execution | LangGraph `.compile().invoke()` | `WorkflowGraph[F].execute(...)` returning `fs2.Stream` |

---

## What the Spec Is Still Good For

- **Node sequence and intent** — the 16 nodes, their goals, and their order are faithfully implemented.
- **State field names** — most field names from the Pydantic schemas map directly to fields in `ProjectContextState` (e.g. `weirdProblems`, `reformulatedProblems`, `empathyMapResult`, `scamperResult`, etc.).
- **Backward flows** — the conditional edge logic (e.g. return to `problem_discovery_task` on weak scores) is implemented as `ConditionalEdge` in `WorkflowGraph`.
- **Agent roles** — the agent persona descriptions (Creative Product Strategist, UX Researcher, Growth Hacker, etc.) are still used as system prompt definitions in `sding.workflow.task.tasks`.

---

## Key Source Files

| What | Where |
|---|---|
| Graph definition and execution engine | `server/src/main/scala/sding/workflow/graph/ProjectContextGraph.scala` |
| All task node implementations | `server/src/main/scala/sding/workflow/task/tasks.scala` |
| Shared state model | `server/src/main/scala/sding/workflow/state/ProjectContextState.scala` |
| All result types (replaces Pydantic schemas) | `server/src/main/scala/sding/workflow/result/models.scala` |
| Agent trait and LLM client | `server/src/main/scala/sding/agent/Agent.scala`, `LiveAgent.scala`, `LiveLlmClient.scala` |
| Human gate / input tasks | `server/src/main/scala/sding/workflow/task/humanTasks.scala` |
