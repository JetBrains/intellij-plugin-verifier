package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.filtering.InternalApiUsageFilter
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tests.InternalApiUsagePluginTest.IdeaPluginSpec
import com.jetbrains.pluginverifier.tests.InternalApiUsagePluginTest.IntellijInternalApiDump
import com.jetbrains.pluginverifier.usages.internal.kotlin.KtInternalClassUsage
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import kotlinx.metadata.KmClass
import kotlinx.metadata.Visibility
import kotlinx.metadata.jvm.JvmMetadataVersion
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.visibility
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers.named
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.lang.reflect.Modifier

class KotlinInternalModifierUsageTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private val byteBuddy = ByteBuddy()

  private val internalMethodUsageMsg = "Internal method com.intellij.openapi.InternalApiService.fortyTwo() : " +
    "int is invoked in usage.Usage.delegateFortyTwo() : int. " +
    "This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation " +
    "or @com.intellij.openapi.util.IntellijInternalApi annotation " +
    "and indicates that the method is not supposed to be used in client code."

  private val internalClassUsageMsg = "Internal class com.intellij.openapi.InternalApiService " +
    "is referenced in usage.Usage.delegateFortyTwo() : int. " +
    "This class is marked with Kotlin `internal` visibility modifier."

  @Test
  fun `JetBrains plugin class uses an internal API`() {
    val (idePlugin, ide) = prepareIde(IdeaPluginSpec("com.intellij.plugin", "JetBrains s.r.o."))

    val apiUsageFilter = InternalApiUsageFilter()

    // Run verification
    val verificationResult = VerificationRunner().runPluginVerification(ide, idePlugin,
      apiUsageFilters = listOf(apiUsageFilter)) as PluginVerificationResult.Verified

    // No warnings should be produced
    assertEquals(emptySet<CompatibilityProblem>(), verificationResult.compatibilityProblems)
    assertEquals(emptySet<CompatibilityWarning>(), verificationResult.compatibilityWarnings)
    // JetBrains Plugin should not report internal usages. These are in the ignored usages
    assertEquals(0, verificationResult.internalApiUsages.size)
    val ignoredUsages = verificationResult.ignoredInternalApiUsages.keys

    val internalClassUsages = ignoredUsages.filterIsInstance<KtInternalClassUsage>()
    assertEquals(1, internalClassUsages.size)
    assertEquals(internalClassUsageMsg, internalClassUsages[0].fullDescription)
  }


  private fun prepareIde(pluginSpec: IdeaPluginSpec): Pair<IdePlugin, Ide> {
    val classLoader = this::class.java.classLoader

    val internalApiServiceClassName = "com.intellij.openapi.InternalApiService"

    val kotlinMetadataKmClass = KmClass().apply {
      name = "internalApiServiceClassName"
      visibility = Visibility.INTERNAL
    }
    val annotationData = KotlinClassMetadata.Class(kotlinMetadataKmClass, JvmMetadataVersion.LATEST_STABLE_SUPPORTED, 0).write()

    val internalApiServiceClassUdt = byteBuddy
      .subclass(Object::class.java)
      .name(internalApiServiceClassName)
      .annotateType(annotationData)
      .defineMethod("fortyTwo", Integer.TYPE, Modifier.PUBLIC).intercept(FixedValue.value(42))
      .make()

    val internalApiClazz = load(internalApiServiceClassUdt, classLoader, internalApiServiceClassName)
    val internalApiService = internalApiClazz.getDeclaredConstructor().newInstance()

    val usageClassName = "usage.Usage"
    val usageClassUdt = byteBuddy
      .subclass(Object::class.java)
      .name(usageClassName)
      .defineMethod("delegateFortyTwo", Integer.TYPE, Modifier.PUBLIC).intercept(
        MethodDelegation
          .withDefaultConfiguration()
          .filter(named("fortyTwo")).to(internalApiService))
      .make()

    val usageClass = load(usageClassUdt, classLoader, usageClassName)
    val usage = usageClass.getDeclaredConstructor().newInstance()

    val result = usageClass.getMethod("delegateFortyTwo").invoke(usage)
    assertEquals(42, result)


    val idePlugin = buildIdePlugin(pluginSpec) {
      dir("usage") {
        file("Usage.class", usageClassUdt.bytes)
      }
    }

    val ide = buildIdeWithBundledPlugins(javaPluginClassesBuilder = {
      dir("com") {
        dir("intellij") {
          dir("openapi") {
            dir("util") {
              file("IntellijInternalApi.class", IntellijInternalApiDump.dump())
            }
            file("InternalApiService.class", internalApiServiceClassUdt.bytes)
          }
        }

      }
    })
    return idePlugin to ide
  }

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  private fun load(classDynamicType: DynamicType.Unloaded<Object>, classLoader: ClassLoader, className: String): Class<out Any> {
    return try {
      classDynamicType.load(classLoader, ClassLoadingStrategy.Default.INJECTION).loaded
    } catch (e: IllegalStateException) {
      // class might have already been injected into classloader from previous runs
      classLoader.loadClass(className)
    }
  }

  private fun buildIdeWithBundledPlugins(
    javaPluginClassesBuilder: (ContentBuilder).() -> Unit
  ): Ide {
    val ideaDirectory = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
      file("build.txt", "IU-192.1")
      dir("lib") {
        zip("idea.jar") {
          dir("META-INF") {
            file("plugin.xml") {
              """
                <idea-plugin>
                  <id>com.intellij</id>
                  <name>IDEA CORE</name>
                  <version>1.0</version>
                  <module value="com.intellij.modules.all"/>                
                </idea-plugin>
                """.trimIndent()
            }
          }
        }
      }
      dir("plugins") {
        dir("java") {
          dir("lib") {
            zip("java.jar") {
              dir("META-INF") {
                file("plugin.xml") {
                  """
                    <idea-plugin>
                      <id>com.intellij.java</id>
                      <module value="com.intellij.modules.java"/>
                    </idea-plugin>
                    """.trimIndent()
                }
              }

              //Generate content of Java plugin.
              javaPluginClassesBuilder()
            }
          }
        }
      }
    }

    // Fast assert IDE is fine

    val ide = IdeManager.createManager().createIde(ideaDirectory)
    assertEquals("IU-192.1", ide.version.asString())

    val javaPlugin = ide.bundledPlugins.find { it.pluginId == "com.intellij.java" }!!
    assertEquals("com.intellij.java", javaPlugin.pluginId)
    assertEquals(setOf("com.intellij.modules.java"), javaPlugin.definedModules)

    return ide
  }

  private fun buildIdePlugin(ideaPluginSpec: IdeaPluginSpec = IdeaPluginSpec("com.intellij", "JetBrains s.r.o."),
                             pluginClassesContentBuilder: (ContentBuilder).() -> Unit
  ): IdePlugin {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath()) {
      pluginClassesContentBuilder()

      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <id>${ideaPluginSpec.id}</id>
              <name>someName</name>
              <version>someVersion</version>
              ""<vendor email="vendor.com" url="url">${ideaPluginSpec.vendor}</vendor>""
              <description>this description is looooooooooong enough</description>
              <change-notes>these change-notes are looooooooooong enough</change-notes>
              <idea-version since-build="131.1"/>
              <depends>org.intellij.groovy</depends>
              <depends>com.intellij.modules.java</depends>
            </idea-plugin>
            """.trimIndent()
        }
      }
    }

    return (IdePluginManager.createManager().createPlugin(pluginFile) as PluginCreationSuccess).plugin
  }
}