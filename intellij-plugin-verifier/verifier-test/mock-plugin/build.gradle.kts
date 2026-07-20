import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

version = "1.0"

// Pinned independently from the root build's language level: this module (and the other
// verifier-test fixtures) stands in for a plugin someone else compiled, so its bytecode
// shape shouldn't drift just because this project bumps its own Kotlin toolchain. Note this
// only decouples apiVersion/languageVersion (the source-language target) -- Gradle allows only
// one Kotlin compiler *binary* version per build, so these fixtures are still compiled by the
// same kotlinc as the rest of the project. See KotlinMethods.kt for why that distinction matters:
// it's what caused `check that all internal API violating usages are found` in VerificationTest
// to regress when the project's Kotlin compiler was bumped past 2.2.
kotlin {
  compilerOptions {
    apiVersion = KotlinVersion.KOTLIN_2_2
    languageVersion = KotlinVersion.KOTLIN_2_2
  }
}

dependencies {
  //compile against "before-idea", "additional-before-idea" and verify against "after-idea", "additional-after-idea"

  compileOnly(project(":verifier-test:before-idea"))
  runtimeOnly(project(":verifier-test:after-idea"))
  compileOnly(project(":verifier-test:additional-before-idea"))
  runtimeOnly(project(":verifier-test:additional-after-idea"))
}
