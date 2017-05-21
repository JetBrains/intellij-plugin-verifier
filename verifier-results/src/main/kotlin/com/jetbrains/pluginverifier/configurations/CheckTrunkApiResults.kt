package com.jetbrains.pluginverifier.configurations

import com.google.gson.annotations.SerializedName

/**
 * @author Sergey Patrikeev
 */
data class CheckTrunkApiResults(@SerializedName("trunkResults") val trunkResults: CheckIdeResults,
                                @SerializedName("releaseResults") val releaseResults: CheckIdeResults) : ConfigurationResults
