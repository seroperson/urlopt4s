# urlopt4s

[![Build Status](https://github.com/seroperson/urlopt4s/actions/workflows/build.yml/badge.svg)](https://github.com/seroperson/urlopt4s/actions/workflows/build.yml)
[![Maven Central Version](https://img.shields.io/maven-central/v/me.seroperson/urlopt4s_2.12)](https://mvnrepository.com/artifact/me.seroperson/urlopt4s)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/seroperson/urlopt4s/LICENSE)

This small library allows you to remove advertising and tracking query
parameters from a given URL in Scala. It does not contain any filtering logic
itself, but instead uses part of the JS [AdGuard][1] adblocker engine under the
hood. Sometimes this method can be excessive, so [read on](#preface) to find out
why we need to do it this way.

## Installation

In case if you use `sbt`:

```sbt
libraryDependencies += "me.seroperson" %% "urlopt4s" % "0.1.0"
```

In case of `mill`:

```scala
ivy"me.seroperson::urlopt4s::0.1.0"
```

## How to use it

The main object is `UrlOptimizer[F]`. It provides a Scala API to remove
advertising and tracking query parameters from a given URL and it interacts with
a JS adblocker engine via GraalJS under the hood. As of usage example, you can
check the tests and also `example` directory, but I'll show some example here
too:

```scala
// ...

object ExampleApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = UrlOptimizer[IO]()
    .use { urlOptimizer =>
      for {
        result <- urlOptimizer.removeAdQueryParams("https://www.google.com/?utm_source=test")
        _ <- IO.println(result) // "https://www.google.com/"
      } yield ExitCode.Success
    }

}
```

You can run a real example like so:

```
./millw 'example.cats[2.12.18].run' 'https://google.com/?utm_source=test'
```

The function `removeAdQueryParams` takes some time to execute, so you usually
want to run your processing in the background. It can be used concurrently, as
all the necessary locks have been implemented internally. Also, be sure not to
block on `UrlOptimizer` resource initialization, as it may take some time for it
to start.

You can also pass your own custom rule list to the `UrlOptimizer.apply` method
if the default one (see `urlopt4s/resources/rules.txt`) does not cover your
needs. You can also redefine the GraalJS context, but usually this is not
necessary.

## Preface

I encountered the neccessity to remove advertising and tracking query parameters
while developing my pet-project, a Telegram bot "[the advanced link saver][4]".
I wanted to implement a feature that strips redundant query parameters from URLs
and, the first thing which I coded was a simple filter for a predefined set of
commonly used tracking query parameters, such as `utm_source`, `utm_medium`,
`fbclid`, etc.

Quite quickly, I realized that this method didn't work well enough. There were a
really lot of parameters around, and you couldn't cover all of them. For
example, the Google Search URL typically looks like this:

```text
https://www.google.com/search?q=hello&sca_esv=494940dbc25649b8&source=hp&ei=rmEhZuPhF6eLxc8P6-C_mAI&iflsig=ANes7DEAAAAAZiFvvg9IypzVMAznAHWL3LCM0tiJHpsL&udm=&ved=0ahUKEwjj8PzlrcyFAxWnRfEDHWvwDyMQ4dUDCA0&uact=5&oq=hello&gs_lp=Egdnd3Mtd2l6IgVoZWxsbzIIEC4YgAQYsQMyCxAuGIAEGLEDGNQCMggQABiABBixAzIIEC4YgAQYsQMyCBAuGIAEGLEDMgsQLhiABBixAxiDATILEC4YgAQYsQMY1AIyCBAAGIAEGLEDMggQABiABBixAzIIEAAYgAQYsQNInAhQAFivBnAAeACQAQCYAUigAdwCqgEBNbgBA8gBAPgBAZgCBaAC5wLCAhEQLhiABBixAxjRAxiDARjHAcICDhAuGIAEGMcBGI4FGK8BwgIEEAAYA8ICCxAAGIAEGLEDGIMBwgIYEC4YgAQYARixAxjRAxiDARjHARiKBRgKwgIFEAAYgATCAhEQLhiABBixAxiDARjHARivAZgDAJIHATWgB5VV&sclient=gws-wiz
```

The only part that matters here is:

```text
https://www.google.com/search?q=hello
```

Of course, you can manually collect a list of redundant query parameters by
visiting the most popular websites and carefully searching for truly ad-related
query parameters. However, if we dive deeply:

- It's a really time-consuming process and it's hard to do it properly.
- A parameter that you think is related to ads may be actually so at one website
  but not at another, and you may never know if something has gone wrong.
- Sometimes you want to filter parameters or match domain names using regular
  expressions.
- And probably some other points that aren't so obvious.

As you can see, this simple task becomes the more and more difficult as you go
along.

## Implementation details

The method I came up with is reusing the code that popular web adblockers
already have. If you have a good adblocker installed as a browser extension, you
may notice that it sometimes rewrites your URLs to get rid of advertising or
tracking query parameters. This means that adblockers actually already manage a
list of trashy query parameters and have all the necessary code to filter URLs.
We just need to find this code and reuse it.

I have chosen [AdGuard ecosystem][1] to do this. They have very friendly
documentation, most things are open-source, and it is relatively easy to get
things right with them. Project [tsurlfilter][2] is the core, which is
responsible for the common logic and is used in all their adblockers. Using this
API, we can initialize the adblocker engine, pass in some URLs, match them
against your adblocker rules, and then perform blocking, filtering or
redirecting and so on, depending on what is matched.

As I said, an adblocker usually works according to a predefined set of rules.
Therefore, we also need to create our own list of rules that only contain
entries related to filtering query parameters. The [FiltersRegistry][3] allows
you to do it. [We will discuss this later](#how-to-make-your-own-rules-list).

Then, if our backend was written in JS, we would have no further problems: just
add dependencies, maybe some polyfills, and run the code. But we're running on
the JVM, and that's actually another purpose of this library - to show that it's
possible to run a large webpack bundle consisting of TypeScript libraries and
modern JS APIs on the JVM.

So, that's how I have done it:

- We are writing `urlopt4s-js` JS module, which interacts with `tsurlfilter`
  library, inits an engine and provides functions to be called from a JVM, like
  `removeAdQueryParams(str)`.
- We are building `urlopt4s-js` bundle with webpack, adding some polyfills, some
  tricks to make JS-on-JVM working.
- We are compiling our custom set of rules, which has only query params
  filtering things.
- Finally, we are writing `urlopt4s` Scala module, and packing JS bundle and
  rules inside. It inits everything and then just provides Scala interface to
  call JS code.

JS-on-JVM is implemented using GraalJS and works quite well, but a lot of tricky
things were required to get everything working together.

Still, there is plenty of room for optimization, and I believe many things could
be improved, but as for now I leave it as it is.

## How to build JAR artifact

Firstly, you have to build webpack bundle which will be included in final
`.jar`. Just go to `urlopt4s-js` and do:

```
npm exec webpack
```

Your bundle will be available at `urlopt4s-js/dist/main-bundle.mjs`. It should
be moved then to `urlopt4s/resources/urlopt4s.mjs`.

Now you can compile and build `.jar`:

```
./millw __.publishLocal
```

## How to make your own rules list

`urlopt4s` comes with predefined set of rules: `urlopt4s/resources/rules.txt`.
It was compiled using [FiltersRegistry][3] repository and contains only
`$removeparam` directives. You may use the default one or compile your own. The
repository has pretty nice documentation, but compiling the list which you see
here requires some additional code. I'm leaving the patch which I did to do it:

```diff
diff --git a/scripts/build/build.js b/scripts/build/build.js
index 8f7332b7657..033c0a59c55 100755
--- a/scripts/build/build.js
+++ b/scripts/build/build.js
@@ -1,6 +1,7 @@
 const fs = require('fs');
 const path = require('path');
 const compiler = require('adguard-filters-compiler');
+const compilerOptimization = require('../../node_modules/adguard-filters-compiler/src/main/optimization.js');

 const customPlatformsConfig = require('./custom_platforms');
 const { formatDate } = require('../utils/strings');
@@ -72,6 +73,8 @@ const buildFilters = async () => {
         await fs.promises.cp(platformsPath, copyPlatformsPath, { recursive: true });
     }

+    compilerOptimization.disableOptimization();
+
     await compiler.compile(
         filtersDir,
         logPath,
diff --git a/scripts/build/custom_platforms.js b/scripts/build/custom_platforms.js
index 71dcb17cd00..867603af78b 100644
--- a/scripts/build/custom_platforms.js
+++ b/scripts/build/custom_platforms.js
@@ -533,7 +533,44 @@ const SAFARI_BASED_EXTENSION_PATTERNS = [
     ...JSONPRUNE_MODIFIER_PATTERNS,
 ];

+const ONLY_REMOVEPARAM_MODIFIER_PATTERNS = [
+    '^(?!.*(\\$(?!#|(path|domain)=.*]).*removeparam(,|=|$))).*$',
+];
+
+const SKIP_CONTENT_TYPE_PATTERNS = [
+    '\\$.*document',
+    '\\$.*subdocument',
+    '\\$.*font',
+    '\\$.*image',
+    '\\$.*media',
+    '\\$.*object',
+    '\\$.*other',
+    '\\$.*ping',
+    '\\$.*script',
+    '\\$.*stylesheet',
+    '\\$.*websocket',
+    '\\$.*xmlhttprequest'
+];
+
 module.exports = {
+    'LINK_OPTIMIZER': {
+        'platform': 'link_optimizer',
+        'path': 'link_optimizer',
+        'expires': '10 days',
+        'configuration': {
+            // removing everything except of $removeparam
+            'removeRulePatterns': [
+                ...ONLY_REMOVEPARAM_MODIFIER_PATTERNS,
+                ...SKIP_CONTENT_TYPE_PATTERNS
+            ],
+            'replacements': null,
+            'ignoreRuleHints': false,
+        },
+        'defines': {
+            'adguard': true,
+            'adguard_ext_chromium': true,
+        },
+    },
     'WINDOWS': {
         'platform': 'windows',
         'path': 'windows',
```

After compiling you will have to concat all the output and get rid of
duplicates.

## License

```text
MIT License

Copyright (c) 2024 Daniil Sivak

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

[1]: https://github.com/AdguardTeam
[2]: https://github.com/AdguardTeam/tsurlfilter
[3]: https://github.com/AdguardTeam/FiltersRegistry
[4]: https://seroperson.me/2023/09/08/link-saver-bot-for-telegram/
