package com.jetbrains.plugin.structure.xinclude

import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import com.jetbrains.plugin.structure.intellij.xinclude.XIncluder
import com.jetbrains.plugin.structure.intellij.xinclude.XIncluderException
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.junit.Assert
import org.junit.Test
import java.io.File

class XIncluderTest {

  private val testDataDirectory = URLUtil.urlToFile(XIncluderTest::class.java.getResource("/xinclude"))

  private val resourceResolver = DefaultResourceResolver

  private fun checkSuccessfullyResolved(testXml: File, expectedXml: File) {
    val testUrl = testXml.toURI().toURL()
    val testDocument = JDOMUtil.loadDocument(testUrl)
    val expectedDocument = JDOMUtil.loadDocument(expectedXml.toURI().toURL())

    val resolvedDocument = XIncluder.resolveXIncludes(testDocument, testUrl, testXml.name, resourceResolver)

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

    val testUrl = testXml.toURI().toURL()
    val testDocument = JDOMUtil.loadDocument(testUrl)
    try {
      XIncluder.resolveXIncludes(testDocument, testUrl, testXml.name, resourceResolver)
    } catch (e: XIncluderException) {
      Assert.assertEquals(errorText, e.message)
      return
    }
    Assert.fail("XIncluderException is not thrown as expected")
  }

  private fun resolveTestBase(testName: String, success: Boolean): File {
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

}