package com.jetbrains.plugin.structure.classes.resolvers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.objectweb.asm.tree.ClassNode

@RunWith(Parameterized::class)
class CompositeResolverTest(private val type: CompositeResolverType) {
  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "resolver-impl={0}")
    fun type(): List<Array<CompositeResolverType>> = listOf(arrayOf(CompositeResolverType.COMPOSITE), arrayOf(CompositeResolverType.SIMPLE))
  }

  enum class CompositeResolverType {
    COMPOSITE,
    SIMPLE
  }

  private lateinit var compositeResolver: Resolver

  val exampleClassName = "com/example/ExampleClass"
  val exampleClassNode = newEmptyClassNode(exampleClassName)
  val anotherClassName = "com/example/AnotherExampleClass"
  val anotherClassNode = newEmptyClassNode(anotherClassName)

  @Before
  fun setUp() {
    val nestedResolver = FixedClassesResolver.create(listOf(exampleClassNode), emptyOrigin())
    val anotherNestedResolver = FixedClassesResolver.create(listOf(anotherClassNode), emptyOrigin())

    compositeResolver = when (type) {
      CompositeResolverType.COMPOSITE -> CompositeResolver.create(listOf(nestedResolver, anotherNestedResolver))
      CompositeResolverType.SIMPLE -> SimpleCompositeResolver.create(listOf(nestedResolver, anotherNestedResolver), "simple")
    }
  }

  @Test
  fun `split package across two resolvers`() {
    with(compositeResolver) {
      assertTrue(containsClass(exampleClassName))
      assertTrue(containsClass(anotherClassName))

      assertTrue(containsPackage("com/example"))
      assertTrue(containsPackage("com"))

      assertFound(exampleClassName) { assertEquals(exampleClassNode, it) }
      assertFound(anotherClassName) { assertEquals(anotherClassNode, it) }
    }
  }

  private fun emptyOrigin() = object : FileOrigin {
    override val parent: FileOrigin? = null
  }

  private fun newEmptyClassNode(binaryClassName: String): ClassNode {
    return ClassNode().apply {
      name = binaryClassName
    }
  }

  private fun Resolver.assertFound(className: String, handle: (ClassNode) -> Unit) {
    val foundClass = resolveClass(className)
    assertTrue(foundClass is ResolutionResult.Found)
    foundClass as ResolutionResult.Found
    handle(foundClass.value)
  }


}