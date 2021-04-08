package com.jetbrains.plugin.structure.ktor.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem

class NoFileNameForTemplate(val position: String) : InvalidDescriptorProblem(null) {
    override val detailedMessage
        get() = "File name must be specified for template position position $position."

    override val level
        get() = Level.ERROR

}

class FileNameNotNeededForTemplate(val position: String) : InvalidDescriptorProblem(null) {
    override val detailedMessage
        get() = "File name is not needed for template position position $position."

    override val level
        get() = Level.ERROR

}