package com.jetbrains.plugin.structure.ktor

data class GradleInstallRecipe(
  val repositories: List<GradleRepository> = emptyList(),
  val plugins: List<GradlePlugin> = emptyList()
)

sealed class GradleRepository {
    /**
     * Gradle repository that can be added by calling a function.
     *
     * Examples: mavenLocal(), jcenter()
     *
     * @param functionName is a name of the function to call (ex.: "mavenLocal", "jcenter")
     * */
    class FunctionDefinedRepository(val functionName: String) : GradleRepository()

    /**
     * Gradle repository that can be added with a maven { url : "..." } construction
     * */
    class UrlDefinedRepository(val url: String) : GradleRepository()
}

data class GradlePlugin(
  val id: String,
  val version: String? = null
)