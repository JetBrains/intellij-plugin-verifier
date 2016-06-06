import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.persistence.Persistence;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.IncompatibleClassChangeProblem;
import com.jetbrains.pluginverifier.problems.MethodNotFoundProblem;
import com.jetbrains.pluginverifier.problems.NoCompatibleUpdatesProblem;
import com.jetbrains.pluginverifier.reports.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Sergey Patrikeev
 */
public class TestReport {
  @Test
  public void testReport() throws Exception {

    DetailsImpl details1 = new DetailsBuilder()
        .setIdeVersion(IdeVersion.createIdeVersion("IC-1500"))
        .setOverview("overview1")
        .setPlugin(new UpdateInfo(100500))
        .addProblem(new ClassNotFoundProblem("NO_CLASS"), ProblemLocation.fromClass("fromClass"))
        .addProblem(new MethodNotFoundProblem("METHOD"), ProblemLocation.fromMethod("fromClass", "method"))
        .addProblem(new IncompatibleClassChangeProblem("CLASS", IncompatibleClassChangeProblem.Change.CLASS_TO_INTERFACE), ProblemLocation.fromMethod("Class", "Method"))
        .addProblem(new IncompatibleClassChangeProblem("CLASS", IncompatibleClassChangeProblem.Change.INTERFACE_TO_CLASS), ProblemLocation.fromMethod("Class", "Method"))
        .build();

    DetailsImpl details2 = new DetailsBuilder()
        .setIdeVersion(IdeVersion.createIdeVersion("IU-1"))
        .setOverview("overview2")
        .setPlugin(new UpdateInfo("id", "name", "version"))
        .addProblem(new NoCompatibleUpdatesProblem("plugin", "id"), ProblemLocation.fromPlugin("pid"))
        .build();


    Report init = new ReportBuilder().add(details1).add(details2).build();

    String json = Persistence.GSON.toJson(init);
    Report converted = Persistence.GSON.fromJson(json, Report.class);

    assertEquals(init.details().size(), converted.details().size());
    for (int i = 0; i < init.details().size(); i++) {
      Details one = init.details().get(i);
      Details two = converted.details().get(i);

      assertEquals(one.checkedIde(), two.checkedIde());
      assertEquals(one.checkedPlugin(), two.checkedPlugin());
      assertEquals(one.overview(), two.overview());
      assertEquals(one.problems(), two.problems());
    }

  }
}
