dependencies {
  implementation(project(":structure-base"))
  implementation(libs.jdom)
  implementation(libs.jaxb.api)
  runtimeOnly(libs.jaxb.runtime)
}
