package com.jetbrains.pluginverifier.utils

import com.google.common.html.HtmlEscapers
import com.google.common.io.Resources
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.results.ProblemSet
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.Predicate
import kotlin.comparisons.compareByDescending

//TODO: rewrite using VPrinter
object HtmlReportBuilder {

  private val UPDATE_DATE_FORMAT = SimpleDateFormat("yyyy.MM.dd HH:mm")

  @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
  @Throws(IOException::class)
  fun build(report: File,
            ideVersion: IdeVersion,
            updateFilter: Predicate<UpdateInfo>,
            results: Map<UpdateInfo, ProblemSet>) {
    //pluginId -> [list of all its checked builds]
    val pidToBuilds: Map<String, List<UpdateInfo>> = results.keys
        .groupBy { it.pluginId!! }
        .mapValues { it.value.sortedWith(compareByDescending(VersionComparatorUtil.COMPARATOR, { it.version })) }

    PrintWriter(report).use { out ->

      out.append("<html>\n<head>\n  <title>Result of checking $ideVersion</title>\n\n  " +
          "<link rel='stylesheet' href='http://code.jquery.com/ui/1.10.4/themes/smoothness/jquery-ui.css'>\n  " +
          "<script src='http://code.jquery.com/jquery-1.9.1.js'></script>\n  " +
          "<script src='http://code.jquery.com/ui/1.10.4/jquery-ui.js'></script>\n  " +
          "<style type='text/css'>\n${Resources.toString(HtmlReportBuilder::class.java.getResource("/reportCss.css"), Charset.forName("UTF-8"))}  " +
          "</style>\n" +
          "</head>\n\n" +
          "<body>\n\n<h2>$ideVersion</h2>\n<label>\n  <input id='problematicOnlyCB' type='checkbox' onchange=\"" +
          "if ($('#problematicOnlyCB').is(':checked')) {$('body').addClass('problematicOnly')} else {$('body').removeClass('problematicOnly')} \">\n  " +
          "Show problematic plugins only\n</label>\n")

      //"<div id='tabs'>\n" +
      //"  <ul>\n" +
      //"    <li><a href='#tab-plugins'>Plugins</a></li>\n" +
      //"    <li><a href='#tab-problems'>Problems</a></li>\n" +
      //"  </ul>\n" +
      //"  <div id='tab-plugins'>\n");
      if (pidToBuilds.isEmpty()) {
        out.print("No plugins checked.\n")
      } else {
        for ((pluginId, checkedBuilds) in pidToBuilds) {

          out.printf("<div class='plugin %s'>\n", if (pluginHasProblems(checkedBuilds, results, updateFilter)) "pluginHasProblem" else "pluginOk")


          var pluginName: String? = null

          if (!checkedBuilds.isEmpty()) {
            pluginName = checkedBuilds[0].pluginName
          }

          if (StringUtil.isEmpty(pluginName)) {
            pluginName = pluginId
          }

          out.printf("  <h3><span class='pMarker'>   </span> %s</h3>\n", HtmlEscapers.htmlEscaper().escape(pluginName))
          out.printf("  <div>\n")

          if (checkedBuilds.isEmpty()) {
            out.printf("There are no updates compatible with %s in the Plugin Repository\n", ideVersion)
          } else {
            for (update in checkedBuilds) {
              val problems = results[update]!!

              out.printf("<div class='update %s %s'>\n",
                  if (problems.isEmpty) "updateOk" else "updateHasProblems",
                  if (updateFilter.test(update)) "" else "excluded")

              out.printf("  <h3><span class='uMarker'>   </span> %s <small>(#%d%s)</small> %s</h3>\n",
                  HtmlEscapers.htmlEscaper().escape(update.version),
                  update.updateId,
                  if (update.cdate == null) "" else ", " + UPDATE_DATE_FORMAT.format(Date(update.cdate!!)),
                  if (problems.isEmpty) "" else "<small>" + problems.count() + " problems found</small>")

              out.printf("  <div>\n")

              if (problems.isEmpty) {
                out.printf("No problems.\n")
              } else {
                val problemList = ProblemUtils.sortProblems(problems.allProblems)

                for (problem in problemList) {
                  out.append("    <div class='errorDetails'>").append(HtmlEscapers.htmlEscaper().escape(problem.description)).append(' ').append("<a href=\"#\" class='detailsLink'>details</a>\n")


                  out.append("<div class='errLoc'>")

                  val locationList = ArrayList(problems.getLocations(problem))
                  Collections.sort(locationList, ToStringCachedComparator<ProblemLocation>())

                  var isFirst = true
                  for (location in locationList) {
                    if (isFirst) {
                      isFirst = false
                    } else {
                      out.append("<br>")
                    }

                    out.append(HtmlEscapers.htmlEscaper().escape(location.toString()))
                  }

                  out.append("</div></div>")
                }
              }

              out.printf("  </div>\n")
              out.printf("  </div>\n") // <div class='update'>
            }
          }

          out.printf("  </div>\n")
          out.printf("</div>\n") //  <div class='plugin'>
        }
      }

      //out.append("  </div>\n" + // tab-plugins
      //           "  <div id='tab-problems'>\n");
      //
      //out.println("    Problems\n");
      //
      //out.append("  </div>\n"); // tab-problems
      //out.append("</div>\n"); // tabs

      out.append("<script>\n")
      out.append(Resources.toString(HtmlReportBuilder::class.java.getResource("/reportScript.js"), Charset.forName("UTF-8")))
      out.append("</script>\n")

      out.append("</body>\n")
      out.append("</html>")

    }

  }

  private fun pluginHasProblems(checkedUpdates: List<UpdateInfo>,
                                results: Map<UpdateInfo, ProblemSet>,
                                updateFilter: Predicate<UpdateInfo>): Boolean =
      checkedUpdates
          .filter { updateFilter.test(it) }
          .filter { !results[it]!!.isEmpty }
          .isNotEmpty()

}
