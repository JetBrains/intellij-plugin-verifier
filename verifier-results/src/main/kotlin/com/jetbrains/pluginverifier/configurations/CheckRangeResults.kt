package com.jetbrains.pluginverifier.configurations

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResults

/**
 * @author Sergey Patrikeev
 */
data class CheckRangeResults(@SerializedName("plugin") val plugin: PluginDescriptor,
                             @SerializedName("checkedIdeList") val checkedIdeList: List<IdeDescriptor>,
                             @SerializedName("results") val vResults: VResults) : Results