package mock.plugin.deprecated;

import deprecated.ScheduledForRemovalInterface;
import deprecated.ScheduledForRemovalMethod;

/*expected(DEPRECATED)
  Deprecated constructor deprecated.ScheduledForRemovalMethod.<init>() invocation

  Deprecated constructor deprecated.ScheduledForRemovalMethod.<init>() is invoked in mock.plugin.deprecated.OverrideScheduledForRemovalMethod.<init>(). This constructor will be removed in 2018.1
*/

/*expected(DEPRECATED)
 Deprecated interface deprecated.ScheduledForRemovalInterface reference

 Deprecated interface deprecated.ScheduledForRemovalInterface is referenced in mock.plugin.deprecated.OverrideScheduledForRemovalMethod. This interface will be removed in 2018.1
*/

public class OverrideScheduledForRemovalMethod extends ScheduledForRemovalMethod implements ScheduledForRemovalInterface {

  /*expected(DEPRECATED)
  Deprecated method deprecated.ScheduledForRemovalMethod.foo(int) is overridden

  Deprecated method deprecated.ScheduledForRemovalMethod.foo(int x) : void is overridden in class mock.plugin.deprecated.OverrideScheduledForRemovalMethod. This method will be removed in 2018.1
  */
  @Override
  public void foo(int x) {
  }

  /*expected(DEPRECATED)
  Deprecated method deprecated.ScheduledForRemovalInterface.bar() is overridden

  Deprecated method deprecated.ScheduledForRemovalInterface.bar() : void is overridden in class mock.plugin.deprecated.OverrideScheduledForRemovalMethod. This method will be removed in 2018.1
  */
  @Override
  public void bar() {

  }
}
