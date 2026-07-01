val asmVersion = "9.5"

dependencies {
  api(project(":structure-base"))
  api(sharedLibs.asm.root)
  api(sharedLibs.asm.tree)

  implementation(sharedLibs.jetbrains.annotations)
  implementation(sharedLibs.slf4j.api)
  implementation(libs.kotlinx.metadata)
  implementation(sharedLibs.caffeine)
}