import com.jetbrains.pluginverifier.problems.*;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import com.jetbrains.pluginverifier.utils.ResultsElement;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.*;

/**
 * @author Sergey Patrikeev
 */
public class TestMarshaller {

  public static File getTempFile(@NotNull String name) {
    final File tempDir = new File(System.getProperty("java.io.tmpdir"), "plugin-verifier-test-data-cache");
    //noinspection ResultOfMethodCallIgnored
    tempDir.mkdirs();
    return new File(tempDir, name);
  }

  @Test
  public void testMarshallUnmarshall() throws Exception {
    File xml = getTempFile("temp.xml");
    Map<UpdateInfo, Collection<Problem>> map = new HashMap<UpdateInfo, Collection<Problem>>();
    UpdateInfo updateInfo = new UpdateInfo();
    updateInfo.setUpdateId(12345);
    List<Problem> problems = Arrays.asList(new ClassNotFoundProblem("someClass"), new MethodNotFoundProblem("someMethod"), new MethodNotImplementedProblem("notImplemented"));
    map.put(updateInfo, problems);
    ProblemUtils.saveProblems(xml, "IU-144.0000", map);
    ResultsElement element = ProblemUtils.loadProblems(xml);
    Assert.assertEquals(problems, element.getAllProblems());
  }

  //should not throw any exception
  @Test
  public void testUnmarshallingBrokenReport() throws Exception {
    File xml = new File(getClass().getResource("brokenReport.xml").toURI());

    ResultsElement element = ProblemUtils.loadProblems(xml);

    Map<UpdateInfo, Collection<Problem>> map = element.asMap();

    Set<Problem> problems = new HashSet<Problem>(element.getAllProblems());

    for (Map.Entry<UpdateInfo, Collection<Problem>> entry : map.entrySet()) {
      for (Problem problem : entry.getValue()) {
        problems.remove(problem);
      }
    }

    Assert.assertTrue(problems.isEmpty());
  }

}

