/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile
import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.base.problems.PluginFileError
import com.jetbrains.plugin.structure.ktor.KtorFeaturePluginManager

class IncorrectKtorVersionFormat(val ktorVersion: String) : InvalidDescriptorProblem(null) {

    override val detailedMessage
        get() = "Provided ktor version (\"$ktorVersion\") has incorrect format. " +
                "Ktor version must be of the following format: \"X.Y.Z\" or \"X.Y.Z-w\", where X, Y, Z are numbers, w is string."

    override val level
        get() = Level.ERROR

}

class KtorVersionDoesNotExist(val ktorVersion: String) : InvalidDescriptorProblem(null) {

    override val detailedMessage
        get() = "Provided ktor version (\"$ktorVersion\") does not exist. " +
                "Please, visit https://mvnrepository.com/artifact/io.ktor/ktor-server-core to see possible ktor versions"

    override val level
        get() = Level.ERROR

}