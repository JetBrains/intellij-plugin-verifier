package org.jetbrains.ide.diff.builder.persistence

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import org.jetbrains.ide.diff.builder.signatures.escapeHtml
import java.io.Closeable
import java.io.Writer

const val AVAILABLE_SINCE_ANNOTATION_NAME = "org.jetbrains.annotations.ApiStatus.AvailableSince"

const val ANNOTATIONS_XML_FILE_NAME = "annotations.xml"

/**
 * Utility class used to save "available since" information
 * as external annotations for a set of [ApiSignature]s
 * corresponding to one package into "annotations.xml" file
 * handled by [writer].
 */
internal class SinceApiXmlWriter(private val writer: Writer) : Closeable {

  fun appendXmlStart() {
    writer.appendln("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    writer.appendln("<root>")
  }

  fun appendSignatureSince(apiSignature: ApiSignature, sinceVersion: IdeVersion) {
    val itemFormat = apiSignature.externalPresentation
    writer.appendln("""
      |  <item name="${itemFormat.escapeHtml()}">
      |    <annotation name="$AVAILABLE_SINCE_ANNOTATION_NAME">
      |      <val name="value" val="&quot;${sinceVersion.asStringWithoutProductCode()}&quot;"/>
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

