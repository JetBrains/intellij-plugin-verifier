dependencies {
  api(project(":structure-base"))

  implementation(sharedLibs.slf4j.api)
  implementation(sharedLibs.jdom)
  implementation(libs.jaxb.api)
  runtimeOnly(libs.jaxb.runtime)
}
