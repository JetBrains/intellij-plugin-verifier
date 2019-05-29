package mock.plugin.deprecated;

import deprecated.DeprecatedInterface;
import deprecated.DeprecatedMethod;

/*expected(DEPRECATED)
  Deprecated interface deprecated.DeprecatedInterface reference

  Deprecated interface deprecated.DeprecatedInterface is referenced in mock.plugin.deprecated.OverrideDeprecatedMethod
*/

public class OverrideDeprecatedMethod extends DeprecatedMethod implements DeprecatedInterface {
  /*expected(DEPRECATED)
  Deprecated constructor deprecated.DeprecatedMethod.<init>() invocation

  Deprecated constructor deprecated.DeprecatedMethod.<init>() is invoked in mock.plugin.deprecated.OverrideDeprecatedMethod.<init>()
  */

  /*expected(DEPRECATED)
  Deprecated method deprecated.DeprecatedMethod.foo(int) is overridden

  Deprecated method deprecated.DeprecatedMethod.foo(int x) : void is overridden in class mock.plugin.deprecated.OverrideDeprecatedMethod
  */
  @Override
  public void foo(int x) {
  }

  /*expected(DEPRECATED)
  Deprecated method deprecated.DeprecatedInterface.bar() is overridden

  Deprecated method deprecated.DeprecatedInterface.bar() : void is overridden in class mock.plugin.deprecated.OverrideDeprecatedMethod
  */
  @Override
  public void bar() {

  }
}
