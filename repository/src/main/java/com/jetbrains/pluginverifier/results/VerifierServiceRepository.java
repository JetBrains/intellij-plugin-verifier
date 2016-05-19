package com.jetbrains.pluginverifier.results;

import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.service.VerifierServiceApi;
import com.jetbrains.pluginverifier.utils.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class VerifierServiceRepository extends ResultsRepository {

  private final String myRepositoryUrl;

  public VerifierServiceRepository(@NotNull String repositoryUrl) {
    myRepositoryUrl = StringUtil.trimEnd(repositoryUrl, "/");
  }

  @NotNull
  @Override
  public List<IdeVersion> getAvailableReportsList() throws IOException {
    return VerifierServiceApi.requestAvailableReports(myRepositoryUrl);
  }

  @NotNull
  @Override
  public File getReportFile(@NotNull IdeVersion ideVersion) throws IOException {
    return VerifierServiceApi.requestReportFile(myRepositoryUrl, ideVersion);
  }

  @Override
  public void uploadReportFile(@NotNull File fileToUpload) throws IOException {
    VerifierServiceApi.uploadReportFile(myRepositoryUrl, fileToUpload);
  }

  @NotNull
  @Override
  public String getRepositoryUrl() {
    return myRepositoryUrl;
  }
}
