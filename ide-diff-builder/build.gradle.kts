import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(sharedLibs.plugins.kotlin.jvm)
    id("org.jetbrains.kotlin.plugin.serialization") version sharedLibs.versions.kotlin apply false
    alias(sharedLibs.plugins.shadow)
}

allprojects {
    apply {
        plugin("java")
        plugin("kotlin")
        plugin("org.jetbrains.kotlin.plugin.serialization")
    }

    repositories {
        maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        mavenCentral()
        mavenLocal()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val manifestAttributes = mapOf("Main-Class" to "org.jetbrains.ide.diff.builder.MainKt")

tasks {
    jar {
        manifest {
            attributes(manifestAttributes)
        }
    }
    shadowJar {
        manifest {
            attributes(manifestAttributes)
        }
        archiveClassifier = "all"
    }
}

val fatJar by tasks.registering(ShadowJar::class) {
    dependsOn(tasks.shadowJar)
}

artifacts {
    fatJar
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        apiVersion = "1.4"
        languageVersion = "1.4"
        freeCompilerArgs = listOf("-Xjvm-default=enable")
    }
}

dependencies {
    runtimeOnly(sharedLibs.logback.classic)

    testImplementation(sharedLibs.junit)
    testImplementation(project(":"))

    val structureVersion = "dev"
    implementation(group = "org.jetbrains.intellij.plugins", name = "verifier-intellij", version = structureVersion)
    implementation(group = "org.jetbrains.intellij.plugins", name = "verifier-cli", version = structureVersion)
    implementation(group = "org.jetbrains.intellij.plugins", name = "structure-ide-classes", version = structureVersion)

    implementation(sharedLibs.spullara.cliParser)
    implementation("org.apache.commons:commons-text:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc")
}

val copyMockIdes by tasks.registering(Copy::class) {
    dependsOn(
        ":mock-old-ide:prepareIde",
        ":mock-new-ide:prepareIde"
    )
    into(layout.buildDirectory.dir("mock-ides"))

    val oldIde = copySpec {
        from(project("mock-old-ide").layout.buildDirectory.dir("mock-ide"))
        into("old-ide")
    }
    val newIde = copySpec {
        from(project("mock-new-ide").layout.buildDirectory.dir("mock-ide"))
        into("new-ide")
    }
    with(oldIde, newIde)
}

tasks.test {
    dependsOn(copyMockIdes, copyMockIdes.get().outputs.files)
}
