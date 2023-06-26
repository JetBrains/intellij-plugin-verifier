# Changelog

Releases prior to January 2023 are tracked on the project GitHub [Releases Page](https://github.com/JetBrains/intellij-plugin-verifier/releases).

## [Unreleased]

### Added 

- Specific words in plugin IDs are discouraged and treated as warnings, mainly JetBrains product names. 
- Specific plugin prefixes are discouraged and treated as warnings, such as `com.example` or `net.example`.
- Ignore internal API usages from JetBrains plugins. Whenever a JetBrains plugin uses an internal API (`@ApiStatus.Internal` or `@IntellijInternalApi`), such usage is reported as ignored and not treated as an error or warning.
- Use additional Java SDK locations for tests.

### Changed

- Workaround for JetBrains Academy plugin ID handling ([#950](https://github.com/JetBrains/intellij-plugin-verifier/pull/950))
- Upgrade dependencies
- Migrate to Java HTTP Client from Retrofit
- Skip Kotlin default methods from internal usage check ([MP-5395](https://github.com/JetBrains/intellij-plugin-verifier/pull/885))

## v1.301 - 2023-05-30

### Changed

- This release is equivalent to 1.300.

## v1.300 - 2023-05-30

### Fixed
- Dependencies in the descriptor that reference the same descriptor file trigger a warning instead of an error ([MP-5523](https://youtrack.jetbrains.com/issue/MP-5523))

## v1.299 - 2023-05-19

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
