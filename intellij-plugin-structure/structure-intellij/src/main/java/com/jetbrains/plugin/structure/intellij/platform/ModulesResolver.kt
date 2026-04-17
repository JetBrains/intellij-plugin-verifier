package com.jetbrains.plugin.structure.intellij.platform

import com.jetbrains.plugin.structure.intellij.beans.ModuleBean

fun interface ModulesResolver  {
  fun resolveModules(): List<ModuleBean>

  fun findModuleByName(name: String): ModuleBean? = resolveModules().find { it.name == name }
}
