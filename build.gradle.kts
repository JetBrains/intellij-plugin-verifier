tasks.register("clean") {
    dependsOn(gradle.includedBuild("ide-diff-builder").task(":clean"))
    dependsOn(gradle.includedBuild("intellij-feature-extractor").task(":clean"))
    dependsOn(gradle.includedBuild("intellij-plugin-structure").task(":clean"))
    dependsOn(gradle.includedBuild("intellij-plugin-verifier").task(":clean"))
}

tasks.register("test") {
  dependsOn(gradle.includedBuild("ide-diff-builder").task(":test"))
  dependsOn(gradle.includedBuild("intellij-feature-extractor").task(":test"))
  dependsOn(gradle.includedBuild("intellij-plugin-structure").task(":test"))
  dependsOn(gradle.includedBuild("intellij-plugin-verifier").task(":test"))
}

tasks.register<JavaExec>("checkPlugin") {
  group = "application"
  description = "Runs IntelliJ Plugin Verifier from CLI options"
  mainClass = "com.jetbrains.pluginverifier.PluginVerifierMain"
  dependsOn(gradle.includedBuild("intellij-plugin-verifier").task(":verifier-cli:shadowJar"))

  val classpathFiles = gradle.includedBuild("intellij-plugin-verifier")
    .projectDir
    .resolve("verifier-cli/build/libs")
    .listFiles { _, name -> name.endsWith("-all.jar") }
  classpath = files(classpathFiles)
  systemProperties = System.getProperties().mapKeysTo(mutableMapOf()) { it.key.toString() }

  val ideValue: String? by project
  val ide = ideValue
    ?: throw InvalidUserDataException("Target IDE must be set in a Gradle project property. Use 'ide' project property, such as -Pide=<value>")

  val pluginValue: String? by project
  val plugin = pluginValue
    ?: throw InvalidUserDataException("The plugin to be verified must be set in a Gradle project property. Use 'plugin' project property, such as -Pplugin=<value>")

  args("check-plugin", plugin, ide)
}