package org.jetbrains.ide.diff.builder.persistence

import org.jetbrains.ide.diff.builder.api.ApiEvent
import org.jetbrains.ide.diff.builder.api.IntroducedIn
import org.jetbrains.ide.diff.builder.api.RemovedIn
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import org.jetbrains.ide.diff.builder.signatures.escapeHtml
import java.io.Closeable
import java.io.Writer

const val AVAILABLE_SINCE_ANNOTATION_NAME = "org.jetbrains.annotations.ApiStatus.AvailableSince"

const val AVAILABLE_UNTIL_ANNOTATION_NAME = "org.jetbrains.annotations.ApiStatus.AvailableUntil"

const val ANNOTATIONS_XML_FILE_NAME = "annotations.xml"

const val BUILD_TXT_FILE_NAME = "build.txt"

/**
 * Utility class used to save API signatures belonging to one package to `annotations.xml`.
 */
internal class ApiXmlWriter(private val writer: Writer) : Closeable {

  fun appendXmlStart() {
    writer.appendln("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    writer.appendln("<root>")
  }

  fun appendSignature(apiSignature: ApiSignature, apiEvent: ApiEvent) {
    val annotationName = when (apiEvent) {
      is IntroducedIn -> AVAILABLE_SINCE_ANNOTATION_NAME
      is RemovedIn -> AVAILABLE_UNTIL_ANNOTATION_NAME
    }

    val ideVersion = when (apiEvent) {
      is IntroducedIn -> apiEvent.ideVersion
      is RemovedIn -> apiEvent.ideVersion
    }

    writer.appendln("""
      |  <item name="${apiSignature.externalPresentation.escapeHtml()}">
      |    <annotation name="$annotationName">
      |      <val name="value" val="&quot;${ideVersion.asStringWithoutProductCode()}&quot;"/>
      |    </annotation>
      |  </item>
    """.trimMargin())
  }

  fun appendXmlEnd() {
    writer.appendln("</root>")
  }

  override fun close() {
    writer.close()
  }
}

