dependencies {
  api(project(":structure-ide"))
  api(project(":structure-classes"))
  api(sharedLibs.asm.tree)

  implementation(sharedLibs.slf4j.api)
  implementation(sharedLibs.caffeine)
}