package com.jetbrains.pluginverifier.dymamic

import com.jetbrains.plugin.structure.intellij.plugin.ExtensionPoint
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.warnings.DynamicPluginStatus
import org.jdom2.Element

/**
 * Utility methods that determine whether a plugin can be dynamically loaded/unloaded [DynamicPluginStatus].
 */
object DynamicPlugins {
  fun getDynamicPluginStatus(context: PluginVerificationContext): DynamicPluginStatus? {
    val verificationDescriptor = context.verificationDescriptor
    val idePlugin = context.idePlugin
    if (verificationDescriptor is PluginVerificationDescriptor.IDE && idePlugin is IdePluginImpl) {
      val reasonsNotToLoadUnloadImmediately = hashSetOf<String>()
      val reasonsNotToLoadUnloadWithoutRestart = hashSetOf<String>()

      val componentReasons = listOf(
        idePlugin.appContainerDescriptor to "application",
        idePlugin.projectContainerDescriptor to "project",
        idePlugin.moduleContainerDescriptor to "module"
      )
        .filter { it.first.components.isNotEmpty() }
        .map { (descriptor, area) ->
          "Plugin declares $area components: " + descriptor.components.map { it.implementationClass }.sorted().joinToString()
        }

      reasonsNotToLoadUnloadImmediately += componentReasons
      reasonsNotToLoadUnloadWithoutRestart += componentReasons

      val allowedImmediateLoadUnloadAllowedExtensions = listOf(
        "com.intellij.themeProvider",
        "com.intellij.bundledKeymap",
        "com.intellij.bundledKeymapProvider"
      )

      val declaredExtensions = idePlugin.extensions.keys
      val nonImmediateEps = declaredExtensions.filter { it !in allowedImmediateLoadUnloadAllowedExtensions }
      if (nonImmediateEps.isNotEmpty()) {
        reasonsNotToLoadUnloadImmediately += "Plugin cannot be loaded/unloaded immediately. " +
          "Only extension points " + allowedImmediateLoadUnloadAllowedExtensions.sorted().joinToString() + " support immediate loading/unloading, " +
          "but the plugin declares " + nonImmediateEps.sorted().joinToString()
      }

      val ide = verificationDescriptor.ide

      val nonDynamicExtensions = arrayListOf<String>()
      for (epName in declaredExtensions) {
        val extensionPoint = ide.bundledPlugins.asSequence()
            .filterIsInstance<IdePluginImpl>()
            .mapNotNull { it.findExtensionPoint(epName) }
            .firstOrNull()
        if (extensionPoint != null && !extensionPoint.isDynamic) {
          nonDynamicExtensions += extensionPoint.extensionPointName
        }
      }
      if (nonDynamicExtensions.isNotEmpty()) {
        reasonsNotToLoadUnloadWithoutRestart += "Plugin declares non-dynamic extensions: " + nonDynamicExtensions.sorted().joinToString()
      }


      val allActionsAndGroups = getAllActionsAndGroupsRecursively(idePlugin)
      if (allActionsAndGroups.isNotEmpty()) {
        reasonsNotToLoadUnloadImmediately += "Plugin cannot be loaded/unloaded immediately because it declares actions or groups"
      }

      for (element in allActionsAndGroups) {
        if (element.name == "group" && element.getAttributeValue("id") == null) {
          reasonsNotToLoadUnloadWithoutRestart += "Plugin cannot be loaded/unloaded without IDE restart because it declares a group without 'id' specified"
          break
        }
      }

      for (element in allActionsAndGroups) {
        if (element.name == "action" && element.getAttributeValue("id") == null && element.getAttributeValue("class") == null) {
          reasonsNotToLoadUnloadWithoutRestart += "Plugin cannot be loaded/unloaded without IDE restart because it declares an action with neither 'id' nor 'class' specified"
          break
        }
      }

      if (reasonsNotToLoadUnloadImmediately.isEmpty()) {
        return DynamicPluginStatus.AllowLoadUnloadImmediately
      }

      if (reasonsNotToLoadUnloadWithoutRestart.isEmpty()) {
        return DynamicPluginStatus.AllowLoadUnloadWithoutRestart(reasonsNotToLoadUnloadImmediately)
      }

      return DynamicPluginStatus.NotDynamic(reasonsNotToLoadUnloadImmediately, reasonsNotToLoadUnloadWithoutRestart)
    }

    return null
  }

  private fun getAllActionsAndGroupsRecursively(idePlugin: IdePluginImpl): List<Element> {
    val result = arrayListOf<Element>()
    fun recursive(element: Element) {
      result += element
      if (element.name == "group") {
        for (child in element.children) {
          recursive(child)
        }
      }
    }
    idePlugin.actions.forEach(::recursive)
    return result
  }

  private fun IdePluginImpl.findExtensionPoint(epName: String): ExtensionPoint? {
    for (descriptor in listOf(appContainerDescriptor, projectContainerDescriptor, moduleContainerDescriptor)) {
      val extensionPoint = descriptor.extensionPoints.find { it.extensionPointName == epName }
      if (extensionPoint != null) {
        return extensionPoint
      }
    }
    return null
  }

}