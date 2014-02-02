package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Predicate;
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
import java.text.DateFormat;
import java.util.*;

public class CheckIdeHtmlReportBuilder {

  @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
  public static void build(@NotNull File report,
                           @NotNull String ideVersion,
                           @NotNull Predicate<Update> updateFilter,
                           @NotNull Map<Update, ProblemSet> results)
    throws IOException {
    Map<String, Map<Update, ProblemSet>> pluginsMap = new TreeMap<String, Map<Update, ProblemSet>>();

    for (Map.Entry<Update, ProblemSet> entry : results.entrySet()) {
      Map<Update, ProblemSet> pluginMap = pluginsMap.get(entry.getKey().getPluginId());
      if (pluginMap == null) {
        pluginMap = new HashMap<Update, ProblemSet>();
        pluginsMap.put(entry.getKey().getPluginId(), pluginMap);
      }

      pluginMap.put(entry.getKey(), entry.getValue());
    }

    PrintWriter out = new PrintWriter(report);

    try {
      out.append("<html>\n" +
                 "<head>\n" +
                 "  <title>Report created at " + DateFormat.getDateTimeInstance().format(new Date()) + "</title>\n" +
                 "\n" +
                 "  <link rel=\"stylesheet\" href=\"http://code.jquery.com/ui/1.10.4/themes/smoothness/jquery-ui.css\">\n" +
                 "  <script src=\"http://code.jquery.com/jquery-1.9.1.js\"></script>\n" +
                 "  <script src=\"http://code.jquery.com/ui/1.10.4/jquery-ui.js\"></script>\n" +

                 "  <style type=\"text/css\">\n" +
                 Resources.toString(CheckIdeHtmlReportBuilder.class.getResource("/reportCss.css"), Charset.forName("UTF-8")) +
                 "  </style>\n" +

                 "</head>\n" +
                 "\n" +
                 "<body>\n" +
                 "\n" +
                 "<h2>" + ideVersion + "</h2>" +
                 "<div id=\"tabs\">\n");

      if (pluginsMap.isEmpty()) {
        out.print("No plugins to check.\n");
      }
      else {
        out.print("  <ul>\n");

        int idx = 1;
        for (String pluginId : pluginsMap.keySet()) {
          out.printf("    <li><a href=\"#tabs-%d\">%s</a></li>\n", idx++, pluginId);
        }

        out.append("  </ul>\n");

        idx = 1;
        for (Map.Entry<String, Map<Update, ProblemSet>> entry : pluginsMap.entrySet()) {
          out.printf("  <div id=\"tabs-%d\">\n", idx++);

          if (entry.getValue().isEmpty()) {
            out.printf("There are no updates compatible with %s in the Plugin Repository\n", ideVersion);
          }
          else {
            List<Update> updates = new ArrayList<Update>(entry.getValue().keySet());
            Collections.sort(updates, Collections.reverseOrder(new UpdatesComparator()));

            for (Update update : updates) {
              ProblemSet problems = entry.getValue().get(update);

              out.printf("<div class=\"updates\">\n");

              out.printf("  <h3 class='%s %s'><span class='marker'>   </span> %s (#%d) %s</h3>\n",
                         problems.isEmpty() ? "ok" : "hasError",
                         updateFilter.apply(update) ? "" : "excluded",
                         HtmlEscapers.htmlEscaper().escape(update.getVersion()),
                         update.getUpdateId(),
                         problems.isEmpty() ? "" : "<small>" + problems.count() + " problems found</small>"
              );

              out.printf("  <div>\n");

              if (problems.isEmpty()) {
                out.printf(" No problems.");
              }
              else {
                List<Problem> problemList = new ArrayList<Problem>(problems.getAllProblems());
                Collections.sort(problemList, new ToStringProblemComparator());

                for (Problem problem : problemList) {
                  out.append("    <div class='errorDetails'>").append(HtmlEscapers.htmlEscaper().escape(problem.getDescription())).append(' ')
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

                    out.append(location.toString());
                  }

                  out.append("</div></div>");
                }
              }

              out.printf("  </div>\n");
              out.printf("</div>\n");
            }
          }

          out.append("  </div>\n");
        }
      }

      out.append("</div>\n"); // tabs


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

  private static class UpdatesComparator implements Comparator<Update> {
    @Override
    public int compare(Update o1, Update o2) {
      return o1.getUpdateId() - o2.getUpdateId();
    }
  }
}
