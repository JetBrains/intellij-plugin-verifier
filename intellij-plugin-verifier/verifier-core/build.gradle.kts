dependencies {
  val intellijStructureVersion : String by rootProject.extra
  implementation("org.jetbrains.intellij.plugins:structure-classes:$intellijStructureVersion")

  implementation(sharedLibs.caffeine)
}
