package verifier.tests;

import com.jetbrains.pluginverifier.commands.NewProblemsCommand;
import com.jetbrains.pluginverifier.results.ResultsRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author Sergey Patrikeev
 */
public class TestPreviousBuilds {

  public static final List<String> BUILDS = Arrays.asList(
      "IU-144.1909",
      "IU-143.989",
      "IU-144.1901",
      "IU-143.879",
      "IU-144.1956",
      "144.01",
      "139.99999"
  );

  @Test
  public void testPreviousBuilds() throws Exception {
    ResultsRepository resultsRepository = new ResultsRepository() {

      @NotNull
      @Override
      public List<String> getAvailableReportsList() throws IOException {
        return BUILDS;
      }

      @NotNull
      @Override
      //not to be invoked
      public File getReportFile(@NotNull String build) throws IOException {
        return new File(".");
      }

      @NotNull
      @Override
      public String getRepositoryUrl() {
        return "Dummy";
      }
    };

    Assert.assertEquals(Arrays.asList("144.01", "IU-144.1901", "IU-144.1909"), NewProblemsCommand.findPreviousBuilds("144.1950", resultsRepository));
    Assert.assertEquals(Collections.emptyList(), NewProblemsCommand.findPreviousBuilds("143.0", resultsRepository));
    Assert.assertEquals(Arrays.asList("144.01", "IU-144.1901", "IU-144.1909", "IU-144.1956"), NewProblemsCommand.findPreviousBuilds("144.9999", resultsRepository));

  }
}
