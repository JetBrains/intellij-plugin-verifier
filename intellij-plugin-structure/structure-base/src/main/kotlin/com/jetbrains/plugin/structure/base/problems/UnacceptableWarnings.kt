package com.jetbrains.plugin.structure.base.problems


class DescriptionNotStartingWithLatinCharacters : InvalidDescriptorProblem(
    descriptorPath = "description",
    detailedMessage = "The plugin description must start with Latin characters and have at least $MIN_DESCRIPTION_LENGTH characters."
) {
    override val level
        get() = Level.UNACCEPTABLE_WARNING
}

class HttpLinkInDescription(link: String) : InvalidDescriptorProblem(
    descriptorPath = "description",
    detailedMessage = "All the links in the plugin description must be HTTPS: $link."
) {
    override val level
        get() = Level.UNACCEPTABLE_WARNING
}