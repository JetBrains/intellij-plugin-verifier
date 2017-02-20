import com.jetbrains.pluginverifier.location.AccessFlags
import com.jetbrains.pluginverifier.location.ClassPath
import com.jetbrains.pluginverifier.location.ProblemLocation
import org.junit.Assert
import org.junit.Test

class TestSignatures {
  @Test
  fun testEnum() {
    assertClass("java/lang/Enum", "<E:Ljava/lang/Enum<TE;>;>Ljava/lang/Object;Ljava/lang/Comparable<TE;>;Ljava/io/Serializable;", "java.lang.Enum<E>")
  }

  @Test
  fun assertArrayList() {
    assertClass("java/util/ArrayList", "<E:Ljava/lang/Object;>Ljava/util/AbstractList<TE;>;Ljava/util/List<TE;>;Ljava/util/RandomAccess;Ljava/lang/Cloneable;Ljava/io/Serializable;", "java.util.ArrayList<E>")
  }

  @Test
  fun assertHashMap() {
    assertClass("java/util/HashMap", "<K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/util/Map<TK;TV;>;Ljava/lang/Cloneable;Ljava/io/Serializable;", "java.util.HashMap<K, V>")
  }

  @Test
  fun assertIterable() {
    assertClass("java/lang/Iterable", "<T:Ljava/lang/Object;>Ljava/lang/Object;", "java.lang.Iterable<T>")
  }

  @Test
  fun assertNonGenericClass() {
    assertClass("org/some/Class\$Inner1\$Inner2", "", "org.some.Class.Inner1.Inner2")
  }

  private fun assertClass(className: String, signature: String, expectedResult: String) {
    val fromClass = genSomeClassLocation(className, signature)
    Assert.assertEquals(expectedResult, fromClass.toString())
  }

  private fun genSomeClassLocation(className: String, signature: String) = ProblemLocation.fromClass(className, signature, ClassPath(ClassPath.Type.ROOT, ""), AccessFlags(0))

  @Test
  fun assertCollectionsMin() {
    assertMethod("min", "(Ljava/util/Collection;)Ljava/lang/Object;", "java/util/Collections", "<E:Ljava/lang/Object;>Ljava/lang/Object;Ljava/util/Collection<TE;>;", listOf("coll"), "<T:Ljava/lang/Object;:Ljava/lang/Comparable<-TT;>;>([[Ljava/util/Collection<+TT;>;)TT;", "java.util.Collections<E>.min(Collection<?>[][] coll) : T")
  }

  @Test
  fun assertArrayListGet() {
    assertMethod("get", "(I)Ljava/lang/Object;", "java/util/ArrayList", "<E:Ljava/lang/Object;>Ljava/util/AbstractList<TE;>;Ljava/util/List<TE;>;Ljava/util/RandomAccess;Ljava/lang/Cloneable;Ljava/io/Serializable;", listOf("index"), "(I)TE;", "java.util.ArrayList<E>.get(int index) : E")
  }

  @Test
  fun assertNonGenericMethod() {
    assertMethod("name", "(IFLjava/lang/Object;)Ljava/lang/String;", "org/some/Class", "<T:Ljava/lang/String;Object:Ljava/lang/Object;>Ljava/lang/Object;", listOf("myInt", "myFloat", "myObject"), "", "org.some.Class<T, Object>.name(int myInt, float myFloat, Object myObject) : String")
  }

  private fun assertMethod(methodName: String, methodDescriptor: String, className: String, classSignature: String, parameterNames: List<String>, signature: String, expected: String) {
    val methodLocation = ProblemLocation.fromMethod(genSomeClassLocation(className, classSignature), methodName, methodDescriptor, parameterNames, signature, AccessFlags(0))
    Assert.assertEquals(expected, methodLocation.toString())
  }

  @Test
  fun assertField() {
    val fieldLocation = ProblemLocation.fromField(genSomeClassLocation("some/Class", "<E:Ljava/lang/Object;>Ljava/lang/Object;Ljava/util/Collection<TE;>;"), "field", "I", "TT;", AccessFlags(0))
    Assert.assertEquals("some.Class<E>.field : T", fieldLocation.toString())
  }
}