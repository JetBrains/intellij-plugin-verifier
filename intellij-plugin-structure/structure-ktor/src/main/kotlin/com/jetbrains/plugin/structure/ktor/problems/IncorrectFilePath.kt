package com.jetbrains.plugin.structure.ktor.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.ktor.bean.SIMPLE_PATH_REGEX

class IncorrectFilePath(val path: String) : InvalidDescriptorProblem(null) {
    override val detailedMessage
        get() = "File path \"$path\" does not match a required format. Only paths, matching $SIMPLE_PATH_REGEX are allowed"

    override val level
        get() = Level.ERROR

}