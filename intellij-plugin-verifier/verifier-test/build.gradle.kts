dependencies {
  implementation(project(":verifier-cli"))

  testRuntimeOnly(sharedLibs.logback.classic)
  testRuntimeOnly(project("mock-plugin"))

  implementation(libs.byteBuddy)

  testImplementation(libs.systemStubs.junit4)
  testImplementation(libs.jimfs)

  testImplementation(sharedLibs.spullara.cliParser)
}

val prepareMockPlugin by tasks.registering(Copy::class) {
  dependsOn(":verifier-test:mock-plugin:build")
  into(layout.buildDirectory.dir("mocks"))
  val mockPluginBuildDir = project("mock-plugin").layout.buildDirectory
  from(mockPluginBuildDir.file("libs/mock-plugin-1.0.jar"))
}

val afterIdeaBuildDir = project("after-idea").layout.buildDirectory
val additionalAfterIdeaBuildDir = project("additional-after-idea").layout.buildDirectory

/**
 * Creates resources.jar file with brokenPlugins.txt file inside.
 */

val prepareResourcesJar by tasks.registering(Jar::class) {
  dependsOn(tasks.getByPath("after-idea:processResources"))
  from(afterIdeaBuildDir.file("resources/main/brokenPlugins.txt"))
  destinationDirectory = layout.buildDirectory
  archiveFileName = "resources.jar"
}

/**
 * Creates "after-idea" directory which content is similar to any IDE distribution:
 * `/`
 * `/build.txt`
 * `lib/resources.jar`
 * `lib/after-idea-1.0.jar` (contains plugin.xml with "IDEA CORE" plugin)
 * `lib/additional-after-idea-1.0.jar (contains extra IDE classes)
 */

val prepareAfterIdea by tasks.registering(Copy::class) {
  dependsOn(":verifier-test:after-idea:build", ":verifier-test:additional-after-idea:build", prepareResourcesJar)
  into(layout.buildDirectory.file("mocks/after-idea"))

  val ideaJar = copySpec {
    from(afterIdeaBuildDir.file("libs/after-idea-1.0.jar"))
    into("lib")
  }
  val additionalJar = copySpec {
    from(additionalAfterIdeaBuildDir.file("libs/additional-after-idea-1.0.jar"))
    into("lib")
  }

  val resourcesJar = copySpec {
    from(prepareResourcesJar)
    into("lib")
  }

  val buildTxt = copySpec {
    from(afterIdeaBuildDir.file("resources/main/build.txt"))
    into(".")
  }
  with(ideaJar, additionalJar, resourcesJar, buildTxt)
}

tasks.named("test") {
  dependsOn(prepareMockPlugin, prepareAfterIdea)
}

tasks.named("jar") {
  dependsOn("test")
}


