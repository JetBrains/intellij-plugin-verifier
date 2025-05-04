dependencies {
  api(project(":structure-intellij"))

  implementation(libs.platform.util)
  implementation(libs.platform.jps.model.core)
  implementation(libs.platform.jps.model.impl)
  implementation(libs.platform.jps.model.serialization)

  testImplementation(sharedLibs.junit)
}
