package com.jetbrains.pluginverifier.tests

import com.jetbrains.pluginverifier.tests.bytecode.Dumps
import com.jetbrains.pluginverifier.tests.bytecode.JavaDumps
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import com.jetbrains.pluginverifier.usages.internal.kotlin.KtInternalClassUsage
import com.jetbrains.pluginverifier.usages.internal.kotlin.KtInternalFieldUsage
import com.jetbrains.pluginverifier.usages.internal.kotlin.KtInternalMethodUsage
import com.jetbrains.pluginverifier.verifiers.resolution.toBinaryClassName
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.KmType
import kotlinx.metadata.Visibility
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.signature
import kotlinx.metadata.visibility
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers.named
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.lang.reflect.Modifier

private const val internalApiServiceClassName = "com.intellij.openapi.InternalApiService"

private const val usageClassName = "usage.Usage"


class KotlinInternalModifierUsageTest : BaseBytecodeTest() {

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

  @Test
  fun `accidental Kotlin Stdlib internal API usage`() {
    assertVerified {
      ide = buildIdeWithBundledPlugins(includeKotlinStdLib = true) {}
      plugin = prepareUsage(pluginSpec, "KotlinUsage", Dumps.KotlinUsage())
      kotlin = true
    }.run {
      with(internalApiUsages.filterIsInstance<KtInternalClassUsage>()) {
        assertEquals(0, size)
      }
    }
  }

  @Test
  fun `Kotlin coroutines internal API usage`() {
    assertVerified {
      ide = buildIdeWithBundledPlugins(includeKotlinStdLib = true) {}
      plugin = prepareUsage(pluginSpec, "KotlinUsage", Dumps.KotlinSuspendLambda())
      kotlin = true
    }.run {
      with(internalApiUsages.filterIsInstance<KtInternalClassUsage>()) {
        assertEquals(0, size)
      }
    }
  }

  @Test
  fun `Kotlin serialization internal API usage`() {
    assertVerified {
      ide = buildIdeWithBundledPlugins(includeKotlinStdLib = true) {}
      plugin = prepareUsage(pluginSpec, "KotlinUsage", JavaDumps.getSerializableVersion())
      kotlin = true
    }.run {
      with(internalApiUsages.filterIsInstance<KtInternalClassUsage>()) {
        assertEquals(0, size)
      }
    }
  }

  /**
   * Generate random API class name to prevent naming clashes in ByteBuddy.
   */
  protected fun generateInternalApiServiceClassName() = internalApiServiceClassName.randomize()
  /**
   * Generate random API Usage class to prevent naming clashes in ByteBuddy.
   */
  protected fun generateUsageClassName() = usageClassName.randomize()

}

