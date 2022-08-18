package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem

class ShortDescription : InvalidDescriptorProblem("description") {

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val detailedMessage
    get() = "Description is too short"
}

class NonLatinDescription : InvalidDescriptorProblem("description") {

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val detailedMessage
    get() = "Please make sure to provide the description in English"
}

class HttpLinkInDescription(private val link: String) : InvalidDescriptorProblem("description") {

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val detailedMessage
    get() = "All links in description must be HTTPS: $link"
}