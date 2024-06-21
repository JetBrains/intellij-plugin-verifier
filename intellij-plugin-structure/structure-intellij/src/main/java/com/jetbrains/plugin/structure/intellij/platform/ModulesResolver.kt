package com.jetbrains.plugin.structure.intellij.platform

import com.jetbrains.plugin.structure.intellij.beans.ModuleBean

fun interface ModulesResolver  {
  fun resolveModules(): List<ModuleBean>
}