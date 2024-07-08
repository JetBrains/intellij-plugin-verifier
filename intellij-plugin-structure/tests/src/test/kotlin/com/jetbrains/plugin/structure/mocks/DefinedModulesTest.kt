package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Test
import java.nio.file.Paths
import org.junit.Assert.assertEquals

class DefinedModulesTest(fileSystemType: FileSystemType) : IdePluginManagerTest(fileSystemType) {

    private val mockPluginRoot = Paths.get(this::class.java.getResource("/python-plugin").toURI())
    private val metaInfDir = mockPluginRoot.resolve("META-INF")

    @Test
    fun `python defined modules`() {
        val plugin = buildPluginSuccess(emptyList()) {
            buildZipFile(temporaryFolder.newFile("plugin.zip")) {
                dir("python") {
                    dir("lib") {
                        zip("plugin.jar") {
                            dir("META-INF", metaInfDir)
                            file(
                                "intellij.commandInterface.xml",
                                mockPluginRoot.resolve("intellij.commandInterface.xml")
                            )
                            file("intellij.django.core.xml", mockPluginRoot.resolve("intellij.django.core.xml"))
                            file("intellij.jinja.xml", mockPluginRoot.resolve("intellij.jinja.xml"))
                            file("intellij.jupyter.core.xml", mockPluginRoot.resolve("intellij.jupyter.core.xml"))
                            file(
                                "intellij.platform.commercial.verifier.xml",
                                mockPluginRoot.resolve("intellij.platform.commercial.verifier.xml")
                            )
                            file(
                                "intellij.python.community.deprecated.extensions.xml",
                                mockPluginRoot.resolve("intellij.python.community.deprecated.extensions.xml")
                            )
                            file(
                                "intellij.python.community.impl.community_only.xml",
                                mockPluginRoot.resolve("intellij.python.community.impl.community_only.xml")
                            )
                            file(
                                "intellij.python.community.impl.huggingFace.xml",
                                mockPluginRoot.resolve("intellij.python.community.impl.huggingFace.xml")
                            )
                            file(
                                "intellij.python.community.impl.poetry.xml",
                                mockPluginRoot.resolve("intellij.python.community.impl.poetry.xml")
                            )
                            file(
                                "intellij.python.community.impl.xml",
                                mockPluginRoot.resolve("intellij.python.community.impl.xml")
                            )
                            file(
                                "intellij.python.community.plugin.impl.xml",
                                mockPluginRoot.resolve("intellij.python.community.plugin.impl.xml")
                            )
                            file(
                                "intellij.python.community.plugin.java.xml",
                                mockPluginRoot.resolve("intellij.python.community.plugin.java.xml")
                            )
                            file(
                                "intellij.python.concurrencyVisualizer.xml",
                                mockPluginRoot.resolve("intellij.python.concurrencyVisualizer.xml")
                            )
                            file(
                                "intellij.python.copyright.xml",
                                mockPluginRoot.resolve("intellij.python.copyright.xml")
                            )
                            file(
                                "intellij.python.djangoDbConfig.xml",
                                mockPluginRoot.resolve("intellij.python.djangoDbConfig.xml")
                            )
                            file("intellij.python.docker.xml", mockPluginRoot.resolve("intellij.python.docker.xml"))
                            file(
                                "intellij.python.duplicatesDetection.xml",
                                mockPluginRoot.resolve("intellij.python.duplicatesDetection.xml")
                            )
                            file(
                                "intellij.python.endpoints.xml",
                                mockPluginRoot.resolve("intellij.python.endpoints.xml")
                            )
                            file(
                                "intellij.python.featuresTrainer.xml",
                                mockPluginRoot.resolve("intellij.python.featuresTrainer.xml")
                            )
                            file("intellij.python.gherkin.xml", mockPluginRoot.resolve("intellij.python.gherkin.xml"))
                            file("intellij.python.grazie.xml", mockPluginRoot.resolve("intellij.python.grazie.xml"))
                            file(
                                "intellij.python.javascript.debugger.xml",
                                mockPluginRoot.resolve("intellij.python.javascript.debugger.xml")
                            )
                            file(
                                "intellij.python.langInjection.xml",
                                mockPluginRoot.resolve("intellij.python.langInjection.xml")
                            )
                            file("intellij.python.markdown.xml", mockPluginRoot.resolve("intellij.python.markdown.xml"))
                            file(
                                "intellij.python.plugin.java.xml",
                                mockPluginRoot.resolve("intellij.python.plugin.java.xml")
                            )
                            file("intellij.python.pro.js.xml", mockPluginRoot.resolve("intellij.python.pro.js.xml"))
                            file(
                                "intellij.python.pro.localization.xml",
                                mockPluginRoot.resolve("intellij.python.pro.localization.xml")
                            )
                            file("intellij.python.profiler.xml", mockPluginRoot.resolve("intellij.python.profiler.xml"))
                            file("intellij.python.pyramid.xml", mockPluginRoot.resolve("intellij.python.pyramid.xml"))
                            file(
                                "intellij.python.pytestBdd.xml",
                                mockPluginRoot.resolve("intellij.python.pytestBdd.xml")
                            )
                            file(
                                "intellij.python.remoteInterpreter.xml",
                                mockPluginRoot.resolve("intellij.python.remoteInterpreter.xml")
                            )
                            file(
                                "intellij.python.reStructuredText.xml",
                                mockPluginRoot.resolve("intellij.python.reStructuredText.xml")
                            )
                            file(
                                "intellij.python.scientific.xml",
                                mockPluginRoot.resolve("intellij.python.scientific.xml")
                            )
                            file(
                                "intellij.python.templateLanguages.xml",
                                mockPluginRoot.resolve("intellij.python.templateLanguages.xml")
                            )
                            file("intellij.python.terminal.xml", mockPluginRoot.resolve("intellij.python.terminal.xml"))
                            file("intellij.python.uml.xml", mockPluginRoot.resolve("intellij.python.uml.xml"))
                            file("intellij.python.wsl.xml", mockPluginRoot.resolve("intellij.python.wsl.xml"))
                            file("intellij.python.xml", mockPluginRoot.resolve("intellij.python.xml"))
                            file("intellij.template.lang.core.xml", mockPluginRoot.resolve("intellij.template.lang.core.xml"))
                            file("intellij.python.endpointsHttpclient.xml", mockPluginRoot.resolve("intellij.python.endpointsHttpclient.xml"))
                            file("intellij.python.endpointsMicroservicesUI.xml", mockPluginRoot.resolve("intellij.python.endpointsMicroservicesUI.xml"))
                        }
                    }
                }
            }
        }

        val expectedModules = listOf(
            "com.intellij.modules.python",
            "com.intellij.modules.python.scientific",
            "intellij.commandInterface",
            "intellij.django.core",
            "intellij.jinja",
            "intellij.python",
            "intellij.python.community.deprecated.extensions",
            "intellij.python.community.impl",
            "intellij.python.community.impl.huggingFace",
            "intellij.python.community.impl.poetry",
            "intellij.python.community.plugin.impl",
            "intellij.python.community.plugin.java",
            "intellij.python.concurrencyVisualizer",
            "intellij.python.copyright",
            "intellij.python.djangoDbConfig",
            "intellij.python.docker",
            "intellij.python.duplicatesDetection",
            "intellij.python.endpoints",
            "intellij.python.endpointsHttpclient",
            "intellij.python.endpointsMicroservicesUI",
            "intellij.python.featuresTrainer",
            "intellij.python.gherkin",
            "intellij.python.grazie",
            "intellij.python.javascript.debugger",
            "intellij.python.langInjection",
            "intellij.python.markdown",
            "intellij.python.plugin.java",
            "intellij.python.pro.js",
            "intellij.python.pro.localization",
            "intellij.python.profiler",
            "intellij.python.pyramid",
            "intellij.python.pytestBdd",
            "intellij.python.reStructuredText",
            "intellij.python.remoteInterpreter",
            "intellij.python.scientific",
            "intellij.python.templateLanguages",
            "intellij.python.terminal",
            "intellij.python.uml",
            "intellij.python.wsl",
            "intellij.template.lang.core",
        )
        val modules = plugin.definedModules
        assertEquals(expectedModules.size, modules.size)
        expectedModules.forEach {
            assert(modules.contains(it)) { "There is no module $it" }
        }
    }
}