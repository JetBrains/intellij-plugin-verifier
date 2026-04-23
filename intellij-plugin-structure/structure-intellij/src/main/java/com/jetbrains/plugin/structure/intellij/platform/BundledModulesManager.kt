package com.jetbrains.plugin.structure.intellij.platform

import com.jetbrains.plugin.structure.intellij.beans.ModuleBean

class BundledModulesManager(modulesResolver: BundledModulesResolver) {
  private val modules = modulesResolver.resolveModules()

  fun findModuleByName(name: String): ModuleBean? {
    return modules.find { it.name == name }
  }
}