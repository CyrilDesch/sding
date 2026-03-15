# SaaS Launch Documents – Template & Guide

> **Role in the project:** This is a **worked example of the final deliverable Sding generates for its users** — not a document about Sding itself. Each section here (context, persona, competitive analysis, business model, MVP plan, etc.) corresponds to artefacts produced by specific pipeline nodes: `synthetize_project_task` assembles the project card, `document_writer_task` assembles the full research deck. This template defines what "good output" looks like so pipeline prompts can be calibrated against it.
>
> **Preceded by:** [Product Building Research](./product-building-research.md)
>
> **Cross-reference:** [LangGraph Pipeline Spec](../05-technical-spec/langgraph-pipeline-spec.md) (the pipeline automates production of these documents)

---

This document provides a **complete example** of a product launch roadmap enriched with analyses and **methodologies** (Design Thinking, Lean Startup, Shape Up, etc.), plus **precise recommendations** on how to fill each section. For each section you will also find **who** (in a team of five) should own it and **how** to collaborate.

---

## 1. Project Context and Rationale

### Expected Content

- **Problematics**: clear presentation of the project's "why."
  - What is missing or causing pain for the user?
  - What triggers (market evolutions, regulatory constraints, technical opportunities) justify launching **now**?
- **Target**: first draft of the audience (freelancers, SMBs, etc.) and why this need is urgent for them.

### Filling Instructions

1. **Formulate user pain**: e.g. *"Freelancers scatter across daily tasks and lose 2 hours per day from lack of structure."* Quantify where possible (statistics, studies, interviews).
2. **Justify the urgency of the problem**: *"Remote work has generalised → isolation → difficulty maintaining focus."*
3. **Provide market context**: growth of freelancers/solopreneurs; absence of solutions adapted to a specific market segment.

### Tools / Methods

- **Design Thinking – Empathy Phase**: interview 5–10 real target users to understand their daily life and frustrations.
- **5 Whys**: drill down to the root of the problem.

### Team Role (5 people)

- **Product Manager**: responsible for drafting and formulating the problem.
- **UX Researcher / Marketing**: field insight collection (interviews, surveys, testimonials).

---

## 2. Target Profile and Value Proposition

### Expected Content

- **Detailed persona**: demographics, profession, motivations, frustrations, tools already used.
- **Deep needs**: what they hope to gain (confidence, serenity, time savings, etc.).
- **Usual objections**: purchase barriers (price fear, complexity, fear of AI...).
- **Value proposition**: functional (time savings), emotional (pride, less stress), economic (revenue increase) benefits.

### Filling Instructions

1. **Create a concrete persona**: give them a name, age, career, precise work context.
2. **List pain points** and gains.
3. **Formulate the value proposition** in one sentence: *"[Product] helps [target profile] to [solve such problem] thanks to [unique approach], enabling them to [key benefit]."*

### Tools / Methods

- **Value Proposition Canvas**: map "pain relievers" and "gain creators" to persona's "pains" and "gains."
- **Jobs to Be Done**: *"what 'mission' is the user trying to accomplish?"*
- **Lean Startup**: quickly confront this persona with real prospects via interviews or a pre-launch landing page.

### Team Role (5 people)

- **Product Manager & UX Researcher**: co-authoring. They hold product vision + field feedback.
- **Marketing Manager**: market insights, refines value proposition for the commercial pitch.

---

## 3. Product Promise (Verbal Pitch Script)

### Expected Content

A concise pitch answering in a few seconds:
1. **For whom?**
2. **Promised transformation?** (before/after for the user)
3. **Unique approach?** (technology, process, UX)
4. **Measurable result?** (time or money savings, etc.)
5. **Why now?**

### Filling Instructions

1. **Keep it short**: 2 or 3 sentences maximum.
2. **Bet on user benefit** and the before/after effect.
3. **Make the tone vivid**: avoid jargon, aim for an emotional hook.

### Example

> "A freelancer loses 2 hours per day from poor organisation. AxoFlo transforms each work session into a 'productive pact' through a supportive AI, an intelligent timer, and motivating rewards. Result: +2 billable hours per day on average, and the pride of mastering your time."

### Tools / Methods

- **Elevator Pitch**: convince in <1 minute as if in an elevator with an investor.
- **Value Proposition Canvas**: clarify the "gain" and "pain" you address.

### Team Role (5 people)

- **Product Manager / Founder**: drafts the first version of the pitch.
- **Marketing Manager**: refines the wording and ensures impact.

---

## 4. Competitive Analysis and Differentiation

### Expected Content

- **Benchmark**: list of direct and indirect competitors, strengths/weaknesses, pricing, market share (if known).
- **Competitive gaps**: what they don't cover or do poorly.
- **Unique advantage**: what makes your solution special (AI approach, gamification, clear ROI, etc.).

### Filling Instructions

1. **Identify 3–5 key competitors** (including indirect) and build a comparison table: key features, price, target audience.
2. **Identify common patterns**: what do they all offer? What is missing? Where is the opportunity?
3. **Clarify your differentiation**: *"We are the only ones to..."* or *"We focus on [such type of customer]."*

### Tools / Methods

- **Benchmark (mapping)**: identify features, customer reviews, price positioning.
- **SWOT**: Strengths, Weaknesses, Opportunities, Threats.
- **Porter's 5 Forces** (optional): for a more established market.

### Team Role (5 people)

- **Product Manager**: leads analysis and synthesises competitive advantage.
- **Marketing Manager**: supports with information search (pricing, positioning, user reviews on forums/app stores).

---

## 5. Product-Market Fit and Marketing

### Expected Content

- **Business model**: subscription, freemium, licences...
- **Acquisition strategy**: marketing channels (SEO, Ads, partnerships, etc.).
- **Go-to-market**: launch planning (beta, soft launch, etc.).
- **KPIs**: conversion rate, CAC, LTV, churn, MRR...

### Filling Instructions

1. **Choose a clear revenue model**: explain why (e.g. "monthly subscription" for continuous value).
2. **List 2–3 priority acquisition channels**: no need to spread everywhere at first.
3. **Plan a marketing timeline** over several months:
   - Pre-launch (email collection)
   - Launch (Ads campaigns, Product Hunt listing, etc.)
   - Post-launch (regular communication, partnerships).

### Tools / Methods

- **Business Model Canvas**: clarify customer segments, revenue streams, cost structure.
- **AARRR (Pirate Metrics)**: Acquisition, Activation, Retention, Referral, Revenue.
- **Lean Startup**: quickly test each marketing channel, measure acquisition cost.

### Team Role (5 people)

- **Product Manager / Founder**: co-builds the business model.
- **Marketing Manager**: develops acquisition strategy, defines marketing KPIs, tracks campaigns.

---

## 6. Core Loop – Heart of the Application (80% of Value)

### Expected Content

- **Critical path**: the central user journey (e.g. install → first "productive pact" → feedback → streak...).
- **Key messages**: pride, autonomy, time valorisation.
- **Priority device / channel**: web, mobile, desktop? Where does the user launch your solution at the exact moment they need it?

### Filling Instructions

1. **Describe the key experience step by step**:
   - Open app → create an objective → launch timer → feedback/reward...
   - Visualise through a small diagram or concise text.
2. **Emphasise perceived value** at each step: how does the user understand they just "gained something"?
3. **Prioritise the platform**: if they absolutely need a smartphone daily, don't start with a website.

### Tools / Methods

- **User Story Mapping**: map the main user flow and "key steps."
- **Rapid Wireframes** (Design Thinking – Prototype): even simple mockups to understand the journey.

### Team Role (5 people)

- **UX/UI Designer**: formalises the journey and designs wireframes.
- **Product Manager**: validates the core loop ensures it addresses the problem and fulfils the promise.
- **Developer**: evaluates technical feasibility and complexity.

---

## 7. Functional Specifications

### Expected Content

- **Functional modules**: list of modules (e.g. AI module, timer, feedback, gamification, etc.).
- **Prioritisation**: Must-have (V1) vs Could-have (later).
- **Essential features**: those that translate the product promise into reality.

### Filling Instructions

1. **List major components**: back-end, front-end, databases, AI integration.
2. **Identify dependencies**: e.g. authentication module before the streak module.
3. **Apply MoSCoW prioritisation**:
   - *Must*: indispensable for MVP
   - *Should*: important but not vital at launch
   - *Could*: "nice extras" if time allows
   - *Won't*: out of immediate scope

### Tools / Methods

- **Lean Startup**: develop only the essentials for a testable MVP.
- **Shape Up**: define "pitches" for each major feature, estimate feasibility in max 6 weeks.

### Team Role (5 people)

- **Product Manager**: defines product priorities.
- **Lead Developer**: evaluates technical architecture and complexity.
- **Designer**: provides UX vision for each module.

---

## 8. MVP and Development Plan

### Expected Content

- **Roadmap by week or cycle** (e.g. 6-week Shape Up cycles).
- **Critical checkpoints**: user test, AI feedback validation, streak validation.
- **Available resources**: who does what in the team.
- **Anti-scope**: what we are NOT doing in V1 to avoid dispersion.

### Filling Instructions

1. **Establish a minimal calendar**, for example:
   - Weeks 1–2: UX prototyping / tests
   - Weeks 3–4: MVP development (must-have features)
   - Week 5: internal beta / corrections
   - Week 6: private beta launch
2. **Highlight milestones**: *"By end of week 2, we must have a clickable prototype."*
3. **Document task allocation** between the 5 people.

### Tools / Methods

- **Shape Up**: 6-week work cycles (shaping + building + pause).
- **Scrum / Kanban**: agile sprint management or visual task management.
- **Lean Startup**: confront the MVP to the market quickly (beta test, real feedback).

### Team Role (5 people)

- **Lead Developer**: plans technically (who codes what, when).
- **Product Manager**: sets content for each iteration / cycle and important milestones.
- **Designer**: prepares mockups in advance for each functional batch.
- **Marketing**: aligns with the calendar to organise launch / communication.

---

## 9. Strategic Annexes

### 9.1 Risks and Hypotheses

**Objective:** Anticipate uncertainties and prepare **monitoring measures**.

- **Key hypotheses**: market acceptance, willingness to pay, technical feasibility, etc.
- **Risks**: AI failure, poor adoption, aggressive competition, etc.
- **Monitoring indicators**: e.g. technical crash rate, volume of negative feedback, etc.

**Methods & Tips**
- Use a **risk matrix** (probability vs. impact).
- Define *"trigger points"*: *"If AI is not functional at 80% accuracy by version X, switch to semi-manual mode."*

### 9.2 Iteration and Validation Plan

**Objective:** Establish a **continuous improvement loop** (Build → Measure → Learn).

- **Regular checkpoints**: user tests, backlog updates, retrospectives.
- **Metrics**: engagement, retention, key feature usage.

**Methods & Tips**
- **Lean Startup**: *"deliver fast, measure, learn, pivot or persevere."*
- **OKR**: define quarterly objectives (e.g. *"Improve Day-7 retention to 30%"*).

### 9.3 Contingency Plan and Adaptability

**Objective:** Plan **alternative scenarios** in case of blockages or market evolution.

- **Possible pivot**: change target segment, adapt pricing, remove overly complex features.
- **Modularity**: make the architecture sufficiently flexible to integrate new approaches.

**Methods & Tips**
- Pivots should not be seen as failure: they are adaptations based on learning.
- Keep a budget and time buffer to react quickly.

### Team Role (5 people — Annexes)

- **Product Manager**: centralises hypotheses, ensures risk tracking & iteration plan.
- **Lead Dev / Designer**: specify technical and UX risks.
- **Marketing**: anticipates market or positioning changes.

---

## Conclusion and Role Distribution

To **effectively complete** this document, you need to:

1. **Co-build the vision**:
   - **Product Manager / Founder**: carries the vision, drafts the problem, drives priorities.
   - **Marketing Manager**: market study, acquisition strategy, validates pitch from "customer value" angle.
   - **UX/UI Designer**: formalises user journey, creates mockups, identifies UX improvement areas.
   - **Lead Developer**: defines architecture, estimates workloads, plans technical roadmap.
   - **UX Researcher**: interviews, field insights, user tests.

2. **Draw on recognised methodologies**:
   - **Design Thinking** for user empathy and prototypes (sections 1, 3, 6).
   - **Lean Startup** for MVP structuring, hypothesis testing, iterations, marketing plan (sections 5, 8, 9).
   - **Value Proposition Canvas** and **Business Model Canvas** for value proposition and business model (sections 3, 5).
   - **Shape Up** or **Scrum/Kanban** to organise development in cycles and maintain a sustained cadence (sections 7, 8).

3. **Maintain coherence** throughout the document:
   - The **problem** (section 1) must correspond to the **value proposition** (section 3), which must reflect in **key features** (sections 6–7).
   - The **marketing strategy** (section 5) must align with the **personas** (section 3) and confirm there is a solvent market.
   - The **KPIs** (sections 5/9) must be measurable via **MVP features** (section 8) and serve the **improvement loop** (section 9).
