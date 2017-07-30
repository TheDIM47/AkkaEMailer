package util

object LoanPattern extends LoanPattern

trait LoanPattern {
  type Closable = {def close(): Unit}

  def using[R <: Closable, A](resource: R)(f: R => A): A = {
    f(resource)
  }
}
