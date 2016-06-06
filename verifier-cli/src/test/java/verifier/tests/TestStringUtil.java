package verifier.tests;

import com.jetbrains.pluginverifier.utils.teamcity.TeamCityUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Sergey Patrikeev
 */
public class TestStringUtil {

  @Test
  public void testConvertProblemClassName() throws Exception {
    Assert.assertEquals("broken plugin", TeamCityUtil.convertNameToPrefix("BrokenPluginProblem"));
    Assert.assertEquals("field not found", TeamCityUtil.convertNameToPrefix("FieldNotFoundProblem"));
    Assert.assertEquals("invoke interface on private method", TeamCityUtil.convertNameToPrefix("InvokeInterfaceOnPrivateMethodProblem"));
    Assert.assertEquals("overriding final method", TeamCityUtil.convertNameToPrefix("OverridingFinalMethodProblem"));
    Assert.assertEquals("missing dependency", TeamCityUtil.convertNameToPrefix("MissingDependencyProblem"));
  }
}
