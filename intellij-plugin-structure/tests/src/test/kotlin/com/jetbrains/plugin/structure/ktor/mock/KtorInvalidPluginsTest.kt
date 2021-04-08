package com.jetbrains.plugin.structure.ktor.mock

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.readText
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.ktor.KtorFeature
import com.jetbrains.plugin.structure.ktor.KtorFeaturePluginManager
import com.jetbrains.plugin.structure.ktor.bean.*
import com.jetbrains.plugin.structure.ktor.problems.*
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class KtorInvalidPluginsTest(fileSystemType: FileSystemType) :
    BasePluginManagerTest<KtorFeature, KtorFeaturePluginManager>(fileSystemType) {
    override fun createManager(extractDirectory: Path): KtorFeaturePluginManager =
        KtorFeaturePluginManager.createManager(extractDirectory)

    @Test(expected = IllegalArgumentException::class)
    fun `file does not exist`() {
        assertProblematicPlugin(Paths.get("does-not-exist.zip"), emptyList())
    }

    @Test
    fun `invalid file extension`() {
        val incorrect = temporaryFolder.newFile("incorrect.json")
        assertProblematicPlugin(incorrect, listOf(createIncorrectKtorFeatureFile(incorrect.simpleName)))
    }

    @Test
    fun `name is not specified`() {
        checkInvalidPlugin(PropertyNotSpecified(NAME)) { name = null }
        checkInvalidPlugin(PropertyNotSpecified(NAME)) { name = "" }
        checkInvalidPlugin(PropertyNotSpecified(NAME)) { name = "\n" }
    }

    @Test
    fun `id is not specified`() {
        checkInvalidPlugin(PropertyNotSpecified(ID)) { id = null }
        checkInvalidPlugin(PropertyNotSpecified(ID)) { id = "" }
        checkInvalidPlugin(PropertyNotSpecified(ID)) { id = "\n" }
    }

    @Test
    fun `vendor is not specified`() {
        checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = null }
        checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = KtorVendor() }
        checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = KtorVendor("") }
        checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = KtorVendor("\n") }
    }

    @Test
    fun `description is not specified`() {
        checkInvalidPlugin(PropertyNotSpecified(SHORT_DESCRIPTION)) { shortDescription = null }
        checkInvalidPlugin(PropertyNotSpecified(SHORT_DESCRIPTION)) { shortDescription = "" }
        checkInvalidPlugin(PropertyNotSpecified(SHORT_DESCRIPTION)) { shortDescription = "\n" }
    }

    @Test
    fun `ktor version is not specified`() {
        checkInvalidPlugin(PropertyNotSpecified(KTOR_VERSION)) { ktorVersion = null }
        checkInvalidPlugin(PropertyNotSpecified(KTOR_VERSION)) {
            ktorVersion = KtorFeatureVersionDescriptor(null, "1.4.9")
        }
        checkInvalidPlugin(PropertyNotSpecified(KTOR_VERSION)) {
            ktorVersion = KtorFeatureVersionDescriptor(null, "1.4.9")
        }
        checkInvalidPlugin(PropertyNotSpecified(KTOR_VERSION)) {
            ktorVersion = KtorFeatureVersionDescriptor("1.4.0", null)
        }
        checkInvalidPlugin(PropertyNotSpecified(KTOR_VERSION)) {
            ktorVersion = KtorFeatureVersionDescriptor("1.4.0", "\n")
        }
        checkInvalidPlugin(PropertyNotSpecified(KTOR_VERSION)) {
            ktorVersion = KtorFeatureVersionDescriptor("\n", "1.4.0")
        }
        checkInvalidPlugin(IncorrectKtorVersionFormat("1.3.3.1")) {
            ktorVersion = KtorFeatureVersionDescriptor("1.3.3.1", "1.3.3.1")
        }

        checkInvalidPlugin(IncorrectKtorVersionRange("1.4.0", "1.3.9")) {
            ktorVersion = KtorFeatureVersionDescriptor("1.4.0-aba", "1.3.9-koko")
        }
    }


    @Test
    fun `documentation is not specified`() {
        checkInvalidPlugin(PropertyNotSpecified(DOCUMENTATION)) { documentation = null }
        checkInvalidPlugin(PropertyNotSpecified(DOCUMENTATION_DESCRIPTION)) {
            documentation!!.description = null
        }
        checkInvalidPlugin(PropertyNotSpecified(DOCUMENTATION_USAGE)) {
            documentation!!.usage = null
        }
        checkInvalidPlugin(PropertyNotSpecified(DOCUMENTATION_OPTIONS)) {
            documentation!!.options = null
        }
    }

    @Test
    fun `resource in documentation`() {
        checkInvalidPlugin(DocumentationContainsResource("description")) {
            documentation!!.description = "Image: ![image](http://google.com/blah-blah)"
        }
        checkInvalidPlugin(DocumentationContainsResource("usage")) {
            documentation!!.usage = "Image: ![image](http://google.com/blah-blah)"
        }
        checkInvalidPlugin(DocumentationContainsResource("options")) {
            documentation!!.options = "Image: ![image](http://google.com/blah-blah)"
        }
    }

    @Test
    fun `gradle recipe without maven and vice versa`() {
        checkInvalidPlugin(PropertyNotSpecified(MAVEN_INSTALL)) {
            gradleInstall {}
        }
        checkInvalidPlugin(PropertyNotSpecified(GRADLE_INSTALL)) {
            mavenInstall {}
        }
    }

    @Test
    fun `bad dependencies`() {
        checkInvalidPlugin(PropertyNotSpecified(DEPENDENCY_GROUP)) {
            dependencies = listOf(
                DependencyJsonBuilder(
                    group = null
                )
            )
        }
        checkInvalidPlugin(PropertyNotSpecified(DEPENDENCY_ARTIFACT)) {
            dependencies = listOf(
                DependencyJsonBuilder(
                    artifact = null
                )
            )
        }
        checkInvalidPlugin(PropertyNotSpecified(DEPENDENCY_VERSION)) {
            dependencies = listOf(
                DependencyJsonBuilder(
                    version = ""
                )
            )
        }
        checkInvalidPlugin(EmptyDependencies()) {
            dependencies = listOf()
            testDependencies = listOf()
        }
    }

    @Test
    fun `bad template position`() {
        checkInvalidPlugin(PropertyNotSpecified(TEMPLATE_POSITION)) {
            installRecipe {
                addTemplate {
                    position = null
                    text = "abacabadabacabaeabacabadabacaba"
                }
            }
        }
    }

    @Test
    fun `file name for template file`() {
        checkInvalidPlugin(NoFileNameForTemplate("\"resources\"")) {
            installRecipe {
                addTemplate {
                    position = "resources"
                    text = "abacabadabacabaeabacabadabacaba"
                }
            }
        }
        checkInvalidPlugin(FileNameNotNeededForTemplate("\"in_routing\"")) {
            installRecipe {
                addTemplate {
                    position = "in_routing"
                    name = "File.kt"
                    text = "abacabadabacabaeabacabadabacaba"
                }
            }
        }
    }

    private fun checkInvalidPlugin(problem: PluginProblem, descriptor: KtorFeatureJsonBuilder.() -> Unit) {
        val pluginFile = buildZipFile(temporaryFolder.newFolder().resolve("feature.zip")) {
            file(KtorFeaturePluginManager.DESCRIPTOR_NAME) {
                val builder = perfectKtorFeatureBuilder
                builder.descriptor()
                builder.asString().also {
                    println("plugin:\n${it}")
                }
            }
        }
        assertProblematicPlugin(pluginFile, listOf(problem))
    }
}