---
sessionId: session-261015-092925-4oh8
---

# Requirements

### Overview & Goals
Show, with a deterministic mocked IDE, that `Ide.findPluginByIdOrModuleId(...)` returns bundled plugins and also IDE **product modules**. A product module is declared as `productModuleV2` in `product-info.json` and is returned as `IdeModule`. Also fix the misleading Javadoc in `Ide.java` that says these lookups return only plugins.

### Verification already done (test262)
I ran `com.jetbrains.plugin.structure.base.IdeTest.test262` against `idea-IU-243.21565.193`. `ide.bundledPlugins` includes `module: intellij.platform.diagnostic.freezeAnalyzer`, which is the `IdeModule.toString()` output. The module comes from `ModuleFactory` via the `productModuleV2` layout entry.

In the code, `findPluginByIdOrModuleId` calls `findPluginById` first. That method looks the ID up in a map built from `getBundledPlugins()`, keyed by `pluginId`. For an `IdeModule`, `pluginId` is the module name. So the result is `PluginProviderResult(type = PLUGIN, plugin = IdeModule)`, which matches what you saw. `MODULE` is only returned for aliases, like `<module value="com.intellij.modules.platform"/>`.

### Scope
**In scope**
- A new test class with a small, self-contained mocked IDE that has one `productModuleV2` entry: `intellij.platform.diagnostic.freezeAnalyzer`.
- Javadoc updates in `Ide.java` only.

**Out of scope**
- Any change to lookup behavior: `PLUGIN` stays as it is.
- Changes to `MockIdeBuilder` or existing tests.
- Changes to `PluginProvider` / `PluginProviderResult` docs.

### Functional Requirements
- `findPluginByIdOrModuleId("intellij.platform.diagnostic.freezeAnalyzer")` returns a non-null result where:
  - `type == PluginProviderResult.Type.PLUGIN`
  - `plugin is IdeModule`
  - `plugin.pluginId == "intellij.platform.diagnostic.freezeAnalyzer"`
  - the classpath contains `lib/modules/intellij.platform.diagnostic.freezeAnalyzer.jar`
- For contrast, the same IDE returns:
  - the core plugin `com.intellij` as `PLUGIN` with a plugin that is not an `IdeModule`
  - the alias `com.intellij.modules.platform` as `MODULE`, owned by `com.intellij`
- The `Ide.java` Javadoc states that the bundled-plugin lookups also cover IDE modules (`IdeModule`).

# Technical Design

### Current Implementation
- `structure-ide/.../Ide.java`:
  - `findPluginById`, `findPluginByModule` and `findPluginByIdOrModuleId` all use lazily built maps over `getBundledPlugins()`.
  - The `Ide` Javadoc talks only about "bundled plugins".
- `ProductInfoLayoutBasedPluginCollectionProvider` passes `LayoutComponent.ProductModuleV2` / `ModuleV2` to `ModuleFactory`. `ModuleFactory` does three things:
  - looks up the descriptor in `modules/module-descriptors.jar` (`<module name=...><resources><resource-root .../>`)
  - loads `<name>.xml` from the resource-root jar
  - wraps the result with `IdeModule.clone(...)`, using `pluginId = module name`
- Mock IDEs are built with `buildDirectory { file / dir / zip }` (see `MockIdeBuilder`, `ProductInfoBasedIdeManagerTest`). They are created with `ProductInfoBasedIdeManager().createIde(root)`.

### Key Decisions
- **Dedicated inline mock** (your choice): the new test builds its own minimal IDE, so the shared `MockIdeBuilder` and the plugin-count assertions in existing tests stay unchanged.
- **Assert the current `PLUGIN` type**: the test records current behavior, confirmed by running test262 and reading the code.
- **Javadoc only in `Ide.java`** (your choice).

### Proposed Changes
**New test** `intellij-plugin-structure/tests/src/test/kotlin/com/jetbrains/plugin/structure/ide/IdeFindPluginByIdOrModuleIdTest.kt`
- Uses a `TemporaryFolder` rule. `@Before` builds the mock IDE layout:
  - `build.txt` containing `IU-243.21565.193`
  - `product-info.json` with the minimal fields used by `MockIdeBuilder`, and `layout` containing:
    ```json
    { "name": "intellij.platform.diagnostic.freezeAnalyzer", "kind": "productModuleV2",
      "classPath": ["lib/modules/intellij.platform.diagnostic.freezeAnalyzer.jar"] }
    ```
  - `lib/product.jar!/META-INF/plugin.xml`: the core plugin `com.intellij` with `<module value="com.intellij.modules.platform"/>`
  - `lib/modules/intellij.platform.diagnostic.freezeAnalyzer.jar!/intellij.platform.diagnostic.freezeAnalyzer.xml`: `<idea-plugin package="com.intellij.diagnostic.freezeAnalyzer"/>`
  - `modules/module-descriptors.jar!/intellij.platform.diagnostic.freezeAnalyzer.xml`: a `<module name=...>` with `<resource-root path="../lib/modules/intellij.platform.diagnostic.freezeAnalyzer.jar"/>`
- Tests (JUnit 4, `org.junit.Assert`):
  - `findPluginByIdOrModuleId returns product module as IdeModule`
  - `findPluginByIdOrModuleId returns core plugin as PLUGIN`
  - `findPluginByIdOrModuleId resolves plugin alias as MODULE`

**Javadoc** in `structure-ide/src/main/java/com/jetbrains/plugin/structure/ide/Ide.java`:
- `getBundledPlugins()`: besides regular bundled plugins, the list may contain IDE modules (`IdeModule`). For `product-info.json`-based IDEs these are layout components of kind `productModuleV2` and `moduleV2`.
- `findPluginById`: may return an `IdeModule` when the ID equals a module name.
- `findPluginByModule`: matches plugin aliases and the IDs of IDE modules.
- `findPluginByIdOrModuleId`: a match by plugin ID **or IDE module name** is returned as `PLUGIN`, possibly as an `IdeModule`. `MODULE` means the match came from a plugin alias.

### Risks
- The mock might miss something `ModuleFactory` needs, such as a missing descriptor or resource root, and then the module is silently skipped (only logged at debug level). To guard against this, the test also asserts `ide.bundledPlugins` contains an `IdeModule` with that ID, so a broken mock fails clearly.

# Testing

### Validation Approach
Run the new test class from `intellij-plugin-structure`:
`./gradlew :tests:test --tests 'com.jetbrains.plugin.structure.ide.IdeFindPluginByIdOrModuleIdTest'`
Then run `ProductInfoBasedIdeManagerTest` to confirm no regressions.

### Key Scenarios
- Looking up the product module ID returns `PLUGIN` with an `IdeModule` and the correct classpath jar.
- Looking up the core plugin ID returns `PLUGIN` with a plugin that is not an `IdeModule`.
- Looking up the alias `com.intellij.modules.platform` returns `MODULE` with `com.intellij`.

### Edge Cases
- An unknown ID returns `null`. This is already covered in `IdePluginProviderTest` and won't be duplicated.

# Delivery Steps

### ✓ Step 1: Add mocked-IDE test for findPluginByIdOrModuleId returning IdeModule
A passing test class shows that a `productModuleV2` from `product-info.json` is returned as an `IdeModule`.

- Create `intellij-plugin-structure/tests/src/test/kotlin/com/jetbrains/plugin/structure/ide/IdeFindPluginByIdOrModuleIdTest.kt`.
- Build the inline mock IDE with `buildDirectory`:
  - `build.txt`
  - minimal `product-info.json` with the freezeAnalyzer `productModuleV2` layout entry
  - core `lib/product.jar` plugin with the `com.intellij.modules.platform` alias
  - `lib/modules/intellij.platform.diagnostic.freezeAnalyzer.jar` with its XML descriptor
  - `modules/module-descriptors.jar` entry with the resource root
- Create the IDE via `ProductInfoBasedIdeManager().createIde(root)`.
- Assert for `intellij.platform.diagnostic.freezeAnalyzer`:
  - the module is present in `bundledPlugins`
  - `type == PLUGIN`
  - `plugin is IdeModule`
  - `pluginId` matches the module name
  - the classpath contains the module jar
- Add contrast tests:
  - `com.intellij` returns `PLUGIN` with a non-`IdeModule` plugin
  - `com.intellij.modules.platform` returns `MODULE`
- Run the new test class and `ProductInfoBasedIdeManagerTest` via Gradle.

### ✓ Step 2: Clarify Ide.java Javadoc about IDE modules in lookups
The `Ide` Javadoc correctly states that bundled-plugin lookups also return IDE modules (`IdeModule`).

- Update the Javadoc of `getBundledPlugins()` to mention the `productModuleV2` / `moduleV2` layout components loaded as `IdeModule`.
- Update `findPluginById` and `findPluginByModule` to say that IDE modules can be matched by their module name.
- Update `findPluginByIdOrModuleId` to explain the result types:
  - `PLUGIN` is a match by plugin ID or IDE module name, possibly an `IdeModule`.
  - `MODULE` is a match by plugin alias.
- Keep the code unchanged. Re-run the new test to confirm the module still compiles and the test passes.