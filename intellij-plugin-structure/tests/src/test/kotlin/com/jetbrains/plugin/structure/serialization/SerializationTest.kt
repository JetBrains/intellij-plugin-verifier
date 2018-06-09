package com.jetbrains.plugin.structure.serialization

import com.jetbrains.plugin.structure.intellij.problems.ShortDescription
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Tests serialization and deserialization of API data classes.
 */
class SerializationTest {

  private inline fun <reified T : Any> serializeAndDeserialize(obj: T) {
    val bos = ByteArrayOutputStream()
    ObjectOutputStream(bos).use {
      it.writeObject(obj)
    }

    val byteArray = bos.toByteArray()
    val deserialized = ObjectInputStream(ByteArrayInputStream(byteArray)).use {
      it.readObject() as T
    }

    Assert.assertEquals("Deserialized object $deserialized is not equal to original $obj", obj, deserialized)
  }

  @Test
  fun ideVersion() {
    serializeAndDeserialize(IdeVersion.createIdeVersion("IU-182.1"))
  }

  @Test
  fun pluginWarnings() {
    serializeAndDeserialize(ShortDescription())
  }

}