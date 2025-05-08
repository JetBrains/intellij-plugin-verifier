dependencies {
  testImplementation(sharedLibs.junit)
  testImplementation(project(":structure-ide"))
  testImplementation(project(":structure-ide-jps"))
  testImplementation(project(path = ":tests", configuration = "testOutput"))

  testImplementation(sharedLibs.asm)
}