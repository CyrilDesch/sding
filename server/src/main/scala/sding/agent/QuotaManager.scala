package sding.agent

trait QuotaManager[F[_]]:
  def acquireSlot: F[Unit]
