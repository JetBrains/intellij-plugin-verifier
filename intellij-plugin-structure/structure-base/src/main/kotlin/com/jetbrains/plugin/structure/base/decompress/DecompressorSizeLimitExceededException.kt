package com.jetbrains.plugin.structure.base.decompress

class DecompressorSizeLimitExceededException(val sizeLimit: Long): RuntimeException()