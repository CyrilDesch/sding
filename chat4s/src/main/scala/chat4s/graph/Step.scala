package chat4s.graph

trait Step[F[_], S]:
  def id: String
  def execute(state: S): F[S]
