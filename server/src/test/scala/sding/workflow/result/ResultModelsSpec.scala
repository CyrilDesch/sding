package sding.workflow.result

import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ResultModelsSpec extends AnyWordSpec with Matchers:

  "WeirdProblems" should {
    "round-trip through JSON" in {
      val wp = WeirdProblems(
        List(WeirdProblem(1, "short", "longer statement"))
      )
      decode[WeirdProblems](wp.asJson.noSpaces) shouldBe Right(wp)
    }
  }

  "ReformulatedProblems" should {
    "round-trip through JSON" in {
      val rp = ReformulatedProblems(
        List(
          ReformulatedProblem(
            problemId = 1,
            statement = "test",
            targetAudience = "devs",
            evidenceSnippet = "evidence",
            situation = "context",
            impactMetric = "10x",
            jobToBeDone = "jtbd"
          )
        )
      )
      decode[ReformulatedProblems](rp.asJson.noSpaces) shouldBe Right(rp)
    }
  }

  "EmpathyMapResult" should {
    "round-trip through JSON" in {
      val em = EmpathyMapResult(
        EmpathyMap(
          personaDescription = "persona",
          see = List("thing"),
          hear = List("sound"),
          think = List("thought"),
          feel = List("emotion"),
          pains = List("pain"),
          desiredOutcomes = List("outcome"),
          insights = List("insight")
        )
      )
      decode[EmpathyMapResult](em.asJson.noSpaces) shouldBe Right(em)
    }
  }

  "JTBDDefinitionResult" should {
    "round-trip through JSON" in {
      val jtbd = JTBDDefinitionResult(
        List(
          JobToBeDone(
            id = 1,
            jobStatement = "When X, I want to Y, so I can Z",
            importance = 4,
            satisfactionToday = 2,
            archetypeLabel = "early adopter"
          )
        )
      )
      decode[JTBDDefinitionResult](jtbd.asJson.noSpaces) shouldBe Right(jtbd)
    }
  }

  "ScamperResult" should {
    "round-trip through JSON" in {
      val sc = ScamperResult(
        List(
          ScamperVariant(
            id = 1,
            hmwId = 1,
            substitute = "s",
            combine = "c",
            adapt = "a",
            modify = "m",
            putToAnotherUse = "p",
            eliminate = "e",
            reverse = "r",
            feasibilityScore = 8
          )
        )
      )
      decode[ScamperResult](sc.asJson.noSpaces) shouldBe Right(sc)
    }
  }

  "ProjectCardSchema" should {
    "round-trip through JSON" in {
      val card = ProjectCardSchema(
        scamperId = 1,
        fiveWs = FiveWs("who", "what", "why", "where", "when"),
        title = "Test Project",
        promise = "Best tool",
        personaChips = List(Chip("dev", "developers")),
        benefitBullets = List("fast", "reliable", "cheap"),
        mvpFeatures = List(MVPFeature("auth", "login system", "High")),
        metrics = List(Metric("100", "users")),
        marketChips = List(Chip("SaaS", "cloud")),
        socialProofQuote = "Great product!"
      )
      decode[ProjectCardSchema](card.asJson.noSpaces) shouldBe Right(card)
    }
  }

  "HumanGateResult" should {
    "round-trip through JSON" in {
      val hg = HumanGateResult(selectedProblemId = 2)
      decode[HumanGateResult](hg.asJson.noSpaces) shouldBe Right(hg)
    }
  }

  "HumanComprehensiveSelectionResult" should {
    "handle None selectedVariantId" in {
      val hc = HumanComprehensiveSelectionResult(None)
      decode[HumanComprehensiveSelectionResult](hc.asJson.noSpaces) shouldBe Right(hc)
    }

    "handle Some selectedVariantId" in {
      val hc = HumanComprehensiveSelectionResult(Some(3))
      decode[HumanComprehensiveSelectionResult](hc.asJson.noSpaces) shouldBe Right(hc)
    }
  }

  "MarkdownGenerationResult" should {
    "round-trip through JSON" in {
      val mg = MarkdownGenerationResult("# Report", "My Report")
      decode[MarkdownGenerationResult](mg.asJson.noSpaces) shouldBe Right(mg)
    }
  }
