package com.jetbrains.pluginverifier.results;

import com.jetbrains.pluginverifier.misc.DownloadUtils;
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Sergey Patrikeev
 */
public class GlobalResultsRepository extends ResultsRepository {

  @NotNull
  @Override
  public List<String> getAvailableReportsList() throws IOException {
    System.out.print("Loading check results list...");
    URL url = new URL(RepositoryConfiguration.getInstance().getPluginRepositoryUrl() + "/problems/resultList?format=txt");

    String text = IOUtils.toString(url);

    List<String> res = new ArrayList<String>();

    for (StringTokenizer st = new StringTokenizer(text, " ,"); st.hasMoreTokens(); ) {
      res.add(st.nextToken());
    }

    System.out.println("done");
    return res;
  }

  @NotNull
  @Override
  public File getReportFile(@NotNull String build) throws IOException {
    return DownloadUtils.getCheckResultFile(build);
  }

}
