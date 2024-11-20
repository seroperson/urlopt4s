package me.seroperson.urlopt4s

import cats.effect.kernel.Async
import cats.effect.kernel.Concurrent
import cats.effect.kernel.Resource
import cats.effect.std.Mutex
import java.nio.file.Paths
import org.graalvm.polyglot.{Source => PolyglotSource}
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.PolyglotAccess
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.IOAccess
import scala.io.Source

trait UrlOptimizer[F[_]] {
  def removeAdQueryParams(url: String): F[String]
}

/**
 * Class used to remove ad-related query params using JS AdGuard rules engine.
 * Use [[UrlOptimizer#apply]] to construct an instance.
 */
private class UrlOptimizerImpl[F[_]: Async: Concurrent] private[urlopt4s] (
  context: Context,
  removeAdQueryParamsJsHandle: Value,
  mutex: Mutex[F]
) extends UrlOptimizer[F] {

  /**
   * Removes ad-related query-params. Usually it is a time-consuming task, so
   * probably you want to run it in background.
   *
   * @param url
   *   given plain URL to remove ad-related query params from.
   *
   * @return
   *   higly likely clean URL.
   */
  def removeAdQueryParams(url: String): F[String] = mutex
    // js context doesn't allow multithread access, so using Mutex here
    .lock
    .surround {
      Async[F].delay {
        context.enter()
        val result = removeAdQueryParamsJsHandle.execute(url).asString()
        context.leave()
        result
      }
    }

}

object UrlOptimizer {

  /**
   * Context builder which defines all the neccessary GraalVM polyglot
   * configuration. If you are going to override context, you should use this
   * builder as your base.
   *
   * @return
   *   configured context builder.
   */
  def defaultContextBuilder(): Context#Builder = Context
    .newBuilder("js")
    .allowExperimentalOptions(true)
    .allowPolyglotAccess(
      PolyglotAccess.newBuilder().allowBindingsAccess("js").build()
    )
    .allowIO(
      IOAccess
        .newBuilder(IOAccess.NONE)
        .fileSystem {
          new RestrictedFileSystem(
            Map(
              (
                Paths.get("urlopt4s.mjs"),
                () =>
                  new ReadOnlySeekableByteArrayChannel(
                    getClass()
                      .getClassLoader()
                      .getResourceAsStream("urlopt4s.mjs")
                      .readAllBytes()
                  )
              )
            )
          )
        }
        .build()
    )
    // Don't warn me when running stock JDK
    .option("engine.WarnInterpreterOnly", "false")
    // Required to be able to get data from executed script
    .option("js.esm-eval-returns-exports", "true")
    // Required to be able to import modules
    .option("js.commonjs-require", "true")
    .option(
      "js.commonjs-require-cwd",
      System.getenv().getOrDefault("HOME", ".")
    )

  /**
   * Constructs [[UrlOptimizer]] instance.
   *
   * @param rulesText
   *   AdGuard rules list. If None, default one will be used.
   * @param contextBuilder
   *   GraalVM polyglot context builder.
   *
   * @return
   *   Configured [[UrlOptimizer]] instance.
   */
  def apply[F[_]: Async: Concurrent](
    rulesText: Option[String] = None,
    contextBuilder: Context#Builder = defaultContextBuilder()
  ): Resource[F, UrlOptimizer[F]] = for {
    context <- Resource.fromAutoCloseable {
      Async[F].pure(contextBuilder.build())
    }
    rules <- Resource.eval {
      rulesText match {
        case Some(rules) => Async[F].pure(rules)
        case None => Async[F].delay {
            Source.fromResource("rules.txt").mkString
          }
      }
    }
    removeAdQueryParamsJsHandle <- Resource.eval {
      Async[F].delay {
        val src = s"""
        |  import * as urlopt from 'urlopt4s.mjs';
        |
        |  let rules = Polyglot.import("rules");
        |  urlopt.loadEngine(rules);
        |
        |  export const removeAdQueryParamsHandle = urlopt.removeAdQueryParams
        """.stripMargin

        val source = PolyglotSource
          .newBuilder("js", src, "bootstrap.js")
          .mimeType("application/javascript+module")
          .build()

        context.getPolyglotBindings().putMember("rules", Value.asValue(rules))
        context.eval(source).getMember("removeAdQueryParamsHandle")
      }
    }
    mutex <- Resource.eval(Mutex[F])
  } yield new UrlOptimizerImpl[F](context, removeAdQueryParamsJsHandle, mutex)

}
