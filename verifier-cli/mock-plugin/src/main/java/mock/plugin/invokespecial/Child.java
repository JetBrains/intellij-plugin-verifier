package mock.plugin.invokespecial;

import invokespecial.AbstractParent;

public abstract class Child extends AbstractParent {
  public void bar() {
    super.foo();
  }

  public void invokeSpecialOnStaticMethod() {
    super.becomeStatic();
  }
}
