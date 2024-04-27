package me.seroperson.urlopt4s

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.implicits._

object ExampleCatsApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = UrlOptimizer[IO]()
    .use { urlOptimizer =>
      for {
        givenUrl <- args.headOption match {
          case Some(url) => IO.pure(url)
          case None =>
            IO.raiseError(new IllegalArgumentException("No args supplied"))
        }
        result <- urlOptimizer.removeAdQueryParams(givenUrl)
        _ <- IO.println(result)
      } yield ExitCode.Success
    }

}
