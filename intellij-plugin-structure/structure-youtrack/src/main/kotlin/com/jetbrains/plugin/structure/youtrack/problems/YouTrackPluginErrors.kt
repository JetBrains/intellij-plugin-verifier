package com.jetbrains.plugin.structure.youtrack.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.youtrack.YouTrackPluginManager.Companion.DESCRIPTOR_NAME
import com.jetbrains.plugin.structure.youtrack.bean.YouTrackAppFields

open class InvalidAppNameProblem(message: String) : InvalidDescriptorProblem(
  descriptorPath = YouTrackAppFields.Manifest.NAME,
  detailedMessage = message
) {
  override val level
    get() = Level.ERROR
}

class AppNameIsBlank : InvalidAppNameProblem(
  "The app name is blank."
)

class UnsupportedSymbolsAppNameProblem : InvalidAppNameProblem(
  "The app name contains unsupported symbols. " +
    "Please use lowercase characters, numbers, and '.'/'-'/'_'/'~' symbols only."
)

open class ManifestPropertyNotSpecified(propertyName: String) : InvalidDescriptorProblem(
  descriptorPath = DESCRIPTOR_NAME,
  detailedMessage = "The property '$propertyName' is not specified."
) {
  override val level
    get() = Level.ERROR
}

open class WidgetManifestPropertyNotSpecified(propertyName: String, widgetKey: String) : InvalidDescriptorProblem(
  descriptorPath = DESCRIPTOR_NAME,
  detailedMessage = "The property '$propertyName' is not specified for widget '$widgetKey'."
) {
  override val level
    get() = Level.ERROR
}

open class InvalidWidgetKeyProblem(message: String) : InvalidDescriptorProblem(
  descriptorPath = YouTrackAppFields.Widget.KEY,
  detailedMessage = message
) {
  override val level
    get() = Level.ERROR
}

class UnsupportedSymbolsWidgetKeyProblem(key: String) : InvalidWidgetKeyProblem(
  "The app widget key '$key' contains unsupported symbols. " +
    "Please use lowercase characters, numbers, and '.'/'-'/'_' symbols only."
)

open class WidgetKeyNotSpecified : InvalidDescriptorProblem(
  descriptorPath = DESCRIPTOR_NAME,
  detailedMessage = "Widget property '${YouTrackAppFields.Widget.KEY}' is not specified."
) {
  override val level
    get() = Level.ERROR
}

open class WidgetKeyIsNotUnique : InvalidDescriptorProblem(
  descriptorPath = DESCRIPTOR_NAME,
  detailedMessage = "Widget property '${YouTrackAppFields.Widget.KEY}' is not unique."
) {
  override val level
    get() = Level.ERROR
}