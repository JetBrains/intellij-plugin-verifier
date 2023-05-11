import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(sharedLibs.plugins.kotlin.jvm)
    id("org.jetbrains.kotlin.plugin.serialization") version sharedLibs.versions.kotlin apply false
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
}

val fatJar by tasks.registering(Jar::class) {
    val jar = tasks.jar.get()
    manifest.from(jar.manifest)

    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    val zipsOrJars = configurations.runtimeClasspath
        .get()
        .map {
            if (it.isDirectory) {
                zipTree(it)
            } else {
                it
            }
        }
    from(zipsOrJars)
        .exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA"
        )
    with(jar)
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
    runtimeOnly(group = "ch.qos.logback", name = "logback-classic", version = "1.4.6")

    testImplementation("junit:junit:4.13.2")
    testImplementation(project(":"))
    implementation(sharedLibs.kotlin.stdlib.jdk8)

    val structureVersion = "dev"
    implementation(group = "org.jetbrains.intellij.plugins", name = "verifier-intellij", version = structureVersion)
    implementation(group = "org.jetbrains.intellij.plugins", name = "verifier-cli", version = structureVersion)
    implementation(group = "org.jetbrains.intellij.plugins", name = "structure-ide-classes", version = structureVersion)

    implementation("com.github.spullara.cli-parser:cli-parser:1.1.6")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc")
}

val copyMockIdes by tasks.registering(Copy::class) {
    dependsOn(
        ":mock-old-ide:prepareIde",
        ":mock-new-ide:prepareIde"
    )
    into("$buildDir/mock-ides")

    val oldIde = copySpec {
        from(File(project("mock-old-ide").buildDir, "mock-ide"))
        into("old-ide")
    }
    val newIde = copySpec {
        from(File(project("mock-new-ide").buildDir, "mock-ide"))
        into("new-ide")
    }
    with(oldIde, newIde)
}

tasks.test {
    dependsOn(copyMockIdes, copyMockIdes.get().outputs.files)
}
