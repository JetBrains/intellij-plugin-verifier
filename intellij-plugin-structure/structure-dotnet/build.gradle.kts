dependencies {
  implementation(sharedLibs.slf4j.api)
  implementation(project(":structure-base"))
  implementation(libs.jdom)
  implementation(libs.jaxb.api)
  runtimeOnly(libs.jaxb.runtime)
}
