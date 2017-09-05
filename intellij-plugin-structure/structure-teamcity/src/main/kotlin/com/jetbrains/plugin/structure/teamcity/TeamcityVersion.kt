package com.jetbrains.plugin.structure.teamcity

data class TeamcityVersion(val build: Long) : Comparable<TeamcityVersion> {
  override fun compareTo(other: TeamcityVersion) = build.compareTo(other.build)
}