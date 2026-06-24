dependencies {
  implementation(project(":structure-classes"))
  implementation(project(":structure-ide"))
  implementation(project(":structure-ide-classes"))

  implementation(libs.platform.util)
  implementation(libs.platform.jps.model.core)
  implementation(libs.platform.jps.model.impl)
  implementation(libs.platform.jps.model.serialization)
}
