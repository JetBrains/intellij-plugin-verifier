package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.filtering.InternalApiUsageFilter
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tests.bytecode.Dumps.ComIntellijTasks_TaskRepositorySubtype
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import com.jetbrains.pluginverifier.tests.mocks.PluginSpec
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.JvmMetadataVersion
import kotlinx.metadata.jvm.KotlinClassMetadata
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender
import net.bytebuddy.implementation.bytecode.ByteCodeAppender.Size
import net.bytebuddy.jar.asm.Label
import net.bytebuddy.jar.asm.MethodVisitor
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.Opcodes.*
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass

abstract class BaseBytecodeTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var byteBuddy: ByteBuddy

  @Before
  fun setUp() {
    byteBuddy = ByteBuddy()
  }

  protected fun verify(ide: Ide, idePlugin: IdePlugin): Set<InternalApiUsage> {
    val apiUsageFilter = InternalApiUsageFilter()

    // Run verification
    val verificationResult = VerificationRunner().runPluginVerification(
      ide, idePlugin,
      apiUsageFilters = listOf(apiUsageFilter)
    ) as PluginVerificationResult.Verified

    // No warnings should be produced
    assertEquals(emptySet<CompatibilityProblem>(), verificationResult.compatibilityProblems)
    assertEquals(emptySet<CompatibilityWarning>(), verificationResult.compatibilityWarnings)
    // JetBrains Plugin should not report internal usages. These are in the ignored usages
    assertEquals(0, verificationResult.internalApiUsages.size)
    return verificationResult.ignoredInternalApiUsages.keys
  }

  protected fun assertVerified(spec: VerificationSpec.() -> Unit) =
    runPluginVerification(spec) as PluginVerificationResult.Verified

  internal fun prepareUsage(
    pluginSpec: IdeaPluginSpec,
    dynamicTypeBuilder: () -> DynamicType.Unloaded<*>
  ): IdePlugin {
    return buildIdePlugin(pluginSpec) {
      usageClass(dynamicTypeBuilder())
    }
  }

  internal fun prepareUsage(
    pluginSpec: IdeaPluginSpec,
    classFileName: String,
    classFileBinaryContent: ByteArray
  ): IdePlugin {
    return buildIdePlugin(pluginSpec) {
      dir("plugin") {
        file("$classFileName.class", classFileBinaryContent)
      }
    }
  }

  protected fun prepareIdeWithApi(platformApiClassTypeBuilder: () -> DynamicType.Unloaded<*>): Ide {
    return buildIdeWithBundledPlugins {
      dir("com") {
        dir("intellij") {
          dir("openapi") {
            apiClass(platformApiClassTypeBuilder())
          }
        }
      }
    }
  }

  private fun ContentBuilder.usageClass(dynamicType: DynamicType.Unloaded<*>) {
    val className = dynamicType.typeDescription.simpleName
    dir("usage") {
      file("$className.class", dynamicType.bytes)
    }
  }

  private fun ContentBuilder.apiClass(dynamicType: DynamicType.Unloaded<*>) {
    val className = dynamicType.typeDescription.simpleName
    file("$className.class", dynamicType.bytes)
  }

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  private fun load(classDynamicType: DynamicType.Unloaded<Object>, classLoader: ClassLoader): Class<out Any> {
    return classDynamicType.load(classLoader, ClassLoadingStrategy.Default.WRAPPER).loaded
  }

  protected fun String.construct() = byteBuddy
    .subclass(Object::class.java)
    .name(this)

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  protected fun String.constructWithMethod(
    method: String,
    returnType: Type,
    modifier: Int = Modifier.PUBLIC
  ): DynamicType.Builder.MethodDefinition.ParameterDefinition.Initial<Object> {
    return construct()
      .defineMethod(method, returnType, modifier)
  }

  protected fun buildIdeWithBundledPlugins(
    includeKotlinStdLib: Boolean = false,
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
        if (includeKotlinStdLib) {
          findKotlinStdLib().apply {
            file(simpleName, this)
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

  /**
   * Builds an instance of the IDE with specified bundled plugins.
   *
   * By default, this IDE contains the `com.intellij` plugin available in the `lib/idea.jar`.
   *
   * @param bundledPlugins List of plugins to include in the `plugins` directory.
   * @param additionalCorePlugins Additional plugins to include in the `lib` directory of the IDE.
   * @param includeKotlinStdLib Whether to include the Kotlin standard library.
   * @param productInfo JSON contents of `product-info.json`
   * @param version The version string of this IDE.
   * @param hasModuleDescriptors If the `module-descriptors.jar` should be created as an empty file.
   * @return The created instance of the IDE.
   */
  internal fun buildIdeWithBundledPlugins(
    bundledPlugins: List<PluginSpec> = emptyList(),
    additionalCorePlugins: List<PluginSpec> = emptyList(),
    includeKotlinStdLib: Boolean = false,
    productInfo: String? = null,
    version: String = "IU-192.1",
    hasModuleDescriptors: Boolean = false
  ): Ide {
    val ideaDirectory = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
      file("build.txt", version)
      productInfo?.let {
        file("product-info.json", it)
      }
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
        additionalCorePlugins.forEach { plugin ->
          plugin.buildJar(this)
        }
        if (includeKotlinStdLib) {
          findKotlinStdLib().apply {
            file(simpleName, this)
          }
        }
        /* A JAR with at least one file in the `com.intellij` package.
          This mimics a regular IDE in order to align with unit tests.
        */
        zip("app.jar") {
          dir("com") {
            dir("intellij") {
              dir("tasks") {
                file("Task.class", ComIntellijTasks_TaskRepositorySubtype())
              }
            }
          }
        }
      }
      dir("plugins") {
        bundledPlugins.forEach { plugin ->
          plugin.build(this)
        }
      }
      if (hasModuleDescriptors) {
        dir("modules") {
          zip("module-descriptors.jar") { /* empty file */ }
        }
      }
    }

    // Fast assert IDE is fine
    val ide = IdeManager.createManager().createIde(ideaDirectory)
    assertEquals(version, ide.version.asString())

    return ide
  }

  private fun buildIdePlugin(
    ideaPluginSpec: IdeaPluginSpec = IdeaPluginSpec("com.intellij", "JetBrains s.r.o."),
    pluginClassesContentBuilder: (ContentBuilder).() -> Unit
  ): IdePlugin {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath()) {
      pluginClassesContentBuilder()

      val additionalDepends = ideaPluginSpec.dependencies.joinToString("\n") { "<depends>$it</depends>" }

      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <id>${ideaPluginSpec.id}</id>
              <name>someName</name>
              <version>someVersion</version>
              <vendor email="vendor.com" url="url">${ideaPluginSpec.vendor}</vendor>
              <description>this description is looooooooooong enough</description>
              <change-notes>these change-notes are looooooooooong enough</change-notes>
              <idea-version since-build="131.1"/>
              <depends>com.intellij.modules.java</depends>
              $additionalDepends
            </idea-plugin>
            """.trimIndent()
        }
      }
    }

    return (IdePluginManager.createManager().createPlugin(pluginFile) as PluginCreationSuccess).plugin
  }

  protected fun kotlinMetadata(configure: KmClass.() -> Unit) = KmClass().apply {
    configure()
  }.let {
    KotlinClassMetadata
      .Class(it, JvmMetadataVersion.LATEST_STABLE_SUPPORTED, 0)
      .write()
  }

  protected fun String.randomize(): String {
    return buildString {
      append(this@randomize)
      append("_")
      append(UUID.randomUUID().toString().replace("-", ""))
    }
  }

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  protected fun DynamicType.Unloaded<Object>.newInstance(classLoader: ClassLoader = this::class.java.classLoader) =
    load(this, classLoader)
      .getDeclaredConstructor().newInstance()

  /**
   * Creates a new instance of `callee` and invokes a `fieldName` while assigning a fixed value on this instance.
   * This occurs within a method of the `caller`.
   * The field value is hardwired to an integer
   */
  protected class DirectFieldAccess(
    private val caller: BinaryClassName,
    private val callee: BinaryClassName,
    private val fieldName: String,
    private val fieldValue: Int
  ) : ByteCodeAppender {

    override fun apply(
      methodVisitor: MethodVisitor,
      implementationContext: Implementation.Context,
      instrumentedMethod: MethodDescription
    ): Size {
      with(methodVisitor) {
        val methodBeginning = Label()
        visitLabel(methodBeginning)
        visitTypeInsn(NEW, callee)
        visitInsn(DUP)
        visitMethodInsn(INVOKESPECIAL, callee, "<init>", "()V", false)
        visitVarInsn(ASTORE, 1)
        val instanceScopeBeginning = Label()
        visitLabel(instanceScopeBeginning)
        visitVarInsn(ALOAD, 1)
        visitIntInsn(BIPUSH, fieldValue)
        visitFieldInsn(PUTFIELD, callee, fieldName, "I")
        visitIntInsn(BIPUSH, fieldValue)
        visitInsn(IRETURN)
        val methodEnd = Label()
        visitLabel(methodEnd)
        visitLocalVariable("this", "L$caller;", null, methodBeginning, methodEnd, 0)
        visitLocalVariable("instance", "L$callee;", null, instanceScopeBeginning, methodEnd, 1)
      }
      return Size(2, 2)
    }

    val implementation: Implementation
      get() = Implementation.Simple(this)
  }

  protected fun assertContains(
    compatibilityProblems: Collection<CompatibilityProblem>,
    compatibilityProblemClass: KClass<out CompatibilityProblem>,
    fullDescription: String? = null
  ) {
    val problems = compatibilityProblems.filterIsInstance(compatibilityProblemClass.java)
    if (problems.isEmpty()) {
      fail("There are no compatibility problems of class [${compatibilityProblemClass.qualifiedName}]")
      return
    }
    if (fullDescription == null) {
      return
    }
    val problemsWithMessage = problems.filter { it.fullDescription == fullDescription }
    if (problemsWithMessage.isEmpty()) {
      fail("Compatibility problems has ${problems.size} problem(s) of class [${compatibilityProblemClass.qualifiedName}], " +
        "but none has a full description '$fullDescription'. " +
        "Found [" + problems.joinToString { it.fullDescription } + "]"
      )
      return
    }
  }

  @Throws(AssertionError::class)
  protected fun PluginVerificationResult.Verified.assertNoCompatibilityProblems() = with(compatibilityProblems) {
    if (isNotEmpty()) {
      fail("Expected no problems, but got $size problem(s): " + joinToString { it.fullDescription })
    }
  }
}