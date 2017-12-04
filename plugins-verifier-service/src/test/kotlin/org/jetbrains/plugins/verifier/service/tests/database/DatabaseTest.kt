package org.jetbrains.plugins.verifier.service.tests.database

import org.jetbrains.plugins.verifier.service.server.database.MapDbServerDatabase
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
      db.setProperty("one", "1")
      db.setProperty("two", "2")
    }

    MapDbServerDatabase(folder).use { db ->
      assertEquals("1", db.getProperty("one"))
      assertEquals("2", db.getProperty("two"))
    }
  }

}