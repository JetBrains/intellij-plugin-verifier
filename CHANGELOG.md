# Changelog

Releases prior to January 2023 are tracked on the project GitHub [Releases Page](https://github.com/JetBrains/intellij-plugin-verifier/releases).

## Unreleased

### Added

### Changed

### Fixed

## 1.307 - 2023-11-27

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

## 1.306 - 2023-10-19

### Added

- Show total duration of plugin verification in CLI ([#1009](https://github.com/JetBrains/intellij-plugin-verifier/pull/1009))
- Distinguish between new and existing plugins. Existing plugins have a less strict set of verification rules ([#1008](https://github.com/JetBrains/intellij-plugin-verifier/pull/1008))
- Respect `DeprecationLevel.HIDDEN` in Kotlin `@Deprecated` annotation ([MP-6006](https://youtrack.jetbrains.com/issue/MP-6006), [#1016](https://github.com/JetBrains/intellij-plugin-verifier/pull/1016))

### Fixed

- Use a proper value for verification reports directory instead of placeholder

### Changed

- Upgrade dependencies

## 1.305 - 2023-10-11

### Added

### Changed

- Improve wording and description on plugin verification problems ([MP-5524](https://youtrack.jetbrains.com/issue/MP-5524/Update-wording-for-error-descriptions), [#1000](https://github.com/JetBrains/intellij-plugin-verifier/pull/1000))
- Report an unacceptable warning when the plugin is not v2 and doesn't have dependencies
- Support API version in the Toolbox plugin descriptor ([#1004](https://github.com/JetBrains/intellij-plugin-verifier/pull/1004))
- Upgrade dependencies

## 1.304 - 2023-09-13

### Added

- Add Markdown output format for verification reports. ([MP-5820](https://youtrack.jetbrains.com/issue/MP-5820), [#981](https://github.com/JetBrains/intellij-plugin-verifier/pull/981)) 
- Add command-line option to suppress internal API usages by internal plugins. ([#999](https://github.com/JetBrains/intellij-plugin-verifier/pull/999)) 
- Add plugin pattern possibility for -keep-only-problems filter  [#995](https://github.com/JetBrains/intellij-plugin-verifier/pull/995) 

### Changed

- Show verification report directory at the end of verification in CLI ([#991](https://github.com/JetBrains/intellij-plugin-verifier/pull/991))
- Upgrade dependencies

## 1.303 - 2023-08-08

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

## 1.301 - 2023-05-30

### Changed

- This release is equivalent to 1.300.

## 1.300 - 2023-05-30

### Fixed
- Dependencies in the descriptor that reference the same descriptor file trigger a warning instead of an error ([MP-5523](https://youtrack.jetbrains.com/issue/MP-5523))

## 1.299 - 2023-05-19

### Added
- Improve description on empty `<vendor>` element ([MP-5490](https://youtrack.jetbrains.com/issue/MP-5490))
- _Plugin Structure, Edu_: Add additional fields `programming_language_id` and `programming_language_version`. Field   `programming_language` is deprecated and treated as `programming_language_id` for backwards compatibility.
- Add support for Java 9 [`VarHandle`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/invoke/VarHandle.html) 

### Fixed

### Changed
- Increase severity for _does not declare explicit dependency on Java plugin_ to an error ([MP-5434](https://youtrack.jetbrains.com/issue/MP-5434)). See [Java functionality extracted as a plugin](https://blog.jetbrains.com/platform/2019/06/java-functionality-extracted-as-a-plugin) blog post.
- Treat optional dependency with an empty `config-file` as an error ([MP-4919](https://youtrack.jetbrains.com/issue/MP-4919))
- Dependencies in the descriptor cannot reference the same descriptor file anymore ([MP-3391](https://youtrack.jetbrains.com/issue/MP-3391))
- Use Gradle 8.1.1 for builds
- Use Kotlin DSL in build scripts
- Use Shadow Gradle Plugin for fat JARs
- Declare dependencies in the Gradle version catalog

## 1.297 - 2023-04-25

### Added

- _Plugin Structure_: Make vendor name in `plugin.xml` a mandatory field.
- _Plugin Structure_: Combine the errors about short description and about non latin description into one.
- _Plugin Structure_: Support V2 plugin modules.

### Fixed
- _Verifier_: Work around unparseable class signatures in plugins

### Changed

- _Plugin Structure_: Update ASM to 9.5

## 1.294 - 2023-02-14

### Added

- Treat `NonExtendable` and `OverrideOnly` API usages as problems`

## 1.289 - 2022-11-11

### Added

- Support Java 17 Records ([MP-4865](https://youtrack.jetbrains.com/issue/MP-4865/Plugin-Verifier-Problems-handling-java.lang.Record))
- Support all variants of `ClientKind` for services  ([MP-4881](https://youtrack.jetbrains.com/issue/MP-4881/Plugin-Verifier-support-all-variants-of-com.intellij.openapi.client.ClientKind-for-services))

[next]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.299...HEAD
[1.299]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.297...v1.299
[1.297]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.294...v1.297
[1.294]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.289...v1.294
[1.289]: https://github.com/JetBrains/intellij-plugin-verifier/compare/v1.288...v1.289
