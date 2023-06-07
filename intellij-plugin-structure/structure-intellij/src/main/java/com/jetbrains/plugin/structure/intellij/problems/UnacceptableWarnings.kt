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

