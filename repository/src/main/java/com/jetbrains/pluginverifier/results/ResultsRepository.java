package com.jetbrains.pluginverifier.results;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public abstract class ResultsRepository {

  @NotNull
  public abstract List<String> getAvailableReportsList() throws IOException;

  @NotNull
  public abstract File getReportFile(@NotNull String build) throws IOException;

  public void uploadReportFile(@NotNull File fileToUpload) throws IOException {
    //Default implementation does nothing
  }

}
