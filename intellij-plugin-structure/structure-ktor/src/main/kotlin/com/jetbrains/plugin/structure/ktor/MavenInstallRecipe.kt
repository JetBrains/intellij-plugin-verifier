package com.jetbrains.plugin.structure.ktor

data class MavenInstallRecipe(
  val repositories: List<MavenRepository> = emptyList(),
  val plugins: List<MavenPlugin> = emptyList()
)

data class MavenRepository(
  val id: String,
  val url: String
)

data class MavenPlugin(
  val group: String,
  val artifact: String,
  val version: String? = null
)