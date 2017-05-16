package com.jetbrains.pluginverifier.tests

import com.intellij.structure.ide.IdeVersion
import org.jetbrains.intellij.plugins.internal.guava.collect.Multimap
import org.jetbrains.intellij.plugins.internal.jdom.Document
import org.jetbrains.intellij.plugins.internal.jdom.Element
import java.io.File
import java.nio.file.Files

object MockUtil {

  fun createMockPlugin(pluginId: String,
                       pluginVersion: String,
                       moduleDependencies: List<PluginDependency> = emptyList(),
                       dependencies: List<PluginDependency> = emptyList(),
                       definedModules: Set<String> = emptySet()): MockPluginAdapter {
    val tempPluginFile = Files.createTempDirectory("mockPlugin").toFile()
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

  fun createMockIde(ideVersion: IdeVersion, bundledPlugins: List<Plugin>): Ide = object : MockIdeAdapter(ideVersion, bundledPlugins) {
    override fun getBundledPlugins(): List<Plugin> = bundledPlugins

    override fun getVersion(): IdeVersion = ideVersion
  }

}

open class MockIdeAdapter(val ideVersion: IdeVersion,
                          private val bundledPlugins: List<Plugin> = emptyList(),
                          private var customPlugins: List<Plugin> = emptyList()) : Ide() {

  override fun getCustomPlugins(): List<Plugin> = customPlugins

  override fun getBundledPlugins(): List<Plugin> = bundledPlugins

  override fun getIdePath(): File {
    throw UnsupportedOperationException("not implemented")
  }

  override fun getVersion(): IdeVersion = ideVersion

  override fun getExpandedIde(plugin: Plugin): Ide {
    val newPlugins = ArrayList(customPlugins)
    newPlugins.add(plugin)
    return MockIdeAdapter(ideVersion, newPlugins)
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

  override fun getAllClassesReferencedFromXml(): Set<String>? {
    return emptySet()
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

  override fun getOptionalDescriptors(): Map<String, Plugin>? {
    return emptyMap()
  }

}