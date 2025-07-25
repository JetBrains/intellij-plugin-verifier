# Changelog

Releases prior to January 2023 are tracked on the project GitHub [Releases Page](https://github.com/JetBrains/intellij-plugin-verifier/releases).

## [Unreleased]

### Added

### Changed

### Fixed

## 1.387 - 2025-06-18

### Added

- IntelliJ Structure: Query plugin provider by explicit attributes ([#1288](https://github.com/JetBrains/intellij-plugin-verifier/pull/1288))
- IntelliJ Structure: Inline content module dependencies explicitly indicate plugin or module ([#1289](https://github.com/JetBrains/intellij-plugin-verifier/pull/1289))
- IntelliJ Structure: TeamCity Recipes: added a "title" field
- IntelliJ Structure: TeamCity Recipes: forbid duplicate properties in YAML
- IntelliJ Structure: TeamCity Recipes: support recipe dependencies ([#1284](https://github.com/JetBrains/intellij-plugin-verifier/pull/1284), [TW-93481](https://youtrack.jetbrains.com/issue/TW-93481/Recipes-return-a-list-of-recipe-dependencies-in-the-Marketplace-code))
- IntelliJ Structure: Support additional Fleet products which respect IJ versioning ([#1287](https://github.com/JetBrains/intellij-plugin-verifier/pull/1287))

### Changed

- Prevent duplicate log messages by reusing the type-safe model of Product Info layout components between IDE model, resource resolvers and class resolvers ([#1285](https://github.com/JetBrains/intellij-plugin-verifier/commit/0fbd3b03141005ea5c701500868a34ee6b3400b4#:~:text=and%20class%20resolvers%20(-,%231285,-)))
- Shorten the error message printed by _IntelliJ API Compatibility Check_ configurations
- Extract content module resolution to separate classes ([#1286](https://github.com/JetBrains/intellij-plugin-verifier/pull/1286))
- IntelliJ Structure: Extract icon loading and dependency loading into separate class ([#1291](https://github.com/JetBrains/intellij-plugin-verifier/pull/1291))
- IntelliJ Structure: Extract away plugin component loaders from `PluginCreator` ([#1292](https://github.com/JetBrains/intellij-plugin-verifier/commit/3fb99fd672c876503ce3fe8445bde21f84df855d))
- Update ByteBuddy to 1.17.6
- Update Gradle to 8.14.2

### Fixed

- Download plugin-declared dependencies missing from the target IDE ([#1294](https://github.com/JetBrains/intellij-plugin-verifier/pull/1294))
- Prevent race condition when a new filesystem has been created between 'get' and 'new' calls

## 1.386 - 2025-05-19

### Fixed

- Allow to decompress tar.gz IDE distributions ([#1278](https://github.com/JetBrains/intellij-plugin-verifier/pull/1278), [MP-7489](https://youtrack.jetbrains.com/issue/MP-7489))
- Allow only JetBrains plugins to declare `com.intellij.*` modules ([#1280](https://github.com/JetBrains/intellij-plugin-verifier/pull/1280), [MP-7447](https://youtrack.jetbrains.com/issue/MP-7447))

## 1.385 - 2025-05-09

### Added

- Check that plugin description starts with Latin Symbols ([MP-7339](https://youtrack.jetbrains.com/issue/MP-7339))

### Changed

- Extract away support for verification and resolution of IDEs that are compiled from the source project ([#1273](https://github.com/JetBrains/intellij-plugin-verifier/pull/1273))
- Structure: Improve caching in singleton JAR filesystem provider ([#1243](https://github.com/JetBrains/intellij-plugin-verifier/pull/1243))
- Structure: Optimize memory usage in JAR handling ([#1251](https://github.com/JetBrains/intellij-plugin-verifier/pull/1251))
- Structure: Optimize memory consumption of transitive dependency resolvers ([#1264](https://github.com/JetBrains/intellij-plugin-verifier/pull/1264))
- Structure: Optimize JAR handling with `CharSequence`s and improved ZIP handling ([#1257](https://github.com/JetBrains/intellij-plugin-verifier/pull/1257), [#1262](https://github.com/JetBrains/intellij-plugin-verifier/pull/1262))
- Structure: Cache JAR filesystems with Caffeine ([#1260](https://github.com/JetBrains/intellij-plugin-verifier/pull/1260))
- Structure: Lazy JAR resolver uses `ZipFile` to resolve resource bundles ([#1271](https://github.com/JetBrains/intellij-plugin-verifier/pull/1271))
- Structure: Product Info `bootClassPathJarNames` share resolvers with core plugin layout components ([#1274](https://github.com/JetBrains/intellij-plugin-verifier/pull/1274))
- Structure: Simplify bundled plugin dependency finder ([#1261](https://github.com/JetBrains/intellij-plugin-verifier/pull/1261))
- Structure: Introduce reopenable filesystems ([#1275](https://github.com/JetBrains/intellij-plugin-verifier/pull/1275))
- Structure: Deprecate `JarFilesResourceResolver`. The use of `JarsResourceResolver` is now recommended ([#1242](https://github.com/JetBrains/intellij-plugin-verifier/pull/1242))
- Structure: Deprecate `allPackages` property on `Resolver` and refactor `Packages` to use more efficient prefix tree ([#1247](https://github.com/JetBrains/intellij-plugin-verifier/pull/1247))
- Remove duplicate classpath entries when creating classpath for plugins and modules ([#1252](https://github.com/JetBrains/intellij-plugin-verifier/pull/1252))
- Structure: Refactor and consolidate class hierarchy of `Resolver`s and `NamedResolver`s ([#1241](https://github.com/JetBrains/intellij-plugin-verifier/pull/1241))
- Improve API integration with JetBrains Marketplace on Edu plugins ([#1234](https://github.com/JetBrains/intellij-plugin-verifier/pull/1234), [#1240](https://github.com/JetBrains/intellij-plugin-verifier/pull/1240))
- Update Apache Commons Text to 1.13.1
- Update ASM to 9.8
- Update ByteBuddy to 1.17.5
- Update JetBrains Plugin Repository REST Client to 2.0.46
- Update Gradle Wrapper Validation Action
- Update Gradle to 8.14

### Fixed

- Address memory management issues causing `OutOfMemoryError`s ([#1239](https://github.com/JetBrains/intellij-plugin-verifier/pull/1239), [MP-7366](https://youtrack.jetbrains.com/issue/MP-7366))
- Don't report a problem if a plugin with content modules declares no dependencies ([MP-7413](https://youtrack.jetbrains.com/issue/MP-7413))
- Don't report a warning if a plugin has 'dependencies' tag with a plugin dependency ([MP-7414](https://youtrack.jetbrains.com/issue/MP-7414)) 
- Broken ZIP doesn't fail verification when scanning for content modules ([#1249](https://github.com/JetBrains/intellij-plugin-verifier/pull/1249))
- Remove duplicate 'file' subschema from URIs ([#1254](https://github.com/JetBrains/intellij-plugin-verifier/pull/1254))

## 1.384 - 2025-03-19

### Added

- Track classpath for IDE plugins. ([#1230](https://github.com/JetBrains/intellij-plugin-verifier/pull/1230), [MP-7299](https://youtrack.jetbrains.com/issue/MP-7299))

## 1.383 - 2025-03-07

### Added

- Add support for Toolbox `apiVersion` in descriptor ([#1226](https://github.com/JetBrains/intellij-plugin-verifier/pull/1226))

### Changed

- Update ByteBuddy dependency ([#1227](https://github.com/JetBrains/intellij-plugin-verifier/pull/1227))

## 1.382 - 2025-02-21

### Added

- Validate values for `-suppress-internal-api-usages` ([#1191](https://github.com/JetBrains/intellij-plugin-verifier/pull/1191))
- Allow to override an internal method of a class that belongs to the same plugin ([MP-7136](https://youtrack.jetbrains.com/issue/MP-7136), [#1193](https://github.com/JetBrains/intellij-plugin-verifier/pull/1193))
- Allow to configure `IdeManager` when layout components are missing ([#1196](https://github.com/JetBrains/intellij-plugin-verifier/pull/1196), [#1197](https://github.com/JetBrains/intellij-plugin-verifier/pull/1197))
- Introduce critical compatibility problems for JetBrains Marketplace verification result ([MP-7151](https://youtrack.jetbrains.com/issue/MP-7151), [#1207](https://github.com/JetBrains/intellij-plugin-verifier/pull/1207))
- Provide internal IDE dumps to improve testing in real-life scenarios
- Validate plugin identifiers for Hub, YouTrack and .NET plugins ([#1209](https://github.com/JetBrains/intellij-plugin-verifier/pull/1209))
- Support inline `<module>` declarations with CDATA in plugin descriptor ([MP-7092](https://youtrack.jetbrains.com/issue/MP-7092), [#1206](https://github.com/JetBrains/intellij-plugin-verifier/pull/1206))
- Reuse filesystem accessed by different URIs to the same file (thanks [fp7](https://github.com/fp7)), ([#1201](https://github.com/JetBrains/intellij-plugin-verifier/pull/1201))
- Support `loading` attribute for Plugin Model V2 content modules ([MP-6904](https://youtrack.jetbrains.com/issue/MP-6904), [95ab4ca](https://github.com/JetBrains/intellij-plugin-verifier/commit/95ab4cad428dc3dd0b4919e71a18379d7bb45129))
- Discover bundled plugins in 'plugins' directory in CLion ([MP-7275](https://youtrack.jetbrains.com/issue/MP-7275), [#1219](https://github.com/JetBrains/intellij-plugin-verifier/pull/1218))
- Support module aliases in class resolvers when used as dependencies ([#1221](https://github.com/JetBrains/intellij-plugin-verifier/pull/1221))

### Changed

- Severity level of the plugin problem `ReleaseVersionWrongFormat` has been changed to a _Warning_ for JetBrains plugins.
- Mark Plugin Model V2 Content module dependencies as optional. Mark plugins and modules in V2 `<dependencies>` as required. ([#1159](https://github.com/JetBrains/intellij-plugin-verifier/pull/1195))
- Improve cache performance to prevent OOM in large runs ([#1217](https://github.com/JetBrains/intellij-plugin-verifier/pull/1217))
- Improve transitive dependency resolution tests ([#1192](https://github.com/JetBrains/intellij-plugin-verifier/pull/1192))
- Increase plugin artifact size limit to 1.5GB ([#1202](https://github.com/JetBrains/intellij-plugin-verifier/pull/1202), [MP-7177](https://youtrack.jetbrains.com/issue/MP-7177/Increase-plugin-artifact-size-limit-to-1.5GB))
- Update the TeamCity Recipes implementation ([#1211](https://github.com/JetBrains/intellij-plugin-verifier/pull/1211), [TW-91829](https://youtrack.jetbrains.com/issue/TW-91829))
- Extract away plugin descriptor parsing and validation ([#1223](https://github.com/JetBrains/intellij-plugin-verifier/pull/1223))
- Rename the YAML field for the inputs of a referenced recipe in TeamCity Recipes ([#1219](https://github.com/JetBrains/intellij-plugin-verifier/pull/1219), [TW-92158](https://youtrack.jetbrains.com/issue/TW-92158))

### Fixed

- Plugin Verifier cannot find `org.jetbrains.android` plugin ([MP-7024](https://youtrack.jetbrains.com/issue/MP-7024), [#1214](https://github.com/JetBrains/intellij-plugin-verifier/pull/1214))

## 1.381 - 2024-11-27

### Changed

- Improve compatibility with SLF4J 1.x ([#1187](https://github.com/JetBrains/intellij-plugin-verifier/pull/1187))
- Support product-info.json in macOS distributions ([#1190](https://github.com/JetBrains/intellij-plugin-verifier/pull/1190))

## 1.380 - 2024-11-26

### Added

- Detect extracted JSON plugin for Platform 2024.3 and newer ([#1173](https://github.com/JetBrains/intellij-plugin-verifier/pull/1173))
- Transitive dependency tree resolution for plugins and modules ([#1185](https://github.com/JetBrains/intellij-plugin-verifier/pull/1185))
- Validate supported runners in TeamCity Actions
- Improve support of Plugin Model V2 in the Platform

### Changed

- Consolidate resolution logic for missing layout component files ([#1188](https://github.com/JetBrains/intellij-plugin-verifier/pull/1188), [#1164](https://github.com/JetBrains/intellij-plugin-verifier/pull/1164))
- Remove the `spec-version` property from TeamCity Actions ([#1177](https://github.com/JetBrains/intellij-plugin-verifier/pull/1177))
- Align code with the TeamCity Recipes ([#1182](https://github.com/JetBrains/intellij-plugin-verifier/pull/1182))
- Remove Unity from supported runners in TeamCity Actions
- Update dependencies

## 1.379 - 2024-09-25

### Added

- Don't verify that `com.intellij.languageBundle` extension point is internal and must be used by JetBrains only ([#1162](https://github.com/JetBrains/intellij-plugin-verifier/pull/1162))
- Handle malformed annotation descriptors when using obfuscation ([MP-6950](https://youtrack.jetbrains.com/issue/MP-6950), [#1160](https://github.com/JetBrains/intellij-plugin-verifier/pull/1160))
- In TeamCity Actions, support composite action names with namespaces ([#1159](https://github.com/JetBrains/intellij-plugin-verifier/pull/1159))

### Changed

- Remove duplicate vendor check when verifying plugin identifier for JetBrains plugins ([#1161](https://github.com/JetBrains/intellij-plugin-verifier/pull/1161))
- Use the same logic for plugin problem classification and remapping ([#1163](https://github.com/JetBrains/intellij-plugin-verifier/pull/1163))

### Fixed

- Fix an empty dotnet plugin name if the title was an empty string ([#1158](https://github.com/JetBrains/intellij-plugin-verifier/pull/1158))

## 1.378 - 2024-09-19

### Added

- Validate `release-version` for paid plugins ([#1140](https://github.com/JetBrains/intellij-plugin-verifier/commit/d7de688aa0ff4ead42b13fedafda3eb7716c7c36), [MP-6824](https://youtrack.jetbrains.com/issue/MP-6824))
- When handling TeamCity Actions, get the content of YAML file for parsed actions ([#1149](https://github.com/JetBrains/intellij-plugin-verifier/pull/1149), [MP-6835](https://youtrack.jetbrains.com/issue/MP-6835/Add-a-way-to-fetch-TeamCity-Action-yaml-file))
- Support resource bundle `@PropertyKey` in constructors of `enum class`-es ([#1144](https://github.com/JetBrains/intellij-plugin-verifier/pull/1144), [MP-6710](https://youtrack.jetbrains.com/issue/MP-6710))
- Allow invocation of private interface methods when using `INVOKEDYNAMIC` ([#1146](https://github.com/JetBrains/intellij-plugin-verifier/pull/1146), [MP-6845](https://youtrack.jetbrains.com/issue/MP-6845))
- Verify compatibility with K2 mode for Kotlin-dependent plugins ([#1150](https://github.com/JetBrains/intellij-plugin-verifier/pull/1150), [#1156](https://github.com/JetBrains/intellij-plugin-verifier/pull/1156), [MP-6825](https://youtrack.jetbrains.com/issue/MP-6825))

### Changed

- Ignore specific packages when tracking Kotlin `internal` API usages. Don't track internal API usages marked as `@PublishedApi` ([#1135](https://github.com/JetBrains/intellij-plugin-verifier/pull/1135), [MP-6784](https://youtrack.jetbrains.com/issue/MP-6784), [MP-6911](https://youtrack.jetbrains.com/issue/MP-6911))
- Consolidate plugin problem level remapping rules with JetBrains Marketplace. Common rules have been moved to the IntelliJ Plugin Structure library ([#1151](https://github.com/JetBrains/intellij-plugin-verifier/pull/1151), [#1157](https://github.com/JetBrains/intellij-plugin-verifier/pull/1157))
- Update dependencies 

### Fixed

- Handle malformed `kotlinx.Metadata` annotation ([#1152](https://github.com/JetBrains/intellij-plugin-verifier/pull/1152))
- Handle plugins created from malformed paths in `product-info.json` ([#1153](https://github.com/JetBrains/intellij-plugin-verifier/pull/1153), [MP-6920](https://youtrack.jetbrains.com/issue/MP-6920))

## 1.375 - 2024-08-30

### Changed

- Update dependencies

### Fixed

- Resolve 'modules' directory in plugin dependencies ([https://youtrack.jetbrains.com/issue/MP-6799](https://youtrack.jetbrains.com/issue/MP-6799), [#1132](https://github.com/JetBrains/intellij-plugin-verifier/pull/1132))

## 1.373 - 2024-08-05

### Added

- Report usages of the `com.intellij.languageBundle` extension point. This extension point is internal and must be used by JetBrains only. ([#1130](https://github.com/JetBrains/intellij-plugin-verifier/pull/1130), [MP-6788](https://youtrack.jetbrains.com/issue/MP-6788))
- Structure: Add TeamCity actions spec versions calculation

### Changed

- Remap plugin problem severity level when resolving plugin dependencies. This resolves unexpected reports of missing plugin dependencies, even when they are actually available. ([#1124](https://github.com/JetBrains/intellij-plugin-verifier/pull/1124))
- Disable API usage checks of Kotlin `internal` modifier. ([#1131](https://github.com/JetBrains/intellij-plugin-verifier/pull/1131))

### Fixed

- Match problems in the plugin and problems in the creation result. This fixes JetBrains Marketplace reports with non-actionable plugin problems for existing plugins, e. g. an invalid plugin identifier. ([#1127](https://github.com/JetBrains/intellij-plugin-verifier/pull/1127), [MP-6733](https://youtrack.jetbrains.com/issue/MP-6773))

## 1.372 - 2024-07-26

### Added

- Report usages of Kotlin classes with `internal` visibility modifier ([#1101](https://github.com/JetBrains/intellij-plugin-verifier/pull/1101))

### Changed

- Attribute `versionSuffix` in `product-info.json` is now optional ([#1128](https://github.com/JetBrains/intellij-plugin-verifier/pull/1128))

### Fixed

- Various fixes for detecting Platform API to Platform API invocations ([MP-6729](https://youtrack.jetbrains.com/issue/MP-6729), [#1121](https://github.com/JetBrains/intellij-plugin-verifier/pull/1121))
- Use plugin problem level remapping rules for bundled plugins ([MP-6757](https://youtrack.jetbrains.com/issue/MP-6757), [IJPL-158170](https://youtrack.jetbrains.com/issue/IJPL-158170), [#1122](https://github.com/JetBrains/intellij-plugin-verifier/pull/1122))

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
