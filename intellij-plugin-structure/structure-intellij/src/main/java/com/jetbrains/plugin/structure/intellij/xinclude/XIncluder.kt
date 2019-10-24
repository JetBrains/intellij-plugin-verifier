/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.plugin.structure.intellij.xinclude

import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import org.jdom2.*
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.regex.Pattern

/**
 * Resolves all `<xi:include>` references in xml documents using the provided path resolver.
 *
 * The inspiring implementation is in IntelliJ Community class [`com.intellij.util.xmlb.JDOMXIncluder`](https://github.com/JetBrains/intellij-community/blob/master/platform/util/src/com/intellij/util/xmlb/JDOMXIncluder.java).
 * This implementation provides better messages.
 */
class XIncluder private constructor(private val resourceResolver: ResourceResolver) {

  companion object {
    @Throws(XIncluderException::class)
    fun resolveXIncludes(
      document: Document,
      documentUrl: URL,
      documentPath: String,
      resourceResolver: ResourceResolver
    ): Document = XIncluder(resourceResolver).resolveXIncludes(document, documentUrl, documentPath)
  }

  private fun resolveXIncludes(document: Document, documentUrl: URL, documentPath: String): Document {
    val startEntry = XIncludeEntry(documentPath, documentUrl)
    if (isIncludeElement(document.rootElement)) {
      throw XIncluderException(listOf(startEntry), "Invalid root element ${document.rootElement.getElementNameAndAttributes()}")
    }
    val bases = Stack<XIncludeEntry>()
    bases.push(startEntry)
    val rootElement = resolveNonXIncludeElement(document.rootElement, bases)
    return Document(rootElement)
  }

  private fun resolveIncludeOrNonInclude(element: Element, bases: Stack<XIncludeEntry>): List<Content> =
    if (isIncludeElement(element)) {
      resolveXIncludeElements(element, bases)
    } else {
      listOf(resolveNonXIncludeElement(element, bases))
    }

  private fun resolveXIncludeElements(xincludeElement: Element, bases: Stack<XIncludeEntry>): List<Content> {
    val href = xincludeElement.getAttributeValue(HREF)
    val presentableXInclude = xincludeElement.getElementNameAndAttributes()
    if (href.isNullOrEmpty()) {
      throw XIncluderException(bases, "Missing or empty 'href' attribute in $presentableXInclude")
    }

    val parseAttribute = xincludeElement.getAttributeValue(PARSE)
    if (parseAttribute != null && parseAttribute != XML) {
      throw XIncluderException(bases, "Attribute 'parse' must be 'xml' but was '$parseAttribute' in $presentableXInclude")
    }

    val baseAttribute = xincludeElement.getAttributeValue(BASE, Namespace.XML_NAMESPACE)
    val baseUrl = if (baseAttribute != null) {
      try {
        URL(baseAttribute)
      } catch (e: MalformedURLException) {
        throw XIncluderException(bases, "Invalid 'base' attribute: '$baseAttribute' in $presentableXInclude")
      }
    } else {
      bases.peek()!!.documentUrl
    }

    when (val resourceResult = resourceResolver.resolveResource(href, baseUrl)) {
      is ResourceResolver.Result.Found -> {
        val remoteDocument = try {
          JDOMUtil.loadDocument(resourceResult.resourceStream)
        } catch (e: Exception) {
          throw XIncluderException(bases, "Invalid document '$href' referenced in $presentableXInclude", e)
        }

        val xincludeEntry = XIncludeEntry(href, resourceResult.url)
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
      throw XIncluderException(prefix, "Circular includes: " + cycle.joinToString(separator = " -> ") { it.documentPath })
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
      ?: return listOf(remoteRootElement)

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
        ?: throw XIncluderException(bases, "No elements are selected in document '${xincludeEntry.documentPath}' referenced in ${xincludeElement.getElementNameAndAttributes()}")
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