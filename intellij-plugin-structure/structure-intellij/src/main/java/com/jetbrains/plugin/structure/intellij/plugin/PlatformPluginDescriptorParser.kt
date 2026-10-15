/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.intellij.platform.pluginSystem.parser.impl.PluginDescriptorReaderContext
import com.intellij.platform.pluginSystem.parser.impl.RawPluginDescriptor
import com.intellij.platform.pluginSystem.parser.impl.XIncludeLoader
import com.intellij.platform.pluginSystem.parser.impl.parsePluginXml
import com.intellij.util.xml.dom.NoOpXmlInterner
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.intellij.problems.ConditionalIncludeNotSupported
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.xinclude.ResourceResolverXIncludeLoader
import com.jetbrains.plugin.structure.intellij.xinclude.RecordingXIncludeLoader
import com.jetbrains.plugin.structure.intellij.xinclude.XIncludeLoadAttempt
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import org.jdom2.Document
import org.jdom2.output.XMLOutputter
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(PlatformPluginDescriptorParser::class.java)

/**
 * Parses a plugin.xml descriptor using JetBrains' own `plugin-system-parser-impl` library
 * (extracted from the IntelliJ Platform, package `com.intellij.platform.pluginSystem.parser.impl`)
 * instead of our hand-rolled JDOM+JAXB pipeline (see [PluginDescriptorParser] / [PluginBeanExtractor]).
 *
 * This is the alternative path to [PluginDescriptorParser]; both are kept side by side, and which one
 * runs is decided once per plugin, at its main descriptor - see [PluginCreator.shouldUsePlatformParser].
 *
 * Design notes (see also RawPluginDescriptorToIdePluginConverter's class doc):
 *
 * 1. XInclude resolution is delegated to the library's own [XIncludeLoader] mechanism, driven by
 *    [ResourceResolverXIncludeLoader] over the same [ResourceResolver] the JAXB path hands to
 *    [XIncluder][com.jetbrains.plugin.structure.intellij.xinclude.XIncluder]. The library diverges
 *    from [XIncluder][com.jetbrains.plugin.structure.intellij.xinclude.XIncluder] on the two
 *    conditional-include attributes and on `xpointer`:
 *
 *    - `includeIf`/`includeUnless` are honoured only for a small, JetBrains-internal allowlist of
 *      plugin IDs, and rejected with an `IllegalArgumentException` for everything else. They are
 *      planned for removal from the platform in 26.3 (IJPL-215563). We surface that rejection as
 *      [ConditionalIncludeNotSupported], see [toConditionalIncludeProblem].
 *    - `xpointer` throws for any value other than an empty one or the inert default, which selects
 *      all children of the root `idea-plugin` element (of its `extensionPoints` element, for an include
 *      inside `<extensionPoints>`).
 *
 * 2. The library throws bare `RuntimeException`/`XMLStreamException` for a fair number of
 *    malformed-input cases that our JAXB path would leave as null fields or flag as a recoverable
 *    [com.jetbrains.plugin.structure.base.problems.PluginProblem] (bad/oversized `namespace` on
 *    `<content>`, missing `name`/`interface`/`beanClass` on `<extensionPoints>`, unknown content item
 *    types, unresolvable required includes, etc). [parse] turns ordinary exceptions into a problem,
 *    since plugin-verifier runs against arbitrary, sometimes broken, third-party plugins. It
 *    deliberately does not catch arbitrary [Throwable]s: JVM-fatal errors and linkage failures must
 *    escape. The library also reports some recoverable content through the platform's
 *    `Logger.error(...)`, whose default implementation throws `AssertionError`; [PlatformParserLogging]
 *    normally prevents that, and [parse] additionally contains such an [AssertionError], only when its
 *    stack identifies `DefaultLogger.error`, as a backstop for when the bridge could not be installed.
 *
 * 3. [RawPluginDescriptor] drops some lexical information (`product-descriptor@eap`, whether
 *    `depends@optional="false"` was explicit, and raw product version/date strings). The
 *    [XIncludeLoader] is wrapped by [RecordingXIncludeLoader], allowing [PlatformDescriptorView]
 *    to recover those values from exactly the root/include documents the platform parser consumed,
 *    without a second resource-resolution pass.
 *
 * 4. Consequence of point 1: [PluginCreator.plugin]'s `underlyingDocument` is the ORIGINAL,
 *    unresolved [Document] on this path (still containing `<xi:include>` elements), not the
 *    fully-inlined one the JAXB path stores. `extensions` are unaffected: the converter builds those
 *    from the library's own, fully resolved model.
 */
internal class PlatformPluginDescriptorParser {

  /**
   * `interner` mirrors JetBrains' own reference usage in `PluginXmlParserTest.parseBytes`
   * (intellij-community, platform/pluginSystem/parser/impl/testSrc) - `NoOpXmlInterner` is the
   * library's ready-made no-op implementation (from `util-xml-dom`, a direct dependency of this module).
   * `isMissingIncludeIgnored = false`: mirrors [XIncluder][com.jetbrains.plugin.structure.intellij.xinclude.XIncluder]'s
   * strictness - an unresolvable required include (no `<xi:fallback>`) fails this plugin's parse
   * instead of leaving a hole in it. An unresolvable include carrying `<xi:fallback>` is unaffected:
   * the library skips it, without using the fallback's content, and carries on.
   */
  private val readerContext = object : PluginDescriptorReaderContext {
    override val interner = NoOpXmlInterner
    override val isMissingIncludeIgnored: Boolean = false
  }

  /**
   * @param document the ORIGINAL, unresolved JDOM [Document] - `<xi:include>` elements are resolved
   *   by the library itself via [ResourceResolverXIncludeLoader], not by
   *   [XIncluder][com.jetbrains.plugin.structure.intellij.xinclude.XIncluder] (see class doc, point 1).
   * @param resourceRoot the root a classloader would resolve this plugin's resources against, see
   *   [PluginCreator.resourceRoot]. `null` for an inline content module, matching IntelliJ's own
   *   loader, which gives embedded module content no XInclude support: the library then has no
   *   loader and fails the parse if an `<xi:include>` is actually encountered.
   * @param resourceResolver the same resolver the JAXB path hands to
   *   [XIncluder][com.jetbrains.plugin.structure.intellij.xinclude.XIncluder].
   * @param selectionReason why this plugin is on the platform-parser path, quoted in
   *   [ConditionalIncludeNotSupported]'s message, see [PluginCreator.describePlatformParserSelection].
   * @return the parsed descriptor plus its [PlatformDescriptorView] for validation, or `null` if parsing
   *   (including XInclude resolution) failed - in which case a problem has already been registered on
   *   [validationContext] (mirroring [PluginDescriptorParser.ParseResult.InvalidBean]).
   */
  fun parse(
    document: Document,
    resourceRoot: Path?,
    resourceResolver: ResourceResolver,
    descriptorPath: String,
    pluginFileName: String,
    selectionReason: String,
    validationContext: ValidationContext
  ): PlatformPluginDescriptorParseResult? {
    return try {
      // Makes the library's own Logger.error(...) diagnostics stay diagnostics - see its class doc.
      PlatformParserLogging.install()
      val xml = serialize(document)
      val xIncludeLoader = resourceRoot
        ?.let { ResourceResolverXIncludeLoader(resourceResolver, it) }
        ?.let { RecordingXIncludeLoader(it) }
      val raw = parsePluginXml(xml, pluginFileName, readerContext, xIncludeLoader).build()
      val sourceDocuments = buildList {
        add(document)
        xIncludeLoader?.attempts.orEmpty()
          .filterIsInstance<XIncludeLoadAttempt.Loaded>()
          .mapNotNullTo(this) { loadIncludedDocument(it) }
      }
      PlatformPluginDescriptorParseResult(raw, PlatformDescriptorView(raw, sourceDocuments))
    } catch (e: Exception) {
      validationContext += e.toConditionalIncludeProblem(descriptorPath, selectionReason)
        ?: UnableToReadDescriptor(descriptorPath, e.localizedMessage)
      LOG.info("Unable to read plugin descriptor $descriptorPath of $pluginFileName via platform parser", e)
      null
    } catch (e: AssertionError) {
      if (!e.isDefaultLoggerError()) throw e
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

  private fun loadIncludedDocument(loaded: XIncludeLoadAttempt.Loaded): Document? {
    return try {
      JDOMUtil.loadDocument(loaded.bytes.inputStream())
    } catch (e: Exception) {
      // The platform parser already accepted this input. Failure of this supplemental, verifier-only
      // view must not change its loading verdict; it only means lexical checks cannot inspect it.
      LOG.warn("Unable to retain loaded XInclude '{}' from {} for descriptor validation", loaded.path, loaded.diagnosticLocation, e)
      null
    }
  }
}

internal data class PlatformPluginDescriptorParseResult(
  val descriptor: RawPluginDescriptor,
  val view: PlatformDescriptorView
)

private fun AssertionError.isDefaultLoggerError(): Boolean =
  stackTrace.any { it.className == "com.intellij.openapi.diagnostic.DefaultLogger" && it.methodName == "error" }

/**
 * Suffix of the message the library's `XmlReader.checkConditionalIncludeIsSupported` builds when it
 * rejects `includeIf`/`includeUnless`: `"$attributeName of 'include' is not supported"`, thrown as a
 * plain `IllegalArgumentException`.
 */
private const val CONDITIONAL_INCLUDE_REJECTION_SUFFIX = " of 'include' is not supported"

/**
 * Recognises the library's rejection of an `includeIf`/`includeUnless` attribute and turns it into a
 * named problem, or returns `null` for any other failure.
 *
 * Matching on the exception's message is the only option available. The library accepts these
 * attributes for a hardcoded, JetBrains-internal allowlist of plugin IDs
 * (`XmlReader.K2_ALLOWED_PLUGIN_IDS`) and rejects them with a bare `IllegalArgumentException` for
 * everything else. The allowlist is not exposed as API and can change between library versions, so
 * replicating it here would drift; detecting the library's verdict after the fact keeps it inside the
 * library.
 *
 * If the message shape ever changes, this degrades to the generic
 * [UnableToReadDescriptor] - still an ERROR, just a less specific one.
 */
private fun Throwable.toConditionalIncludeProblem(
  descriptorPath: String,
  selectionReason: String
): ConditionalIncludeNotSupported? {
  val attributeName = conditionalIncludeAttributeName() ?: return null
  return ConditionalIncludeNotSupported(descriptorPath, attributeName, selectionReason)
}

private fun Throwable.conditionalIncludeAttributeName(): String? {
  var cause: Throwable? = this
  val seen = hashSetOf<Throwable>()
  while (cause != null && seen.add(cause)) {
    val message = cause.message
    if (cause is IllegalArgumentException && message != null && message.endsWith(CONDITIONAL_INCLUDE_REJECTION_SUFFIX)) {
      return message.removeSuffix(CONDITIONAL_INCLUDE_REJECTION_SUFFIX).ifEmpty { null }
    }
    cause = cause.cause
  }
  return null
}
