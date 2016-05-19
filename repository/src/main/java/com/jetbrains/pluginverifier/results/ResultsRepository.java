package com.jetbrains.pluginverifier.results;

import com.intellij.structure.domain.IdeVersion;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public abstract class ResultsRepository {

  /**
   * Returns IDEA-builds which have check reports in the repository.
   */
  @NotNull
  public abstract List<IdeVersion> getAvailableReportsList() throws IOException;

  @NotNull
  public abstract File getReportFile(@NotNull IdeVersion ideVersion) throws IOException;

  public void uploadReportFile(@NotNull File fileToUpload) throws IOException {
    //Default implementation does nothing
  }

  @NotNull
  public abstract String getRepositoryUrl();
}
