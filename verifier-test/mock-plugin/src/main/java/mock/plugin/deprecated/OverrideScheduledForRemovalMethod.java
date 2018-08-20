package mock.plugin.deprecated;

import deprecated.ScheduledForRemovalInterface;
import deprecated.ScheduledForRemovalMethod;

public class OverrideScheduledForRemovalMethod extends ScheduledForRemovalMethod implements ScheduledForRemovalInterface {
  @Override
  public void foo(int x) {
  }

  @Override
  public void bar() {

  }
}
