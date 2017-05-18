package com.jetbrains.pluginverifier.ide

import com.intellij.structure.ide.Ide
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.misc.closeLogged
import java.io.Closeable

data class CreateIdeResult(val ide: Ide, val ideResolver: Resolver) : Closeable {
  override fun close() {
    ideResolver.closeLogged()
  }
}