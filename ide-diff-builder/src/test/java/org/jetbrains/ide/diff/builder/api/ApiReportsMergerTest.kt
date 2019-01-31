package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import org.jetbrains.ide.diff.builder.signatures.ClassSignature
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ```
 * base IDE <---------base diff-----------------------------
 *                                    |                    |
 *                                    |                    |
 *                                    ∨                    |
 *    last IDE <----last diff---> current IDE              |
 *                     |                                   |
 *                     |                                   |
 *                     ---------------------------------   |
 *                                                     |   |
 *                                                     ∨   ∨
 *     <last result> --------------------------> <current result>
 * ```
 * - base IDE    = latest IDE from the previous branch (like 183.999)
 * - last IDE    = previous IDE build in the current branch (like 191.4)
 * - current IDE = IDE build in the current branch (like 191.5)
 *
 * 1) base-diff   = Δ(base IDE, current IDE)
 * 2) last-diff   = Δ(last IDE, current IDE)
 * 3) last-result = result built for last IDE.
 */
class ApiReportsMergerTest {

  private val oldBranchIde = ideVersion("IU-191.3")
  private val lastIde = ideVersion("IU-191.4")
  private val currentIde = ideVersion("IU-191.5")

  private fun testMerge(
      signature: ApiSignature,
      baseDiffEvents: Any?,
      lastDiffEvents: Any?,
      lastResultEvents: Any?,
      expectedEvents: Any?
  ) {
    @Suppress("UNCHECKED_CAST")
    fun Any?.toEvents(): List<ApiEvent> =
        when {
          this == null -> emptyList()
          this is ApiEvent -> listOf(this)
          else -> this as List<ApiEvent>
        }

    val baseDiff = createApiReport(currentIde) {
      baseDiffEvents.toEvents().forEach { this += it to signature }
    }

    val lastDiff = createApiReport(currentIde) {
      lastDiffEvents.toEvents().forEach { this += it to signature }
    }

    val lastResult = createApiReport(lastIde) {
      lastResultEvents.toEvents().forEach { this += it to signature }
    }

    val expectedReport = createApiReport(currentIde) {
      expectedEvents.toEvents().forEach { this += it to signature }
    }

    val mergedReport = mergeReports(currentIde, baseDiff, lastDiff, lastResult)
    assertReportsEqual(expectedReport, mergedReport)
  }


  @Test
  fun `added in current branch`() {
    testMerge(
        classSignature("added.class"),
        baseDiffEvents = IntroducedIn(currentIde),
        lastDiffEvents = null,
        lastResultEvents = IntroducedIn(oldBranchIde),
        expectedEvents = IntroducedIn(oldBranchIde)
    )
  }

  @Test
  fun `added in current IDE`() {
    testMerge(
        classSignature("added.class"),
        baseDiffEvents = IntroducedIn(currentIde),
        lastDiffEvents = IntroducedIn(currentIde),
        lastResultEvents = null,
        expectedEvents = IntroducedIn(currentIde)
    )
  }

  @Test
  fun `removed in current branch`() {
    testMerge(
        classSignature("removed.class"),
        baseDiffEvents = RemovedIn(currentIde),
        lastDiffEvents = null,
        lastResultEvents = RemovedIn(oldBranchIde),
        expectedEvents = RemovedIn(oldBranchIde)
    )
  }

  @Test
  fun `removed in current IDE`() {
    testMerge(
        classSignature("removed.class"),
        baseDiffEvents = RemovedIn(currentIde),
        lastDiffEvents = RemovedIn(currentIde),
        lastResultEvents = null,
        expectedEvents = RemovedIn(currentIde)
    )
  }

  /**
   * A class is not present in current IDE, but is "introduced in" some build, as stated in the last result.
   * It means that the last result contains outdated info, and it must be fixed based on actual diff between last and current IDEs.
   */
  @Test
  fun `class is removed in current branch but then available in last result but not in current IDE`() {
    testMerge(
        classSignature("added.then.removed.class"),
        baseDiffEvents = RemovedIn(currentIde),
        lastDiffEvents = RemovedIn(currentIde),
        lastResultEvents = IntroducedIn(oldBranchIde),
        expectedEvents = listOf(IntroducedIn(oldBranchIde), RemovedIn(currentIde))
    )
  }

  private fun assertReportsEqual(expectedReport: ApiReport, actualReport: ApiReport) {
    assertEquals(expectedReport.apiSignatureToEvents, actualReport.apiSignatureToEvents)
  }

  private fun mergeReports(
      ideVersion: IdeVersion,
      baseDiff: ApiReport,
      lastDiff: ApiReport,
      lastResult: ApiReport?
  ): ApiReport {
    val reports = listOfNotNull(baseDiff, lastDiff, lastResult)
    return ApiReportsMerger().mergeApiReports(ideVersion, reports)
  }

}

private fun ideVersion(s: String) = IdeVersion.createIdeVersion(s)

private fun classSignature(fullClassName: String) =
    ClassSignature(fullClassName.substringBeforeLast("."), fullClassName.substringAfterLast("."))

private fun createApiReport(
    ideVersion: IdeVersion,
    eventsAppender: MutableList<Pair<ApiEvent, ApiSignature>>.() -> Unit
): ApiReport {
  val events: MutableList<Pair<ApiEvent, ApiSignature>> = mutableListOf()
  events.eventsAppender()

  val signatureToEvents = hashMapOf<ApiSignature, MutableSet<ApiEvent>>()
  for ((event, signature) in events) {
    signatureToEvents.getOrPut(signature) { hashSetOf() } += event
  }

  return ApiReport(ideVersion, signatureToEvents)
}

