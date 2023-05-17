val intellijStructureVersion : String by rootProject.extra

dependencies {
  api("org.jetbrains.intellij.plugins:structure-intellij-classes:$intellijStructureVersion")
  api("org.jetbrains.intellij.plugins:structure-ide:$intellijStructureVersion")

  implementation(sharedLibs.gson)

  implementation(sharedLibs.guava)
  implementation(libs.commons.compress)
  implementation(sharedLibs.xz)

  implementation(sharedLibs.okhttp)
  implementation(sharedLibs.okhttp.loggingInterceptor)
  implementation(sharedLibs.retrofit)
  implementation(sharedLibs.retrofit.gson)

  implementation(libs.jetbrains.pluginRepositoryRestClient)
  testImplementation(sharedLibs.junit)
}

tasks {
  test {
    val customProperties = project.properties.filterKeys {
      it.startsWith("com.jetbrains.plugin.verifier.repository.custom.properties.")
    }
    systemProperties(customProperties)
  }
}
