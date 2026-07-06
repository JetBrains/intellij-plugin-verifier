val asmVersion = "9.5"

dependencies {
  api(sharedLibs.asm.root)
  api(sharedLibs.asm.commons)
  api(sharedLibs.asm.util)
  api(sharedLibs.asm.tree)
  api(sharedLibs.asm.analysis)

  api(libs.kotlinx.metadata)

  implementation(project(":structure-base"))

  api(sharedLibs.caffeine)
}