val intellijStructureVersion : String by rootProject.extra

dependencies {
  api("org.jetbrains.intellij.plugins:structure-intellij-classes:$intellijStructureVersion")
  api("org.jetbrains.intellij.plugins:structure-ide:$intellijStructureVersion")

  implementation(libs.gson)

  implementation(libs.guava)
  implementation(libs.commons.compress)
  implementation(sharedLibs.xz)

  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.retrofit)
  implementation(libs.retrofit.gson)

  implementation(libs.jetbrains.pluginRepositoryRestClient)
  testImplementation(libs.junit)
}

tasks {
  test {
    val customProperties = project.properties.filterKeys {
      it.startsWith("com.jetbrains.plugin.verifier.repository.custom.properties.")
    }
    systemProperties(customProperties)
  }
}
