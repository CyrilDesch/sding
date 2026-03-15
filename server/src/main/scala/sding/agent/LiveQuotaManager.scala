package sding.agent

import cats.effect.Ref
import cats.effect.Temporal
import cats.syntax.all.*
import scala.concurrent.duration.*

object LiveQuotaManager:
  def make[F[_]: Temporal](maxCallsPerMinute: Int = 10): F[QuotaManager[F]] =
    Ref.of[F, List[FiniteDuration]](List.empty).map { timestampsRef =>
      new QuotaManager[F]:
        private val window: FiniteDuration = 60.seconds

        def acquireSlot: F[Unit] =
          Temporal[F].monotonic.flatMap { now =>
            timestampsRef
              .modify { timestamps =>
                val active = timestamps.filter(ts => now - ts < window)
                if active.length < maxCallsPerMinute then (now :: active, None)
                else
                  val oldest  = active.min
                  val waitFor = window - (now - oldest)
                  (active, Some(waitFor))
              }
              .flatMap {
                case None       => Temporal[F].unit
                case Some(wait) => Temporal[F].sleep(wait) *> acquireSlot
              }
          }
    }
