
val platformVersion = "213.7172.25"

dependencies {
  api(project(":structure-intellij"))

  implementation(libs.platform.util)
  implementation(libs.platform.jps.model.core)
  implementation(libs.platform.jps.model.impl)
  implementation(libs.platform.jps.model.serialization)
}
