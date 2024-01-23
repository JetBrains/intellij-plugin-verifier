/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dymamic

import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import org.jdom2.Element

/**
 * Utility methods that determine whether a plugin can be dynamically enabled/disabled [DynamicPluginStatus].
 */
object DynamicPlugins {
  const val MESSAGE = "Plugin probably cannot be enabled or disabled without IDE restart"
  fun getDynamicPluginStatus(context: PluginVerificationContext): DynamicPluginStatus? {
    val verificationDescriptor = context.verificationDescriptor
    val idePlugin = context.idePlugin
    if (verificationDescriptor is PluginVerificationDescriptor.IDE && idePlugin is IdePluginImpl) {
      val reasonsNotToLoadUnloadWithoutRestart = hashSetOf<String>()

      listOf(
        idePlugin.appContainerDescriptor to "application",
        idePlugin.projectContainerDescriptor to "project",
        idePlugin.moduleContainerDescriptor to "module"
      )
        .filter { it.first.components.isNotEmpty() }
        .mapTo(reasonsNotToLoadUnloadWithoutRestart) { (descriptor, area) ->
          "$MESSAGE because it declares $area components: " + formatListOfNames(descriptor.components.map { it.implementationClass })
        }

      val declaredExtensions = idePlugin.extensions.keys

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
        reasonsNotToLoadUnloadWithoutRestart += "$MESSAGE because it declares non-dynamic extensions: " +
          formatListOfNames(nonDynamicExtensions)
      }


      val allActionsAndGroups = getAllActionsAndGroupsRecursively(idePlugin)

      for (element in allActionsAndGroups) {
        if (element.name == "group" && element.getAttributeValue("id") == null) {
          reasonsNotToLoadUnloadWithoutRestart += "$MESSAGE because it declares a group without 'id' specified"
          break
        }
      }

      for (element in allActionsAndGroups) {
        if (element.name == "action" && element.getAttributeValue("id") == null && element.getAttributeValue("class") == null) {
          reasonsNotToLoadUnloadWithoutRestart += "$MESSAGE because it declares an action with neither 'id' nor 'class' specified"
          break
        }
      }

      if (reasonsNotToLoadUnloadWithoutRestart.isEmpty()) {
        return DynamicPluginStatus.MaybeDynamic
      }

      return DynamicPluginStatus.NotDynamic(reasonsNotToLoadUnloadWithoutRestart)
    }

    return null
  }

  fun DynamicPluginStatus.NotDynamic.simplifiedReasonsNotToLoadUnloadWithoutRestart(): List<String> {
    return reasonsNotToLoadUnloadWithoutRestart.map { reason ->
      reason.removePrefix("$MESSAGE because it ").capitalize()
    }
  }

  private fun formatListOfNames(names: List<String>): String =
    names.sorted().joinToString { "`$it`" }

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

  private fun IdePluginImpl.findExtensionPoint(epName: String): IdePluginContentDescriptor.ExtensionPoint? {
    for (descriptor in listOf(appContainerDescriptor, projectContainerDescriptor, moduleContainerDescriptor)) {
      val extensionPoint = descriptor.extensionPoints.find { it.extensionPointName == epName }
      if (extensionPoint != null) {
        return extensionPoint
      }
    }
    return null
  }

}