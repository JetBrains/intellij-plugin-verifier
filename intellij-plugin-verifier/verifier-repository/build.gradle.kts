val intellijStructureVersion : String by rootProject.extra

dependencies {
  api("org.jetbrains.intellij.plugins:structure-intellij-classes:$intellijStructureVersion")
  api("org.jetbrains.intellij.plugins:structure-ide:$intellijStructureVersion")

  implementation("com.google.code.gson:gson:2.10.1")

  implementation("com.google.guava:guava:31.1-jre")
  implementation("org.apache.commons:commons-compress:1.23.0")
  implementation("org.tukaani:xz:1.9")

  implementation("com.squareup.okhttp3:okhttp:4.11.0")
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

  implementation("org.jetbrains.intellij:plugin-repository-rest-client:2.0.32")
  testImplementation("junit:junit:4.13.2")
}

tasks {
  test {
    val customProperties = project.properties.filterKeys {
      it.startsWith("com.jetbrains.plugin.verifier.repository.custom.properties.")
    }
    systemProperties(customProperties)
  }
}
