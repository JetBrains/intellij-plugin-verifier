import com.jetbrains.pluginverifier.results.presentation.toFullJavaClassName
import com.jetbrains.pluginverifier.results.signatures.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.objectweb.asm.signature.SignatureReader

/**
 * Asserts that [SigVisitor] returns proper
 * nodes of [FormattableSignature]s,
 * which preserve initial signature in [toString],
 * and return correct presentations in [FormattableSignature.format].
 */
class SigVisitorTest {

  private val fullOptions = FormatOptions(
      superClass = true,
      superInterfaces = true,
      formalTypeParameters = true,
      formalTypeParametersBounds = true,
      typeArguments = true,
      internalNameConverter = toFullJavaClassName,
      methodThrows = true
  )

  @Test
  fun `class signatures`() {
    val testData = listOf(
        Triple(
            "<E extends java.lang.Enum<E>> implements java.lang.Comparable<E>, java.io.Serializable",
            "<E:Ljava/lang/Enum<TE;>;>Ljava/lang/Object;Ljava/lang/Comparable<TE;>;Ljava/io/Serializable;",
            fullOptions
        ),

        Triple(
            "<D extends java.lang.reflect.GenericDeclaration> extends java.lang.reflect.Type",
            "<D::Ljava/lang/reflect/GenericDeclaration;>Ljava/lang/Object;Ljava/lang/reflect/Type;",
            fullOptions.copy(isInterface = true)
        ),

        Triple(
            "<K, V> extends java.util.AbstractMap<K, V> implements java.util.concurrent.ConcurrentMap<K, V>, java.io.Serializable",
            "<K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/util/concurrent/ConcurrentMap<TK;TV;>;Ljava/io/Serializable;",
            fullOptions
        ),

        Triple(
            "<T, R extends T>",
            "<T:Ljava/lang/Object;R:TT;>Ljava/lang/Object;",
            fullOptions
        )
    )

    for ((expected, signature, options) in testData) {
      checkClassSignature(expected, signature, options)
    }
  }

  @Test
  fun `method signatures`() {
    val testData = listOf(
        "void() throws E, F" to
            "()V^TE;^TF;",

        "void(A<E>.B<F>)" to
            "(LA<TE;>.B<TF;>;)V",

        "void(A<E>.B<F>)" to
            "(LA<TE;>.B<TF;>;)V",

        "void(boolean, byte, char, short, int, float, long, double)" to
            "(ZBCSIFJD)V",

        "<E extends java.lang.Class> java.lang.Class<? extends E>()" to
            "<E:Ljava/lang/Class;>()Ljava/lang/Class<+TE;>;",

        "<E extends java.lang.Class> java.lang.Class<? super E>()" to
            "<E:Ljava/lang/Class;>()Ljava/lang/Class<-TE;>;",

        "void(java.lang.String, java.lang.Class<?>, java.lang.reflect.Method[], java.lang.reflect.Method, java.lang.reflect.Method)" to
            "(Ljava/lang/String;Ljava/lang/Class<*>;[Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)V",

        "java.util.Map<java.lang.Object, java.lang.String>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>)" to
            "(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;",

        "<T> java.util.Map<java.lang.Object, java.lang.String>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>, T)" to
            "<T:Ljava/lang/Object;>(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;TT;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;",

        "<E, T extends java.lang.Comparable<E>> java.util.Map<java.lang.Object, java.lang.String>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>, T)" to
            "<E:Ljava/lang/Object;T::Ljava/lang/Comparable<TE;>;>(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;TT;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;"
    )

    for ((expected, signature) in testData) {
      checkMethodSignature(expected, signature, fullOptions)
    }
  }

  @Test
  fun `field signatures`() {
    val testData = listOf(
        "T[]" to
            "[TT;",

        "AA<byte[][]>" to
            "LAA<[[B>;",

        "java.lang.Class<?>" to
            "Ljava/lang/Class<*>;",

        "java.lang.reflect.Constructor<T>" to
            "Ljava/lang/reflect/Constructor<TT;>;",

        "java.util.Hashtable<?, ?>" to
            "Ljava/util/Hashtable<**>;",

        "java.util.concurrent.atomic.AtomicReferenceFieldUpdater<java.io.BufferedInputStream, byte[]>" to
            "Ljava/util/concurrent/atomic/AtomicReferenceFieldUpdater<Ljava/io/BufferedInputStream;[B>;",

        "AA<java.util.Map<java.lang.String, java.lang.String>[][]>" to
            "LAA<[[Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;>;",

        "java.util.Hashtable<java.lang.Object, java.lang.String>" to
            "Ljava/util/Hashtable<Ljava/lang/Object;Ljava/lang/String;>;"
    )

    for ((expected, signature) in testData) {
      checkFieldSignature(expected, signature, fullOptions)
    }
  }

  private fun checkClassSignature(expected: String, signature: String, formatOptions: FormatOptions) {
    val classSignature = parseClassSignature(signature)
    assertEquals("toString() must return original signature", signature, classSignature.toString())
    assertEquals(expected, classSignature.format(formatOptions))
  }

  private fun checkMethodSignature(expected: String, signature: String, formatOptions: FormatOptions) {
    val methodSignature = parseMethodSignature(signature)
    assertEquals("toString() must return original signature", signature, methodSignature.toString())
    assertEquals(expected, methodSignature.format(formatOptions))
  }

  private fun checkFieldSignature(expected: String, signature: String, formatOptions: FormatOptions) {
    val fieldSignature = parseFieldSignature(signature)
    assertEquals("toString() must return original signature", signature, fieldSignature.toString())
    assertEquals(expected, fieldSignature.format(formatOptions))
  }

  private fun parseClassSignature(signature: String): ClassSignature =
      SigVisitor().also { SignatureReader(signature).accept(it) }.getClassSignature()

  private fun parseMethodSignature(signature: String): MethodSignature =
      SigVisitor().also { SignatureReader(signature).accept(it) }.getMethodSignature()

  private fun parseFieldSignature(signature: String): FieldSignature =
      SigVisitor().also { SignatureReader(signature).acceptType(it) }.getFieldSignature()

}