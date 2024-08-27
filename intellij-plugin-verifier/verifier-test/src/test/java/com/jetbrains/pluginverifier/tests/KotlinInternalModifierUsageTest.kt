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
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.usages.internal.kotlin.KtInternalClassUsage
import com.jetbrains.pluginverifier.usages.internal.kotlin.KtInternalFieldUsage
import com.jetbrains.pluginverifier.usages.internal.kotlin.KtInternalMethodUsage
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.verifiers.resolution.toBinaryClassName
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.KmType
import kotlinx.metadata.Visibility
import kotlinx.metadata.jvm.JvmMetadataVersion
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.signature
import kotlinx.metadata.visibility
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender
import net.bytebuddy.implementation.bytecode.ByteCodeAppender.Size
import net.bytebuddy.jar.asm.Label
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.matcher.ElementMatchers.named
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.Opcodes.*
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.util.*

private const val internalApiServiceClassName = "com.intellij.openapi.InternalApiService"

private const val usageClassName = "usage.Usage"


class KotlinInternalModifierUsageTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var byteBuddy: ByteBuddy

  @Before
  fun setUp() {
    byteBuddy = ByteBuddy()
  }

  private fun getInternalMethodUsageMsg(caller: String, callee: String) =
    "Internal method $callee.internalFortyTwo() : int " +
      "is invoked in $caller.delegateInternalFortyTwo() : int. " +
      "This method is marked with Kotlin `internal` visibility modifier, indicating " +
      "that it is not supposed to be invoked by the client code outside the declaring module."

  private fun getInternalClassUsageMsg(caller: String, callee: String) = "Internal class $callee " +
    "is referenced in $caller.delegateInternalFortyTwo() : int. " +
    "This class is marked with Kotlin `internal` visibility modifier, indicating " +
    "that it is not supposed to be referenced in client code outside the declaring module."

  private fun getInternalFieldUsageMsg(caller: String, callee: String) =
    "Internal field $callee.internalField : int " +
      "is accessed in $caller.accessInternalField() : int. " +
      "This field is marked with Kotlin `internal` visibility modifier, " +
      "indicating that it is not supposed to be used in client code outside the declaring module."


  private val pluginSpec = IdeaPluginSpec("com.intellij.plugin", "JetBrains s.r.o.")

  @Test
  @Ignore
  fun `JetBrains plugin class calls a public method in an internal class `() {

    val internalApiServiceClassName = generateInternalApiServiceClassName()

    val publicMethodName = "publicFortyTwo"
    val internalApiServiceClassUdt = internalApiServiceClassName
      .constructWithMethod(publicMethodName, Integer.TYPE)
      .intercept(FixedValue.value(42))
      .annotateType(kotlinMetadata {
        name = internalApiServiceClassName
        visibility = Visibility.INTERNAL
        functions += KmFunction(publicMethodName).apply {
          visibility = Visibility.PUBLIC
          returnType = KmType().apply {
            classifier = KmClassifier.Class("I")
          }
          signature = JvmMethodSignature(publicMethodName, "()I")
        }
      })
      .make()

    val internalApiService = internalApiServiceClassUdt.newInstance()

    val usageClassName = generateUsageClassName()
    val usageClassUdt = usageClassName
      .constructWithMethod("delegateInternalFortyTwo", Integer.TYPE)
      .intercept(
        MethodDelegation
          .withDefaultConfiguration()
          .filter(named(publicMethodName)).to(internalApiService)
      )
      .make()

    val ide = prepareIdeWithApi { internalApiServiceClassUdt }
    val plugin = prepareUsage(pluginSpec) { usageClassUdt }

    verify(ide, plugin).run {
      assertEquals(1, size)
      with(filterIsInstance<KtInternalClassUsage>()) {
        assertEquals(1, size)
        assertEquals(getInternalClassUsageMsg(usageClassName, internalApiServiceClassName), this[0].fullDescription)
      }
    }
  }

  @Test
  @Ignore
  fun `JetBrains plugin class uses an internal class and an internal method name`() {
    val internalApiServiceClassName = generateInternalApiServiceClassName()

    val internalMethodName = "internalFortyTwo"
    val internalApiServiceClassUdt = internalApiServiceClassName
      .constructWithMethod(internalMethodName, Integer.TYPE)
      .intercept(FixedValue.value(42))
      .annotateType(kotlinMetadata {
        name = internalApiServiceClassName
        visibility = Visibility.INTERNAL
        functions += KmFunction(internalMethodName).apply {
          visibility = Visibility.INTERNAL
          returnType = KmType().apply {
            classifier = KmClassifier.Class("I")
          }
          signature = JvmMethodSignature(internalMethodName, "()I")
        }
      })
      .make()


    val internalApiService = internalApiServiceClassUdt.newInstance()

    val usageClassName = generateUsageClassName()
    val usageClassUdt = usageClassName
      .constructWithMethod("delegateInternalFortyTwo", Integer.TYPE)
      .intercept(
        MethodDelegation
          .withDefaultConfiguration()
          .filter(named(internalMethodName)).to(internalApiService)
      )
      .make()

    val ide = prepareIdeWithApi { internalApiServiceClassUdt }
    val plugin = prepareUsage(pluginSpec) { usageClassUdt }

    verify(ide, plugin).run {
      assertEquals(2, size)
      with(filterIsInstance<KtInternalClassUsage>()) {
        assertEquals(1, size)
        assertEquals(getInternalClassUsageMsg(usageClassName, internalApiServiceClassName), this[0].fullDescription)
      }

      with(filterIsInstance<KtInternalMethodUsage>()) {
        assertEquals(1, size)
        assertEquals(getInternalMethodUsageMsg(usageClassName, internalApiServiceClassName), this[0].fullDescription)
      }
    }
  }

  @Test
  @Ignore
  fun `internal field access is reported as an internal Kotlin API usage`() {
    val internalFieldName = "internalField"
    val internalFieldValue = 17

    val internalApiServiceClassName = generateInternalApiServiceClassName()
    val usageClassName = generateUsageClassName()

    val idePlugin = prepareUsage(pluginSpec) {
      usageClassName.constructWithMethod("accessInternalField", Integer.TYPE)
        .intercept(
          DirectFieldAccess(
              usageClassName.toBinaryClassName(),
              internalApiServiceClassName.toBinaryClassName(),
              internalFieldName,
              internalFieldValue
          ).implementation
        )
        .make()
    }
    val ide = prepareIdeWithApi {
      internalApiServiceClassName.construct()
        .annotateType(kotlinMetadata {
          name = internalApiServiceClassName
          visibility = Visibility.INTERNAL
          properties += KmProperty(internalFieldName).apply {
            visibility = Visibility.INTERNAL
            returnType = KmType().apply {
              classifier = KmClassifier.Class("I")
            }
          }
        })
        .defineField(internalFieldName, Integer.TYPE, Modifier.PUBLIC)
        .make()
    }
    verify(ide, idePlugin).run {
      with(filterIsInstance<KtInternalFieldUsage>()) {
        assertEquals(1, size)
        assertEquals(getInternalFieldUsageMsg(usageClassName, internalApiServiceClassName), this[0].fullDescription)
      }
    }
  }

  private fun verify(ide: Ide, idePlugin: IdePlugin): Set<InternalApiUsage> {
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

  private fun prepareUsage(
    pluginSpec: IdeaPluginSpec,
    dynamicTypeBuilder: () -> DynamicType.Unloaded<*>
  ): IdePlugin {
    return buildIdePlugin(pluginSpec) {
      usageClass(dynamicTypeBuilder())
    }
  }

  private fun prepareIdeWithApi(platformApiClassTypeBuilder: () -> DynamicType.Unloaded<*>): Ide {
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

  private fun String.construct() = byteBuddy
    .subclass(Object::class.java)
    .name(this)

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  private fun String.constructWithMethod(
    method: String,
    returnType: Type,
    modifier: Int = Modifier.PUBLIC
  ): DynamicType.Builder.MethodDefinition.ParameterDefinition.Initial<Object> {
    return construct()
      .defineMethod(method, returnType, modifier)
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

  private fun buildIdePlugin(
    ideaPluginSpec: IdeaPluginSpec = IdeaPluginSpec("com.intellij", "JetBrains s.r.o."),
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
              <depends>com.intellij.modules.java</depends>
            </idea-plugin>
            """.trimIndent()
        }
      }
    }

    return (IdePluginManager.createManager().createPlugin(pluginFile) as PluginCreationSuccess).plugin
  }

  /**
   * Generate random API class name to prevent naming clashes in ByteBuddy.
   */
  private fun generateInternalApiServiceClassName() = internalApiServiceClassName.randomize()
  /**
   * Generate random API Usage class to prevent naming clashes in ByteBuddy.
   */
  private fun generateUsageClassName() = usageClassName.randomize()

  private fun kotlinMetadata(configure: KmClass.() -> Unit) = KmClass().apply {
    configure()
  }.let {
    KotlinClassMetadata
      .Class(it, JvmMetadataVersion.LATEST_STABLE_SUPPORTED, 0)
      .write()
  }

  private fun String.randomize(): String {
    return buildString {
      append(this@randomize)
      append("_")
      append(UUID.randomUUID().toString().replace("-", ""))
    }
  }

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  private fun DynamicType.Unloaded<Object>.newInstance(classLoader: ClassLoader = this::class.java.classLoader) =
    load(this, classLoader)
      .getDeclaredConstructor().newInstance()

  /**
   * Creates a new instance of `callee` and invokes a `fieldName` while assigning a fixed value on this instance.
   * This occurs within a method of the `caller`.
   * The field value is hardwired to an integer
   */
  private class DirectFieldAccess(
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

}

