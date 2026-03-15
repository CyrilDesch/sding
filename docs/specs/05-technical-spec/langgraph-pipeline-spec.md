# LangGraph Pipeline – Specification

> **Role in the project:** Full specification of the 16-node LangGraph workflow. Defines the goal, inputs, Pydantic output schemas, agent roles, and backward flows for every pipeline stage.
>
> **Preceded by:** [Product Building Research](../04-product-design/product-building-research.md)
>
> **Leads to:** [Database Schema](./database-schema.md)
>
> **Cross-reference:** [Design Thinking + SCAMPER](../01-research/design-thinking-scamper.md) (nodes 11–12), [LLM Creativity Research](../01-research/llm-creativity-research.md) (agent design), [Prompt Engineering Guide](../02-methodology/prompt-engineering-guide.md) (agent prompt patterns)

---

**Objective:** Guide aspiring founders from idea to profitable SaaS through a methodical framework. Capture user problems, market timing, and idea validation by automating the work of design thinking experts like IDEO.

---

## 1. `human_requirements_task`

**Goal:** Gather initial project requirements and constraints from the user.

**Input:** User-provided information

**Agents involved:** User Interface

**Expected Output (Pydantic):**

```json
{
  "project_requirements": {
    "project_type": "string",
    "project_problem": "string | None",
    "project_domain": "string | None",
    "project_promise_statement": "string | None",
    "project_budget": "string",
    "project_team_capability": "string",
    "project_team_size": "string",
    "project_development_duration": "string"
  }
}

```

---

## 2.A `weird_problem_generation_task`

**Goal:** Generate problem statements (not solutions) that fit the project requirements.

**Input:** Project requirements from the human requirements task

**Agents involved:** Creative Product Strategist

**Expected Output (Pydantic):**

```json
{
  "discover_problems": {
    "problems": [
      {
        "id": 0,
        "weird_phrase": "We’re drowning in the sacred waters of performative work."
      }
      /* 10 items, only id + phrase */
    ]
  }
}

```

---

## 2.B `problem_reformulation_task`

**Goal:** From previous weird problem, find real problem

**Input:** Weird problem ideas

**Agents involved:** Product Strategist

**Expected Output (Pydantic):**

```json
{
  "reformulated_problems": {
    "problems": [
      {
        "base_problem_id": 0,
        "statement": "string",
        "target_audience": "string",
        "evidence_snippet": "string",
        "situation": "string",
        "impact_metric": "string",
        "job_to_be_done": "string"
      }
      // 3 problems total
    ]
  }
}

```

---

## 3. `trend_analysis_task`

**Goal:** Mine signals to size demand and surface complaints for each problem.

**Input:** Problems from problem discovery task

**Agents involved:** Data Scientist

**Expected Output (Pydantic):**

```json
{
  "discover_problem_trends": {
    "trends": [
      {
        "discover_problem_id": 0,
        "buzz_score": 0.0,
        "search_complaint_volume": 0,
        "retention_proxy_score": 0.0,
        "complaints": ["string"]
      }
      // One per problem
    ]
  }
}

```

---

## 4. `problem_selection_task`

**Goal:** Evaluate and select the best problems using a decision matrix.

**Input:** Problems and trends from previous tasks

**Agents involved:** Product Strategist

**Expected Output (Pydantic):**

```json
{
  "problems_selected": {
    "decision_matrix": [
      {
        "problem_id": 0,
        "desirability": 0-10,
        "viability": 0-10,
        "feasibility": 0-10,
        "strategic_fit": 0-10,
        "average_score": 0.0
      }
      // One per problem
    ],
    "weaknesses_feedback": "string | None"
  }
}

```

**Backwards Flow:**

- Return to `problem_discovery_task` if weaknesses_feedback is provided

---

## 5. `human_problem_selection_task`

**Goal:** Allow stakeholder to manually choose one problem to pursue.

**Input:** Problem selection results from previous task

**Agents involved:** User Interface

**Expected Output (Pydantic):**

```json
{
  "human_problem_selection_result": {
    "selected_problem_id": 0
  }
}

```

**Backwards Flow:**

- Return to `problem_discovery_task` if desired_modification is provided

---

## 6. User Interviews

### 6.1 `hyper_concerned_interview_task`

**Goal:** Simulate an interview with a user who is deeply affected by the problem.

**Input:** Selected problem from human problem selection

**Agent involved:** Hyper-concerned User Agent

### 6.2 `skeptical_interview_task`

**Goal:** Simulate an interview with a user who questions the problem or solution.

**Input:** Selected problem from human problem selection

**Agent involved:** Skeptical User Agent

**Combined Expected Output (Pydantic):**

```json
{
  "user_interviews": {
    "interviews": [
      {
        "key_quotes": ["string"],
        "pain_severity": 0-5,
        "current_solutions": ["string"],
        "willingness_to_pay": 0-5
      }
      // Multiple interviews
    ]
  }
}

```

---

## 7. `empathy_map_task`

**Goal:** Build an empathy map directly from interview quotes related to the problem.

**Input:** User interview results

**Agents involved:** UX Researcher

**Expected Output (Pydantic):**

```json
{
  "empathy_map_result": {
    "empathy_map": {
      "persona_description": "string",
      "see": ["string"],
      "hear": ["string"],
      "think": ["string"],
      "feel": ["string"],
      "pains": ["string"],
      "desired_outcomes": ["string"],
      "insights": ["string"]
    }
  }
}

```

---

## 8. `jtbd_definition_task`

**Goal:** Define primary Jobs-to-be-Done opportunity statements and archetypes.

**Input:** Empathy map results

**Agents involved:** UX Researcher

**Expected Output (Pydantic):**

```json
{
  "jtbd_definition_result": {
    "jobs": [
      {
        "id": 0,
        "job_statement": "When ___, I want to ___, so I can ___",
        "importance": 0-5,
        "satisfaction_today": 0-5,
        "archetype_label": "string"
      }
      // 3 jobs total
    ]
  }
}

```

---

## 9. `hmw_task`

**Goal:** Create "How Might We" questions, score them, and select the top X.

**Input:** JTBD definitions and empathy map

**Agents involved:** Growth Hacker

**Expected Output (Pydantic):**

```json
{
  "hmw_top_result": {
    "top_questions": [
      {
        "id": 0,
        "question": "How might we...?",
        "impact": 0-10,
        "effort": 0-10,
        "viral_loop_strength": 0-10,
        "jobs_id": 0
      }
      // Top X questions
    ]
  }
}

```

---

## 10. `crazy8s_task`

**Goal:** Produce sketch concept variants for each top HMW question.

**Input:** Top HMW questions

**Agents involved:** UX Designer

**Expected Output (Pydantic):**

```json
{
  "crazy8s_result": {
    "variants": [
      {
        "hmw_id": 0,
        "sketch_descriptions": ["string"]
      }
      // One per top HMW
    ]
  }
}

```

---

## 11. `scamper_task`

**Goal:** Apply SCAMPER methodology to refine each sketch, assess feasibility and fit.

**Input:** Crazy8s variants

**Agents involved:** UX Designer

**Expected Output (Pydantic):**

```json
{
  "scamper_result": {
    "scamper_variants": [
      {
        "id": 0,
        "hmw_id": 0,
        "substitute": "string",
        "combine": "string",
        "adapt": "string",
        "modify": "string",
        "put_to_another_use": "string",
        "eliminate": "string",
        "reverse": "string",
        "feasibility": 0-10
      }
      // Multiple variants
    ]
  }
}

```

---

## 12. `competitive_analysis_task`

**Goal:** Benchmark each refined variant against competitors.

**Input:** SCAMPER variants

**Agents involved:** Business Data Scientist

**Expected Output (Pydantic):**

```json
{
  "competitive_analysis_result": {
    "vp_competitive_analysis": [
      {
        "scamper_id": 0,
        "competitor_offerings": ["string"],
        "competitive_advantages": ["string"],
        "distribution_advantage": ["string"],
        "opportunity": ["string"]
      }
      // One per variant
    ],
    "competitive_analysis_feedback": "string | None"
  }
}

```

**Backwards Flow:**

- Return to `hmw_task` if competitive_analysis_feedback is provided

---

## 13. `prototype_build_task` / `run_prototype_builds`

**Goal:** Generate storyboards (prototypes) for each idea.

**Input:** All previous results including competitive analysis

**Agents involved:** UX Designer

**Expected Output (Pydantic):**

```json
{
  "prototype_build_result": {
    "storyboards": [
      {
        "scamper_id": 0,
        "steps": [
          {
            "step_number": 0,
            "description": "string"
          }
          // Multiple steps
        ]
      }
      // Multiple storyboards
    ]
  }
}

```

---

## 14. `user_test_task`

**Goal:** Users test prototypes and gives feedback if needed

**Input:** All prototype builds

**Agents involved:** Skeptical user & Hyper concerned user

**Expected Output (Pydantic):**

```json
{
  "user_tests_result": {
    "user_tests": [
      {
			  "variant_global_id": "integer",
			  "initial_reaction": "string",
			  "perceived_value": "string",
			  "key_questions": ["string"],
			  "interest_level": "string",
			  "would_use": "boolean",
			  "would_recommend": "boolean",
			  "would_pay": "string",
			  "verbatim_quote": "string",
			  "avg_completion_pct": "float",
			  "avg_sus": "float",
			  "avg_wtp_rate_pct": "float",
			  "feedback": "string (nullable)"
			},
			...
    ]
  }
}

```

**Backwards Flow:**

- Return to `prototype_builds` if feedback is provided

---

## 15. `synthetize_project_task`

**Goal:** Synthesize all project information into a presentable format for the user.

**Input:** All previous results and selections

**Agents involved:** Professional Document Writer

**Expected Output (Pydantic):**

```json
{
  "project_card": {
    "title": "string",
    "tagline": "string",
    "persona_chips": [
      { "text": "string" }
      // 3 chips total
    ],
    "benefit_bullets": ["string"],
    "metrics": [
      {
        "value": "string",
        "label": "string"
      }
      // 6 metrics total
    ],
    "market_chips": [
      { "text": "string" }
      // 3 chips total
    ],
    "quote": "string"
  }
}
```

### 15.1 **Human gate**

Leadership decides which project use

Store in vector store all the datas

---

## 📄 16. `document_writer_task`

**Goal:** Auto‑assemble a **living research deck**; latest artefacts override older ones.

**Input:** *All artefacts to date*

**Agents implied:** Professional Document Writer

**Expected Output (JSON):**

```json
{
  "content_sections": [
    "string"
  ],
}
```