/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.extractor.PluginBeanExtractor
import com.jetbrains.plugin.structure.intellij.plugin.PluginDescriptorParser.ParseResult.InvalidBean
import com.jetbrains.plugin.structure.intellij.plugin.PluginDescriptorParser.ParseResult.Parsed
import com.jetbrains.plugin.structure.intellij.problems.XIncludeResolutionErrors
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.xinclude.XIncluder
import com.jetbrains.plugin.structure.intellij.xinclude.XIncluderException
import org.jdom2.Document
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(PluginDescriptorParser::class.java)

class PluginDescriptorParser {
  fun parse(
    descriptorPath: String,
    pluginFileName: String,
    originalDocument: Document,
    documentPath: Path,
    documentName: String,
    pathResolver: ResourceResolver,
    validationContext: ValidationContext
  ): ParseResult {
    val document = resolveXIncludesOfDocument(
      descriptorPath,
      pluginFileName,
      originalDocument,
      documentName,
      pathResolver,
      documentPath,
      validationContext
    ) ?: return ParseResult.InvalidXml
    return readDocumentIntoXmlBean(descriptorPath, pluginFileName, document, validationContext)
  }

  private fun readDocumentIntoXmlBean(
    descriptorPath: String,
    pluginFileName: String,
    document: Document,
    validationContext: ValidationContext
  ): ParseResult {
    return try {
      val bean = PluginBeanExtractor.extractPluginBean(document)
      Parsed(document, bean)
    } catch (e: Exception) {
      validationContext += UnableToReadDescriptor(descriptorPath, e.localizedMessage)
      LOG.info("Unable to read plugin descriptor $descriptorPath of $pluginFileName", e)
      InvalidBean(document)
    }
  }

  private fun resolveXIncludesOfDocument(
    descriptorPath: String,
    pluginFileName: String,
    document: Document,
    presentablePath: String,
    pathResolver: ResourceResolver,
    documentPath: Path,
    validationContext: ValidationContext
  ): Document? = try {
    XIncluder.resolveXIncludes(document, presentablePath, pathResolver, documentPath)
  } catch (e: XIncluderException) {
    LOG.info("Unable to resolve <xi:include> elements of descriptor '$descriptorPath' from '$pluginFileName'", e)
    validationContext += XIncludeResolutionErrors(descriptorPath, e.message)
    null
  }

  sealed class ParseResult {
    object InvalidXml: ParseResult()
    data class InvalidBean(val document: Document) : ParseResult()
    data class Parsed(val document: Document, val bean: PluginBean) : ParseResult()
  }
}