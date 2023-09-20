package com.jetbrains.pluginverifier.output

/**
 * These messages are parsed by the Plugin DevKit plugin.
 * If any of these is modified, please adjust it in the Plugin DevKit as well.
 */
const val VERIFICATION_REPORTS_DIRECTORY = "Verification reports directory: %s"
const val READING_PLUGIN_FROM = "Reading plugin to check from %s"
const val READING_IDE_FROM = "Reading IDE from %s"
const val DYNAMIC_PLUGIN_PASS = "Plugin can probably be enabled or disabled without IDE restart"
const val DYNAMIC_PLUGIN_FAIL = "Plugin probably cannot be enabled or disabled without IDE restart"