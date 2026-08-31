# Platform Parser — Implementation Plan

Follows `PARSER_POC_ROOT_CAUSE.md`. That document opened the design questions; this one records what was
decided and what has to be built. Where a claim here differs from the root-cause document or from the
branch's own class docs, the difference is called out explicitly — several of those documents are wrong,
and the corrections are load-bearing.

**Branch state.** `worktree-platform-plugin-parser-poc` rebased onto `origin/master` @ `0e22d44e5`.
The branch's own `7bcc85eec` ("Escape XML markup in randomly generated invalid plugin names in tests")
was dropped automatically — it merged upstream as `0e22d44e5` with an identical `patch-id`
(`ebfdb17ea86b81aac83eb9780fa74298a8ef2058`). Four POC commits remain, no conflicts,
`:structure-intellij:compileKotlin` passes.

**Library version.** All bytecode findings below are from `plugin-system-parser-impl-262.8665.270`,
the version this branch depends on. A dependabot branch bumping to `262.9437.288` exists on the remote;
the conditional-include allowlist is JetBrains-internal and may differ there.

---

## Decisions

| # | Question | Decision |
|---|---|---|
| 1 | Which baseline is the cutoff? | **263** — where `includeIf`/`includeUnless` are actually removed (IJPL-215563, 26.3), not 262 (the new-parser baseline) |
| 2 | Selection rule | Interval `[since, until]` must overlap `[263, ∞)` — i.e. driven **only** by `until-build` |
| 3 | Descriptor with no `until-build` | Eager, but gated on `since-build ≥ 252` |
| 4 | Content modules / V2 modules / optional descriptors | **Inherit the parent's** parser choice |
| 5 | XInclude resolution | The **library's own engine**, driven by a loader we supply |
| 6 | Loader implementation | Adapter over the **existing composite `ResourceResolver` chain** |
| 7 | Inline content modules | Thread the **parent's resource root** down |
| 8 | Non-allowlisted `includeIf`/`includeUnless` | A **clear named error** that fails the plugin |
| 9 | Rollout for evaluation | Selection rule implemented and tested, but **forced on for every descriptor** for now |

---

## Step 1 — Selection rule

`PluginCreator.kt:41,326`. Replace `PLATFORM_PARSER_MIN_BASELINE = 262`, which no longer describes
anything the code does, with two constants that state their own meaning:

```kotlin
/**
 * Baseline at which `includeIf`/`includeUnless` are removed from the platform (IJPL-215563, 26.3).
 * A plugin declaring compatibility at or past this point cannot rely on them, which is what makes it
 * safe to hand to a parser that rejects them.
 */
private const val CONDITIONAL_INCLUDE_REMOVAL_BASELINE = 263

/**
 * Trust floor for a descriptor that declares no upper bound. An unbounded `until-build` claims
 * compatibility with every future IDE, including post-removal ones; that claim is only meaningful if
 * the plugin was built recently enough for the author to have considered it.
 */
private const val UNBOUNDED_UNTIL_SINCE_FLOOR = 252
```

The rule:

```kotlin
private fun shouldUsePlatformParser(document: Document): Boolean {
  val ideaVersion = document.rootElement.getChild("idea-version")
  val untilBuild = ideaVersion?.getAttributeValue("until-build")
    ?.let { IdeVersion.createIdeVersionIfValid(it) }
  if (untilBuild != null) {
    return untilBuild.baselineVersion >= CONDITIONAL_INCLUDE_REMOVAL_BASELINE
  }
  val sinceBuild = ideaVersion?.getAttributeValue("since-build")
    ?.let { IdeVersion.createIdeVersionIfValid(it) } ?: return false
  return sinceBuild.baselineVersion >= UNBOUNDED_UNTIL_SINCE_FLOOR
}
```

Notes:

- **`since-build` drops out of the primary decision.** Overlap with a half-open upper interval only
  ever constrains the upper bound. It survives solely as the trust floor for the unbounded case.
- The existing KDoc's "chicken-and-egg / read raw JDOM before either full parse" rationale still holds
  and should be kept. Its framing — *"the plugin's OWN declared minimum supported platform version
  decides"*, and the quoted team discussion *"old path for old plugins, library for 26.2 plugins"* —
  is now wrong and must be replaced.
- **Baseline comparison is sufficient.** Verified against `IdeVersionImpl.fromString`: `262.*` →
  baseline 262 with `SNAPSHOT_VALUE` components; `IU-262.*` → product code stripped
  (`lastIndexOf('-')`), baseline 262; bare `262` → `buildNumber <= 2000` so treated as a baseline,
  giving components `[262, 0]`. No full-version comparison needed.
- An unparseable `until-build` falls through to the `since-build` clause. This is deliberate: a
  malformed upper bound is not a declaration of compatibility. The JAXB path separately registers a
  descriptor problem for it; selection runs before validation and must not depend on it.

### Consequence, measured — this rule is *less* active than the branch's current one

Deduplicated by plugin ID (newest cached version of each) over the local verifier cache,
119 distinct plugins:

| rule | on the new parser |
|---|---|
| branch today (`since ≥ 262`) | 53 (44%) |
| **this plan (cutoff 263, floor 252)** | **9 (7%)** |
| cutoff 263, floor 241 | 23 (19%) |
| cutoff 263, no floor | 41 (34%) |
| cutoff 262, floor 252 | 64 (53%) |

80 of 119 declare an explicit `until-build` (mostly `261.*`/`262.*`, since the IntelliJ Platform Gradle
Plugin pins it by default); 39 omit it, and only 7 of those have `since ≥ 252`.

The rule is "eager" in the intended sense — it does not require `since ≥ cutoff`, so a plugin declaring
`since=260` with no `until` qualifies — but moving the cutoff to 263 excludes everything pinned to
`262.*`. Note the inversion this produces: a plugin declaring `since=262.2500 until=262.2500` is by any
normal reading *newer* than one declaring `since=260` with no `until`, yet gets the **old** parser while
the older-looking one gets the new. That is also why **all 133 bundled plugin descriptors** in
`idea-IU-262.2500` — every one of which declares exactly
`<idea-version since-build="262.2500" until-build="262.2500"/>` — land on the old parser, which removes
the entire `plugins/Kotlin` failure (C1) from the picture.

The 9 selected plugins are `IdeaVIM`, `club.doki7.ffm-plus`, `code-review-plus`,
`com.ai.dev.tools.languagePack.ru`, `com.alextdev.MermaidVisualizer`, `com.apollographql.ijplugin`,
`tech.droidr.kim`, `com.abouten.svnpackager` (`until=300.*`) and `com.alexandria.scratchpad-plugin`
(`until=263.*`). **None of them uses `xi:include`**, and none of the large descriptors (Kotlin, Scala,
Android, Rust, Go) is among them — which is why Step 6 exists.

## Step 2 — Sub-descriptors inherit the parent's choice

Today `shouldUsePlatformParser` is evaluated per *descriptor*, from that descriptor's own document.
Content modules, V2 module descriptors and `<depends config-file="...">` descriptors carry no
`idea-version`, so they silently take the JAXB path even when the main descriptor took the platform
parser (`ModuleFromDescriptorLoader.kt:28` → `PluginCreator.createPlugin`, which re-runs the check).
Every plugin with content modules is therefore parsed by **both** parsers today, unremarked.

Fix: record the choice on `PluginCreator` and consult `parentPlugin` first, so the rule is evaluated
once per plugin at its main descriptor and inherited by everything below it.

```kotlin
internal val usedPlatformParser: Boolean = ...   // set in resolveDocumentAndValidateBean

private fun shouldUsePlatformParser(document: Document): Boolean {
  parentPlugin?.let { return it.usedPlatformParser }
  ...
}
```

This is a behaviour change in its own right, independent of the cutoff: it puts content modules of
selected plugins onto the platform parser for the first time, which means the four
`PluginModuleResolver` conventions flagged in `PARSER_POC_FINDINGS.md` §5 (the `../name.xml` synthesis,
the `isNullOrBlank()` file-vs-inline discrimination) finally get exercised. They are still unverified,
not disproven — expect findings here.

## Step 3 — XInclude through the library, resolved by the existing chain

The library's engine drives include traversal; we supply the loader. **`ResourceRootXIncludeLoader` is
deleted**, along with the `pluginRoot()`-derived filesystem lookup, and replaced by an adapter over the
resolver chain `PluginCreator` is already handed.

### Why the adapter is trivial, contrary to this branch's class doc

`ResourceRootXIncludeLoader`'s doc asserts it *"deliberately"* does not reuse
`ResourceResolver.resolveResource` because the contracts don't fit, and warns that a composite resolver
"re-appends its own `META-INF/` segment", so feeding a pre-joined path in "would silently double up path
segments". Both halves are wrong:

1. `JarsResourceResolver.resolveResource(relativePath, basePath)` is driven **entirely** by
   `basePath.resolveSibling(relativePath)` — first in the current artifact via `DefaultResourceResolver`,
   then that same joined string searched across every jar in `lib/`. Anchor `basePath` at the resource
   root and the two contracts agree exactly: `root.resolve(ANCHOR).resolveSibling(P) == root/P`.
2. `MetaInfResourceResolver` and `InParentPathResourceResolver` — the resolvers named in that warning —
   are **`XIncluder`-internal wrappers**, added conditionally at `XIncluder.kt:119-120`. They are not in
   the chain given to `PluginCreator`. That chain is a plain
   `CompositeResourceResolver(JarsResourceResolver(lib jars) + caller's)`
   (`LibDirectoryPluginLoader.kt:63-64`). There is nothing to double-join.

So reusing the chain is both correct and the cheapest option, and it brings
`JarsResourceResolver`'s sibling-jar search along for free — which is precisely the cross-jar gap
(C4(ii)) that broke `plugins/Kotlin` on `META-INF/base-codeInsight-minimal.xml`, a file living in
`kotlin-plugin-shared.jar`.

```kotlin
internal class ResourceResolverXIncludeLoader(
  private val resolver: ResourceResolver,
  private val resourceRoot: Path
) : XIncludeLoader {
  /**
   * [path] arrives already joined by the library, relative to the plugin's resource root
   * ("absolute path from a resource root, without leading '/'" — `XIncludeLoader`'s own doc), having
   * been built by `LoadPathUtil.toLoadPath`/`getChildBaseDir`. [ResourceResolver], by contrast, joins
   * via `basePath.resolveSibling(relativePath)`. Anchoring [basePath] at the resource root makes the
   * two agree, and lets `JarsResourceResolver`'s sibling-jar fallback apply unchanged.
   */
  override fun loadXIncludeReference(path: String): LoadedXIncludeReference? {
    val basePath = resourceRoot.resolve(ANCHOR)
    return when (val result = resolver.resolveResource(path, basePath)) {
      is ResourceResolver.Result.Found ->
        result.use { LoadedXIncludeReference(it.resourceStream.readBytes(), it.description) }
      ResourceResolver.Result.NotFound -> null
      is ResourceResolver.Result.Failed -> throw result.exception
    }
  }

  private companion object { const val ANCHOR = "__xinclude_anchor__" }
}
```

### Resource root derivation

The root cause of every XInclude failure on this branch (C2): a `ZipPath` from
`FileSystem.getPath("META-INF/plugin.xml")` is **relative**, so `parent.parent` is `null` and
`pluginRoot()` returned `null` for every jar-packaged descriptor — which is nearly all of them.
`ResourceRootXIncludeLoader` has therefore **never executed**, and every corpus number gathered on this
branch was taken with the bridge inert.

The existing two-branch logic is correct once the path is absolutized:

| descriptor | absolutized `documentPath` | root |
|---|---|---|
| main `plugin.xml` in a jar | `/META-INF/plugin.xml` | parent is `META-INF` → `parent.parent` = `/` |
| file-based content module in a jar | `/intellij.database.xml` | `parent` = `/` |
| exploded plugin directory | `<pluginDir>/META-INF/plugin.xml` | `<pluginDir>` |

So: `documentPath.toAbsolutePath()` at the top of root derivation, and both branches start working.

**Implementation risk to verify:** the derived root is a `Path` in a zip filesystem that may be closed
by the time the library calls back. `XIncluder` operates under the same constraint and works, so the
open/close scoping in `JarPluginLoader` is probably already correct — but confirm it rather than assume,
since a closed-filesystem failure would surface as a generic unreadable descriptor.

### Reader context

`isMissingIncludeIgnored = false` stays — it mirrors `XIncluder`'s strictness. With a non-null loader,
`<xi:fallback>` starts working for genuinely optional includes; today it cannot, because the null-loader
throw precedes all attribute reading (see the bytecode note in Step 5).

## Step 4 — Inline content modules

`<module name="...">CDATA</module>` descriptors have no filesystem path at all: `DescriptorResource.filePath`
is synthesised from a URI fragment as a bare, parentless single-segment `Path`, so there is no root to
derive. Previously irrelevant, because inline modules fell to JAXB by accident; now that Step 2 makes
them inherit the platform parser, it has to be solved.

`DescriptorResource.parentDescriptorUri` already points at the containing `plugin.xml`. Thread the
parent's **resolved resource root** down alongside the inherited parser choice, so an inline module
resolves includes against the artifact its parent came from. This requires changing the shared
`createPlugin` / `resolveDocumentAndValidateBean` signature that the JAXB path also uses — explicitly
out of scope for the POC, explicitly in scope now.

Once done, the `pluginRoot()` null-returning branch disappears entirely: every descriptor has a root,
either derived from its own absolutized path or inherited from its parent.

## Step 5 — Conditional includes become a named error

### What the library actually does — from bytecode, not error strings

`javap -p -c -constants` on `XmlReader` in `plugin-system-parser-impl-262.8665.270`:

```
private static final void checkConditionalIncludeIsSupported(String, PluginDescriptorBuilder):
   0: getstatic     K2_ALLOWED_PLUGIN_IDS:Ljava/util/Set;
   7: invokeinterface PluginDescriptorBuilder.getId:()Ljava/lang/String;
  12: invokestatic  CollectionsKt.contains(Iterable, Object)Z
  15: ifne          46                       // in the set -> return
  18: new           java/lang/IllegalArgumentException
  33: ldc           " of 'include' is not supported"
  45: athrow
  46: return
```

`K2_ALLOWED_PLUGIN_IDS`, from the static initialiser (two `listOf` calls unioned by `SetsKt.plus`):
`org.jetbrains.kotlin`, `com.intellij.appcode.kmm`, `org.jetbrains.kotlin.native.appcode`,
`org.jetbrains.android`, `androidx.compose.plugins.idea`, `com.jetbrains.kmm`,
`com.jetbrains.kotlin.ocswift`, `com.jetbrains.rider.android`.

The call site in `readInclude`, in bytecode order:

```
 18: ldc "include is not supported because no pathResolver"    // fires FIRST, before any attribute read
122: ldc "includeIf"
136: ldc "includeUnless"
150: ldc "xpointer"
164: ldc "href"
272: ldc "includeIf attribute support is deprecated and is planned for removal in 26.3 version IJPL-215563 (plugin id="
317: invokestatic checkConditionalIncludeIsSupported
339: invokestatic java/lang/System.getProperty
351: ldc "true"
549: ldc "Attribute `xpointer` is not supported anymore (xpointer="
```

Three corrections to this branch's `PlatformPluginDescriptorParser` class doc, point 1:

- It claims the library has "disabled" these attributes — "recognized but now a no-op with a warning".
  **False.** For an allowlisted id it logs the deprecation warning and then evaluates the condition
  against a real `System.getProperty(...) == "true"`. For every other id it throws.
- It cites a corpus scan finding **"zero using `includeIf`/`includeUnless`"**. Re-running the scan over
  `idea-IU-262.2500` finds **1** (`plugins/Kotlin/lib/kotlin-plugin.jar!META-INF/plugin.xml`, rows
  23–24), and over the local plugin cache finds **4** more. The scan that justified this design decision
  was wrong.
- The `xpointer` half is right: the inert default is filtered out and any other value throws. Confirmed
  harmless in practice — the only value in the wild, across 165 cached plugins, is
  `xpointer(/idea-plugin/*)` (15 occurrences).

The null-loader throw at offset 18 preceding all attribute reading is also why an *optional* include
with `<xi:fallback/>` cannot survive a null loader — which is what killed `completionMlRanking`.

### What to build

Map the allowlist rejection to a named `PluginProblem` at ERROR level — the plugin is invalid — with a
message that says what happened and why, routed through the same
`LevelRemappingPluginCreationResultResolver` channel as every other problem so severity stays
controllable (`PARSER_POC_FINDINGS.md` §6; note the trap that inline modules install
`AnyProblemToWarningPluginCreationResultResolver` and downgrade everything).

Wording should name the attribute, the removal (26.3, IJPL-215563) and the plugin's own declared
compatibility, since that is the justification for failing it: the plugin says it runs on IDEs where
these attributes no longer exist.

This is a deliberate divergence from master, which drops the include silently — and it is the direction
the cutoff choice implies. Exposure today is nil: the 4 cached plugins using these attributes all
declare `until=253.*`/`261.*` and stay on the JAXB path, and all of them use Kotlin's own
`idea.kotlin.plugin.use.k1` property rather than inventing their own.

Do not encode the eight allowlisted IDs anywhere in our code. The set is JetBrains-internal, already
scheduled for deletion, and can change between library versions.

## Step 6 — Forced on for evaluation

The rule from Step 1 selects 7% of plugins, none of which uses `xi:include`. Shipping Steps 3–5 under it
would give the entire XInclude rework **zero natural coverage**.

So: implement and unit-test the rule as specified, then force the platform parser on for every
descriptor at the single call site, with a marker comment that makes restoring the rule a one-line
change:

```kotlin
// POC: forced on so the corpus exercises the platform parser everywhere. The real rule below is
// implemented and unit-tested; delete this line to restore it. See PARSER_PLAN.md, Step 6.
val proceed = if (true /* shouldUsePlatformParser(originalDocument) */) { ... }
```

Keep the function itself live and covered by tests rather than commenting it out, so it cannot rot
while disabled. Note that forcing it on also re-exposes everything the 263 cutoff would have hidden —
all 133 bundled descriptors, `plugins/Kotlin` included — which is the point: that is where the XInclude
work gets tested.

## Step 7 — Tests

There are currently **no tests anywhere** referencing `PlatformPluginDescriptorParser` or the selection
rule. Minimum set:

*Selection rule* (needs the function visible to tests): no `idea-version`; `since` only, above and below
the floor; `until` absent; `until` = `261.*`, `262.*`, `263.*`, `300.*`, `999.*`, bare `262`, `IU-262.*`,
malformed; `since > until`; and inheritance — a parent selected/rejected, with a content module and an
inline module each following the parent.

*XInclude* (the part with no natural corpus coverage, so fixtures carry the weight): include resolved
within the same jar; include resolved from a **sibling jar** in `lib/` (the `plugins/Kotlin` shape);
include from an exploded directory plugin; missing required include → problem, not silent hole; missing
optional include with `<xi:fallback/>` → resolves to the fallback; `xpointer` with the inert default →
ignored; `xpointer` with any other value → problem; `includeIf` on a non-allowlisted id → the new named
ERROR; nested includes crossing artifacts.

*Regression:* `:intellij-plugin-verifier:verifier-test:test` must stay green.

## Step 8 — Measurement

Acceptance signals, per C5/C6 — **not** total problem count, which falls when a plugin disappears
because many class-level problems collapse into one package-level one:

- count of successfully created bundled plugins in the IDE (a `WARN` today, easily missed);
- per-plugin **verdict** equality between the two parsers on a fixed corpus;
- distinct `(plugin, problem)` pairs rather than deduplicated problem totals.

Local specimen from the root-cause document, expected to flip from broken to working once Step 3 lands:

```bash
java -jar intellij-plugin-verifier/verifier-cli/build/libs/verifier-cli-dev-all.jar \
  check-plugin '#1002092' ~/Downloads/idea-latest/idea-IU-262.2500
```

Master says `Compatible`; the branch today says `1 missing mandatory dependency … org.jetbrains.kotlin:
Unavailable`. With Steps 3–4 it should say `Compatible` again — and the Kotlin descriptor should then
resolve to something far richer than master's, because master's `XIncluder` drops **both** K1 and K2
branches (16 elements, 10 root children, versus 921 elements with conditional resolution enabled). That
divergence is a change in verification results and needs to be a stated outcome, not a surprise.

---

## Flagged, not decided

- **Validation parity.** `com.intellij.ml.llm:262.8665.396` is rejected by master
  (`release-version` and plugin version must share a beginning) and **accepted** by the platform path,
  which reports 186 problems instead. The two paths' validation sets have diverged in both directions —
  too strict on Kotlin, too lax here — and nothing keeps them in sync; the since/until commit bridged
  one check by hand. The structural fix is validators over a common post-parse model rather than
  per-parser. Not in this plan's scope; needs a decision.
- **`underlyingDocument`.** On the platform path it stays the *unresolved* document, still containing
  `<xi:include>` elements, because no resolved JDOM `Document` is ever built. `extensions` are fine —
  the converter builds them from the library's model. `structure-intellij` is published API and
  `IdePlugin.extensions` is confirmed consumed by Marketplace's Platform Explorer, while
  `underlyingDocument`'s consumers are unknown. Worth a downstream census before anyone relies on it.
- **`<product-descriptor eap="true">` is silently lost** on the platform path: `RawPluginDescriptor` has
  no `eap` field. Pre-existing known gap, unchanged by this plan.
- **Failed bundled-plugin load is a `WARN`**, with reasons only at DEBUG. A run whose IDE is missing a
  bundled plugin is describing a different IDE than the one requested, and reports look *cleaner*, not
  worse. Independent of the parser work; worth fixing separately.

## Evidence

- Bytecode: `javap -p -c -constants -classpath <plugin-system-parser-impl-262.8665.270.jar>
  com.intellij.platform.pluginSystem.parser.impl.XmlReader`. No sources jar is published.
- Bundled-descriptor survey: 133 `META-INF/plugin.xml` entries under `idea-IU-262.2500/plugins/*/lib/*.jar`,
  all declaring `since-build="262.2500" until-build="262.2500"`.
- Corpus scan: `~/.pluginVerifier/loaded-plugins`, 165 artifacts / 119 distinct plugin IDs, recursing one
  level into nested jars. 24 plugins use `<xi:include>`, all inside jars; 4 use
  `includeIf`/`includeUnless`; 6 use `xpointer`, all the inert default.
- Earlier work: `PARSER_POC_ROOT_CAUSE.md` (conclusions C1–C7, design questions D1–D6),
  `PARSER_POC_FINDINGS.md` (live-debug study; §5 module conventions, §6 problem-resolver channel,
  §7 resolver chain, §9 IDE-cache conflict).
