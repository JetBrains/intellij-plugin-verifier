package mock.plugin.invokeClassMethodOnInterface;

import misc.BecomeInterface;

public class Caller {
  public void call(BecomeInterface b) {
    b.invokeVirtualMethod();
  }
}
