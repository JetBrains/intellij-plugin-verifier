package com.jetbrains.pluginverifier.tests

import com.google.common.collect.Multimap
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.io.File

object MockUtil {

  fun createMockPlugin(pluginId: String,
                       pluginVersion: String,
                       dependencies: List<PluginDependency>,
                       definedModules: Set<String>): MockPluginAdapter {

    return object : MockPluginAdapter() {
      override val pluginId: String? = pluginId

      override val pluginVersion: String = pluginVersion

      override fun getDependencies(): List<PluginDependency> = dependencies

      override fun getDefinedModules(): Set<String> = definedModules

      override fun getOriginalFile(): File? = null
    }
  }

  fun createMockIde(ideVersion: IdeVersion, bundledPlugins: List<IdePlugin>): Ide = object : MockIdeAdapter(ideVersion, bundledPlugins) {
    override fun getBundledPlugins(): List<IdePlugin> = bundledPlugins

    override fun getVersion(): IdeVersion = ideVersion
  }

}

open class MockIdeAdapter(val ideVersion: IdeVersion,
                          private val bundledPlugins: List<IdePlugin> = emptyList(),
                          private var customPlugins: List<IdePlugin> = emptyList()) : Ide() {

  override fun getCustomPlugins(): List<IdePlugin> = customPlugins

  override fun getBundledPlugins(): List<IdePlugin> = bundledPlugins

  override fun getIdePath(): File {
    throw UnsupportedOperationException("not implemented")
  }

  override fun getVersion(): IdeVersion = ideVersion

  override fun getExpandedIde(plugin: IdePlugin): Ide {
    val newPlugins = ArrayList(customPlugins)
    newPlugins.add(plugin)
    return MockIdeAdapter(ideVersion, newPlugins)
  }

}

open class MockPluginAdapter : IdePlugin {
  override fun getUnderlyingDocument(): Document = throw UnsupportedOperationException()

  override fun getAllClassesReferencedFromXml(): Set<String> {
    return emptySet()
  }

  override fun getOptionalDescriptors(): Map<String, IdePlugin> {
    return emptyMap()
  }

  override fun getExtensions(): Multimap<String, Element> {
    throw UnsupportedOperationException()
  }

  override fun isCompatibleWithIde(ideVersion: IdeVersion): Boolean {
    throw UnsupportedOperationException()
  }

  override fun getUntilBuild(): IdeVersion? {
    throw UnsupportedOperationException()
  }

  override fun getSinceBuild(): IdeVersion? {
    throw UnsupportedOperationException()
  }

  override fun getDefinedModules(): Set<String> {
    throw UnsupportedOperationException()
  }

  override fun getOriginalFile(): File? {
    throw UnsupportedOperationException()
  }

  override fun getDependencies(): List<PluginDependency> {
    throw UnsupportedOperationException()
  }


  override val changeNotes: String?
    get() = throw UnsupportedOperationException()
  override val description: String?
    get() = throw UnsupportedOperationException()
  override val pluginId: String?
    get() = throw UnsupportedOperationException()
  override val pluginName: String?
    get() = throw UnsupportedOperationException()
  override val pluginVersion: String?
    get() = throw UnsupportedOperationException()
  override val url: String?
    get() = throw UnsupportedOperationException()
  override val vendor: String?
    get() = throw UnsupportedOperationException()
  override val vendorEmail: String?
    get() = throw UnsupportedOperationException()
  override val vendorUrl: String?
    get() = throw UnsupportedOperationException()


}