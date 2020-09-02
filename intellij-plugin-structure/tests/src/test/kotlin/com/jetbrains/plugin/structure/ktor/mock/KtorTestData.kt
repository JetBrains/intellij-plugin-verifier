package com.jetbrains.plugin.structure.ktor.mock

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.ktor.bean.BuildSystemDependency
import com.jetbrains.plugin.structure.ktor.bean.KtorFeatureDocumentation
import com.jetbrains.plugin.structure.ktor.bean.KtorVendor

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CodeTemplateJsonBuilder(
  var position: String? = "inside_app",
  var text: String? = "routing {\n\tget(\"/\") {\n\n\t}\n}"
) {
  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FeatureInstallRecipeJsonBuilder(
  var imports: List<String> = emptyList(),
  @JsonProperty("install_block")
  var installBlock: String? = null,
  @JsonProperty("templates")
  var templatesValue: List<String> = emptyList(),
  @JsonProperty("test_templates")
  var testTemplatesValue: List<String> = emptyList()
) {
  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)

  fun addTemplate(build: CodeTemplateJsonBuilder.() -> Unit) {
    val builder = CodeTemplateJsonBuilder()
    builder.build()
    templatesValue += builder.asString()
  }

  fun addTestTemplate(build: CodeTemplateJsonBuilder.() -> Unit) {
    val builder = CodeTemplateJsonBuilder()
    builder.build()
    testTemplatesValue += builder.asString()
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GradleInstallRecipeJsonBuilder(
  var repositories: List<String> = emptyList(),
  val plugins: List<String> = emptyList(),
  val dependencies: List<String> = emptyList(),
  @JsonProperty("String")
  val testDependencies: List<String> = emptyList()
) {
  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MavenInstallRecipeJsonBuilder(
  var repositories: List<String> = emptyList(),
  val plugins: List<String> = emptyList(),
  val dependencies: List<BuildSystemDependency> = emptyList(),
  @JsonProperty("String")
  val testDependencies: List<String> = emptyList()
) {
  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KtorFeatureDocumentationJsonBuilder(
  var description: String? = "Awesome feature, really!",
  var usage: String? = "No, I mean it's reeeeally awesome\na\nw\ne\ns\no\nm\ne",
  var options: String? = "But without options :("
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KtorFeatureJsonBuilder(
  var id: String? = "feature.ktor",
  var name: String? = "Ktor feature",
  @JsonProperty("short_description")
  var shortDescription: String? = "description",
  var version: String? = "1.0.0",
  var copyright: String? = "copyright",
  var vendor: KtorVendor? = KtorVendor("JetBrains s.r.o.", "", "http://jetbrains.com/"),
  @JsonProperty("required_feature_ids")
  var requiredFeatures: List<String>? = listOf("feature2.ktor"),
  @JsonProperty("install_recipe")
  var installRecipeValue: FeatureInstallRecipeJsonBuilder? = null,
  @JsonProperty("gradle_install")
  var gradleInstallValue: GradleInstallRecipeJsonBuilder? = null,
  @JsonProperty("maven_install")
  var mavenInstallValue: MavenInstallRecipeJsonBuilder? = null,
  var documentation: KtorFeatureDocumentationJsonBuilder? = KtorFeatureDocumentationJsonBuilder()
) {
  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)

  fun installRecipe(build: FeatureInstallRecipeJsonBuilder.()-> Unit) {
    val builder = FeatureInstallRecipeJsonBuilder()
    builder.build()
    installRecipeValue = builder
  }

  fun gradleInstall(build: GradleInstallRecipeJsonBuilder.()-> Unit) {
    val builder = GradleInstallRecipeJsonBuilder()
    builder.build()
    gradleInstallValue = builder
  }

  fun mavenInstall(build: MavenInstallRecipeJsonBuilder.()-> Unit) {
    val builder = MavenInstallRecipeJsonBuilder()
    builder.build()
    mavenInstallValue = builder
  }
}

val perfectKtorFeatureBuilder: KtorFeatureJsonBuilder
  get() = KtorFeatureJsonBuilder()


fun getMockPluginJsonContent(fileName: String): String {
  return object {}.javaClass.getResource("/ktor/$fileName.json").readText()
}
