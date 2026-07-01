dependencies {
  api(project(":structure-ide"))
  api(project(":structure-classes"))
  api(project(":structure-ide-classes"))
  api(project(":structure-intellij"))

  implementation(libs.platform.util)
  implementation(libs.platform.jps.model.core)
  implementation(libs.platform.jps.model.impl)
  implementation(libs.platform.jps.model.serialization)
}
