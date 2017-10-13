package mock.plugin.deprecated;

import deprecated.DeprecatedMethod;

/**
 * @author Sergey Patrikeev
 */
public class OverrideDeprecatedMethod extends DeprecatedMethod {
  @Override
  public void foo(int x) {
    super.foo(x);
  }
}
