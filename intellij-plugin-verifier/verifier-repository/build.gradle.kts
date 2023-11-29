val intellijStructureVersion : String by rootProject.extra

dependencies {
  api("org.jetbrains.intellij.plugins:structure-intellij-classes:$intellijStructureVersion")
  api("org.jetbrains.intellij.plugins:structure-ide:$intellijStructureVersion")

  implementation(sharedLibs.caffeine)
  implementation(libs.commons.compress)
  implementation(sharedLibs.xz)


  implementation(sharedLibs.jackson.module.kotlin)

  implementation(libs.jetbrains.pluginRepositoryRestClient)
  testImplementation(sharedLibs.junit)
  testImplementation(libs.okhttp.mockwebserver)
}

tasks {
  test {
    val customProperties = project.properties.filterKeys {
      it.startsWith("com.jetbrains.plugin.verifier.repository.custom.properties.")
    }
    systemProperties(customProperties)
  }
}
