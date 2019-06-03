package com.jetbrains.plugin.structure.ide

import java.io.File

/**
 * Indicates that a IDE instance can't be created because IDE residing by path is not a valid IDE.
 */
class InvalidIdeException(val idePath: File, val reason: String) : RuntimeException("IDE by path '$idePath' is invalid: $reason")