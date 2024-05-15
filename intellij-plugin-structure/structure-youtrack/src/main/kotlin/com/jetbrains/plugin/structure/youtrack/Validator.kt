package com.jetbrains.plugin.structure.youtrack

import com.jetbrains.plugin.structure.base.problems.MAX_CHANGE_NOTES_LENGTH
import com.jetbrains.plugin.structure.base.problems.MAX_NAME_LENGTH
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.validatePropertyLength
import com.jetbrains.plugin.structure.youtrack.YouTrackPluginManager.Companion.DESCRIPTOR_NAME
import com.jetbrains.plugin.structure.youtrack.bean.YouTrackAppManifest
import com.jetbrains.plugin.structure.youtrack.bean.YouTrackAppWidget
import com.jetbrains.plugin.structure.youtrack.problems.*


private val ID_REGEX = "^([a-z\\d\\-._~]+)\$".toRegex()

fun validateYouTrackManifest(manifest: YouTrackAppManifest): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()

  validateManifestName(manifest.name, problems)

  if (manifest.title.isNullOrBlank()) {
    problems.add(ManifestPropertyNotSpecified("title"))
  }

  if (manifest.title != null) {
    validatePropertyLength(DESCRIPTOR_NAME, "title", manifest.title, MAX_NAME_LENGTH, problems)
  }

  if (manifest.description.isNullOrBlank()) {
    problems.add(ManifestPropertyNotSpecified("description"))
  }

  if (manifest.version.isNullOrBlank()) {
    problems.add(ManifestPropertyNotSpecified("version"))
  }

  if (manifest.changeNotes != null) {
    validatePropertyLength(DESCRIPTOR_NAME, "changeNotes", manifest.changeNotes, MAX_CHANGE_NOTES_LENGTH, problems)
  }

  manifest.widgets?.let { widgets ->
    if (widgets.map { it.key }.toSet().size != widgets.size) {
      problems.add(WidgetKeyIsNotUnique())
    }
    widgets.forEach { validateWidget(it, problems) }
  }

  return problems
}

private fun validateManifestName(name: String?, problems: MutableList<PluginProblem>) {
  if (name == null) {
    problems.add(ManifestPropertyNotSpecified("name"))
    return
  }

  if (name.isBlank()) {
    problems.add(AppNameIsBlank())
    return
  }

  validatePropertyLength(DESCRIPTOR_NAME, "name", name, MAX_NAME_LENGTH, problems)

  if (!ID_REGEX.matches(name)) {
    problems.add(UnsupportedSymbolsAppNameProblem())
  }
}

private fun validateWidget(widget: YouTrackAppWidget, problems: MutableList<PluginProblem>) {
  if (widget.key == null) {
    problems.add(WidgetKeyNotSpecified())
    return
  }

  if (!ID_REGEX.matches(widget.key)) {
    problems.add(UnsupportedSymbolsWidgetKeyProblem(widget.key))
  }

  if (widget.indexPath == null) {
    problems.add(WidgetManifestPropertyNotSpecified("indexPath", widget.key))
  }

  if (widget.extensionPoint == null) {
    problems.add(WidgetManifestPropertyNotSpecified("extensionPoint", widget.key))
  }
}