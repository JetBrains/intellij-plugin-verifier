package org.jetbrains.plugins.verifier.service.tests.database

import com.jetbrains.pluginverifier.filtering.IgnoreCondition
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

  private val databasePath by lazy {
    temp.resolve("database")
  }

  @Test
  fun `database open-write-close-reopen-read test`() {
    MapDbServerDatabase(databasePath).use { db ->
      val serviceDAO = ServiceDAO(db)
      serviceDAO.setProperty("one", "1")
      serviceDAO.setProperty("two", "2")
    }

    MapDbServerDatabase(databasePath).use { db ->
      val serviceDAO = ServiceDAO(db)
      assertEquals("1", serviceDAO.getProperty("one"))
      assertEquals("2", serviceDAO.getProperty("two"))
    }
  }

  private fun <T, R> openSetAndRun(valueType: ValueType<T>, f: MutableSet<T>.() -> R) =
      MapDbServerDatabase(temp
          .resolve("database")).use {
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
    MapDbServerDatabase(databasePath).use { db ->
      val set = db.openOrCreateSet("lists", ValueType.SERIALIZABLE)
      set.add(list)
    }

    MapDbServerDatabase(databasePath).use { db ->
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

  @Test
  fun `test list`() {
    MapDbServerDatabase(databasePath).use { db ->
      val list = db.openOrCreateList("list", ValueType.STRING)
      list.add("c")
      list.add("a")
      list.add("b")
    }

    MapDbServerDatabase(databasePath).use { db ->
      val list = db.openOrCreateList("list", ValueType.STRING)
      assertEquals(listOf("c", "a", "b"), list)
    }
  }

  @Test
  fun `serialize and deserialize problems ignoring conditions`() {
    val one = IgnoreCondition(null, null, Regex("xxx"))
    val two = IgnoreCondition("pluginId", null, Regex("xxx"))
    val three = IgnoreCondition("pluginId", "version", Regex("xxx"))

    MapDbServerDatabase(databasePath).use { db ->
      val serviceDAO = ServiceDAO(db)
      serviceDAO.addIgnoreCondition(one)
      serviceDAO.addIgnoreCondition(two)
      serviceDAO.addIgnoreCondition(three)
    }

    MapDbServerDatabase(databasePath).use { db ->
      val serviceDAO = ServiceDAO(db)
      val ignoreConditions = serviceDAO.ignoreConditions
      assertEquals(listOf(one, two, three), ignoreConditions)
    }
  }
}