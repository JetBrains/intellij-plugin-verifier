
dependencies {
  implementation(sharedLibs.slf4j.api)
  implementation(project(":structure-intellij"))
  implementation(sharedLibs.woodstox)
  testImplementation(sharedLibs.junit)
}
