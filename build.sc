import mill._
import mill.scalalib._
import mill.scalalib.publish._

object Version {
  val scala212 = "2.12.18"
  val scala213 = "2.13.13"
  val scala336 = "3.3.6"
  val scalaCross = Seq(scala212, scala213, scala336)

  val cats = "2.13.0"
  val catsEffect = "3.6.3"

  val zio = "2.1.20"
  val zioInteropCats = "23.1.0.5"

  val graalvm = "24.2.2"
}

object Library {
  val catsCore = ivy"org.typelevel::cats-core::${Version.cats}"
  val catsEffectStd = ivy"org.typelevel::cats-effect-std::${Version.catsEffect}"

  // example/
  val catsEffect = ivy"org.typelevel::cats-effect::${Version.catsEffect}"

  // example/zio
  val zio = ivy"dev.zio::zio::${Version.zio}"
  val zioInteropCats = ivy"dev.zio::zio-interop-cats::${Version.zioInteropCats}"

  // graalvm
  val graalvmPolyglot = ivy"org.graalvm.polyglot:polyglot:${Version.graalvm}"
  val graalvmJs = ivy"org.graalvm.polyglot:js-community:${Version.graalvm}"

  // tests
  val weaverCats = ivy"com.disneystreaming::weaver-cats::0.8.4"
}

trait BaseModule extends CrossScalaModule {

  override def scalacOptions = Seq(
    "-unchecked",
    "-deprecation",
    "-language:_",
    "-encoding",
    "UTF-8",
    "-feature"
  )

}

object urlopt4s extends Cross[UrlOpt4sModule](Version.scalaCross)

trait UrlOpt4sModule extends BaseModule with PublishModule {

  override def publishVersion: T[String] = "0.3.0"

  override def pomSettings = PomSettings(
    description =
      "Allows you to remove ad/tracking query params from a given URL.",
    organization = "me.seroperson",
    url = "https://github.com/seroperson/urlopt4s",
    licenses = Seq(License.Common.MIT),
    versionControl = VersionControl.github("seroperson", "urlopt4s"),
    developers = Seq(
      Developer(
        "seroperson",
        "Daniil Sivak",
        "https://seroperson.me/"
      )
    )
  )

  override def ivyDeps = T {
    super.ivyDeps() ++
      Agg(
        Library.catsCore,
        Library.catsEffectStd,
        Library.graalvmPolyglot,
        Library.graalvmJs
      )
  }

  object test extends ScalaTests with TestModule.ScalaTest {

    override def ivyDeps = T {
      super.ivyDeps() ++ Agg(Library.weaverCats, Library.catsEffect)
    }

    override def testFramework = "weaver.framework.CatsEffect"

  }

}

object example extends Module {

  object cats extends Cross[CatsModule](Version.scalaCross)

  trait CatsModule extends BaseModule {

    override def ivyDeps = T {
      super.ivyDeps() ++ Agg(Library.catsEffect)
    }

    override def moduleDeps = super.moduleDeps ++ Seq(urlopt4s())
  }

  object zio extends Cross[ZioModule](Version.scalaCross)

  trait ZioModule extends BaseModule {

    override def ivyDeps = T {
      super.ivyDeps() ++
        Agg(
          Library.zio,
          Library.zioInteropCats,
          Library.catsEffect
        )
    }

    override def moduleDeps = super.moduleDeps ++ Seq(urlopt4s())
  }

}
