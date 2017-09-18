package com.jetbrains.pluginverifier.repository

data class UpdateInfo(val pluginId: String,
                      val pluginName: String,
                      val version: String,
                      val updateId: Int,
                      val vendor: String?) {

  override fun toString(): String = "$pluginId:$version (#$updateId)"
}