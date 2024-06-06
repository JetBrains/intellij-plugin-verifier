package com.jetbrains.plugin.structure.xinclude

import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.readText
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import com.jetbrains.plugin.structure.intellij.xinclude.XIncluder
import com.jetbrains.plugin.structure.intellij.xinclude.XIncluderException
import org.jdom2.Document
import org.jdom2.JDOMException
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

class XIncluderTest {

  private val testDataDirectory = Paths.get(XIncluderTest::class.java.getResource("/xinclude").toURI())

  private val resourceResolver = DefaultResourceResolver

  @Throws(JDOMException::class, IOException::class, URISyntaxException::class)
  private fun loadDocument(url: URL): Document =
    url.openStream().use { stream -> JDOMUtil.loadDocument(stream) }
  
  private fun checkSuccessfullyResolved(testXml: Path, expectedXml: Path) {
    val testUrl = testXml.toUri().toURL()
    val testDocument = loadDocument(testUrl)
    val expectedDocument = loadDocument(expectedXml.toUri().toURL())

    val resolvedDocument = XIncluder.resolveXIncludes(testDocument, testXml.simpleName, resourceResolver, testXml)

    val xmlOutputter = XMLOutputter(Format.getPrettyFormat())
    val expectedString = xmlOutputter.outputString(expectedDocument)
    val resultString = xmlOutputter.outputString(resolvedDocument)
    Assert.assertEquals(expectedString, resultString)
  }

  private fun testSuccess(testName: String) {
    val testBase = resolveTestBase(testName, true)
    checkSuccessfullyResolved(testBase.resolve("test.xml"), testBase.resolve("expected.xml"))
  }

  private fun testError(testName: String) {
    val testBase = resolveTestBase(testName, false)

    val testXml = testBase.resolve("test.xml")
    val errorText = testBase.resolve("expectedError.txt").readText()

    val testUrl = testXml.toUri().toURL()
    val testDocument = loadDocument(testUrl)
    try {
      XIncluder.resolveXIncludes(testDocument, testXml.simpleName, resourceResolver, testXml)
    } catch (e: XIncluderException) {
      Assert.assertEquals(errorText, e.message)
      return
    }
    Assert.fail("XIncluderException is not thrown as expected")
  }

  private fun resolveTestBase(testName: String, success: Boolean): Path {
    val testBase = testDataDirectory.resolve(if (success) "success" else "error").resolve(testName)
    check(testBase.isDirectory) { "$testBase does not exist" }
    return testBase
  }

  @Test
  fun `one x include`() {
    testSuccess("onePart")
  }

  @Test
  fun `two x includes`() {
    testSuccess("twoParts")
  }

  @Test
  fun `select children`() {
    testSuccess("selector")
  }

  @Test
  fun `fallback element provided`() {
    testSuccess("fallbackProvided")
  }

  @Test
  fun `if no xpointer is specified then include all the children`() {
    testSuccess("emptyXPointer")
  }

  @Test
  fun `cycle error`() {
    testError("cycle")
  }

  @Test
  fun `no fallback provided error`() {
    testError("noFallback")
  }

  @Test
  fun `invalid referenced document error`() {
    testError("invalidDocument")
  }

  @Test
  fun `nothing selected error`() {
    testError("nothingSelected")
  }

  @Test
  fun `includeIf variable is set`() {
    withConditionalXIncludes {
      val property = "xinclude.enabled"
      try {
        System.setProperty(property, true.toString())
        testSuccess("includeIf")
      } finally {
        System.clearProperty(property)
      }
    }
  }

  @Test
  fun `includeIf variable is not set`() {
    withConditionalXIncludes {
      testSuccess("includeIfWithUnsetProperty")
    }
  }

  @Test
  fun `includeUnless variable is set`() {
    withConditionalXIncludes {
      testSuccess("includeUnless")
    }
  }

  @Test
  fun `includeUnless with variable that is set`() {
    withConditionalXIncludes {
      val property = "xinclude.disabled"
      try {
        System.setProperty(property, true.toString())
        testSuccess("includeUnlessWithSetProperty")
      } finally {
        System.clearProperty(property)
      }
    }
  }

  @Test
  fun `include in META-INF`() {
    testSuccess("metaInfResolveInResourceRoot/META-INF")
  }

  @Test
  fun `include document in META-INF from resource root`() {
    testSuccess("resourceRootResolveInMetaInf")
  }

  @Test
  fun `include document in META-INF from resource root and from that include a file in the META-INF`() {
    testSuccess("metaInfResolveInResourceRootAndBack/META-INF")
  }

}