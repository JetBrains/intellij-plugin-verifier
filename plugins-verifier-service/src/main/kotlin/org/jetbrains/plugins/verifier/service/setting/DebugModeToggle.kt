package org.jetbrains.plugins.verifier.service.setting

object DebugModeToggle {

  private const val INTELLIJ_STRUCTURE_JAR_FILE_SYSTEMS_DEBUG_LEVEL = "intellij.structure.jar.file.systems.debug.level"

  val isDebug: Boolean
    get() = System.getProperty(INTELLIJ_STRUCTURE_JAR_FILE_SYSTEMS_DEBUG_LEVEL, "false") != "false"

  fun enable() {
    System.setProperty(INTELLIJ_STRUCTURE_JAR_FILE_SYSTEMS_DEBUG_LEVEL, "true")
  }

  fun disable() {
    System.setProperty(INTELLIJ_STRUCTURE_JAR_FILE_SYSTEMS_DEBUG_LEVEL, "false")
  }
}