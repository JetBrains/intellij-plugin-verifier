import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

version = "1.0"

// Pinned independently from the root build -- see verifier-test/mock-plugin/build.gradle.kts
// for why (bytecode-shape stability of these plugin-under-test fixtures vs. the project's own
// Kotlin toolchain), and the caveat that this doesn't pin the actual compiler binary.
kotlin {
  compilerOptions {
    apiVersion = KotlinVersion.KOTLIN_2_2
    languageVersion = KotlinVersion.KOTLIN_2_2
  }
}

dependencies {
  implementation(project(":verifier-core"))
}

// Fixture sources use bare `@ApiStatus.*` annotations on package declarations, which
// the javadoc tool reads as unknown tags. These modules are not consumed for docs.
tasks.javadoc { enabled = false }
