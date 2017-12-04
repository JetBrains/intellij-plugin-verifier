package org.jetbrains.plugins.verifier.service.tests.database

import org.jetbrains.plugins.verifier.service.server.ServiceDAO
import org.jetbrains.plugins.verifier.service.server.database.MapDbServerDatabase
import org.jetbrains.plugins.verifier.service.server.database.ValueType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabaseTest {

  @JvmField
  @Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun `database open-write-close-reopen-read test`() {
    val folder = tempFolder.newFolder().toPath()
    MapDbServerDatabase(folder).use { db ->
      val serviceDAO = ServiceDAO(db)
      serviceDAO.setProperty("one", "1")
      serviceDAO.setProperty("two", "2")
    }

    MapDbServerDatabase(folder).use { db ->
      val serviceDAO = ServiceDAO(db)
      assertEquals("1", serviceDAO.getProperty("one"))
      assertEquals("2", serviceDAO.getProperty("two"))
    }
  }

  @Test
  fun `database set open-write-recreate-read`() {
    val folder = tempFolder.newFolder().toPath()
    MapDbServerDatabase(folder).use { db ->
      val set = db.openOrCreateSet("newMap", ValueType.STRING)
      set.addAll(listOf("one", "two", "three"))
    }

    MapDbServerDatabase(folder).use { db ->
      val set = db.openOrCreateSet("newMap", ValueType.STRING)
      assertEquals(setOf("one", "two", "three"), set)
    }
  }
}