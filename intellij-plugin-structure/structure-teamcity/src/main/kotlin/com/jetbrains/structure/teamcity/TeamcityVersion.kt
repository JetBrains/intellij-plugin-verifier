package com.jetbrains.structure.teamcity

import com.jetbrains.structure.product.ProductVersion

data class TeamcityVersion(val build: Long) : ProductVersion<TeamcityVersion> {
  override fun compareTo(other: TeamcityVersion) = build.compareTo(other.build)
}