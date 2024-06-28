package com.jetbrains.plugin.structure.youtrack

import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.youtrack.YouTrackPluginManager.Companion.DESCRIPTOR_NAME
import com.jetbrains.plugin.structure.youtrack.bean.YouTrackAppFields
import com.jetbrains.plugin.structure.youtrack.bean.YouTrackAppManifest
import com.jetbrains.plugin.structure.youtrack.bean.YouTrackAppWidget
import com.jetbrains.plugin.structure.youtrack.problems.*
import com.vdurmont.semver4j.Semver


private val ID_REGEX = "^([a-z\\d\\-._~]+)\$".toRegex()

fun validateYouTrackManifest(manifest: YouTrackAppManifest): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()

  validateManifestName(manifest.name, problems)

  if (manifest.title.isNullOrBlank()) {
    problems.add(ManifestPropertyNotSpecified(YouTrackAppFields.Manifest.TITLE))
  }

  if (manifest.title != null) {
    validatePropertyLength(DESCRIPTOR_NAME, YouTrackAppFields.Manifest.TITLE, manifest.title, MAX_NAME_LENGTH, problems)
  }

  if (manifest.description.isNullOrBlank()) {
    problems.add(ManifestPropertyNotSpecified(YouTrackAppFields.Manifest.DESCRIPTION))
  }

  if (manifest.version.isNullOrBlank()) {
    problems.add(ManifestPropertyNotSpecified(YouTrackAppFields.Manifest.VERSION))
  }

  if (manifest.changeNotes != null) {
    validatePropertyLength(DESCRIPTOR_NAME, YouTrackAppFields.Manifest.NOTES, manifest.changeNotes, MAX_CHANGE_NOTES_LENGTH, problems)
  }

  validateYouTrackRange(
    minYouTrackVersion = manifest.minYouTrackVersion,
    maxYouTrackVersion = manifest.maxYouTrackVersion,
    problems = problems
  )

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
    problems.add(ManifestPropertyNotSpecified(YouTrackAppFields.Manifest.NAME))
    return
  }

  if (name.isBlank()) {
    problems.add(AppNameIsBlank())
    return
  }

  validatePropertyLength(DESCRIPTOR_NAME, YouTrackAppFields.Manifest.NAME, name, MAX_NAME_LENGTH, problems)

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
    problems.add(WidgetManifestPropertyNotSpecified(YouTrackAppFields.Widget.INDEX_PATH, widget.key))
  }

  if (widget.extensionPoint == null) {
    problems.add(WidgetManifestPropertyNotSpecified(YouTrackAppFields.Widget.EXTENSION_POINT, widget.key))
  }
}

private fun validateYouTrackRange(
  minYouTrackVersion: String?,
  maxYouTrackVersion: String?,
  problems: MutableList<PluginProblem>
) {
  val since = getYouTrackVersionOrNull(
    versionName = YouTrackAppFields.Manifest.SINCE,
    version = minYouTrackVersion,
    problems = problems
  )
  val until = getYouTrackVersionOrNull(
    versionName = YouTrackAppFields.Manifest.UNTIL,
    version = maxYouTrackVersion,
    problems = problems
  )

  if (since == null || until == null) return

  if (since.isGreaterThan(until)) {
    problems.add(InvalidVersionRange(
      descriptorPath = DESCRIPTOR_NAME,
      since = minYouTrackVersion!!,
      until = maxYouTrackVersion!!
    ))
  }
}

private fun getYouTrackVersionOrNull(
  versionName: String,
  version: String?,
  problems: MutableList<PluginProblem>
): Semver? {
  if (version == null) return null

  return runCatching {
    Semver(version, Semver.SemverType.STRICT)
  }.onFailure {
    problems.add(
      InvalidSemverVersion(
        descriptorPath = DESCRIPTOR_NAME,
        versionName = versionName,
        version = version
      )
    )
  }.getOrNull()
}