package mock.plugin.deprecated;

import deprecated.DeprecatedInterface;
import deprecated.DeprecatedMethod;

/**
 * @author Sergey Patrikeev
 */
public class OverrideDeprecatedMethod extends DeprecatedMethod implements DeprecatedInterface {
  @Override
  public void foo(int x) {
  }

  @Override
  public void bar() {

  }
}
