package com.jetbrains.pluginverifier.tests

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginDependency
import org.apache.commons.io.FileUtils
import org.jetbrains.intellij.plugins.internal.guava.collect.Multimap
import org.jetbrains.intellij.plugins.internal.jdom.Document
import org.jetbrains.intellij.plugins.internal.jdom.Element
import java.io.File

object MockUtil {

  fun createMockPlugin(pluginId: String,
                       pluginVersion: String,
                       moduleDependencies: List<PluginDependency> = emptyList(),
                       dependencies: List<PluginDependency> = emptyList(),
                       definedModules: Set<String> = emptySet()): MockPluginAdapter {
    val tempPluginFile = File.createTempFile("testPlugin", "testPlugin", FileUtils.getTempDirectory())
    tempPluginFile.deleteOnExit()

    return object : MockPluginAdapter() {
      override fun getPluginId(): String = pluginId

      override fun getPluginVersion(): String = pluginVersion

      override fun getModuleDependencies(): List<PluginDependency> = moduleDependencies

      override fun getDependencies(): List<PluginDependency> = dependencies

      override fun getDefinedModules(): Set<String> = definedModules

      override fun getPluginFile(): File = tempPluginFile
    }
  }

  fun createTestIde(ideVersion: IdeVersion, bundledPlugins: List<Plugin>): Ide = object : MockIdeAdapter() {
    override fun getBundledPlugins(): List<Plugin> = bundledPlugins

    override fun getVersion(): IdeVersion = ideVersion
  }

/*
  fun createPlugin() {
    val root = File("for_tests" + File.separator + "testIde")
    val metaInf1 = File(root, "plugins" + File.separator + "pluginOne" + File.separator + "META-INF")
    val metaInf2 = File(root, "plugins" + File.separator + "pluginTwo" + File.separator + "META-INF")

    metaInf1.mkdirs()
    metaInf2.mkdirs()
    File(metaInf1, "plugin.xml").writeText(getPluginXmlContent("pluginOne"))
    File(metaInf2, "plugin.xml").writeText(getPluginXmlContent("pluginTwo"))
    val buildXml = File(root, "build.txt")
    buildXml.parentFile.mkdirs()
    buildXml.writeText(ideVersion)
    return IdeManager.getInstance().createIde(root)
  }

  private fun getPluginXmlContent(id: String): String = """<idea-plugin version="2">
    <id>$id</id>
    <name>$id</name>
    <version>1.0</version>
    <vendor email="no.com" url="http://www.no.com">vendor</vendor>
    <description><![CDATA[]]></description>
    <idea-version since-build="131"/>
</idea-plugin>
"""
*/

  open class MockIdeAdapter : Ide() {
    override fun getCustomPlugins(): List<Plugin> = emptyList()

    override fun getBundledPlugins(): List<Plugin> {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getIdePath(): File {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getVersion(): IdeVersion {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getExpandedIde(plugin: Plugin?): Ide {
      throw UnsupportedOperationException("not implemented")
    }

  }

  open class MockPluginAdapter : Plugin {
    override fun getPluginId(): String {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getPluginVersion(): String {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getModuleDependencies(): List<PluginDependency>? {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getVendor(): String {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getChangeNotes(): String {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getVendorLogo(): ByteArray {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getUnderlyingDocument(): Document {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getAllClassesReferencedFromXml(): MutableSet<String>? {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getExtensions(): Multimap<String, Element> {
      throw UnsupportedOperationException("not implemented")
    }

    override fun isCompatibleWithIde(ideVersion: IdeVersion?): Boolean {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getDescription(): String {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getUrl(): String {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getVendorEmail(): String {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getVendorUrl(): String {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getVendorLogoUrl(): String {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getPluginName(): String {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getUntilBuild(): IdeVersion {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getSinceBuild(): IdeVersion {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getPluginFile(): File {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getDefinedModules(): Set<String>? {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getDependencies(): List<PluginDependency>? {
      throw UnsupportedOperationException("not implemented")
    }

    override fun getOptionalDescriptors(): MutableMap<String, Plugin>? {
      throw UnsupportedOperationException("not implemented")
    }

  }
}
