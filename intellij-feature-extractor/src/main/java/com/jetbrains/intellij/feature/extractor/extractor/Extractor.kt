package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

interface Extractor {

  fun extract(plugin: IdePlugin, resolver: Resolver): List<ExtensionPointFeatures>

}