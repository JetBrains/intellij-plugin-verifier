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

  @Test
  fun `database set open-write-recreate-read`() {
    MapDbServerDatabase(temp).use { db ->
      val set = db.openOrCreateSet("newMap", ValueType.STRING)
      set.addAll(listOf("one", "two", "three"))
    }

    MapDbServerDatabase(temp).use { db ->
      val set = db.openOrCreateSet("newMap", ValueType.STRING)
      assertEquals(setOf("one", "two", "three"), set)
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
}