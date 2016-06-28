package com.jetbrains.pluginverifier.utils;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.Resources;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;

public class CheckIdeHtmlReportBuilder {

  private static final DateFormat UPDATE_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm");

  @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
  public static void build(@NotNull File report,
                           @NotNull String ideVersion,
                           @NotNull List<String> pluginIds,
                           @NotNull Predicate<UpdateInfo> updateFilter,
                           @NotNull Map<UpdateInfo, ProblemSet> results) throws IOException {
    //pluginId -> [list of all its checked builds]
    Map<String, List<UpdateInfo>> pluginsMap = getCheckedPluginsMap(pluginIds, results);

    PrintWriter out = new PrintWriter(report);

    try {
      out.append("<html>\n" +
          "<head>\n" +
          "  <title>Result of checking " + ideVersion + "</title>\n" +
          "\n" +
          "  <link rel='stylesheet' href='http://code.jquery.com/ui/1.10.4/themes/smoothness/jquery-ui.css'>\n" +
          "  <script src='http://code.jquery.com/jquery-1.9.1.js'></script>\n" +
          "  <script src='http://code.jquery.com/ui/1.10.4/jquery-ui.js'></script>\n" +

          "  <style type='text/css'>\n" +
          Resources.toString(CheckIdeHtmlReportBuilder.class.getResource("/reportCss.css"), Charset.forName("UTF-8")) +
          "  </style>\n" +
          "</head>\n" +
          "\n" +
          "<body>\n" +
          "\n" +
          "<h2>" + ideVersion + "</h2>\n" +
          "<label>\n" +
          "  <input id='problematicOnlyCB' type='checkbox' onchange=\"if ($('#problematicOnlyCB').is(':checked')) {$('body').addClass('problematicOnly')} else {$('body').removeClass('problematicOnly')} \">\n" +
          "  Show problematic plugins only\n" +
          "</label>\n");

      //"<div id='tabs'>\n" +
      //"  <ul>\n" +
      //"    <li><a href='#tab-plugins'>Plugins</a></li>\n" +
      //"    <li><a href='#tab-problems'>Problems</a></li>\n" +
      //"  </ul>\n" +
      //"  <div id='tab-plugins'>\n");
      if (pluginsMap.isEmpty()) {
        out.print("No plugins checked.\n");
      } else {
        for (Map.Entry<String, List<UpdateInfo>> entry : pluginsMap.entrySet()) {
          String pluginId = entry.getKey();
          List<UpdateInfo> checkedBuilds = entry.getValue();

          out.printf("<div class='plugin %s'>\n", pluginHasProblems(checkedBuilds, results, updateFilter) ? "pluginHasProblem" : "pluginOk");


          String pluginName = null;

          if (!checkedBuilds.isEmpty()) {
            pluginName = checkedBuilds.get(0).getPluginName();
          }

          if (StringUtil.isEmpty(pluginName)) {
            pluginName = pluginId;
          }

          out.printf("  <h3><span class='pMarker'>   </span> %s</h3>\n", HtmlEscapers.htmlEscaper().escape(pluginName));
          out.printf("  <div>\n");

          if (checkedBuilds.isEmpty()) {
            out.printf("There are no updates compatible with %s in the Plugin Repository\n", ideVersion);
          } else {
            for (UpdateInfo update : checkedBuilds) {
              ProblemSet problems = results.get(update);

              out.printf("<div class='update %s %s'>\n",
                  problems.isEmpty() ? "updateOk" : "updateHasProblems",
                  updateFilter.test(update) ? "" : "excluded");

              out.printf("  <h3><span class='uMarker'>   </span> %s <small>(#%d%s)</small> %s</h3>\n",
                  HtmlEscapers.htmlEscaper().escape(update.getVersion()),
                  update.getUpdateId(),
                  update.getCdate() == null ? "" : ", " + UPDATE_DATE_FORMAT.format(new Date(update.getCdate())),
                  problems.isEmpty() ? "" : "<small>" + problems.count() + " problems found</small>"
              );

              out.printf("  <div>\n");

              if (problems.isEmpty()) {
                out.printf("No problems.\n");
              } else {
                List<Problem> problemList = ProblemUtils.sortProblems(problems.getAllProblems());

                for (Problem problem : problemList) {
                  out.append("    <div class='errorDetails'>").append(HtmlEscapers.htmlEscaper().escape(problem.getDescription()))
                      .append(' ')
                      .append("<a href=\"#\" class='detailsLink'>details</a>\n");


                  out.append("<div class='errLoc'>");

                  List<ProblemLocation> locationList = new ArrayList<ProblemLocation>(problems.getLocations(problem));
                  Collections.sort(locationList, new ToStringCachedComparator<ProblemLocation>());

                  boolean isFirst = true;
                  for (ProblemLocation location : locationList) {
                    if (isFirst) {
                      isFirst = false;
                    } else {
                      out.append("<br>");
                    }

                    out.append(HtmlEscapers.htmlEscaper().escape(location.toString()));
                  }

                  out.append("</div></div>");
                }
              }

              out.printf("  </div>\n");
              out.printf("  </div>\n"); // <div class='update'>
            }
          }

          out.printf("  </div>\n");
          out.printf("</div>\n"); //  <div class='plugin'>
        }
      }

      //out.append("  </div>\n" + // tab-plugins
      //           "  <div id='tab-problems'>\n");
      //
      //out.println("    Problems\n");
      //
      //out.append("  </div>\n"); // tab-problems
      //out.append("</div>\n"); // tabs

      out.append("<script>\n");
      out.append(Resources.toString(CheckIdeHtmlReportBuilder.class.getResource("/reportScript.js"), Charset.forName("UTF-8")));
      out.append("</script>\n");

      out.append("</body>\n");
      out.append("</html>");
    } finally {
      out.close();
    }
  }

  /**
   * @param pluginIds initial list of all the pluginId to be checked
   * @param results   map of problems of this check
   * @return map from pluginId TO all its checked builds (in DESC order)
   */
  @NotNull
  private static Map<String, List<UpdateInfo>> getCheckedPluginsMap(@NotNull List<String> pluginIds,
                                                                    @NotNull Map<UpdateInfo, ProblemSet> results) {
    Map<String, List<UpdateInfo>> pluginsMap = new TreeMap<String, List<UpdateInfo>>();

    for (String pluginId : pluginIds) {
      pluginsMap.put(pluginId, new ArrayList<UpdateInfo>());
    }

    for (UpdateInfo brokenUpdate : results.keySet()) {
      List<UpdateInfo> updatesList = pluginsMap.get(brokenUpdate.getPluginId());
      if (updatesList == null) {
        throw new IllegalArgumentException("Invalid arguments, pluginIds doesn't contain " + brokenUpdate.getPluginId());
      }

      updatesList.add(brokenUpdate);
    }

    for (List<UpdateInfo> updateList : pluginsMap.values()) {
      //sort ids in DESC order
      Collections.sort(updateList, Collections.reverseOrder(UpdatesComparator.INSTANCE));
    }

    return pluginsMap;
  }

  /**
   * @return true iff plugin was checked AND is not excluded AND has some problems
   */
  private static boolean pluginHasProblems(@NotNull List<UpdateInfo> checkedUpdates,
                                           @NotNull Map<UpdateInfo, ProblemSet> results,
                                           @NotNull Predicate<UpdateInfo> updateFilter) {
    for (UpdateInfo update : checkedUpdates) {
      if (updateFilter.test(update)) {
        if (!results.get(update).isEmpty()) return true;
      }
    }

    return false;
  }

  private static class UpdatesComparator implements Comparator<UpdateInfo> {

    private static final Comparator<UpdateInfo> INSTANCE = new UpdatesComparator();

    @Override
    public int compare(UpdateInfo o1, UpdateInfo o2) {
      Ordering<Comparable> c = Ordering.natural().nullsLast();

      return ComparisonChain.start()
          .compare(o1.getUpdateId(), o2.getUpdateId(), c)
          .compare(o1.getPluginId(), o2.getPluginId(), c)
          .compare(o1.getVersion(), o2.getVersion(), c)
          .result();
    }
  }
}
