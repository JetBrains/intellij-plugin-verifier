package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiEventsMergerTest {
  @Test
  fun `only introduced`() {
    check(
        listOf(i("1.0")),
        listOf(i("1.0"))
    )
  }

  @Test
  fun `only removed`() {
    check(
        listOf(r("1.0")),
        listOf(r("1.0"))
    )
  }

  @Test
  fun `introduced and then removed`() {
    check(
        listOf(i("1.0"), r("2.0")),
        listOf(r("2.0"), i("1.0"))
    )
  }

  @Test
  fun `removed and then re-introduced`() {
    check(
        listOf(r("1.0"), i("2.0")),
        listOf(i("2.0"), r("1.0"))
    )
  }

  @Test
  fun `conflicting introduce version, select the earlier one`() {
    check(
        listOf(i("1.0")),
        listOf(i("2.0"), i("1.0"))
    )
  }

  @Test
  fun `conflicting remove version, select the earlier one`() {
    check(
        listOf(r("1.0")),
        listOf(r("2.0"), r("1.0"))
    )
  }

  @Test
  fun `introduced then removed then re-introduced`() {
    check(
        listOf(i("1.0"), r("2.0"), i("3.0")),
        listOf(r("2.0"), i("1.0"), i("3.0"))
    )
  }

  private fun check(expected: List<ApiEvent>, actual: List<ApiEvent>) {
    val mergedEvents = mergeEvents(actual)
    assertEquals(expected, mergedEvents)
  }

  private fun mergeEvents(events: List<ApiEvent>) = ApiEventsMerger().mergeEvents(events)

  private fun i(v: String) = IntroducedIn(IdeVersion.createIdeVersion(v))
  private fun r(v: String) = RemovedIn(IdeVersion.createIdeVersion(v))

}