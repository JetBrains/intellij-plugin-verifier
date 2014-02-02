package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.Resources;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ProblemSet;
import com.jetbrains.pluginverifier.utils.ToStringCachedComparator;
import com.jetbrains.pluginverifier.utils.ToStringProblemComparator;
import com.jetbrains.pluginverifier.utils.Update;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;

public class CheckIdeHtmlReportBuilder {

  @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
  public static void build(@NotNull File report,
                           @NotNull String ideVersion,
                           List<String> pluginIds, @NotNull Predicate<Update> updateFilter,
                           @NotNull Map<Update, ProblemSet> results)
    throws IOException {
    Map<String, List<Update>> pluginsMap = new TreeMap<String, List<Update>>();

    for (String pluginId : pluginIds) {
      pluginsMap.put(pluginId, new ArrayList<Update>());
    }

    for (Update update : results.keySet()) {
      List<Update> updatesList = pluginsMap.get(update.getPluginId());
      assert updatesList != null : "Invalid arguments, pluginIds doesn't contains " + update.getPluginId();

      updatesList.add(update);
    }

    for (List<Update> updateList : pluginsMap.values()) {
      Collections.sort(updateList, Collections.reverseOrder(UpdatesComparator.INSTANCE));
    }

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
      }
      else {
        for (Map.Entry<String, List<Update>> entry : pluginsMap.entrySet()) {
          out.printf("<div class='plugin %s'>\n",
                     pluginHasProblems(entry.getValue(), results, updateFilter) ? "pluginHasProblem" : "pluginOk");

          String pluginId = entry.getKey();

          out.printf("  <h3><span class='pMarker'>   </span> %s</h3>\n", HtmlEscapers.htmlEscaper().escape(pluginId));
          out.printf("  <div>\n");

          if (entry.getValue().isEmpty()) {
            out.printf("There are no updates compatible with %s in the Plugin Repository\n", ideVersion);
          }
          else {
            for (Update update : entry.getValue()) {
              ProblemSet problems = results.get(update);

              out.printf("<div class='update %s %s'>\n",
                         problems.isEmpty() ? "updateOk" : "updateHasProblems",
                         updateFilter.apply(update) ? "" : "excluded");

              out.printf("  <h3><span class='uMarker'>   </span> %s (#%d) %s</h3>\n",
                         HtmlEscapers.htmlEscaper().escape(update.getVersion()),
                         update.getUpdateId(),
                         problems.isEmpty() ? "" : "<small>" + problems.count() + " problems found</small>"
              );

              out.printf("  <div>\n");

              if (problems.isEmpty()) {
                out.printf("No problems.\n");
              }
              else {
                List<Problem> problemList = new ArrayList<Problem>(problems.getAllProblems());
                Collections.sort(problemList, new ToStringProblemComparator());

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
                    }
                    else {
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
    }
    finally {
      out.close();
    }
  }

  private static boolean pluginHasProblems(List<Update> updates, Map<Update, ProblemSet> results, Predicate<Update> updateFilter) {
    for (Update update : updates) {
      if (updateFilter.apply(update)) {
        if (!results.get(update).isEmpty()) return true;
      }
    }

    return false;
  }

  private static class UpdatesComparator implements Comparator<Update> {

    private static final Comparator<Update> INSTANCE = new UpdatesComparator();

    @Override
    public int compare(Update o1, Update o2) {
      Ordering<Comparable> c = Ordering.natural().nullsLast();

      return ComparisonChain.start()
        .compare(o1.getUpdateId(), o2.getUpdateId(), c)
        .compare(o1.getPluginId(), o2.getPluginId(), c)
        .compare(o1.getVersion(), o2.getVersion(), c)
        .result();
    }
  }
}
