package org.llm4s.types.typeclass

import cats.Functor
import org.llm4s.types.Result

/**
 * Higher-Kinded Type class for functors.
 * Enables mapping over wrapped values.
 */
trait LLMFunctor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}

object Functor {
  def apply[F[_]](implicit ev: Functor[F]): Functor[F] = ev

  // Extension methods
  implicit class FunctorOps[F[_]: Functor, A](fa: F[A]) {
    def map[B](f: A => B): F[B] = Functor[F].map(fa)(f)
  }

  // Result instance
  implicit val resultFunctor: Functor[Result] = new Functor[Result] {
    def map[A, B](fa: Result[A])(f: A => B): Result[B] = fa.map(f)
  }
}
