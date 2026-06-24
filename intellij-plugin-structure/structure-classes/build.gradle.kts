val asmVersion = "9.5"

dependencies {
  api(project(":structure-base"))

  implementation(sharedLibs.jetbrains.annotations)

  implementation(sharedLibs.slf4j.api)
  api(libs.asm.root)
  api(libs.asm.tree)

  implementation(libs.kotlinx.metadata)

  implementation(sharedLibs.caffeine)
}