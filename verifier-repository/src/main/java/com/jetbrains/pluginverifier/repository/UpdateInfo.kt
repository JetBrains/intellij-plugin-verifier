package com.jetbrains.pluginverifier.repository

data class UpdateInfo(override val pluginId: String,
                      val pluginName: String,
                      override val version: String,
                      val updateId: Int,
                      val vendor: String?) : PluginInfo {

  override fun toString(): String = "$pluginId:$version (#$updateId)"
}