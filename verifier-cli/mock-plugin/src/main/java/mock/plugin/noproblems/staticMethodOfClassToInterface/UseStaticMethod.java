package mock.plugin.noproblems.staticMethodOfClassToInterface;

import statics.BecomeInterface;

public class UseStaticMethod {
  public void bar() {
    BecomeInterface.foo();
  }
}
