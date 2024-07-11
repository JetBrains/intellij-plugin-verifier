package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.tests.mocks.MockVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.FullyQualifiedClassName
import com.jetbrains.pluginverifier.verifiers.resolution.MethodAsm
import com.jetbrains.pluginverifier.verifiers.resolution.toBinaryClassName
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode


private const val NO_PARAMS_RETURN_VOID_DESCRIPTOR = "()V"
private const val STRING_PARAM_RETURN_PACKAGE_DESCRIPTOR = "(Ljava/lang/String;)Ljava/lang/Package;"

private const val CLEAR_METHOD = "clear"
private const val GET_PACKAGE_METHOD = "getPackage"

class DelegateCallOnOverrideOnlyUsageFilterTest {
  private lateinit var verificationContext: VerificationContext

  private lateinit var filter: ApiUsageFilter

  @Before
  fun setUp() {
    verificationContext = MockVerificationContext()
    filter = DelegateCallOnOverrideOnlyUsageFilter()
  }

  @Test
  fun `invocation of super as delegate is intentionally ignored`() {
    val className = "mock.plugin.overrideOnly.ClearCountingContainer"
    val clearMethod = ClearCountingContainerNode().loadMethod(CLEAR_METHOD, NO_PARAMS_RETURN_VOID_DESCRIPTOR)
    assertNotNull("Unable to find '$CLEAR_METHOD' method on the $className", clearMethod)
    clearMethod!!

    val containerFqn = "mock.plugin.overrideOnly.Container"
    val containerClearMethodAsm = ContainerNode().loadMethod(CLEAR_METHOD, NO_PARAMS_RETURN_VOID_DESCRIPTOR)
    assertNotNull("Unable to find '$CLEAR_METHOD' method on the $className", containerClearMethodAsm)
    containerClearMethodAsm!!

    val superClearInstruction = clearMethod.findInvocation(containerFqn.toBinaryClassName(), CLEAR_METHOD, NO_PARAMS_RETURN_VOID_DESCRIPTOR)
    if (superClearInstruction == null) fail("Unable to find '$CLEAR_METHOD' method on the $className")
    superClearInstruction!!

    val isAllowed = filter.allow(containerClearMethodAsm, superClearInstruction, clearMethod, verificationContext)
    assertFalse(isAllowed)
  }

  @Test
  fun `invocation of similarly named static method is ignored`() {
    val className = "mock.plugin.overrideOnly.PackageInvokingBox"
    val getPackageMethodAsm = PackageInvokingBoxNode().loadMethod(GET_PACKAGE_METHOD, STRING_PARAM_RETURN_PACKAGE_DESCRIPTOR)
    assertNotNull("Unable to find '$CLEAR_METHOD' method on the $className", getPackageMethodAsm)
    getPackageMethodAsm!!

    val targetClassName = "java.lang.Package"
    val getPackageInstruction = getPackageMethodAsm.findInvocation(targetClassName.toBinaryClassName(), GET_PACKAGE_METHOD, STRING_PARAM_RETURN_PACKAGE_DESCRIPTOR)
    if (getPackageInstruction == null) fail("Unable to find '$GET_PACKAGE_METHOD' method on the $className")
    getPackageInstruction!!

    val getPackageTargetMethodAsm = loadMethod(targetClassName, GET_PACKAGE_METHOD, STRING_PARAM_RETURN_PACKAGE_DESCRIPTOR)
    assertNotNull("Unable to find '$GET_PACKAGE_METHOD' method on the $targetClassName", getPackageTargetMethodAsm)
    getPackageTargetMethodAsm!!

    val isAllowed = filter.allow(getPackageTargetMethodAsm, getPackageInstruction, getPackageMethodAsm, verificationContext)
    assertFalse(isAllowed)
  }

  private fun MethodAsm.findInvocation(
    invokedMethodOwner: BinaryClassName,
    invokedMethodName: String,
    invokedMethodDescriptor: String
  ): MethodInsnNode? =
    asmNode.instructions.find {
      it is MethodInsnNode
        && it.name == invokedMethodName
        && it.owner == invokedMethodOwner
        && it.desc == invokedMethodDescriptor
    }?.let { it as MethodInsnNode }

  private fun ClassNode.loadMethod(methodName: String, methodDescriptor: String): MethodAsm? {
    val classFile = ClassFileAsm(this, TestClasspathFileOrigin)
    return classFile.methods.find { it.name == methodName && it.descriptor == methodDescriptor }
  }

  private fun loadMethod(className: FullyQualifiedClassName, methodName: String, methodDescriptor: String): MethodAsm? {
    val classFile = getClassFile(className)
    return classFile.methods.find { it.name == methodName && it.descriptor == methodDescriptor }
  }

  private fun getClassFile(className: FullyQualifiedClassName): ClassFileAsm {
    val classNode = loadClass(className)
    return ClassFileAsm(classNode, TestClasspathFileOrigin)
  }

  private fun loadClass(className: FullyQualifiedClassName): ClassNode {
    val classNode = ClassNode()
    val classReader = ClassReader(className)
    classReader.accept(classNode, 0)
    return classNode
  }

  private object TestClasspathFileOrigin : FileOrigin {
    override val parent: FileOrigin? = null
  }

}