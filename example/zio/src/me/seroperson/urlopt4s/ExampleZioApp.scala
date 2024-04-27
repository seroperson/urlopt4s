package me.seroperson.urlopt4s

import zio.Console
import zio.ExitCode
import zio.Scope
import zio.Task
import zio.ZIO
import zio.ZIOAppArgs
import zio.ZIOAppDefault

object ExampleZioApp extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    import zio.interop.catz._

    for {
      args <- getArgs
      givenUrl <- ZIO
        .fromOption(args.headOption)
        .mapError { _ =>
          new IllegalStateException("No args supplied")
        }
      urlOptimizer <- UrlOptimizer[Task]().toScopedZIO
      result <- urlOptimizer.removeAdQueryParams(givenUrl)
      _ <- Console.printLine(result)
    } yield ExitCode.success
  }

}
