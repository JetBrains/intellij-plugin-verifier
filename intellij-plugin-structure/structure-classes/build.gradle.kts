val asmVersion = "9.5"

dependencies {
  api("org.ow2.asm:asm:$asmVersion")
  api("org.ow2.asm:asm-commons:$asmVersion")
  api("org.ow2.asm:asm-util:$asmVersion")
  api("org.ow2.asm:asm-tree:$asmVersion")
  api("org.ow2.asm:asm-analysis:$asmVersion")

  implementation(project(":structure-base"))

  implementation("com.google.guava:guava:31.1-jre")
}