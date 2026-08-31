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
 *    [ResourceResolverXIncludeLoader] over the very [ResourceResolver] chain this module already
 *    builds for [XIncluder][com.jetbrains.plugin.structure.intellij.xinclude.XIncluder]. The library
 *    diverges from [XIncluder][com.jetbrains.plugin.structure.intellij.xinclude.XIncluder] on the two
 *    conditional-include attributes and on `xpointer`:
 *
 *    - `includeIf`/`includeUnless` are honoured only for a small, JetBrains-internal allowlist of
 *      plugin IDs, and rejected with an `IllegalArgumentException` for everything else. They are
 *      scheduled for removal from the platform in 26.3 (IJPL-215563). We surface that rejection as
 *      [ConditionalIncludeNotSupported], see [toConditionalIncludeProblem].
 *    - `xpointer` throws for any value other than the inert default (an XPointer selecting all
 *      children of the root `idea-plugin` element), which is the only value observed in the wild.
 *
 * 2. The library throws bare `RuntimeException`/`XMLStreamException` for a fair number of
 *    malformed-input cases that our JAXB path would just leave as null fields or flag as a
 *    recoverable [com.jetbrains.plugin.structure.base.problems.PluginProblem] (bad/oversized
 *    `namespace` on `<content>`, missing `name`/`interface`/`beanClass` on `<extensionPoints>`,
 *    unknown content item types, unresolvable required includes, etc). It also routes any
 *    `idea-plugin` child element it doesn't recognize (e.g. `<icon>`, unsupported by this parser
 *    build at all - see [RawPluginDescriptor], which has no icon field) through the platform's own
 *    `Logger.error(...)`, whose default (non-IDE) implementation throws `AssertionError` - an
 *    `Error`, not an `Exception`. Since plugin-verifier's job is running against arbitrary -
 *    sometimes broken or adversarial - third-party plugins, [parse] wraps the whole call (XInclude
 *    resolution included) in a broad `catch (Throwable)`, mirroring
 *    [PluginDescriptorParser.readDocumentIntoXmlBean]'s existing pattern but widened past `Exception`
 *    specifically to still contain that `AssertionError` case.
 *
 * 3. KNOWN GAP: [RawPluginDescriptor] has no `eap` field at all - the library's `readProduct()` only
 *    reads code/release-date/release-version/`optional` (`optional` maps to the top-level
 *    `isLicenseOptional`, not a nested product-descriptor field). A plugin declaring
 *    `<product-descriptor eap="true">` will silently lose that flag through this path. See
 *    RawPluginDescriptorToIdePluginConverter for where this surfaces.
 *
 * 4. Consequence of point 1: [PluginCreator.plugin]'s `underlyingDocument` is set to the ORIGINAL,
 *    unresolved [Document] on this path (still containing `<xi:include>` elements) - it no longer
 *    reflects fully-inlined content the way the JAXB path's does. `extensions` are unaffected: the
 *    converter builds those from the library's own model, which is fully resolved.
 */
internal class PlatformPluginDescriptorParser {

  /**
   * `interner` mirrors JetBrains' own reference usage in `PluginXmlParserTest.parseBytes`
   * (intellij-community, platform/pluginSystem/parser/impl/testSrc) - `NoOpXmlInterner` is the
   * library's own ready-made no-op implementation (from `util-xml-dom`, a transitive dependency).
   * `isMissingIncludeIgnored = false`: mirrors [XIncluder][com.jetbrains.plugin.structure.intellij.xinclude.XIncluder]'s
   * strictness - an unresolvable required include (no `<xi:fallback>`) should fail this plugin's
   * parse, not silently produce a plugin with a hole in it. An unresolvable OPTIONAL include, i.e.
   * one carrying `<xi:fallback>`, is unaffected: the library reads the fallback and carries on.
   */
  private val readerContext = object : PluginDescriptorReaderContext {
    override val interner = NoOpXmlInterner
    override val isMissingIncludeIgnored: Boolean = false
  }

  /**
   * @param document the ORIGINAL, unresolved JDOM [Document] - `<xi:include>` elements are resolved
   *   by the library itself via [ResourceResolverXIncludeLoader], not by
   *   [XIncluder][com.jetbrains.plugin.structure.intellij.xinclude.XIncluder] (see class doc, point 1).
   * @param resourceRoot the root a classloader would resolve this plugin's resources against, as
   *   derived by [PluginCreator.resourceRoot]. `null` only for a descriptor that has neither a
   *   filesystem path of its own nor a parent to inherit one from, in which case `<xi:include>` is
   *   unsupported for this descriptor: the library treats a null loader as "no `<xi:include>`
   *   support" and fails cleanly if one is actually encountered, rather than resolving against a
   *   guessed-wrong root.
   * @param resourceResolver the same composite resolver chain the JAXB path drives
   *   [XIncluder][com.jetbrains.plugin.structure.intellij.xinclude.XIncluder] with.
   * @return the parsed descriptor, or `null` if parsing (including XInclude resolution) failed - in
   *   which case a problem has already been registered on [validationContext] (mirroring
   *   [PluginDescriptorParser.ParseResult.InvalidBean]).
   */
  fun parse(
    document: Document,
    resourceRoot: Path?,
    resourceResolver: ResourceResolver,
    descriptorPath: String,
    pluginFileName: String,
    validationContext: ValidationContext
  ): RawPluginDescriptor? {
    return try {
      // Makes the library's own Logger.error(...) diagnostics stay diagnostics - see its class doc.
      PlatformParserLogging.install()
      val xml = serialize(document)
      val xIncludeLoader = resourceRoot?.let { ResourceResolverXIncludeLoader(resourceResolver, it) }
      parsePluginXml(xml, pluginFileName, readerContext, xIncludeLoader).build()
    } catch (e: Throwable) {
      // Broad catch by design: see class doc, point 2. The library throws plain RuntimeException /
      // XMLStreamException, not our PluginProblem hierarchy, for many malformed-input / unresolved-
      // include cases - and, for any `idea-plugin` child element it doesn't recognize (e.g. `<icon>`,
      // which this parser build has no support for at all), it goes through the platform's own
      // `Logger.error(...)`, whose default (non-IDE) implementation throws `AssertionError` - an
      // `Error`, not an `Exception`. Catching only `Exception` here let that escape uncaught, which
      // took down the whole verifier worker process instead of just failing this one plugin.
      validationContext += e.toConditionalIncludeProblem(document, descriptorPath)
        ?: UnableToReadDescriptor(descriptorPath, e.localizedMessage)
      LOG.info("Unable to read plugin descriptor $descriptorPath of $pluginFileName via platform parser", e)
      null
    }
  }

  private fun serialize(document: Document): ByteArray {
    val out = ByteArrayOutputStream()
    XMLOutputter().output(document, out)
    return out.toByteArray()
  }
}

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
 * Matching on the exception's message is deliberate, and the only option available. The library
 * accepts these attributes for a hardcoded, JetBrains-internal allowlist of plugin IDs
 * (`XmlReader.K2_ALLOWED_PLUGIN_IDS`) and rejects them with a bare `IllegalArgumentException` for
 * everything else. The allowlist is not exposed as API, is already scheduled for deletion along with
 * the attributes themselves, and can change between library versions - so replicating it here to
 * decide up front, before handing the descriptor over, would be both a layering violation and a
 * guaranteed source of drift. Detecting the library's own verdict after the fact keeps the allowlist
 * exactly where it belongs: inside the library.
 *
 * If the message shape ever changes, this degrades to the generic
 * [UnableToReadDescriptor] - still an ERROR, just a less specific one.
 */
private fun Throwable.toConditionalIncludeProblem(
  document: Document,
  descriptorPath: String
): ConditionalIncludeNotSupported? {
  val attributeName = conditionalIncludeAttributeName() ?: return null
  return ConditionalIncludeNotSupported(descriptorPath, attributeName, document.declaredCompatibility())
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

/**
 * The plugin's own `<idea-version>` range, rendered for [ConditionalIncludeNotSupported]'s message:
 * it is the plugin's declared compatibility that justifies failing it, so the message quotes it back.
 */
private fun Document.declaredCompatibility(): String {
  val ideaVersion = rootElement.getChild(IDEA_VERSION_ELEMENT)
  val since = ideaVersion?.getAttributeValue(SINCE_BUILD_ATTRIBUTE)
  val until = ideaVersion?.getAttributeValue(UNTIL_BUILD_ATTRIBUTE)
  return when {
    since != null && until != null -> "IDEs from $since up to $until"
    since != null -> "IDEs from $since onwards, with no upper bound"
    until != null -> "IDEs up to $until"
    else -> "any IDE version, as it declares no <idea-version> range"
  }
}
