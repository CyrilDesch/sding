package sding.service

import cats.effect.Concurrent
import cats.effect.Ref
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import sding.protocol.SseEvent

final class EventLog[F[_]: Concurrent](
    private val log: Ref[F, Vector[SseEvent]],
    private val sizeSignal: SignallingRef[F, Int]
):

  def publish(event: SseEvent): F[Unit] =
    log.modify(v => (v :+ event, v.size + 1)).flatMap(sizeSignal.set)

  /** Stream events from [fromIndex, ∞). Emits the catchup backlog immediately,
    * then tails new events as they are published. Never terminates on its own.
    *
    * Prepends the current size so that late subscribers (connecting after the
    * workflow is already blocked waiting for input) still receive all buffered
    * events, not just events published after the subscription.
    */
  def subscribeFrom(fromIndex: Int): Stream[F, SseEvent] =
    Stream.eval(Ref.of[F, Int](fromIndex)).flatMap { cursorRef =>
      (Stream.eval(sizeSignal.get) ++ sizeSignal.discrete).flatMap { currentSize =>
        Stream
          .eval(
            cursorRef.modify { last =>
              val end = currentSize max last
              (end, (last, end))
            }
          )
          .flatMap { case (from, to) =>
            if from < to then Stream.eval(log.get).flatMap(v => Stream.emits(v.slice(from, to)))
            else Stream.empty
          }
      }
    }

  def currentSize: F[Int] = sizeSignal.get

object EventLog:
  def make[F[_]: Concurrent]: F[EventLog[F]] =
    for
      log    <- Ref.of[F, Vector[SseEvent]](Vector.empty)
      signal <- SignallingRef.of[F, Int](0)
    yield new EventLog(log, signal)
