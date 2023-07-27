### IntelliJ Plugin Verifier
Read the description of the tool and its command-line options in the main [README](../README.md).

#### How  it works
Plugin Verifier accepts as its input a plugin `P` and an IDE build `X` to check compatibility with.
1) read plugin configuration using `structure-intellij` library: `IdePluginManager.createManager().createPlugin(Path pluginFile)` and get instance of [`IdePlugin`](../intellij-plugin-structure/structure-intellij/src/main/java/com/jetbrains/plugin/structure/intellij/plugin/IdePlugin.kt)
2) read IDE configuration using `structure-ide` library `IdeManager.createIde(Path idePath)` and get instance of [`Ide`](../intellij-plugin-structure/structure-ide/src/main/java/com/jetbrains/plugin/structure/ide/Ide.java)
3) read class files of the plugin using `structure-intellij-classes` library and build a [`Resolver`](../intellij-plugin-structure/structure-classes/src/main/java/com/jetbrains/plugin/structure/classes/resolvers/Resolver.kt) that can find plugin classes by name 
4) read class files of the IDE using `structure-classes` and `structure-ide-classes` libraries and build a [`Resolver`](../intellij-plugin-structure/structure-classes/src/main/java/com/jetbrains/plugin/structure/classes/resolvers/Resolver.kt)
5) resolve plugin dependencies specified in the `plugin.xml`: resolve bundled IDE plugins and modules or download plugins from [JetBrains Marketplace](https://plugins.jetbrains.com/). Repeat steps 1 and 3 for them. Build a composite [`Resolver`](../intellij-plugin-structure/structure-classes/src/main/java/com/jetbrains/plugin/structure/classes/resolvers/Resolver.kt) that can find classes from plugin or its dependencies
6) build a composite `Resolver` for plugin's classes, classes of the plugin's dependencies and classes of the IDE.
   This resolver *imitates* loading of the class files of a real plugin installed to an IDE. 
7) one-by-one verify class files of the checked plugin 
   - resolve all JVM references to corresponding classes, methods and fields, check access modifiers according to JVM runtime specification
   - detect potential compatibility problems
   - detect usages of deprecated/experimental/internal API and register corresponding warnings
   - aggregate all the found problems and warnings in a [`PluginVerificationResult`](../intellij-plugin-verifier/verifier-intellij/src/main/java/com/jetbrains/pluginverifier/PluginVerificationResult.kt)
8) `PluginVerificationResult` can be processed in several ways:
   - saved to `verification-<timestamp>/` directory in form of several files:
     - `compatibility-warnings.txt`
     - `compatibility-problems.txt`
     - `dependencies.txt`
     - `deprecated-usages.txt`
     - `experimental-api-usages.txt`
     - `internal-api-usages.txt`
     - `override-only-usages.txt`
     - `non-extendable-api-usages.txt`
     - `plugin-structure-warnings.txt`
   - displayed as TeamCity failed tests (`TeamCityResultPrinter`)
   - converted to JSON and sent to [JetBrains Marketplace](https://plugins.jetbrains.com/) by the Plugin Verifier Service

#### How to verify plugin locally

Run `Verify plugin` configuration, which starts `verifier.jar check-plugin #plugin-update-id [latest-IU]`

#### Modules

- `verifier-cli` — command-line interface of the IntelliJ Plugin Verifier tool `PluginVerifierMain`
- `verifier-core` — main logic of the bytecode verification, JVM references resolution and detecting binary
  compatibility issues
- `verifier-intellij` — module specific for IntelliJ Platform Plugins that runs additional checks such as detecting
  usages of `@Deprecated`, `@ApiStatus.Internal`, `@IntellijInternalApi` and `@ApiStatus.Experimental` APIs inside the plugin.
- `verifier-repository` — APIs for downloading plugins from
  [JetBrains Marketplace](https://plugins.jetbrains.com/) (`MarketplaceRepository`) and IDE builds from corresponding IDE
  repositories (`IdeRepository`).
- `verifier-test` — tests checking correctness of the verifier: build a plugin against an "old" IDE build and then
  verify it against the "new" IDE build with a known set of compatibility problems.

#### How to publish a new release

1) Run [Upload Plugin Verifier](https://buildserver.labs.intellij.net/buildConfiguration/ijplatform_Service_PluginVerifier_UploadPluginVerifier?branch=%3Cdefault%3E&buildTypeTab=overview&mode=builds)
build configuration.
2) Go to [GitHub Releases](https://github.com/JetBrains/intellij-plugin-verifier/releases)
3) Draft a new release with version tag `v<version>` for example `v1.260`
4) Go
   to https://packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-verifier/org/jetbrains/intellij/plugins/verifier-cli/
5) Download `verifier-cli-<version>-all.jar` for the latest uploaded version.
6) Attach the `-all-.jar` as a binary to the GitHub Release.
7) Add Release title and comment
8) Publish the Release
