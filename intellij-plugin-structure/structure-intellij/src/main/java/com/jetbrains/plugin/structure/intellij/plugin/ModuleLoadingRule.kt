package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Corresponds to com.intellij.ide.plugins.ModuleLoadingRule from IntelliJ Platform.
 */
class ModuleLoadingRule private constructor(val id: String) {
  companion object {
    val OPTIONAL = ModuleLoadingRule("optional")
    val REQUIRED = ModuleLoadingRule("required")
    val EMBEDDED = ModuleLoadingRule("embedded")
    val ON_DEMAND = ModuleLoadingRule("on-demand")
    
    fun create(ruleString: String?): ModuleLoadingRule = when (ruleString) {
      null, OPTIONAL.id -> OPTIONAL
      REQUIRED.id -> REQUIRED
      EMBEDDED.id -> EMBEDDED
      ON_DEMAND.id -> ON_DEMAND
      else -> ModuleLoadingRule(ruleString)
    }
  }

  val required: Boolean get() = this == REQUIRED || this == EMBEDDED
  
  override fun hashCode(): Int = id.hashCode()
  override fun equals(other: Any?): Boolean = (other as? ModuleLoadingRule)?.id == id
  override fun toString(): String = id
}