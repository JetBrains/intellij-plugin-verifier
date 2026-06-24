dependencies {
  val intellijStructureVersion : String by rootProject.extra
  implementation("org.jetbrains.intellij.plugins:structure-classes:$intellijStructureVersion")

  api(sharedLibs.asm.root)
  api(sharedLibs.asm.tree)
  api(sharedLibs.asm.analysis)

  implementation(sharedLibs.caffeine)
}
