dependencies {
  api(project(":structure-classes"))
  api(project(":structure-ide"))
  api(project(":structure-ide-classes"))

  implementation(libs.platform.util)
  implementation(libs.platform.jps.model.core)
  implementation(libs.platform.jps.model.impl)
  implementation(libs.platform.jps.model.serialization)

  testImplementation(sharedLibs.junit)
}
