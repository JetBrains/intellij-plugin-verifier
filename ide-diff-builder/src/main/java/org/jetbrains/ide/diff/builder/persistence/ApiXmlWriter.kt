package org.jetbrains.ide.diff.builder.persistence

import org.jetbrains.ide.diff.builder.api.ApiEvent
import org.jetbrains.ide.diff.builder.api.IntroducedIn
import org.jetbrains.ide.diff.builder.api.RemovedIn
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import org.jetbrains.ide.diff.builder.signatures.escapeHtml
import java.io.Closeable
import java.io.Writer

const val AVAILABLE_SINCE_ANNOTATION_NAME = "org.jetbrains.annotations.ApiStatus.AvailableSince"

const val SCHEDULED_FOR_REMOVAL_ANNOTATION_NAME = "org.jetbrains.annotations.ApiStatus.ScheduledForRemoval"

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
      is RemovedIn -> SCHEDULED_FOR_REMOVAL_ANNOTATION_NAME
    }

    val ideVersion = when (apiEvent) {
      is IntroducedIn -> apiEvent.ideVersion
      is RemovedIn -> apiEvent.ideVersion
    }

    with(writer) {
      appendln("""  <item name="${apiSignature.externalPresentation.escapeHtml()}">""")
      appendln("""    <annotation name="$annotationName">""")
      appendln("""      <val name="value" val="&quot;${ideVersion.asStringWithoutProductCode()}&quot;"/>""")
      appendln("""    </annotation>""")
      appendln("""  </item>""")
    }
  }

  fun appendXmlEnd() {
    writer.appendln("</root>")
  }

  override fun close() {
    writer.close()
  }
}

