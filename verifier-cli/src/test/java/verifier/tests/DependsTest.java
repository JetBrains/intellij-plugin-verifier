package verifier.tests;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Patrikeev
 */
public class DependsTest {

  //Plugin download url to number of .xml should be found
  private static final Map<String, Integer> MAP = new HashMap<String, Integer>();

  static {
    MAP.put("KotlinIJ141-17.zip", 14);
    MAP.put("Scala1_9_4.zip", 16);
  }

  @Test
  public void testDepends() throws Exception {
    for (Map.Entry<String, Integer> entry : MAP.entrySet()) {

      File file = TestData.fetchResource(entry.getKey(), false);

      System.out.println("Verifying " + file + "...");

      Plugin ideaPlugin = PluginManager.getInstance().createPlugin(file);

      Assert.assertEquals((int) entry.getValue(), ideaPlugin.getDependencies().size());

    }

  }

}
