/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import com.jetbrains.pluginverifier.usages.internal.InternalClassUsage
import com.jetbrains.pluginverifier.usages.internal.InternalMethodUsage
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.jar.asm.AnnotationVisitor
import net.bytebuddy.jar.asm.ClassWriter
import net.bytebuddy.jar.asm.Opcodes
import net.bytebuddy.jar.asm.Opcodes.*
import net.bytebuddy.matcher.ElementMatchers.named
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.lang.reflect.Modifier

class InternalApiUsagePluginTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private val internalMethodUsageMsg = "Internal method com.intellij.openapi.InternalApiService.fortyTwo() : " +
    "int is invoked in usage.Usage.delegateFortyTwo() : int. " +
    "This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation " +
    "or @com.intellij.openapi.util.IntellijInternalApi annotation " +
    "and indicates that the method is not supposed to be used in client code."

  private val internalClassUsageMsg = "Internal class com.intellij.openapi.InternalApiService " +
    "is referenced in usage.Usage.delegateFortyTwo() : int. " +
    "This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation " +
    "or @com.intellij.openapi.util.IntellijInternalApi annotation " +
    "and indicates that the class is not supposed to be used in client code."

  @Test
  fun `plugin class uses an internal API`() {
    val (idePlugin, ide) = prepareIde(IdeaPluginSpec("some.plugin", "Plugin Factory Inc."))

    // Run verification
    val verificationResult = VerificationRunner().runPluginVerification(ide, idePlugin) as PluginVerificationResult.Verified

    // No warnings should be produced
    assertEquals(emptySet<CompatibilityProblem>(), verificationResult.compatibilityProblems)
    assertEquals(emptySet<CompatibilityWarning>(), verificationResult.compatibilityWarnings)
    // Internal API usages should be gathered
    assertEquals(3, verificationResult.internalApiUsages.size)
    val internalMethodUsages = verificationResult.internalApiUsages.filterIsInstance<InternalMethodUsage>()
    assertEquals(1, internalMethodUsages.size)
    assertEquals(internalMethodUsageMsg, internalMethodUsages[0].fullDescription)

    val internalClassUsages = verificationResult.internalApiUsages.filterIsInstance<InternalClassUsage>()
    assertEquals(2, internalClassUsages.size)
    // ignore internal ByteBuddy delegates due to MethodDelegations
    val relevantInternalClassUsages = internalClassUsages.filterNot { u -> u.fullDescription.contains("usage.Usage.delegate$") }
    assertEquals(1, relevantInternalClassUsages.size)
    assertEquals(internalClassUsageMsg, relevantInternalClassUsages[0].fullDescription)
  }

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
    assertEquals(3, ignoredUsages.size)
    val internalMethodUsages = ignoredUsages.filterIsInstance<InternalMethodUsage>()
    assertEquals(internalMethodUsageMsg, internalMethodUsages[0].fullDescription)

    val internalClassUsages = ignoredUsages.filterIsInstance<InternalClassUsage>()
    assertEquals(2, internalClassUsages.size)
    // ignore internal ByteBuddy delegates due to MethodDelegations
    val relevantInternalClassUsages = internalClassUsages.filterNot { u -> u.fullDescription.contains("usage.Usage.delegate$") }
    assertEquals(1, relevantInternalClassUsages.size)
    assertEquals(internalClassUsageMsg, relevantInternalClassUsages[0].fullDescription)
  }

  @Test
  fun `in the same plugin, a class overrides an internal API method of another class`() {
    val (idePlugin, ide) = prepareServiceAndOverrider(IdeaPluginSpec("com.example.somePlugin", "Some Vendor"))

    val apiUsageFilter = InternalApiUsageFilter()

    // Run verification
    val verificationResult = VerificationRunner().runPluginVerification(ide, idePlugin,
      apiUsageFilters = listOf(apiUsageFilter)) as PluginVerificationResult.Verified

    assertEquals(0, verificationResult.internalApiUsages.size)
  }

  @Suppress("UNCHECKED_CAST")
  private fun prepareServiceAndOverrider(pluginSpec: IdeaPluginSpec): Pair<IdePlugin, Ide> {
    val classLoader = this::class.java.classLoader
    val byteBuddy = ByteBuddy()

    val internalServiceClassName = "com.example.plugin.InternalService"
    val internalServiceUdt = byteBuddy
      .subclass(Object::class.java)
      .name(internalServiceClassName)
      .annotateType(intelliJInternalApi())
      .defineMethod("fortyTwo", Integer.TYPE, Modifier.PUBLIC).intercept(FixedValue.value(42))
      .make()
    val internalServiceClass = load(internalServiceUdt, classLoader, internalServiceClassName)

    val overriderClassName = "com.example.plugin.Overrider"
    val overriderUdt = byteBuddy
      .subclass(internalServiceClass)
      .name(overriderClassName)
      // override method from parent
      .defineMethod("fortyTwo", Integer.TYPE, Modifier.PUBLIC).intercept(FixedValue.value(42))
      .make() as DynamicType.Unloaded<Object>

    val idePlugin = buildIdePlugin(pluginSpec) {
      dirs("com/example/plugin") {
        file("InternalService.class", internalServiceUdt.bytes)
        file("Overrider.class", overriderUdt.bytes)
      }
    }

    val ide = buildIdeWithBundledPlugins(javaPluginClassesBuilder = {}, groovyPluginClassesBuilder = {})

    return idePlugin to ide
  }

  @Suppress("UNCHECKED_CAST")
  private fun intelliJInternalApi(): AnnotationDescription {
     val intellijInternalApiClass = Class
      .forName("com.intellij.openapi.util.IntellijInternalApi") as Class<Annotation>
    return AnnotationDescription.Builder
      .ofType(intellijInternalApiClass)
      .build()
  }

  private fun prepareIde(pluginSpec: IdeaPluginSpec): Pair<IdePlugin, Ide> {
    val classLoader = this::class.java.classLoader
    val byteBuddy = ByteBuddy()

    val internalApiServiceClassName = "com.intellij.openapi.InternalApiService"
    val internalApiServiceClassUdt = byteBuddy
      .subclass(Object::class.java)
      .name(internalApiServiceClassName)
      .annotateType(intelliJInternalApi())
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
    }, groovyPluginClassesBuilder = {})
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

  private fun buildIdePlugin(
    ideaPluginSpec: IdeaPluginSpec = IdeaPluginSpec("com.intellij", "JetBrains s.r.o."),
    pluginClassesContentBuilder: (ContentBuilder).() -> Unit
  ): IdePlugin {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath()) {
      this.pluginClassesContentBuilder()

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

  private fun buildIdeWithBundledPlugins(
          javaPluginClassesBuilder: (ContentBuilder).() -> Unit,
          groovyPluginClassesBuilder: (ContentBuilder).() -> Unit
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
              this.javaPluginClassesBuilder()
            }
          }
        }
        dir("groovy") {
          dir("lib") {
            zip("groovy.jar") {
              dir("META-INF") {
                file("plugin.xml") {
                  """
                    <idea-plugin>
                      <id>org.intellij.groovy</id>
                      <depends>com.intellij.modules.java</depends>
                    </idea-plugin>
                  """
                }
              }

              //Generate content of Groovy plugin.
              this.groovyPluginClassesBuilder()
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

    val groovyPlugin = ide.bundledPlugins.find { it.pluginId == "org.intellij.groovy" }!!
    assertEquals("org.intellij.groovy", groovyPlugin.pluginId)

    return ide
  }

  object IntellijInternalApiDump : Opcodes {
    @Throws(Exception::class)
    fun dump(): ByteArray {
      val classWriter = ClassWriter(0)
      var annotationVisitor: AnnotationVisitor
      classWriter.visit(V1_8, ACC_PUBLIC or ACC_ANNOTATION or ACC_ABSTRACT or ACC_INTERFACE, "com/intellij/openapi/util/IntellijInternalApi", null, "java/lang/Object", arrayOf<String>("java/lang/annotation/Annotation"))
      classWriter.visitSource("IntellijInternalApi.java", null)
      run {
        annotationVisitor = classWriter.visitAnnotation("Ljava/lang/annotation/Retention;", true)
        annotationVisitor.visitEnum("value", "Ljava/lang/annotation/RetentionPolicy;", "RUNTIME")
        annotationVisitor.visitEnd()
      }
      classWriter.visitEnd()
      return classWriter.toByteArray()
    }
  }

}
