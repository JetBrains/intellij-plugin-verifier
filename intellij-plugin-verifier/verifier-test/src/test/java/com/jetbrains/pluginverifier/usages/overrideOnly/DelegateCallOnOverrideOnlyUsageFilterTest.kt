package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.tests.mocks.MockVerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.FullyQualifiedClassName
import com.jetbrains.pluginverifier.verifiers.resolution.MethodAsm
import com.jetbrains.pluginverifier.verifiers.resolution.toBinaryClassName
import org.junit.Assert.fail
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

private const val NO_PARAMS_RETURN_VOID_DESCRIPTOR = "()V"
class DelegateCallOnOverrideOnlyUsageFilterTest {
  @Test
  fun `invocation of super as delegate is intentionally ignored`() {
    val context = MockVerificationContext()
    val filter = DelegateCallOnOverrideOnlyUsageFilter()

    val className = "mock.plugin.overrideOnly.ClearCountingVector"
    val clearMethodAsm = loadMethod(className, "clear", NO_PARAMS_RETURN_VOID_DESCRIPTOR)
    if (clearMethodAsm == null) {
      fail("Unable to find 'clear' method on the $className")
    }
    clearMethodAsm!!

    val vectorFqn = "java.util.Vector"
    val vectorClearMethodAsm = loadMethod(vectorFqn, "clear", NO_PARAMS_RETURN_VOID_DESCRIPTOR)
    if (vectorClearMethodAsm == null) {
      fail("Unable to find 'clear' method on the $className")
    }
    vectorClearMethodAsm!!

    val superClearInstruction = clearMethodAsm.asmNode.instructions.find { it is MethodInsnNode
      && it.name == "clear"
      && it.owner == vectorFqn.toBinaryClassName()
      && it.desc == NO_PARAMS_RETURN_VOID_DESCRIPTOR
    }
    if (superClearInstruction == null) fail("Unable to find 'clear' method on the $className")
    superClearInstruction!!

    filter.allow(vectorClearMethodAsm, superClearInstruction, clearMethodAsm, context)
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