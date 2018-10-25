import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import org.junit.Assert
import org.junit.Test

class TestSignatures {
  @Test
  fun testEnum() {
    assertClass(
        "java/lang/Enum",
        "<E:Ljava/lang/Enum<TE;>;>Ljava/lang/Object;Ljava/lang/Comparable<TE;>;Ljava/io/Serializable;",
        "java.lang.Enum"
    )
  }

  @Test
  fun assertArrayList() {
    assertClass(
        "java/util/ArrayList",
        "<E:Ljava/lang/Object;>Ljava/util/AbstractList<TE;>;Ljava/util/List<TE;>;Ljava/util/RandomAccess;Ljava/lang/Cloneable;Ljava/io/Serializable;",
        "java.util.ArrayList"
    )
  }

  @Test
  fun assertHashMap() {
    assertClass(
        "java/util/HashMap",
        "<K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/util/Map<TK;TV;>;Ljava/lang/Cloneable;Ljava/io/Serializable;",
        "java.util.HashMap"
    )
  }

  @Test
  fun assertIterable() {
    assertClass(
        "java/lang/Iterable",
        "<T:Ljava/lang/Object;>Ljava/lang/Object;",
        "java.lang.Iterable"
    )
  }

  @Test
  fun assertNonGenericClass() {
    assertClass(
        "org/some/Class\$Inner1\$Inner2",
        "",
        "org.some.Class.Inner1.Inner2"
    )
  }

  private fun assertClass(className: String, signature: String, expectedResult: String) {
    val fromClass = genSomeClassLocation(className, signature)
    Assert.assertEquals(expectedResult, fromClass.toString())
  }

  private fun genSomeClassLocation(className: String, signature: String) = ClassLocation(className, signature, Modifiers(0))

  @Test
  fun assertCollectionsMin() {
    assertMethod(
        "min",
        "(Ljava/util/Collection;)Ljava/lang/Object;",
        "java/util/Collections",
        "<E:Ljava/lang/Object;>Ljava/lang/Object;Ljava/util/Collection<TE;>;",
        listOf("coll"),
        "<T:Ljava/lang/Object;:Ljava/lang/Comparable<-TT;>;>([[Ljava/util/Collection<+TT;>;)TT;",
        "java.util.Collections.min(Collection[][] coll) : T"
    )
  }

  @Test
  fun assertArrayListGet() {
    assertMethod(
        "get",
        "(I)Ljava/lang/Object;",
        "java/util/ArrayList",
        "<E:Ljava/lang/Object;>Ljava/util/AbstractList<TE;>;Ljava/util/List<TE;>;Ljava/util/RandomAccess;Ljava/lang/Cloneable;Ljava/io/Serializable;",
        listOf("index"),
        "(I)TE;",
        "java.util.ArrayList.get(int index) : E"
    )
  }

  @Test
  fun assertNonGenericMethod() {
    assertMethod(
        "name",
        "(IFLjava/lang/Object;)Ljava/lang/String;",
        "org/some/Class",
        "<T:Ljava/lang/String;Object:Ljava/lang/Object;>Ljava/lang/Object;",
        listOf("myInt", "myFloat", "myObject"),
        "",
        "org.some.Class.name(int myInt, float myFloat, Object myObject) : String"
    )
  }

  private fun assertMethod(
      methodName: String,
      methodDescriptor: String,
      className: String,
      classSignature: String,
      parameterNames: List<String>,
      signature: String,
      expected: String
  ) {
    val methodLocation = MethodLocation(genSomeClassLocation(className, classSignature), methodName, methodDescriptor, parameterNames, signature, Modifiers(0))
    Assert.assertEquals(expected, methodLocation.toString())
  }

  @Test
  fun assertField() {
    val fieldLocation = FieldLocation(
        genSomeClassLocation(
            "some/Class", "<E:Ljava/lang/Object;>Ljava/lang/Object;Ljava/util/Collection<TE;>;"
        ),
        "field",
        "I",
        "TT;",
        Modifiers(0)
    )
    Assert.assertEquals("some.Class.field : T", fieldLocation.toString())
  }
}