package com.jetbrains.pluginverifier.repository

/**
 * Identifier of an abstract plugin, which may
 * represent either a plugin in the Plugin Repository ([UpdateInfo]),
 * or a locally stored file ([LocalPluginInfo]).
 */
interface PluginInfo {
  val pluginId: String

  val version: String
}