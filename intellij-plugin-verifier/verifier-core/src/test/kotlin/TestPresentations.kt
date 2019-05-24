import com.jetbrains.pluginverifier.results.presentation.toFullJavaClassName
import org.junit.Assert.assertEquals
import org.junit.Test

class TestPresentations {

  @Test
  fun `test class name conversion`() {
    assertEquals("SomeClass", toFullJavaClassName("SomeClass"))
    assertEquals("SomeClass.Nested", toFullJavaClassName("SomeClass\$Nested"))
    assertEquals("SomeClass.Static.Inner", toFullJavaClassName("SomeClass\$Static\$Inner"))
    assertEquals("SomeClass.Static$2", toFullJavaClassName("SomeClass\$Static\$2"))
    assertEquals("SomeClass$", toFullJavaClassName("SomeClass$"))
    assertEquals("SomeClass$4", toFullJavaClassName("SomeClass$4"))
    assertEquals("SomeClass$4$5$6", toFullJavaClassName("SomeClass$4$5$6"))
    assertEquals("SomeClass\$321.XXX\$555", toFullJavaClassName("SomeClass\$321\$XXX\$555"))
  }

}