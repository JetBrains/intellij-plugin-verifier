package com.jetbrains.pluginverifier.results;

import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.misc.DownloadUtils;
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final static Logger LOG = LoggerFactory.getLogger(GlobalResultsRepository.class);

  @NotNull
  @Override
  public List<IdeVersion> getAvailableReportsList() throws IOException {
    LOG.info("Loading check results list...");
    URL url = new URL(RepositoryConfiguration.getInstance().getPluginRepositoryUrl() + "/problems/resultList?format=txt");

    String text = IOUtils.toString(url);

    List<IdeVersion> res = new ArrayList<IdeVersion>();

    for (StringTokenizer st = new StringTokenizer(text, " ,"); st.hasMoreTokens(); ) {
      res.add(IdeVersion.createIdeVersion(st.nextToken()));
    }

    LOG.info("done");
    return res;
  }

  @NotNull
  @Override
  public File getReportFile(@NotNull IdeVersion ideVersion) throws IOException {
    return DownloadUtils.getCheckResultFile(ideVersion);
  }

  @NotNull
  @Override
  public String getRepositoryUrl() {
    return RepositoryConfiguration.getInstance().getPluginRepositoryUrl();
  }

}
