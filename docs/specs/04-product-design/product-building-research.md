# How to Build the Perfect Product

> **Role in the project:** This is a **sample of the kind of document Sding produces for its users** — not a document about Sding itself. It serves as a reference model: the methodology guidelines, section structure, and recommendations shown here are what the pipeline's `document_writer_task` and `synthetize_project_task` nodes are designed to generate automatically for a founder's project. Read it as "here is what good output looks like."
>
> **Preceded by:** [Questions Loop](../03-vision/questions-loop.md)
>
> **Leads to:** [SaaS Launch Documents](./saas-launch-documents.md), [LangGraph Pipeline Spec](../05-technical-spec/langgraph-pipeline-spec.md)

---

## Context and Project Rationale

**General assessment:** This section must expose **why** the project exists, describing the problem to be solved and the market context. The **problem** must be formulated from the user or target customer's perspective, with contextual elements (market trends, technological changes, gaps in current offerings) justifying the initiative.

**Suggested improvements:** Ensure this section **clearly defines the problem** your product will solve and **why** it is important to solve it **now**. Add data or observations from preliminary user research or market studies. Reformulate the problem clearly (*"[Target person] experiences [such difficulty] in [such context], which leads to [such consequence]"*). Avoid generalities and aim for deep understanding: explain **why** no current solution correctly addresses the problem, or how market evolution creates a **unique opportunity**.

**Recommended methodologies:** Use **Design Thinking**. The **empathy** phase consists of deeply understanding the needs, motivations, and challenges of users by gathering their perspectives. This may involve interviews, observations, or surveys. The **problem definition** phase then reformulates the problem clearly from gathered information to derive precise objectives. In practice, this means the team must synthesise user feedback into a central problem statement. Tools: empathy map, user journey, the "5 Whys" technique.

**Responsible:** Primarily the **Product Manager** or project founder. They can rely on a **UX Researcher** or **Marketing Manager** to collect user data and market information.

---

## Target Profile and Value Proposition

**General assessment:** This section must describe **for whom** the product is designed and **what value** it will bring. A profile too broad is a sign of lack of focus. A value proposition generic and centred on features rather than user benefits should be improved.

**Suggested improvements:** Create a **detailed user persona** with a name, context (age, profession, behaviours), their objectives, and above all their **pain points** linked to the identified problem. Express the **value proposition** clearly: *"[Product] helps [target profile] to [solve such problem] by offering [unique solution element], enabling them to [key benefit]."* Add what differentiates your offer — what makes it **unique or better** compared to alternatives.

**Recommended methodologies:**
- **Value Proposition Canvas**: map your "pain relievers" and "gain creators" to your persona's "pains" and "gains."
- **Jobs to Be Done (JTBD)**: what "mission" is the customer trying to accomplish and how does your product help?
- **Lean Startup**: quickly confront this persona with real prospects via interviews or a pre-launch landing page.

**Responsible:** The **Product Manager** with the **UX Designer / UX Researcher**. The **Marketing Manager** can also contribute for market segmentation and value proposition refinement.

---

## Market Analysis and Competition

**General assessment:** This section must demonstrate that you have studied the environment in which your product will operate. It should cover **market size or potential** and present a **competitor analysis** (direct and indirect). Neglecting competitive analysis is a classic pitfall.

**Suggested improvements:** **Map competitors** — direct (similar solution for the same problem) and indirect (alternative solutions that address the need differently). Build a **benchmark** table: key features, business model, pricing, approximate market share. Include **global market information**: potential user numbers, sector growth, trends. Draw **lessons** from the analysis: where is the gap? Where is your angle of attack?

**Recommended methodologies:**
- **Benchmark (competitive mapping)**: analyse competitors' practices, products, strengths, and weaknesses.
- **SWOT analysis**: Strengths, Weaknesses, Opportunities, Threats.
- **Porter's 5 Forces** (optional): for assessing sector attractiveness.

**Responsible:** The **Product Manager** orchestrates this, with the **Marketing Manager** taking charge of data collection. Consult the **Developer** or **Designer** if technical or UX aspects of competitors merit analysis.

---

## Proposed Solution and Key Features

**General assessment:** Describe **what solution** you will bring to the problem and **how**. Focus on the **key features** that materialise the value proposition. Avoid listing a multitude of features without priority. Do not describe the solution from an internal perspective (technology, technical characteristics) but from the user angle (how this feature concretely helps them).

**Suggested improvements:** Start with a brief **solution description** in one or two sentences (technical elevator pitch). Then detail **key features in prioritised form**, distinguishing **MVP** features from those coming **later**. List 3 to 5 **must-have** features. For each, add a sentence explaining the corresponding **user benefit**. Describe the main **user journey** in your solution to verify that features chain together coherently.

**Recommended methodologies:**
- **Lean Startup — MVP**: select a minimal set of features that solve the core problem and temporarily set aside secondary features. *"Don't wait for your product to be perfect to launch."*
- **MoSCoW prioritisation**: Must-have (indispensable), Should-have (important but not vital for MVP), Could-have (nice to have), Won't-have (out of scope for now).
- **Rapid prototyping** (Design Thinking): design wireframes of key features and test them with a few pilot users before full development.

**Responsible:** The **Product Manager** arbitrates which features go into the MVP, in close collaboration with the **Technical Team** and the **Designer**. The Lead Dev provides technical feasibility input; the Designer ensures a coherent user experience.

---

## Business Model

**General assessment:** Explains **how the product will generate revenue** and be **financially sustainable**. Identify **revenue streams** (subscription, commissions, advertising, one-shot licence, etc.), give an idea of **pricing strategy**, and sketch **main costs** to verify potential margins.

**Suggested improvements:** Clearly describe **who pays, how much, and for what**. Include **key costs** to assess viability (e.g. if the product costs €5/month but customer acquisition cost is €100, it won't work). Add a **break-even calculation sketch**: *"With X paying customers at Y€/month, monthly revenue would be...; fixed costs are estimated at...; we would need ~Z customers to break even."* Validate price alignment with the market. Add **CAC** (Customer Acquisition Cost) and **LTV** (Lifetime Value) metrics — the fundamental condition is LTV > CAC.

**Recommended methodologies:**
- **Business Model Canvas (BMC)**: formalises 9 key components of the business model on one page (customer segments, value proposition, channels, customer relationships, revenue streams, key resources, key activities, key partners, cost structure).
- **Lean Canvas** (Ash Maurya): adapted for startups; replaces some BMC blocks with Problem, Solution, Key Metrics, Unfair Advantage.
- **Iterative approach**: treat the business model as a hypothesis to validate. Pre-test willingness to pay via interviews or pre-sales.

**Responsible:** The **Founder or Product Manager** together with the **Marketing Manager** (pricing and customer buying behaviour knowledge).

---

## Product Development Plan (Technical Roadmap)

**General assessment:** Presents **how you will build and deliver the product**, with a timeline or at least a sequence of development stages. Avoid a overly rigid 2-year plan — innovative projects will evolve. A good development roadmap indicates **priorities**, **iteration or sprint duration**, and provides time for **testing** and adjustment.

**Suggested improvements:** Detail a **provisional timeline**: *Months 1-2: design and prototyping; Months 3-4: MVP development (features A, B, C); Month 5: user testing and corrections; Month 6: private beta launch; Months 7-8: iterations on beta feedback, add features D...* Indicate **development priorities** and **resource allocation** (who works on what). Mention how you will ensure **quality**: unit tests, beta test period, client feedback integrated into planning.

**Recommended methodologies:**
- **Shape Up (Basecamp)**: work in fixed cycles (typically **6 weeks**) during which the team commits to delivering a defined scope. If a project is not finished within the time allotted, stop and reassess rather than overrunning indefinitely. This avoids **scope creep** and forces drastic prioritisation choices upfront. Phases: **Shaping** (defining the solution's broad strokes) → **Betting Table** (choosing what to build next) → **Building** (6-week execution) → **Cool-down** (~2 weeks for maintenance, planning).
- **Lean Startup**: iterative approach to integrate feedback quickly.
- **Agile/Scrum rituals**: sprint demos to gather stakeholder feedback.

**Responsible:** The **Lead Developer** owns the technical planning. The **Product Manager** defines priorities and objectives for each phase. The **UX/UI Designer** produces mockups in advance for each functional batch.

---

## Launch and Marketing Strategy

**General assessment:** Explains **how you will make the product known and acquire your first users/customers**. A good product without users leads nowhere. Expected content includes: **marketing channels** envisaged (SEO, social media campaigns, content blog, email, partnerships, events, online advertising, etc.), **message positioning**, and a marketing actions calendar around launch.

**Suggested improvements:** Clarify **which audience you are targeting for launch** and **where/how to reach them**. Select **2 or 3 main acquisition channels** to concentrate initial efforts. Distinguish **pre-launch strategy** (teaser, email collection, private beta for pre-registrants) and **launch strategy** (day-of and following weeks). Include **message and content creation elements**: a slogan, type of marketing content (demo video, infographic, webinars). Include **marketing budget** and allocation between channels.

**Recommended methodologies:**
- **"Pirate Metrics" AARRR** (Acquisition, Activation, Retention, Referral, Revenue): focus on Acquisition and Activation at launch.
- **Lean Startup experimentation**: treat each marketing action as an experiment to test. Use a **landing page** to validate attractiveness before launch: *"Create a landing page that explains what you do... collect opinions and client contacts via a form."*
- **19 Traction Channels** (Traction — Weinberg & Mares): list all possible channels then test them methodically.
- **SMART objectives**: e.g. *"Reach 1,000 registered users in the first 3 months."*

**Responsible:** The **Marketing Manager** (or Growth Hacker) pilots this section. The **Designer** creates visual assets (landing page, social media visuals). The **Developer** implements necessary technical tools (analytics integration, marketing website).

---

## Tracking, Key Indicators, and Iterations

**General assessment:** Addresses **how you will measure product success and continuously improve it**. A good section lists the **KPIs** to track: active users, **retention rate**, **conversion rate** (prospect → registered → paying), **CAC**, **churn**, **MRR/ARR**. Also shows how user **feedback** will be collected and a planned **improvement loop**.

**Suggested improvements:** Define **measurable key objectives**: *"Achieve 30% retention at 1 month"*, *"Obtain an NPS of 50+"*, *"Generate €10k MRR in 12 months."* Describe the **feedback loop**: *"Each week, we will analyse these indicators and qualitative feedback. Based on this information, we will decide on improvements for the next version."* Propose a post-launch **iteration rhythm**: e.g. 2-week improvement sprints. Anticipate **pivot decisions**: *"If after 3 months paid adoption rate is well below expectations, we will consider adapting our model or targeting a different customer segment."*

**Recommended methodologies:**
- **Build-Measure-Learn cycle (Lean Startup)**: after building and launching, measure results, then learn from those measurements to evolve the product. Repeat *"until product-market fit is found."*
- **OKR framework**: quarterly objectives (e.g. OKR goal = *"Improve user retention"*, KR = *"Day-30 retention rate at 25% vs 15% currently"*).
- **North Star Metric**: a single metric best reflecting the value delivered to users; keeps the team aligned long term.
- **Qualitative feedback (Design Thinking)**: continue user interviews and in-product observation sessions post-launch.
- **Retrospectives**: define decision thresholds for pivoting or persevering.

**Responsible:** Tracking and iteration loop ownership belongs primarily to the **Product Manager**. The **Marketing Manager** tracks acquisition metrics; the **Developer** instruments the product for data; the **UX/UI Designer** analyses qualitative ergonomics feedback.
