package me.seroperson.urlopt4s

import cats.effect.IO
import cats.effect.Resource
import weaver.MutableIOSuite

object UrlOptimizerSuite extends MutableIOSuite {

  override type Res = UrlOptimizer[IO]

  override def sharedResource: Resource[IO, UrlOptimizer[IO]] =
    UrlOptimizer[IO]()

  test(
    "UrlOptimizer should correctly remove utm_source, utm_medium"
  ) { urlOptimizer =>
    for {
      result <- urlOptimizer.removeAdQueryParams(
        "https://mylink.com/?utm_source=123&utm_medium=123"
      )
    } yield expect(result == "https://mylink.com/")
  }

  test(
    "UrlOptimizer should correctly handle www.google.com case"
  ) { urlOptimizer =>
    for {
      result <- urlOptimizer.removeAdQueryParams(
        "https://www.google.com/search?q=hello&sca_esv=f7d78de355298e03&sca_upv=1&ei=bU8gZtnOCb7DwPAPhrG62AU&udm=&ved=0ahUKEwiZuc2fqMqFAxW-IRAIHYaYDlsQ4dUDCBA&uact=5&oq=hello&gs_lp=Egxnd3Mtd2l6LXNlcnAiBWhlbGxvMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQLhiwAxjWBBhHMg0QABiABBiwAxhDGIoFMg0QABiABBiwAxhDGIoFMg0QABiABBiwAxhDGIoFMg0QABiABBiwAxhDGIoFMg4QABiwAxjkAhjWBNgBATIOEAAYsAMY5AIY1gTYAQEyDhAAGLADGOQCGNYE2AEBMhMQLhiABBiwAxhDGMgDGIoF2AECMhMQLhiABBiwAxhDGMgDGIoF2AECMhMQLhiABBiwAxhDGMgDGIoF2AECMhMQLhiABBiwAxhDGMgDGIoF2AECSKsFUABYAHABeAGQAQCYAQCgAQCqAQC4AQPIAQCYAgGgAgqYAwCIBgGQBhO6BgYIARABGAm6BgYIAhABGAiSBwExoAcA&sclient=gws-wiz-serp"
      )
    } yield expect(result == "https://www.google.com/search?q=hello")
  }

  test(
    "UrlOptimizer should correctly handle google.com case"
  ) { urlOptimizer =>
    for {
      result <- urlOptimizer.removeAdQueryParams(
        "https://google.com/search?q=hello&sca_esv=f7d78de355298e03&sca_upv=1&ei=bU8gZtnOCb7DwPAPhrG62AU&udm=&ved=0ahUKEwiZuc2fqMqFAxW-IRAIHYaYDlsQ4dUDCBA&uact=5&oq=hello&gs_lp=Egxnd3Mtd2l6LXNlcnAiBWhlbGxvMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQABiwAxjWBBhHMgoQLhiwAxjWBBhHMg0QABiABBiwAxhDGIoFMg0QABiABBiwAxhDGIoFMg0QABiABBiwAxhDGIoFMg0QABiABBiwAxhDGIoFMg4QABiwAxjkAhjWBNgBATIOEAAYsAMY5AIY1gTYAQEyDhAAGLADGOQCGNYE2AEBMhMQLhiABBiwAxhDGMgDGIoF2AECMhMQLhiABBiwAxhDGMgDGIoF2AECMhMQLhiABBiwAxhDGMgDGIoF2AECMhMQLhiABBiwAxhDGMgDGIoF2AECSKsFUABYAHABeAGQAQCYAQCgAQCqAQC4AQPIAQCYAgGgAgqYAwCIBgGQBhO6BgYIARABGAm6BgYIAhABGAiSBwExoAcA&sclient=gws-wiz-serp"
      )
    } yield expect(result == "https://google.com/search?q=hello")
  }

  test(
    "UrlOptimizer should correctly handle yandex.ru case"
  ) { urlOptimizer =>
    for {
      result <- urlOptimizer.removeAdQueryParams(
        "https://yandex.ru/search/?text=hello&search_source=dzen_desktop_safe&lr=39"
      )
    } yield expect(
      result == "https://yandex.ru/search/?text=hello&lr=39"
    )
  }

}
