package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.tests.mocks.MockVerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.FullyQualifiedClassName
import com.jetbrains.pluginverifier.verifiers.resolution.MethodAsm
import com.jetbrains.pluginverifier.verifiers.resolution.toBinaryClassName
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode


private const val NO_PARAMS_RETURN_VOID_DESCRIPTOR = "()V"
private const val STRING_PARAM_RETURN_PACKAGE_DESCRIPTOR = "(Ljava/lang/String;)Ljava/lang/Package;"

private const val CLEAR_METHOD = "clear"
private const val GET_PACKAGE_METHOD = "getPackage"

class DelegateCallOnOverrideOnlyUsageFilterTest {
  @Test
  fun `invocation of super as delegate is intentionally ignored`() {
    val context = MockVerificationContext()
    val filter = DelegateCallOnOverrideOnlyUsageFilter()

    val className = "mock.plugin.overrideOnly.ClearCountingContainer"
    val clearMethodAsm = ClearCountingContainerNode().loadMethod(CLEAR_METHOD, NO_PARAMS_RETURN_VOID_DESCRIPTOR)
    if (clearMethodAsm == null) {
      fail("Unable to find '$CLEAR_METHOD' method on the $className")
    }
    clearMethodAsm!!

    val containerFqn = "mock.plugin.overrideOnly.Container"
    val containerClearMethodAsm = ContainerNode().loadMethod(CLEAR_METHOD, NO_PARAMS_RETURN_VOID_DESCRIPTOR)
    if (containerClearMethodAsm == null) {
      fail("Unable to find '$CLEAR_METHOD' method on the $className")
    }
    containerClearMethodAsm!!

    val superClearInstruction = clearMethodAsm.asmNode.instructions.find { it is MethodInsnNode
      && it.name == CLEAR_METHOD
      && it.owner == containerFqn.toBinaryClassName()
      && it.desc == NO_PARAMS_RETURN_VOID_DESCRIPTOR
    }
    if (superClearInstruction == null) fail("Unable to find '$CLEAR_METHOD' method on the $className")
    superClearInstruction!!

    val isAllowed = filter.allow(containerClearMethodAsm, superClearInstruction, clearMethodAsm, context)
    Assert.assertFalse(isAllowed)
  }

  @Test
  fun `invocation of similarly named static method is ignored`() {
    val context = MockVerificationContext()
    val filter = DelegateCallOnOverrideOnlyUsageFilter()

    val className = "mock.plugin.overrideOnly.PackageInvokingBox"
    val method = PackageInvokingBoxNode().loadMethod(GET_PACKAGE_METHOD, STRING_PARAM_RETURN_PACKAGE_DESCRIPTOR)
    if (method == null) {
      fail("Unable to find '$CLEAR_METHOD' method on the $className")
    }
    method!!

    val targetClassName = "java.lang.Package"
    val instruction = method.asmNode.instructions.find {
      it is MethodInsnNode
        && it.name == GET_PACKAGE_METHOD
        && it.owner == targetClassName.toBinaryClassName()
        && it.desc == STRING_PARAM_RETURN_PACKAGE_DESCRIPTOR
    }
    if (instruction == null) fail("Unable to find '$GET_PACKAGE_METHOD' method on the $className")
    instruction!!

    val targetMethod = loadMethod(targetClassName, GET_PACKAGE_METHOD, STRING_PARAM_RETURN_PACKAGE_DESCRIPTOR)
    if (targetMethod == null) {
      fail("Unable to find '$GET_PACKAGE_METHOD' method on the $targetClassName")
    }
    targetMethod!!

    val isAllowed = filter.allow(targetMethod, instruction, method, context)
    Assert.assertFalse(isAllowed)
  }


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