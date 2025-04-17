val asmVersion = "9.5"

dependencies {
  api(libs.asm.root)
  api(libs.asm.commons)
  api(libs.asm.util)
  api(libs.asm.tree)
  api(libs.asm.analysis)

  api(libs.kotlinx.metadata)

  implementation(project(":structure-base"))

  api(sharedLibs.caffeine)

  testImplementation(sharedLibs.junit)
}