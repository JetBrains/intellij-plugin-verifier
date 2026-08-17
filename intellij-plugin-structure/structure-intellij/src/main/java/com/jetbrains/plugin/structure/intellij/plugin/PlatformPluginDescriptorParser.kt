/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.intellij.platform.pluginSystem.parser.impl.LoadedXIncludeReference
import com.intellij.platform.pluginSystem.parser.impl.PluginDescriptorReaderContext
import com.intellij.platform.pluginSystem.parser.impl.RawPluginDescriptor
import com.intellij.platform.pluginSystem.parser.impl.XIncludeLoader
import com.intellij.platform.pluginSystem.parser.impl.parsePluginXml
import com.intellij.util.xml.dom.NoOpXmlInterner
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import org.jdom2.Document
import org.jdom2.output.XMLOutputter
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(PlatformPluginDescriptorParser::class.java)

/**
 * POC: parses a plugin.xml descriptor using JetBrains' own `plugin-system-parser-impl` library
 * (extracted from the IntelliJ Platform, package `com.intellij.platform.pluginSystem.parser.impl`)
 * instead of our hand-rolled JDOM+JAXB pipeline (see [PluginDescriptorParser] / [PluginBeanExtractor]).
 *
 * This is the alternative path to [PluginDescriptorParser]; both are kept side by side, and which one
 * runs is decided per plugin - see [PluginCreator.shouldUsePlatformParser] (a plugin's own declared
 * `since-build` decides, not a global switch).
 *
 * Design notes (see also RawPluginDescriptorToIdePluginConverter's class doc):
 *
 * 1. XInclude resolution is delegated to the new library's own [XIncludeLoader] mechanism (via
 *    [ResourceRootXIncludeLoader] below), not to [XIncluder]. The new library has disabled
 *    `includeIf`/`includeUnless` (JetBrains ticket IJPL-215563 - recognized but now a no-op with a
 *    warning) and throws a hard `RuntimeException` for any non-default `xpointer` value; [XIncluder]
 *    still honors both. A scan of every bundled plugin shipped with a real, current IntelliJ IDEA
 *    install (1348 jars, 2062 descriptor-shaped XML files) found: 27 files using plain
 *    `<xi:include href="...">` (the core mechanism is alive, just a minority pattern used mostly by
 *    JetBrains' own largest plugins - Java, Kotlin, Database Tools); zero using
 *    `includeIf`/`includeUnless`; and the only 3 `xpointer` usages found were all the inert default
 *    value (an XPointer selecting all children of the root `idea-plugin` element), which both parsers
 *    already treat as a no-op. So the new library resolving XIncludes itself is unlikely to regress
 *    real plugins on this specific axis - though the CI corpus run is what confirms that at scale, not
 *    this local sample.
 *
 * 2. The new library throws bare `RuntimeException`/`XMLStreamException` for a fair number of
 *    malformed-input cases that our JAXB path would just leave as null fields or flag as a
 *    recoverable [com.jetbrains.plugin.structure.base.problems.PluginProblem] (bad/oversized
 *    `namespace` on `<content>`, missing `name`/`interface`/`beanClass` on `<extensionPoints>`,
 *    unknown content item types, unresolvable required includes, etc). It also routes any
 *    `idea-plugin` child element it doesn't recognize (e.g. `<icon>`, unsupported by this parser
 *    build at all - see `RawPluginDescriptor`, which has no icon field) through the platform's own
 *    `Logger.error(...)`, whose default (non-IDE) implementation throws `AssertionError` - an
 *    `Error`, not an `Exception`. Since plugin-verifier's job is running against arbitrary -
 *    sometimes broken or adversarial - third-party plugins, [parse] wraps the whole call (XInclude
 *    resolution included) in a broad `catch (Throwable)`, mirroring
 *    [PluginDescriptorParser.readDocumentIntoXmlBean]'s existing pattern but widened past `Exception`
 *    specifically to still contain that `AssertionError` case.
 *
 * 3. KNOWN GAP (accepted for this POC, not yet a blocker): [RawPluginDescriptor] has no `eap` field
 *    at all - the library's `readProduct()` only reads code/release-date/release-version/`optional`
 *    (`optional` maps to the top-level `isLicenseOptional`, not a nested product-descriptor field).
 *    A plugin declaring `<product-descriptor eap="true">` will silently lose that flag through this
 *    path. See RawPluginDescriptorToIdePluginConverter for where this surfaces.
 *
 * 4. Consequence of point 1: [PluginCreator.plugin]'s `underlyingDocument` is set to the ORIGINAL,
 *    unresolved [Document] on this path (still containing `<xi:include>` elements) - it no longer
 *    reflects fully-inlined content the way the JAXB path's does. Checked: nothing outside this
 *    module reads `underlyingDocument` today (only declared/copied/set), so this is a cosmetic gap
 *    for the POC, not a functional one - but worth knowing if a future consumer starts relying on it.
 */
internal class PlatformPluginDescriptorParser {

  /**
   * `interner` mirrors JetBrains' own reference usage in `PluginXmlParserTest.parseBytes`
   * (intellij-community, platform/pluginSystem/parser/impl/testSrc) - `NoOpXmlInterner` is the
   * library's own ready-made no-op implementation (from `util-xml-dom`, a transitive dependency).
   * `isMissingIncludeIgnored = false`: mirrors [XIncluder]'s strictness - an unresolvable required
   * include (no `<xi:fallback>`) should fail this plugin's parse, not silently produce a plugin
   * with a hole in it. See `readInclude` in the vendored library source: this only matters when
   * [ResourceRootXIncludeLoader] returns `null` for a non-optional include.
   */
  private val readerContext = object : PluginDescriptorReaderContext {
    override val interner = NoOpXmlInterner
    override val isMissingIncludeIgnored: Boolean = false
  }

  /**
   * @param document the ORIGINAL, unresolved JDOM [Document] - `<xi:include>` elements are resolved
   *   by the new library itself via [ResourceRootXIncludeLoader], not by [XIncluder] (see class doc,
   *   point 1).
   * @param documentPath the descriptor's own file path (e.g. `.../META-INF/plugin.xml`), used to
   *   derive the plugin's resource root for [ResourceRootXIncludeLoader].
   * @return the parsed descriptor, or `null` if parsing (including XInclude resolution) failed - in
   *   which case a problem has already been registered on [validationContext] (mirroring
   *   [PluginDescriptorParser.ParseResult.InvalidBean]).
   */
  fun parse(
    document: Document,
    documentPath: Path,
    descriptorPath: String,
    pluginFileName: String,
    validationContext: ValidationContext
  ): RawPluginDescriptor? {
    return try {
      val xml = serialize(document)
      // null when pluginRoot() has no usable root to give it (see its doc) - the library treats a
      // null loader as "no <xi:include> support", which fails cleanly if one is actually encountered
      // rather than resolving against a guessed-wrong root.
      val xIncludeLoader = pluginRoot(documentPath)?.let { ResourceRootXIncludeLoader(it) }
      parsePluginXml(xml, pluginFileName, readerContext, xIncludeLoader).build()
    } catch (e: Throwable) {
      // Broad catch by design: see class doc, point 2. The library throws plain RuntimeException /
      // XMLStreamException, not our PluginProblem hierarchy, for many malformed-input / unresolved-
      // include cases - and, for any `idea-plugin` child element it doesn't recognize (e.g. `<icon>`,
      // which this parser build has no support for at all), it goes through the platform's own
      // `Logger.error(...)`, whose default (non-IDE) implementation throws `AssertionError` - an
      // `Error`, not an `Exception`. Catching only `Exception` here let that escape uncaught, which
      // took down the whole verifier worker process instead of just failing this one plugin.
      validationContext += UnableToReadDescriptor(descriptorPath, e.localizedMessage)
      LOG.info("Unable to read plugin descriptor $descriptorPath of $pluginFileName via platform parser", e)
      null
    }
  }

  private fun serialize(document: Document): ByteArray {
    val out = ByteArrayOutputStream()
    XMLOutputter().output(document, out)
    return out.toByteArray()
  }

  /**
   * Derives the resource root a classloader would use for this descriptor - i.e. what
   * `LoadPathUtil`-joined paths (e.g. `"META-INF/extensions.xml"`) are relative to - so
   * [ResourceRootXIncludeLoader] can resolve them. Two shapes are handled, distinguished by whether
   * `documentPath`'s own parent directory is named `META-INF`:
   *
   *  - Main plugin.xml and V1 optional-dependency config-file descriptors
   *    (`<depends optional="true" config-file="...">`) both live directly under `<root>/META-INF/` -
   *    confirmed against `JarPluginLoader.kt` (`jar.getPluginDescriptor("META-INF/$descriptorPath")`),
   *    `PluginDirectoryLoader.kt` (`pluginDirectory.resolve(META_INF).resolve(descriptorPath)`), and
   *    `OptionalDependencyResolver.kt` (loaded the same way, no `../` in that convention). Root is
   *    `documentPath.parent.parent`.
   *  - File-based content modules ([PluginModuleResolver]'s `"../$moduleName.xml"` convention,
   *    [FileBasedModuleDescriptorResolver]) always land at some jar/directory's own root, never under
   *    `META-INF`: the forced `META-INF/` prefix the two loaders above always add cancels out against
   *    the module reference's own leading `../`. That "some root" is either the main plugin artifact
   *    itself, or - when the module ships as its own separate jar under `lib/modules/<name>.jar`
   *    (`FileBasedModuleDescriptorResolver.kt:45`, the common packaging for modern content modules) -
   *    that module's own jar. Either way `documentPath.parent` IS that root directly.
   *
   * Returns null for inline content modules (`<module name="...">CDATA</module>`,
   * [InlineModuleDescriptorResolver]), which have no real filesystem path at all -
   * `DescriptorResource.filePath` is built from a URI fragment into a bare, parentless single-segment
   * `Path`, so `documentPath.parent` is `null` and there's nothing to derive a root from.
   * `DescriptorResource.parentDescriptorUri` does point at the containing plugin.xml and could fix
   * this, but threading it down would mean changing `PluginCreator.createPlugin`/
   * `resolveDocumentAndValidateBean`'s shared signature (also used by the JAXB path) - out of scope here.
   */
  private fun pluginRoot(documentPath: Path): Path? {
    val parent = documentPath.parent ?: return null
    return if (parent.fileName?.toString() == IdePluginManager.META_INF) parent.parent else parent
  }
}

/**
 * Bridges the new library's [XIncludeLoader] contract to a direct resource-root-relative file
 * lookup, deliberately NOT reusing [ResourceResolver.resolveResource] - see
 * [PlatformPluginDescriptorParser]'s class doc, point 1, for why letting the new parser resolve
 * XIncludes itself is the point, and see [XIncluder]'s call site (`resolveXIncludeElements`,
 * `resolver.resolveResource(href, basePath)`) for why that method's contract doesn't fit here:
 * it resolves the RAW `href` against the CURRENT document's own path
 * (`basePath.resolveSibling(relativePath)`), re-run fresh at each nesting level. This method's
 * `path` argument, by contrast, arrives ALREADY joined by the library itself
 * (`LoadPathUtil.toLoadPath`/`getChildBaseDir`) into a single path relative to the plugin's resource
 * root (e.g. `"META-INF/extensions.xml"`, or a bare `"intellij.foo.bar"` for V2 module descriptor
 * references) - documented on [XIncludeLoader.loadXIncludeReference] itself: "absolute path from a
 * resource root, without leading '/'". Feeding that pre-joined path into a sibling-relative resolver
 * (or a composite one like `MetaInfResourceResolver`, which re-appends its own `META-INF/` segment)
 * would silently double up path segments or otherwise resolve to the wrong file.
 *
 * KNOWN LIMITATION: does not replicate `MetaInfResourceResolver` / `InParentPathResourceResolver` /
 * `JarsResourceResolver`'s fallback strategies (e.g. resolving into a sibling library jar). Acceptable
 * for this POC's purpose - measuring how the new parser's core XInclude resolution fares against a
 * large corpus - not necessarily for a production version.
 */
internal class ResourceRootXIncludeLoader(private val pluginRoot: Path) : XIncludeLoader {
  override fun loadXIncludeReference(path: String): LoadedXIncludeReference? {
    val target = pluginRoot.resolve(path)
    if (!Files.isRegularFile(target)) {
      return null
    }
    return LoadedXIncludeReference(Files.readAllBytes(target), target.toString())
  }
}
