package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem

const val MIN_DESCRIPTION_LENGTH = 40

class ShortOrNonLatinDescription : InvalidDescriptorProblem("description") {

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val detailedMessage
    get() = "Please provide a long-enough English description."
}

class HttpLinkInDescription(private val link: String) : InvalidDescriptorProblem("description") {

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val detailedMessage
    get() = "All links in description must be HTTPS: $link"
}

open class IllegalPluginId(private val illegalPluginId: String) : InvalidDescriptorProblem("id") {

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val detailedMessage
    get() = "Plugin ID '$illegalPluginId' is not valid. See https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html#idea-plugin__id"
}

class IllegalPluginIdPrefix(private val illegalPluginId: String, private val illegalPrefix: String) : IllegalPluginId(illegalPluginId) {
  override val detailedMessage
    get() = "Plugin ID '$illegalPluginId' has an illegal prefix '$illegalPrefix'. See https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html#idea-plugin__id"
}

