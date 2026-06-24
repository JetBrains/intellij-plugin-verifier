val asmVersion = "9.5"

dependencies {
  implementation(sharedLibs.slf4j.api)
  implementation(libs.asm.root)
  implementation(libs.asm.commons)
  implementation(libs.asm.util)
  implementation(libs.asm.tree)
  implementation(libs.asm.analysis)

  implementation(libs.kotlinx.metadata)

  implementation(project(":structure-base"))

  implementation(sharedLibs.caffeine)
}