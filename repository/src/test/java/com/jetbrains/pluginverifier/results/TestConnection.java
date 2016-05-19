package com.jetbrains.pluginverifier.results;

import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.service.VerifierServiceApi;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class TestConnection {
  public static void main(String[] args) throws IOException {
    VerifierServiceRepository repository = new VerifierServiceRepository(VerifierServiceApi.DEFAULT_SERVICE_URL);
    List<IdeVersion> reportsList = repository.getAvailableReportsList();
    System.out.println("Reports list: " + reportsList);
    File dir = new File("for_tests");
    FileUtils.forceMkdir(dir);
    for (IdeVersion s : reportsList) {
      File reportFile = repository.getReportFile(s);
      System.out.println("Loaded: " + reportFile);
    }
  }
}

class UploadSomeReport {
  public static void main(String[] args) throws IOException {
    VerifierServiceRepository repository = new VerifierServiceRepository(VerifierServiceApi.DEFAULT_SERVICE_URL);
    File file1 = new File(".");
    System.out.println(file1.getAbsolutePath());
    File file = new File("for_tests/build-report.xml");
    repository.uploadReportFile(file);

  }
}
