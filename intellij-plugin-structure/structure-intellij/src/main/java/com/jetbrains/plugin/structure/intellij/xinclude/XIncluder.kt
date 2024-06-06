/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.xinclude

import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import org.jdom2.*
import java.lang.Boolean.parseBoolean
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern

/**
 * Resolves all `<xi:include>` references in xml documents using the provided path resolver.
 *
 * The inspiring implementation is in IntelliJ Community class [`com.intellij.util.xmlb.JDOMXIncluder`](https://github.com/JetBrains/intellij-community/blob/master/platform/util/src/com/intellij/util/xmlb/JDOMXIncluder.java).
 * This implementation provides better messages.
 */
class XIncluder private constructor(private val resourceResolver: ResourceResolver, private val properties: Properties) {

  companion object {
    @Throws(XIncluderException::class)
    fun resolveXIncludes(
      document: Document,
      presentablePath: String,
      resourceResolver: ResourceResolver,
      documentPath: Path
    ): Document = XIncluder(resourceResolver, System.getProperties()).resolveXIncludes(document, presentablePath, documentPath)
  }

  private fun resolveXIncludes(document: Document, presentablePath: String, documentPath: Path): Document {
    val startEntry = XIncludeEntry(presentablePath, documentPath)
    if (isIncludeElement(document.rootElement)) {
      throw XIncluderException(listOf(startEntry), "Invalid root element ${document.rootElement.getElementNameAndAttributes()}")
    }
    val bases = Stack<XIncludeEntry>()
    bases.push(startEntry)
    val rootElement = resolveNonXIncludeElement(document.rootElement, bases)
    return Document(rootElement)
  }

  private fun resolveIncludeOrNonInclude(element: Element, bases: Stack<XIncludeEntry>): List<Content> {
    return if (isIncludeElement(element)) {
      if (shouldXInclude(element, bases)) {
        resolveXIncludeElements(element, bases)
      } else {
        emptyList()
      }
    } else {
      listOf(resolveNonXIncludeElement(element, bases))
    }
  }

  private fun shouldXInclude(element: Element, bases: Stack<XIncludeEntry>): Boolean {
    val includeUnless: String? = element.getAttributeValueByLocalName(INCLUDE_UNLESS_ATTR_NAME)
    val includeIf: String? = element.getAttributeValueByLocalName(INCLUDE_IF_ATTR_NAME)
    if (isResolvingConditionalIncludes && includeUnless != null && includeIf != null) {
      throw XIncluderException(
        bases, "Cannot use '$INCLUDE_IF_ATTR_NAME' and '$INCLUDE_UNLESS_ATTR_NAME' attributes simultaneously. " +
          "Specify either of these attributes or none to always include the document"
      )
    }

    return if ((includeIf != null || includeUnless != null) && !isResolvingConditionalIncludes) {
      false
    } else includeIf == null && includeUnless == null
      || (includeIf != null && properties.isTrue(includeIf))
      || (includeUnless != null && properties.isFalse(includeUnless))
  }

  private fun resolveXIncludeElements(xincludeElement: Element, bases: Stack<XIncludeEntry>): List<Content> {
    //V2 included configs can be located only in root
    val href = xincludeElement.getAttributeValue(HREF).let { if (PluginCreator.v2ModulePrefix.matches(it)) "/$it" else it}
    val presentableXInclude = xincludeElement.getElementNameAndAttributes()
    if (href.isNullOrEmpty()) {
      throw XIncluderException(bases, "Missing or empty 'href' attribute in $presentableXInclude")
    }

    val parseAttribute = xincludeElement.getAttributeValue(PARSE)
    if (parseAttribute != null && parseAttribute != XML) {
      throw XIncluderException(bases, "Attribute 'parse' must be 'xml' but was '$parseAttribute' in $presentableXInclude")
    }

    val baseAttribute = xincludeElement.getAttributeValue(BASE, Namespace.XML_NAMESPACE)
    if (baseAttribute != null) {
      throw XIncluderException(bases, "'base' attribute of xi:include is not supported!")
    }

    val basePath = bases.peek()!!.documentPath
    val resolver = CompositeResourceResolver(mutableListOf<ResourceResolver>().apply {
      add(resourceResolver)
      if (basePath.isInMetaInf()) add(InParentPathResourceResolver(resourceResolver))
      if (basesHaveMetaInfResolution(bases)) add(MetaInfResourceResolver(resourceResolver))
    })

    when (val resourceResult = resolver.resolveResource(href, basePath)) {
      is ResourceResolver.Result.Found -> resourceResult.use {
        val remoteDocument = try {
          JDOMUtil.loadDocument(it.resourceStream.buffered())
        } catch (e: Exception) {
          throw XIncluderException(bases, "Invalid document '$href' referenced in $presentableXInclude", e)
        }

        val xincludeEntry = XIncludeEntry(href, resourceResult.path)
        val xIncludeElements = resolveXIncludesOfRemoteDocument(remoteDocument, xincludeElement, xincludeEntry, bases)
        val startComment = Comment("Start $presentableXInclude")
        val endComment = Comment("End $presentableXInclude")
        return listOf(startComment) + xIncludeElements + listOf(endComment)
      }
      is ResourceResolver.Result.NotFound -> {
        val fallbackElement = xincludeElement.getChild("fallback", xincludeElement.namespace)
        if (fallbackElement != null) {
          return emptyList()
        }
        throw XIncluderException(bases, "Not found document '$href' referenced in $presentableXInclude. <xi:fallback> element is not provided.")
      }
      is ResourceResolver.Result.Failed -> {
        throw XIncluderException(bases, "Failed to load document referenced in $presentableXInclude", resourceResult.exception)
      }
    }
  }

  private fun Path.isInMetaInf(): Boolean {
    val parent: Path? = parent
    return parent?.simpleName == "META-INF"
  }

  private fun basesHaveMetaInfResolution(bases: Stack<XIncludeEntry>): Boolean {
    return bases.any { it.documentPath.isInMetaInf() }
  }

  private fun resolveXIncludesOfRemoteDocument(
    remoteDocument: Document,
    xincludeElement: Element,
    xincludeEntry: XIncludeEntry,
    bases: Stack<XIncludeEntry>
  ): List<Content> {
    val presentableXInclude = xincludeElement.getElementNameAndAttributes()
    checkCyclicReference(xincludeEntry, bases)

    if (!remoteDocument.hasRootElement()) {
      throw XIncluderException(bases, "Remote root element is not set for document referenced in $presentableXInclude")
    }

    if (remoteDocument.content.count { it is Element } > 1) {
      throw XIncluderException(bases, "Multiple root elements in document referenced in $presentableXInclude")
    }

    bases.push(xincludeEntry)
    val remoteContents = try {
      resolveIncludeOrNonInclude(remoteDocument.rootElement, bases)
    } finally {
      bases.pop()
    }

    if (remoteContents.isEmpty()) {
      return emptyList()
    }

    if (remoteContents.size > 1) {
      throw XIncluderException(bases, "Multiple elements referenced in $presentableXInclude")
    }

    val remoteRootElement = remoteContents.single() as? Element
      ?: throw XIncluderException(bases, "Root element, not '${remoteContents.single().cType}', must have been resolved in $presentableXInclude")

    return selectContents(xincludeElement, xincludeEntry, remoteRootElement, bases)
  }

  private fun checkCyclicReference(xincludeEntry: XIncludeEntry, bases: Stack<XIncludeEntry>) {
    val index = bases.indexOf(xincludeEntry)
    if (index >= 0) {
      val cycle = bases.drop(index) + listOf(xincludeEntry)
      val prefix = bases.take(index + 1)
      throw XIncluderException(prefix, "Circular includes: " + cycle.joinToString(separator = " -> ") { it.presentablePath })
    }
  }

  private fun resolveNonXIncludeElement(element: Element, bases: Stack<XIncludeEntry>): Element {
    val result = Element(element.name, element.namespace)
    if (element.hasAttributes()) {
      for (attribute in element.attributes) {
        result.setAttribute(attribute.clone())
      }
    }

    if (element.hasAdditionalNamespaces()) {
      for (additionalNamespace in element.additionalNamespaces) {
        result.addNamespaceDeclaration(additionalNamespace)
      }
    }

    for (content in element.content) {
      if (content is Element) {
        result.addContent(resolveIncludeOrNonInclude(content, bases))
      } else {
        result.addContent(content.clone())
      }
    }

    return result
  }

  private fun selectContents(
    xincludeElement: Element,
    xincludeEntry: XIncludeEntry,
    remoteRootElement: Element,
    bases: Stack<XIncludeEntry>
  ): List<Content> {
    val xPointer = xincludeElement.getAttributeValue(XPOINTER)
      ?: return remoteRootElement.content.toList().map { it.detach() }

    val pointerMatcher = XPOINTER_PATTERN.matcher(xPointer)
    if (!pointerMatcher.matches()) {
      throw XIncluderException(bases, "Invalid xpointer value in ${xincludeElement.getElementNameAndAttributes()}")
    }

    val pointerSelector = pointerMatcher.group(1)

    val selectorMatcher = XPOINTER_SELECTOR_PATTERN.matcher(pointerSelector)
    if (!selectorMatcher.matches()) {
      throw XIncluderException(bases, "Invalid xpointer selector value in ${xincludeElement.getElementNameAndAttributes()}")
    }

    val rootTagName = selectorMatcher.group(1)

    if (remoteRootElement.name != rootTagName) {
      return emptyList()
    }

    val subTagName = selectorMatcher.group(2)?.drop(1)
    val selectedChildren = if (subTagName != null) {
      val child = remoteRootElement.getChild(subTagName)
        ?: throw XIncluderException(bases, "No elements are selected in document '${xincludeEntry.presentablePath}' referenced in ${xincludeElement.getElementNameAndAttributes()}")
      child.content
    } else {
      remoteRootElement.content
    }.toList()

    selectedChildren.forEach { it.detach() }
    return selectedChildren
  }

  private fun Element.getElementNameAndAttributes(): String {
    return "<$qualifiedName " + attributes.joinToString { "${it.name}=\"${it.value}\"" } + "/>"
  }

  private fun isIncludeElement(element: Element): Boolean =
    element.name == INCLUDE && element.namespace == HTTP_XINCLUDE_NAMESPACE

  private fun Element.getAttributeValueByLocalName(attributeLocalName: String): String? {
    val attr = this.attributes.find { it.name == attributeLocalName }
    return attr?.value
  }

  private fun Properties.isTrue(key: String?): Boolean {
    return parseBoolean(getProperty(key))
  }

  private fun Properties.isFalse(key: String?): Boolean {
    return !parseBoolean(getProperty(key))
  }

  private val isResolvingConditionalIncludes: Boolean
    get() = properties.isTrue(IS_RESOLVING_CONDITIONAL_INCLUDES_PROPERTY)

}

private const val HTTP_WWW_W3_ORG_2001_XINCLUDE = "http://www.w3.org/2001/XInclude"
private const val XI = "xi"
private const val INCLUDE = "include"
private const val HREF = "href"
private const val BASE = "base"
private const val PARSE = "parse"
private const val XML = "xml"
private const val XPOINTER = "xpointer"
private val HTTP_XINCLUDE_NAMESPACE = Namespace.getNamespace(XI, HTTP_WWW_W3_ORG_2001_XINCLUDE)

private val XPOINTER_PATTERN = Pattern.compile("xpointer\\((.*)\\)")
private val XPOINTER_SELECTOR_PATTERN = Pattern.compile("/([^/]*)(/[^/]*)?/\\*")

private const val INCLUDE_UNLESS_ATTR_NAME = "includeUnless"
private const val INCLUDE_IF_ATTR_NAME = "includeIf"

const val IS_RESOLVING_CONDITIONAL_INCLUDES_PROPERTY = "com.jetbrains.plugin.structure.intellij.xinclude.isResolvingConditionalIncludes"