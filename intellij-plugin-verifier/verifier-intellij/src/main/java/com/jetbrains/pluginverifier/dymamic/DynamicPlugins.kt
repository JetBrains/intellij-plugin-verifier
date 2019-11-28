package com.jetbrains.pluginverifier.dymamic

import com.jetbrains.plugin.structure.intellij.plugin.ExtensionPoint
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.warnings.DynamicPluginStatus

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

      val declaredExtensions = idePlugin.extensions.keySet()
      if (declaredExtensions.any { it !in allowedImmediateLoadUnloadAllowedExtensions }) {
        reasonsNotToLoadUnloadImmediately += "Plugin declares extension points other than " + allowedImmediateLoadUnloadAllowedExtensions.joinToString()
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


      val allActions = idePlugin.actions
      if (allActions.isNotEmpty()) {
        reasonsNotToLoadUnloadImmediately += "Plugin declares actions or groups, which can't be loaded immediately"
      }

      for (element in allActions) {
        if (element.name == "group" && element.getAttributeValue("id") == null) {
          reasonsNotToLoadUnloadWithoutRestart += "Plugin declares a group with no ID specified. Groups without ID can't be unloaded"
          break
        }
      }

      for (element in allActions) {
        if (element.name == "action" && element.getAttributeValue("id") == null && element.getAttributeValue("class") == null) {
          reasonsNotToLoadUnloadWithoutRestart += "Plugin declares an action with neither 'id' nor 'class' specified"
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