package com.jetbrains.plugin.structure.intellij.platform

import com.jetbrains.plugin.structure.intellij.beans.ModuleBean

class BundledModulesManager(modulesResolver: ModulesResolver) {
  private val modulesByName = modulesResolver.resolveModules().let { modules ->
    LinkedHashMap<String, ModuleBean>(modules.size).apply {
      modules.forEach { putIfAbsent(it.name, it) }
    }
  }

  fun findModuleByName(name: String): ModuleBean? {
    return modulesByName[name]
  }
}
