package com.jetbrains.plugin.structure.intellij.platform

import com.jetbrains.plugin.structure.intellij.beans.ModuleBean

class BundledModulesManager(modulesResolver: BundledModulesResolver) {
  private val modulesResolver = modulesResolver
  private val modulesByName = mutableMapOf<String, ModuleBean?>()

  fun findModuleByName(name: String): ModuleBean? {
    return modulesByName.getOrPut(name) {
      modulesResolver.findModuleByName(name)
    }
  }
}
