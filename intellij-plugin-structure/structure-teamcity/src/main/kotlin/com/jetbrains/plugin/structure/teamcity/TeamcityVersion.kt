package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.product.ProductVersion

data class TeamcityVersion(val build: Long) : ProductVersion<TeamcityVersion> {
  override fun compareTo(other: TeamcityVersion) = build.compareTo(other.build)
}