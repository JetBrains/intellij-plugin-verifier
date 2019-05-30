package com.jetbrains.plugin.structure.intellij.plugin

import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.io.File
import java.time.LocalDate
import java.util.*

class IdePluginImpl internal constructor(
    private val underlyingDocument: Document,
    bean: PluginBean
) : IdePlugin {

  companion object {
    private const val INTELLIJ_MODULES_PREFIX = "com.intellij.modules."
  }

  private val definedModules = hashSetOf<String>()
  private val dependencies = arrayListOf<PluginDependency>()
  private val declaredThemes = arrayListOf<IdeTheme>()
  private val optionalConfigFiles = hashMapOf<PluginDependency, String>()
  private val optionalDescriptors = hashMapOf<String, IdePlugin>()
  private val _icons = arrayListOf<PluginIcon>()
  private var extensions: Multimap<String, Element>? = null
  private var originalFile: File? = null

  var extractDirectory: File? = null

  override val icons: List<PluginIcon>
    get() = Collections.unmodifiableList(_icons)

  override var pluginName: String? = null
    private set

  override var pluginVersion: String? = null
    private set

  override var pluginId: String? = null
    private set

  override var vendor: String? = null
    private set

  override var vendorEmail: String? = null
    private set

  override var vendorUrl: String? = null
    private set

  override var description: String? = null
    private set

  override var url: String? = null
    private set

  override var changeNotes: String? = null
    private set

  private var sinceBuild: IdeVersion? = null
  private var untilBuild: IdeVersion? = null
  private var productDescriptor: ProductDescriptor? = null

  internal val optionalDependenciesConfigFiles: Map<PluginDependency, String>
    get() = Collections.unmodifiableMap(optionalConfigFiles)

  init {
    setInfoFromBean(bean)
  }

  override fun getExtensions(): Multimap<String, Element> =
      Multimaps.unmodifiableMultimap(extensions!!)

  override fun getDependencies(): List<PluginDependency> =
      Collections.unmodifiableList(dependencies)

  override fun getSinceBuild(): IdeVersion? =
      sinceBuild

  override fun getUntilBuild(): IdeVersion? =
      untilBuild

  override fun isCompatibleWithIde(ideVersion: IdeVersion): Boolean =
      if (sinceBuild == null) {
        true
      } else {
        sinceBuild!! <= ideVersion && (untilBuild == null || ideVersion <= untilBuild!!)
      }

  override fun getDefinedModules(): Set<String> = Collections.unmodifiableSet(definedModules)

  override fun getDeclaredThemes(): List<IdeTheme> = declaredThemes

  fun setDeclaredThemes(declaredThemes: List<IdeTheme>) {
    this.declaredThemes.clear()
    this.declaredThemes.addAll(declaredThemes)
  }

  private fun setInfoFromBean(bean: PluginBean) {
    pluginName = bean.name
    pluginId = if (bean.id != null) bean.id else bean.name
    url = bean.url
    pluginVersion = if (bean.pluginVersion != null) bean.pluginVersion.trim { it <= ' ' } else null
    definedModules.addAll(bean.modules)
    extensions = bean.extensions

    val ideaVersionBean = bean.ideaVersion
    if (ideaVersionBean != null) {
      sinceBuild = if (ideaVersionBean.sinceBuild != null) IdeVersion.createIdeVersion(ideaVersionBean.sinceBuild) else null
      var untilBuild: String? = ideaVersionBean.untilBuild
      if (untilBuild != null && untilBuild.isNotEmpty()) {
        if (untilBuild.endsWith(".*")) {
          val idx = untilBuild.lastIndexOf('.')
          untilBuild = untilBuild.substring(0, idx + 1) + Integer.MAX_VALUE
        }
        this.untilBuild = IdeVersion.createIdeVersion(untilBuild)
      }
    }

    if (bean.dependencies != null) {
      for (dependencyBean in bean.dependencies) {
        if (dependencyBean.dependencyId != null) {
          val isModule = dependencyBean.dependencyId.startsWith(INTELLIJ_MODULES_PREFIX)
          val isOptional = java.lang.Boolean.TRUE == dependencyBean.optional
          val dependency = PluginDependencyImpl(dependencyBean.dependencyId, isOptional, isModule)
          dependencies.add(dependency)

          if (dependency.isOptional && dependencyBean.configFile != null) {
            optionalConfigFiles[dependency] = dependencyBean.configFile
          }
        }
      }
    }

    val vendorBean = bean.vendor
    if (vendorBean != null) {
      vendor = if (vendorBean.name != null) vendorBean.name.trim { it <= ' ' } else null
      vendorUrl = vendorBean.url
      vendorEmail = vendorBean.email
    }
    val productDescriptorBean = bean.productDescriptor
    if (productDescriptorBean != null) {

      productDescriptor = ProductDescriptor(
          productDescriptorBean.code,
          LocalDate.parse(productDescriptorBean.releaseDate, PluginCreator.releaseDateFormatter),
          Integer.parseInt(productDescriptorBean.releaseVersion)
      )
    }
    changeNotes = bean.changeNotes
    description = bean.description
  }

  override fun getOptionalDescriptors(): Map<String, IdePlugin> =
      Collections.unmodifiableMap(optionalDescriptors)

  fun setIcons(icons: List<PluginIcon>) {
    this._icons.clear()
    this._icons.addAll(icons)
  }

  internal fun addOptionalDescriptor(configurationFile: String, optionalPlugin: IdePlugin) {
    optionalDescriptors[configurationFile] = optionalPlugin
    extensions!!.putAll(optionalPlugin.extensions)
  }

  override fun getUnderlyingDocument(): Document = underlyingDocument.clone()

  override fun getOriginalFile(): File? = originalFile

  internal fun setOriginalPluginFile(originalFile: File) {
    this.originalFile = originalFile
  }

  override fun getProductDescriptor(): ProductDescriptor? {
    return productDescriptor
  }

  override fun toString(): String {
    var id = pluginId
    if (id == null || id.isEmpty()) {
      id = pluginName
    }
    if (id == null || id.isEmpty()) {
      id = url
    }
    return id!! + if (pluginVersion != null) ":" + pluginVersion!! else ""
  }
}
