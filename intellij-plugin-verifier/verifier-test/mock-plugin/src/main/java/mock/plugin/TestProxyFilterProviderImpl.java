package mock.plugin;

import com.intellij.execution.testframework.sm.runner.TestProxyFilterProvider;

public class TestProxyFilterProviderImpl implements TestProxyFilterProvider {

  //FilterImpl is a constriction of Filter interface
  //but bridge method is generated => no problems should be found here

  @Override
  public FilterImpl getFilter(String nodeType, String nodeName, String nodeArguments) {
    return null;
  }
}
