val asmVersion = "9.5"

dependencies {
  api(libs.asm.root)
  api(libs.asm.commons)
  api(libs.asm.util)
  api(libs.asm.tree)
  api(libs.asm.analysis)

  implementation(project(":structure-base"))

  api(sharedLibs.caffeine)
}