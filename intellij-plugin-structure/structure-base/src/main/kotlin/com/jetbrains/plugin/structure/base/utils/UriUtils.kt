package com.jetbrains.plugin.structure.base.utils

import java.net.URI

fun URI.withSuperScheme(superScheme: String) = URI("$superScheme:$this")

