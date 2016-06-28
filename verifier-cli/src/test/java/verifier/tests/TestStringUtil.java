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
    Assert.assertEquals("broken plugin", TeamCityUtil.INSTANCE.convertNameToPrefix("BrokenPluginProblem"));
    Assert.assertEquals("field not found", TeamCityUtil.INSTANCE.convertNameToPrefix("FieldNotFoundProblem"));
    Assert.assertEquals("invoke interface on private method", TeamCityUtil.INSTANCE.convertNameToPrefix("InvokeInterfaceOnPrivateMethodProblem"));
    Assert.assertEquals("overriding final method", TeamCityUtil.INSTANCE.convertNameToPrefix("OverridingFinalMethodProblem"));
    Assert.assertEquals("missing dependency", TeamCityUtil.INSTANCE.convertNameToPrefix("MissingDependencyProblem"));
  }
}
