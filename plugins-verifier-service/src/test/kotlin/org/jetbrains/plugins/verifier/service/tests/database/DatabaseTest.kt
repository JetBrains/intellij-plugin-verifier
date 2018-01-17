package org.jetbrains.plugins.verifier.service.tests.database

import org.jetbrains.plugins.verifier.service.database.MapDbServerDatabase
import org.jetbrains.plugins.verifier.service.database.ValueType
import org.jetbrains.plugins.verifier.service.server.ServiceDAO
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabaseTest {

  @JvmField
  @Rule
  val tempFolder = TemporaryFolder()

  private val temp by lazy {
    tempFolder.newFolder().toPath()
  }

  @Test
  fun `database open-write-close-reopen-read test`() {
    MapDbServerDatabase(temp).use { db ->
      val serviceDAO = ServiceDAO(db)
      serviceDAO.setProperty("one", "1")
      serviceDAO.setProperty("two", "2")
    }

    MapDbServerDatabase(temp).use { db ->
      val serviceDAO = ServiceDAO(db)
      assertEquals("1", serviceDAO.getProperty("one"))
      assertEquals("2", serviceDAO.getProperty("two"))
    }
  }

  private fun <T, R> openSetAndRun(valueType: ValueType<T>, f: MutableSet<T>.() -> R) =
      MapDbServerDatabase(temp).use {
        val set = it.openOrCreateSet("set", valueType)
        f(set)
      }

  @Test
  fun `database set open-write-recreate-read`() {
    openSetAndRun(ValueType.STRING) {
      addAll(listOf("one", "two", "three"))
    }

    openSetAndRun(ValueType.STRING) {
      assertEquals(setOf("one", "two", "three"), this)
    }
  }

  @Test
  fun `serialization of the java-io-Serializable objects`() {
    val list = listOf(1, 2, 3)
    MapDbServerDatabase(temp).use { db ->
      val set = db.openOrCreateSet("lists", ValueType.SERIALIZABLE)
      set.add(list)
    }

    MapDbServerDatabase(temp).use { db ->
      val set = db.openOrCreateSet("lists", ValueType.SERIALIZABLE)
      assertEquals(1, set.size)
      assertEquals(list, set.first())
    }
  }

  @Test
  fun `serialization of the toString serializer`() {
    val valueType = ValueType.StringBased(
        toString = { it.toString() },
        fromString = { Integer.parseInt(it) }
    )

    openSetAndRun(valueType) {
      add(1)
      add(2)
      add(2)
      remove(1)
    }

    openSetAndRun(valueType) {
      assertEquals(setOf(2), this)
    }
  }
}