package mock.plugin.inheritance;

import inheritance.MultipleDefaultMethod1;
import inheritance.MultipleDefaultMethod2;

public class MultipleMethods implements MultipleDefaultMethod1, MultipleDefaultMethod2 {

  public void invokeInterface(CommonInterface c) {
    c.foo();
  }

}
