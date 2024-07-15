# Changelog

Releases prior to January 2023 are tracked on the project GitHub [Releases Page](https://github.com/JetBrains/intellij-plugin-verifier/releases).

## [Unreleased]

### Added

### Changed

### Fixed

## 1.371 - 2024-07-12

### Added

- In paid or freemium plugins, the [`<release-date>`](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html#idea-plugin__product-descriptor)  
must be set to a date that is no more than 5 days in the future from today's date. ([#1119](https://github.com/JetBrains/intellij-plugin-verifier/pull/1119))
- Improve documentation on the `-ignored-problems` CLI switch ([#1110](https://github.com/JetBrains/intellij-plugin-verifier/pull/1110))

### Fixed

- Fix false positives in Platform API when invoking another `@Experimental` Platform API, e. g. in PSI calls. ([#1120](https://github.com/JetBrains/intellij-plugin-verifier/pull/1120), [MP-6729](https://youtrack.jetbrains.com/issue/MP-6729))  
- Fix `NullPointerException` on delegate calls in `OverrideOnly` checks. ([#1111](https://github.com/JetBrains/intellij-plugin-verifier/pull/1111), [#1112](https://github.com/JetBrains/intellij-plugin-verifier/pull/1112), [#1113](https://github.com/JetBrains/intellij-plugin-verifier/pull/1113), [MP-6724](https://youtrack.jetbrains.com/issue/MP-6724))
- Upgrade dependencies

## 1.370 - 2024-07-08

### Added

- Add `sinceVersion` and `untilVersion` to YouTrack plugins ([#1099](https://github.com/JetBrains/intellij-plugin-verifier/pull/1099))
- Improve support for dependency resolution in the 2024.2+ Platform layout ([#1106](https://github.com/JetBrains/intellij-plugin-verifier/pull/1106), [MP-6704](https://youtrack.jetbrains.com/issue/MP-6704))
- Parse and validate TeamCity Actions YAML descriptor ([#1091](https://github.com/JetBrains/intellij-plugin-verifier/pull/1091))
- Log call stack when tracing annotation usage ([#1102](https://github.com/JetBrains/intellij-plugin-verifier/pull/1102))

### Changed

- Do not report API usages in elements annotated with `@ApiStatus` family of annotations which are declared within a plugin and invoked from the plugin itself. 
 The following annotations are supported: `@ApiStatus.OverrideOnly`, `@ApiStatus.Experimental`, `@ApiStatus.ScheduledForRemoval`, and `@ApiStatus.Internal`  ([#1103](https://github.com/JetBrains/intellij-plugin-verifier/pull/1103), [#1105](https://github.com/JetBrains/intellij-plugin-verifier/pull/1105))
- Improve YouTrack compatibility and reuse code from other plugin models ([#1104](https://github.com/JetBrains/intellij-plugin-verifier/pull/1104))
- Improve Plugin Module v2 support for modules

### Fixed

- Improve module resolution in the 2024.2+ Platform layout. Handle `com.intellij.modules` family of modules, such as `rider` or `php`. ([#1107](https://github.com/JetBrains/intellij-plugin-verifier/pull/1107), [MP-6707](https://youtrack.jetbrains.com/issue/MP-6707))

## 1.369 - 2024-06-22

### Added

- Add a module for YouTrack plugins
- Support `product-info.json`-based Platform layout for 2024.2 and newer ([#1100](https://github.com/JetBrains/intellij-plugin-verifier/pull/1100))  

### Changed

- Resolve XInclude targets both in `META-INF` and resource roots ([#1097](https://github.com/JetBrains/intellij-plugin-verifier/pull/1097))
- Support conditional inclusion in XInclude directives (`includeIf`, `includeUnless`) ([#1097](https://github.com/JetBrains/intellij-plugin-verifier/pull/1097))
- Upgrade dependencies

### Fixed

- Resolve issues with bundled plugins not being found (e. g. Kotlin, Python). ([#1100](https://github.com/JetBrains/intellij-plugin-verifier/pull/1100), [MP-6594](https://youtrack.jetbrains.com/issue/MP-6594))

## 1.367 - 2024-05-29

### Added

- Introduce a CLI switch to `–mute` specific plugin problems ([#1078](https://github.com/JetBrains/intellij-plugin-verifier/pull/1078))
- Distinguish errors and other plugin problems in Markdown and Console outputs ([#1075](https://github.com/JetBrains/intellij-plugin-verifier/pull/1075))
- Show structure warnings in verification reports for HTML, Markdown and Stdout outputs ([#1080](https://github.com/JetBrains/intellij-plugin-verifier/pull/1080))
- Provide problem solution hint for structure warnings in Stdout output, including the ability to mute a specific problem ([#1088](https://github.com/JetBrains/intellij-plugin-verifier/pull/1088))
- Discover JAR files in `lib/modules` for Platform 2024.2 ([#1093](https://github.com/JetBrains/intellij-plugin-verifier/pull/1093))
- Add YouTrack App plugin structure parser ([#1090](https://github.com/JetBrains/intellij-plugin-verifier/pull/1090))

### Changed

- Establish stricter verification rules for plugin `until-build`. 
Indicate illegal `until-build` values, such as `241` (wildcards should be used). 
Improve messages to indicate that the attribute can be omitted to provide compatibility with all future versions.
Mark specific magic build values - e. g. `999` - as invalid. 
([#1083](https://github.com/JetBrains/intellij-plugin-verifier/pull/1083))
- Consider `ServiceExtensionPointPreloadNotSupported` as warning for JetBrains plugins
- Upgrade dependencies

### Fixed

- Recognize covariant return types when detecting method overrides. 
This fixes false positives connected with indexes and `DataIndexer`s.
([#1082](https://github.com/JetBrains/intellij-plugin-verifier/pull/1082))

## 1.365 - 2024-03-21

### Added

- Introduce a set of plugin problem remappings for JetBrains plugins ([#1074](https://github.com/JetBrains/intellij-plugin-verifier/pull/1074), [MP-6388](https://youtrack.jetbrains.com/issue/MP-6388)) 
- Indicate if an IntelliJ plugin contains a `dotnet` directory ([#1070](https://github.com/JetBrains/intellij-plugin-verifier/pull/1070), [MP-6371](https://youtrack.jetbrains.com/issue/MP-6371))
- Allow `OverrideOnly` method calls for delegation and wrapping in the same class hierarchy ([#1068](https://github.com/JetBrains/intellij-plugin-verifier/pull/1068), [MP-6077](https://youtrack.jetbrains.com/issue/MP-6076/ApiStatus.OverrideOnly-allow-delegation-wrapping), [IDEA-336988](https://youtrack.jetbrains.com/issue/IDEA-336988/Action-System-add-ApiStatus.OverrideOnly-in-relevant-API))

### Fixed

- Fix case when reclassified plugin problem level might not be used properly, leading to a plugin descriptor discovery issue. ([#1074](https://github.com/JetBrains/intellij-plugin-verifier/pull/1074), [MP-6388](https://youtrack.jetbrains.com/issue/MP-6388))
- Unwrap plugin problem which have reclassified problem level. ([#1074](https://github.com/JetBrains/intellij-plugin-verifier/pull/1074), [MP-6388](https://youtrack.jetbrains.com/issue/MP-6388))

## 1.364 - 2024-03-10

### Added

- Publish Plugin Verifier to Maven Central
- Structure: Support _frontend_ client types
- Provide dynamic format for plugin problem level remapping ([#1060](https://github.com/JetBrains/intellij-plugin-verifier/pull/1060))
- When remapping plugin problem severity level, allow to ignore a problem. ([#1047](https://github.com/JetBrains/intellij-plugin-verifier/pull/1047))
- When remapping plugin problem severity level, allow to escalate to an _error_. ([#1061](https://github.com/JetBrains/intellij-plugin-verifier/pull/1061))
- Changelog is maintained with [Gradle Changelog Plugin](https://github.com/JetBrains/gradle-changelog-plugin)
- Structure: Allow `frontend-only` field for Fleet plugins ([#1069](https://github.com/JetBrains/intellij-plugin-verifier/pull/1069))

### Changed

- Remove Guava dependency ([#1040](https://github.com/JetBrains/intellij-plugin-verifier/pull/1040))
- Structure: Do not search for additional logos when primary logo is not found ([#1044](https://github.com/JetBrains/intellij-plugin-verifier/pull/1044))
- Remove `--major-ide-version` command-line switch as it was not used anywhere ([#1058](https://github.com/JetBrains/intellij-plugin-verifier/pull/1058))
- Upgrade dependencies
- Build with Gradle 8.6
- Deprecate _Plugin Verifier Service_ module in the repository

## [1.307] - 2023-11-27

### Added

- Simplify JRT filesystem handling to make Plugin Verifier compatible with Java 17 ([#1013](https://github.com/JetBrains/intellij-plugin-verifier/pull/1013))
- Collect telemetry data when verifying plugins
- Use custom name field for EDU sections and lessons ([#1039](https://github.com/JetBrains/intellij-plugin-verifier/pull/1039))

### Changed

- Speedup on IDE JAR and directory scanning ([#1030](https://github.com/JetBrains/intellij-plugin-verifier/pull/1030))
- Remove `apiVersion` from Toolbox plugin descriptor and revert to just using the compatibility range
- Upgrade dependencies

### Fixed

- Clarify `-runtime-dir` switch semantics  ([#1020](https://github.com/JetBrains/intellij-plugin-verifier/pull/1020))
- Improve the instructional message on TeamCity runs when checking trunk API

## [1.306] - 2023-10-19

### Added

- Show total duration of plugin verification in CLI ([#1009](https://github.com/JetBrains/intellij-plugin-verifier/pull/1009))
- Distinguish between new and existing plugins. Existing plugins have a less strict set of verification rules ([#1008](https://github.com/JetBrains/intellij-plugin-verifier/pull/1008))
- Respect `DeprecationLevel.HIDDEN` in Kotlin `@Deprecated` annotation ([MP-6006](https://youtrack.jetbrains.com/issue/MP-6006), [#1016](https://github.com/JetBrains/intellij-plugin-verifier/pull/1016))

### Fixed

- Use a proper value for verification reports directory instead of placeholder

### Changed

- Upgrade dependencies

## [1.305] - 2023-10-11

### Changed

- Improve wording and description on plugin verification problems ([MP-5524](https://youtrack.jetbrains.com/issue/MP-5524/Update-wording-for-error-descriptions), [#1000](https://github.com/JetBrains/intellij-plugin-verifier/pull/1000))
- Report an unacceptable warning when the plugin is not v2 and doesn't have dependencies
- Support API version in the Toolbox plugin descriptor ([#1004](https://github.com/JetBrains/intellij-plugin-verifier/pull/1004))
- Upgrade dependencies

## [1.304] - 2023-09-13

### Added

- Add Markdown output format for verification reports. ([MP-5820](https://youtrack.jetbrains.com/issue/MP-5820), [#981](https://github.com/JetBrains/intellij-plugin-verifier/pull/981))
- Add command-line option to suppress internal API usages by internal plugins. ([#999](https://github.com/JetBrains/intellij-plugin-verifier/pull/999))
- Add plugin pattern possibility for -keep-only-problems filter  [#995](https://github.com/JetBrains/intellij-plugin-verifier/pull/995)

### Changed

- Show verification report directory at the end of verification in CLI ([#991](https://github.com/JetBrains/intellij-plugin-verifier/pull/991))
- Upgrade dependencies

## [1.303] - 2023-08-08

### Added

- Specific words in plugin IDs are discouraged and treated as warnings, mainly JetBrains product names.
- Specific plugin prefixes are discouraged and treated as warnings, such as `com.example` or `net.example`.
- Ignore internal API usages from JetBrains plugins. Whenever a JetBrains plugin uses an internal API (`@ApiStatus.Internal` or `@IntellijInternalApi`), such usage is reported as ignored and not treated as an error or warning.
- Treat service preloading as an error ([#975](https://github.com/JetBrains/intellij-plugin-verifier/pull/975))
- Treat `statusBarWidgetFactory` missing an ID as an error ([#980](https://github.com/JetBrains/intellij-plugin-verifier/pull/980))
- Use additional Java SDK locations for tests.
- Establish a safety net for plugin errors that fail-fast ([#977](https://github.com/JetBrains/intellij-plugin-verifier/pull/977))

### Changed

- Workaround for JetBrains Academy plugin ID handling ([#950](https://github.com/JetBrains/intellij-plugin-verifier/pull/950))
- Upgrade dependencies
- Migrate to Java HTTP Client from Retrofit
- Skip Kotlin default methods from internal usage check ([MP-5395](https://github.com/JetBrains/intellij-plugin-verifier/pull/885))

## [1.301] - 2023-05-30

### Changed

- This release is equivalent to 1.300.

## [1.300] - 2023-05-30

### Fixed

- Dependencies in the descriptor that reference the same descriptor file trigger a warning instead of an error ([MP-5523](https://youtrack.jetbrains.com/issue/MP-5523))

## [1.299] - 2023-05-19

### Added

- Improve description on empty `<vendor>` element ([MP-5490](https://youtrack.jetbrains.com/issue/MP-5490))
- _Plugin Structure, Edu_: Add additional fields `programming_language_id` and `programming_language_version`. Field   `programming_language` is deprecated and treated as `programming_language_id` for backwards compatibility.
- Add support for Java 9 [`VarHandle`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/invoke/VarHandle.html)

### Changed

- Increase severity for _does not declare explicit dependency on Java plugin_ to an error ([MP-5434](https://youtrack.jetbrains.com/issue/MP-5434)). See [Java functionality extracted as a plugin](https://blog.jetbrains.com/platform/2019/06/java-functionality-extracted-as-a-plugin) blog post.
- Treat optional dependency with an empty `config-file` as an error ([MP-4919](https://youtrack.jetbrains.com/issue/MP-4919))
- Dependencies in the descriptor cannot reference the same descriptor file anymore ([MP-3391](https://youtrack.jetbrains.com/issue/MP-3391))
- Use Gradle 8.1.1 for builds
- Use Kotlin DSL in build scripts
- Use Shadow Gradle Plugin for fat JARs
- Declare dependencies in the Gradle version catalog

## [1.297] - 2023-04-25

### Added

- _Plugin Structure_: Make vendor name in `plugin.xml` a mandatory field.
- _Plugin Structure_: Combine the errors about short description and about non latin description into one.
- _Plugin Structure_: Support V2 plugin modules.

### Fixed

- _Verifier_: Work around unparseable class signatures in plugins

### Changed

- _Plugin Structure_: Update ASM to 9.5

## [1.294] - 2023-02-14

### Added

- Treat `NonExtendable` and `OverrideOnly` API usages as problems`

## [1.289] - 2022-11-11

### Added

- Support Java 17 Records ([MP-4865](https://youtrack.jetbrains.com/issue/MP-4865/Plugin-Verifier-Problems-handling-java.lang.Record))
- Support all variants of `ClientKind` for services  ([MP-4881](https://youtrack.jetbrains.com/issue/MP-4881/Plugin-Verifier-support-all-variants-of-com.intellij.openapi.client.ClientKind-for-services))

[Unreleased]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.307...HEAD
[1.289]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.288...v1.289
[1.294]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.289...v1.294
[1.297]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.294...v1.297
[1.299]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.297...v1.299
[1.300]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.299...v1.300
[1.301]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.300...v1.301
[1.303]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.301...v1.303
[1.304]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.303...v1.304
[1.305]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.304...v1.305
[1.306]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.305...v1.306
[1.307]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.306...v1.307
