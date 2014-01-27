package com.jetbrains.pluginverifier.commands;

import com.google.common.html.HtmlEscapers;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.util.UpdateJson;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.*;

public class CheckIdeHtmlReportBuilder {

  @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
  public static void build(@NotNull File report, @NotNull String ideVersion, @NotNull Map<UpdateJson, Collection<Problem>> results)
    throws IOException {
    Map<String, Map<UpdateJson, Collection<Problem>>> pluginsMap = new TreeMap<String, Map<UpdateJson, Collection<Problem>>>();

    for (Map.Entry<UpdateJson, Collection<Problem>> entry : results.entrySet()) {
      Map<UpdateJson, Collection<Problem>> pluginMap = pluginsMap.get(entry.getKey().getPluginId());
      if (pluginMap == null) {
        pluginMap = new HashMap<UpdateJson, Collection<Problem>>();
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
                 "    .errorDetails {\n" +
                 "      padding: 2px;\n" +
                 "    }\n" +
                 "" +
                 "    .updates div div:nth-child(odd) {\n" +
                 "      background: #eee;\n" +
                 "    }\n" +
                 "" +
                 "    .marker {\n" +
                 "      white-space: pre;\n" +
                 "    }\n" +
                 "\n" +
                 "    .ok .marker {\n" +
                 "      background: #0f0;\n" +
                 "    }\n" +
                 "    .hasError .marker {\n" +
                 "      background: #f00;\n" +
                 "    }\n" +
                 "  </style>\n" +

                 "</head>\n" +
                 "\n" +
                 "<script>\n" +
                 "  $(function() {\n" +
                 "    $( \"#tabs\" ).tabs();\n" +
                 "    $( \".updates\" ).accordion({active: false, collapsible: true, heightStyle: 'content'});\n" +
                 "  });\n" +
                 "</script>\n" +
                 "\n" +
                 "<body>\n" +
                 "\n" +
                 "<div id=\"tabs\">\n" +
                 "  <ul>\n");

      int idx = 1;
      for (String pluginId : pluginsMap.keySet()) {
        out.printf("    <li><a href=\"#tabs-%d\">%s</a></li>\n", idx++, pluginId);
      }

      out.append("  </ul>\n");

      idx = 1;
      for (Map.Entry<String, Map<UpdateJson, Collection<Problem>>> entry : pluginsMap.entrySet()) {
        out.printf("  <div id=\"tabs-%d\">\n", idx++);

        if (entry.getValue().isEmpty()) {
          out.printf("There are no updates compatible with %s in the Plugin Repository\n", ideVersion);
        }
        else {
          List<UpdateJson> updates = new ArrayList<UpdateJson>(entry.getValue().keySet());
          Collections.sort(updates, Collections.reverseOrder(new UpdatesComparator()));

          for (UpdateJson update : updates) {
            Collection<Problem> problems = entry.getValue().get(update);

            out.printf("<div class=\"updates\">\n");

            out.printf("  <h3 class='%s'><span class='marker'>   </span> %s (#%d)</h3>\n",
                       problems.isEmpty() ? "ok" : "hasError",
                       HtmlEscapers.htmlEscaper().escape(update.getVersion()),
                       update.getUpdateId());

            out.printf("  <div>\n");

            if (problems.isEmpty()) {
              out.printf(" No problems.");
            }
            else {
              for (Problem problem : problems) {
                out.printf("    <div class='errorDetails'>%s</div>", HtmlEscapers.htmlEscaper().escape(problem.getDescription()));
              }
            }

            out.printf("  </div>\n");
            out.printf("</div>\n");
          }
        }

        out.append("  </div>\n");
      }

      out.append("</div>\n"); // tabs
      out.append("</body>\n");
      out.append("</html>");
    }
    finally {
      out.close();
    }
  }

  private static class UpdatesComparator implements Comparator<UpdateJson> {
    @Override
    public int compare(UpdateJson o1, UpdateJson o2) {
      return o1.getUpdateId() - o2.getUpdateId();
    }
  }
}
