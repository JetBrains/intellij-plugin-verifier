package mock.plugin.invokeClassMethodOnInterface;

import misc.BecomeClass;
import misc.BecomeInterface;
import statics.MethodBecameStatic;

public class Caller {
  public void call(BecomeInterface b) {
    b.invokeVirtualMethod();
  }

  public void call2(BecomeClass b) {
    b.invokeInterfaceOnClass();
  }

  public void call3(MethodBecameStatic b) {
    b.becomeStatic();
  }

  public void call4(MethodBecameStatic b) {
    b.privateInterfaceMethodTestName();
  }
}
