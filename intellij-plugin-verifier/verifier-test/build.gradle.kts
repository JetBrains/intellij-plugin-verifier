dependencies {
  implementation(project(":verifier-cli"))

  testRuntimeOnly(sharedLibs.logback.classic)
  testRuntimeOnly(project("mock-plugin"))

  //bytecode generation library
  implementation(libs.byteBuddy)

  testImplementation("uk.org.webcompere:system-stubs-junit4:2.0.2")
  testImplementation("uk.org.webcompere:system-stubs-junit4:2.0.2")
  testImplementation("com.google.jimfs:jimfs:1.2")
}

val prepareMockPlugin by tasks.registering(Copy::class) {
  dependsOn(":verifier-test:mock-plugin:build")
  into("$buildDir/mocks")
  val mockPluginBuildDir = project("mock-plugin").buildDir
  from(File(mockPluginBuildDir, "libs/mock-plugin-1.0.jar"))
}

val afterIdeaBuildDir = project("after-idea").buildDir
val additionalAfterIdeaBuildDir = project("additional-after-idea").buildDir

/**
 * Creates resources.jar file with brokenPlugins.txt file inside.
 */

val prepareResourcesJar by tasks.registering(Jar::class) {
  dependsOn(tasks.getByPath("after-idea:processResources"))
  from("$afterIdeaBuildDir/resources/main/brokenPlugins.txt")
  destinationDirectory.set(buildDir)
  archiveFileName.set("resources.jar")
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
  into("$buildDir/mocks/after-idea")

  val ideaJar = copySpec {
    from("$afterIdeaBuildDir/libs/after-idea-1.0.jar")
    into("lib")
  }
  val additionalJar = copySpec {
    from("$additionalAfterIdeaBuildDir/libs/additional-after-idea-1.0.jar")
    into("lib")
  }

  val resourcesJar = copySpec {
    from(prepareResourcesJar)
    into("lib")
  }

  val buildTxt = copySpec {
    from("$afterIdeaBuildDir/resources/main/build.txt")
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


