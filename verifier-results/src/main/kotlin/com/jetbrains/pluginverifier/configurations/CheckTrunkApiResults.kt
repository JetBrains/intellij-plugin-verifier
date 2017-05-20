package com.jetbrains.pluginverifier.configurations

import com.google.gson.annotations.SerializedName

/**
 * @author Sergey Patrikeev
 */
data class CheckTrunkApiResults(@SerializedName("trunkResults") val trunkResults: CheckIdeResults,
                                @SerializedName("trunkBundledPlugins") val trunkBundledPlugins: BundledPlugins,
                                @SerializedName("releaseResults") val releaseResults: CheckIdeResults,
                                @SerializedName("releaseBundledPlugins") val releaseBundledPlugins: BundledPlugins) : ConfigurationResults

data class BundledPlugins(@SerializedName("pluginIds") val pluginIds: List<String>,
                          @SerializedName("moduleIds") val moduleIds: List<String>)

