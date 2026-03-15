package sding.agent

import cats.effect.IO
import cats.effect.Ref
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.testkit.TestControl
import cats.syntax.all.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import scala.concurrent.duration.*

class QuotaManagerSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  "QuotaManager" should {

    "allow calls within rate limit" in {
      for
        qm <- LiveQuotaManager.make[IO](maxCallsPerMinute = 3)
        _  <- qm.acquireSlot
        _  <- qm.acquireSlot
        _  <- qm.acquireSlot
      yield succeed
    }

    "block when rate limit is exceeded and release after window expires" in {
      TestControl.executeEmbed {
        for
          qm       <- LiveQuotaManager.make[IO](maxCallsPerMinute = 2)
          _        <- qm.acquireSlot
          _        <- qm.acquireSlot
          released <- Ref.of[IO, Boolean](false)
          fiber    <- (qm.acquireSlot *> released.set(true)).start
          _        <- IO.sleep(30.seconds)
          before   <- released.get
          _ = before shouldBe false
          _     <- IO.sleep(31.seconds)
          _     <- fiber.joinWithNever
          after <- released.get
          _ = after shouldBe true
        yield succeed
      }
    }

    "handle concurrent slot acquisition safely" in {
      for
        qm      <- LiveQuotaManager.make[IO](maxCallsPerMinute = 5)
        results <- List.fill(5)(qm.acquireSlot).parSequence
      yield results.length shouldBe 5
    }
  }
