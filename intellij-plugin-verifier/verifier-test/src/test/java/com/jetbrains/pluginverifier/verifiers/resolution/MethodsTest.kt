package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.*
import com.jetbrains.pluginverifier.jdk.JdkDescriptorCreator
import com.jetbrains.pluginverifier.tests.findMockPluginJarPath
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.fail
import org.junit.Test
import java.nio.file.Path

const val JAVA_LANG_OBJECT: BinaryClassName = "java/lang/Object"

class MethodsTest {
  @Test
  fun testSearchParentOverrides() {
    val javaHome = System.getProperty("java.home")
    val jdkDescriptor = JdkDescriptorCreator.createJdkDescriptor(Path.of(javaHome))

    val jdkResolver = jdkDescriptor.jdkResolver
    val mockPluginResolver = createTestResolver()
    val resolver = CompositeResolver.create(
      jdkResolver, mockPluginResolver
    )

    val observedClassName = "mock/plugin/method/MethodOverriddenFromJavaLangObject"
    val overriddenMethodName = "toString"
    val observerClassResult = resolver.resolveClass(observedClassName)
    if (observerClassResult !is ResolutionResult.Found) {
      fail("Class '$observedClassName' not found in the mock plugin by resolver")
    } else {
      val classNode = observerClassResult.value
      val toStringMethod = classNode.methods.first { it.name == overriddenMethodName }
      val classFile = ClassFileAsm(observerClassResult.value, observerClassResult.fileOrigin)
      val method: Method = MethodAsm(classFile, toStringMethod)

      val methodOverrides = method.searchParentOverrides(resolver, ignoreJavaLangObject = false)
      assertThat(methodOverrides.size, `is`(1))
      val javaLangObjectToString = methodOverrides.first()
      assertThat(javaLangObjectToString.klass.name, `is`(JAVA_LANG_OBJECT))

      val nonJavaLangObjectMethodOverrides = method.searchParentOverrides(resolver)
      assertThat(nonJavaLangObjectMethodOverrides.size, `is`(0))
    }
  }

  private fun createTestResolver(): Resolver =
    JarFileResolver(
      findMockPluginJarPath(),
      Resolver.ReadMode.FULL,
      object : FileOrigin {
        override val parent: FileOrigin? = null
      }
    )
}