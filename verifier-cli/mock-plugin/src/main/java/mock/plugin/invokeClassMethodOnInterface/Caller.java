package mock.plugin.invokeClassMethodOnInterface;

import misc.BecomeClass;
import misc.BecomeInterface;

public class Caller {
  public void call(BecomeInterface b) {
    b.invokeVirtualMethod();
  }

  public void call2(BecomeClass b) {
    b.invokeInterfaceOnClass();
  }
}
