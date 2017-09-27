package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations

interface ClassesForCheckSelector {
  fun getClassesForCheck(classesLocations: IdePluginClassesLocations): Iterator<String>
}