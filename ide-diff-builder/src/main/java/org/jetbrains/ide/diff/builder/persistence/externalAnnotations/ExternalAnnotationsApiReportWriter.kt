/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.persistence.externalAnnotations

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation
import com.jetbrains.pluginverifier.results.presentation.toFullJavaClassName
import com.jetbrains.pluginverifier.results.signatures.FormatOptions
import com.jetbrains.pluginverifier.results.signatures.SigVisitor
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.ide.diff.builder.api.*
import org.jetbrains.ide.diff.builder.persistence.ApiReportWriter
import org.objectweb.asm.signature.SignatureReader
import java.io.Closeable
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Utility class used to save [ApiReport] to external annotations roots.
 *
 * Creates necessary package-like directory structure, for example
 * ```
 * build.txt
 * org/
 * org/some/
 * org/some/annotations.xml
 * org/some/util/
 * org/some/util/annotations.xml
 * ```
 */
class ExternalAnnotationsApiReportWriter : ApiReportWriter {

  override fun saveReport(apiReport: ApiReport, reportPath: Path) {
    require(reportPath.extension == "" || reportPath.extension == "zip") {
      "Only directory or .zip roots are supported"
    }

    val saveZip = reportPath.extension == "zip"

    val rootDirectory = if (saveZip) {
      reportPath.resolveSibling(reportPath.simpleName + ".dir")
    } else {
      reportPath
    }

    rootDirectory.deleteLogged()
    rootDirectory.createDir()

    rootDirectory.resolve(BUILD_TXT_FILE_NAME).writeText(apiReport.ideBuildNumber.asStringWithoutProductCode())

    try {
      doSaveReport(rootDirectory, apiReport)
    } catch (e: Exception) {
      rootDirectory.deleteLogged()
      throw e
    }

    if (saveZip) {
      try {
        rootDirectory.archiveDirectoryTo(reportPath)
      } finally {
        rootDirectory.deleteLogged()
      }
    }
  }

  private fun doSaveReport(rootDirectory: Path, apiReport: ApiReport) {
    val maxOpenFiles = 50
    val packageWriters = object : LinkedHashMap<String, ExternalAnnotationsXmlWriter>() {
      override fun removeEldestEntry(eldest: Map.Entry<String, ExternalAnnotationsXmlWriter>): Boolean {
        if (size > maxOpenFiles) {
          val xmlWriter = eldest.value
          xmlWriter.closeLogged()
          return true
        }
        return false
      }
    }

    fun resolveXmlFile(packageName: String): Path {
      val packageRoot = rootDirectory.resolve(packageName.replace('.', '/'))
      val annotationsFile = packageRoot.resolve(ANNOTATIONS_XML_FILE_NAME)
      if (!annotationsFile.exists()) {
        annotationsFile.parent.createDir()
      }
      return annotationsFile
    }

    fun getXmlWriter(packageName: String): ExternalAnnotationsXmlWriter {
      val openedWriter = packageWriters[packageName]
      if (openedWriter != null) {
        return openedWriter
      }
      val xmlFile = resolveXmlFile(packageName)
      val needStart = !xmlFile.exists() || Files.size(xmlFile) == 0L
      val xmlWriter = ExternalAnnotationsXmlWriter(
        Files.newBufferedWriter(
          xmlFile,
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE,
          StandardOpenOption.APPEND
        )
      )
      if (needStart) {
        xmlWriter.appendXmlStart()
      }
      packageWriters[packageName] = xmlWriter
      return xmlWriter
    }

    val allPackages = hashSetOf<String>()

    //Ensures that .xml file does not contain two equivalent signatures associated with one annotation type.
    // Duplicate entries in .xml files lead to exceptions in ExternalAnnotationsManager (IDEA).
    val processedSignatures = hashSetOf<Pair<String, Class<ApiEvent>>>()

    for ((apiSignature, apiEvent) in apiReport.asSequence()) {
      val packageName = apiSignature.javaPackageName
      allPackages += packageName
      if (processedSignatures.add(apiSignature.externalPresentation to apiEvent.javaClass)) {
        getXmlWriter(packageName).appendSignature(apiSignature, apiEvent)
      }
    }

    try {
      for (packageName in allPackages) {
        val xmlWriter = getXmlWriter(packageName)
        xmlWriter.appendXmlEnd()
      }
    } finally {
      for (xmlWriter in packageWriters.values) {
        xmlWriter.closeLogged()
      }
      packageWriters.clear()
    }
  }

}

sealed class ApiEventAnnotation(val annotationName: String, val valueName: String)

object AvailableSinceAnnotation : ApiEventAnnotation("org.jetbrains.annotations.ApiStatus.AvailableSince", "value")

object ScheduledForRemovalAnnotation : ApiEventAnnotation("org.jetbrains.annotations.ApiStatus.ScheduledForRemoval", "inVersion")

object DeprecatedSinceAnnotation : ApiEventAnnotation("org.jetbrains.idea.devkit.inspections.missingApi.DeprecatedSince", "sinceVersion")

const val ANNOTATIONS_XML_FILE_NAME = "annotations.xml"

const val BUILD_TXT_FILE_NAME = "build.txt"

/**
 * Utility class used to save API signatures belonging to one package to `annotations.xml`.
 */
private class ExternalAnnotationsXmlWriter(private val writer: Writer) : Closeable {

  fun appendXmlStart() {
    writer.appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    writer.appendLine("<root>")
  }

  fun appendSignature(apiSignature: ApiSignature, apiEvent: ApiEvent) {
    val apiEventAnnotation = when (apiEvent) {
      is IntroducedIn -> AvailableSinceAnnotation
      is RemovedIn -> ScheduledForRemovalAnnotation
      is MarkedDeprecatedIn -> DeprecatedSinceAnnotation
      else -> return
    }

    val ideVersion = when (apiEvent) {
      is IntroducedIn -> apiEvent.ideVersion
      is RemovedIn -> apiEvent.ideVersion
      is MarkedDeprecatedIn -> apiEvent.ideVersion
      else -> return
    }

    with(writer) {
      appendLine("""  <item name="${StringEscapeUtils.escapeHtml4(apiSignature.externalPresentation)}">""")
      appendLine("""    <annotation name="${apiEventAnnotation.annotationName}">""")
      appendLine("""      <val name="${apiEventAnnotation.valueName}" val="&quot;${ideVersion.asStringWithoutProductCode()}&quot;"/>""")
      appendLine("""    </annotation>""")
      appendLine("""  </item>""")
    }
  }

  fun appendXmlEnd() {
    writer.appendLine("</root>")
  }

  override fun close() {
    writer.close()
  }
}

/**
 * Converts internal presentation of API to string presentation
 * expected by external annotations system.
 * In IDEA codebase the same conversion from PSI element to string is done
 * in `com.intellij.psi.util.PsiFormatUtil.getExternalName`.
 *
 * Examples of external names:
 * ```
 * pkg.A
 * pkg.A A()
 * pkg.A A(java.lang.String)
 * pkg.A void m1()
 * pkg.A void m2(java.util.List<java.lang.String>)
 * pkg.A T m3()
 * pkg.A T m4(java.util.Map<java.lang.String,java.lang.Object>)
 * pkg.A field
 * ```
 * For more conversion examples refer to unit tests of this method.
 */
val ApiSignature.externalPresentation: String
  get() = when (this) {
    is ClassSignature -> externalPresentation
    is MethodSignature -> externalPresentation
    is FieldSignature -> externalPresentation
  }

val ApiSignature.javaPackageName: String
  get() = when (this) {
    is ClassSignature -> className.substringBeforeLast("/", "").replace('/', '.')
    is MethodSignature -> hostSignature.javaPackageName
    is FieldSignature -> hostSignature.javaPackageName
  }

private val ClassSignature.externalPresentation: String
  get() = toFullJavaClassName(className)

private val MethodSignature.externalPresentation: String
  get() {
    val (name, paramTypes, returnType) = convertMethodSignature()
    return buildString {
      append(hostSignature.externalPresentation)
      append(" ")
      if (this@externalPresentation.methodName != "<init>") {
        append("$returnType ")
      }
      append(name)
      append("($paramTypes)")
    }
  }

private val FieldSignature.externalPresentation: String
  get() = hostSignature.externalPresentation + " $fieldName"

private fun MethodSignature.convertMethodSignature(): Triple<String, String, String> {
  val outerClassSignature = hostSignature.containingClassSignature
  val (rawParamDescriptors, rawReturnDescriptor) = JvmDescriptorsPresentation.splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
  val rawParamTypes = rawParamDescriptors.map { JvmDescriptorsPresentation.convertJvmDescriptorToNormalPresentation(it, toFullJavaClassName) }
  val rawReturnType = JvmDescriptorsPresentation.convertJvmDescriptorToNormalPresentation(rawReturnDescriptor, toFullJavaClassName)

  val isConstructor = methodName == "<init>"
  val mustDropFirstParameter = isConstructor && outerClassSignature != null && rawParamTypes.firstOrNull() == toFullJavaClassName(outerClassSignature.className)
  val dropParamsNumber = if (mustDropFirstParameter) 1 else 0

  val name = if (isConstructor) {
    hostSignature.className.substringAfterLast('/').substringAfterLast('$')
  } else {
    methodName
  }

  return if (signature.isNullOrEmpty()) {
    Triple(
      name,
      rawParamTypes.drop(dropParamsNumber).joinToString(),
      rawReturnType
    )
  } else {
    val methodSignature = SigVisitor().also { SignatureReader(signature).accept(it) }.getMethodSignature()
    val formatOptions = FormatOptions(
      isInterface = false,
      formalTypeParameters = false,
      formalTypeParametersBounds = false,
      methodThrows = false,
      internalNameConverter = toFullJavaClassName,

      typeArguments = true,
      superClass = true,
      superInterfaces = true,

      /**
       * Type arguments in method signature parameters and return type
       * are not separated with ", " but with ",".
       * This is due to "canonical" presentation of those parameters
       * used in [com.intellij.psi.util.PsiFormatUtil#formatType].
       */

      /**
       * Type arguments in method signature parameters and return type
       * are not separated with ", " but with ",".
       * This is due to "canonical" presentation of those parameters
       * used in [com.intellij.psi.util.PsiFormatUtil#formatType].
       */
      typeArgumentsSeparator = ","
    )
    Triple(
      name,
      methodSignature.parameterSignatures.drop(dropParamsNumber).joinToString { it.format(formatOptions) },
      methodSignature.result.format(formatOptions)
    )
  }
}