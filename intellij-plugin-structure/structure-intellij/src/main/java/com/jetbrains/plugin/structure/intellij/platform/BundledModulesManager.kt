package com.jetbrains.plugin.structure.intellij.platform

import com.jetbrains.plugin.structure.intellij.beans.ModuleBean

class BundledModulesManager(private val modulesResolver: BundledModulesResolver) {
  fun findModuleByName(name: String): ModuleBean? {
    return modulesResolver.findModuleByName(name)
  }
}
