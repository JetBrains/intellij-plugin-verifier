package mock.plugin.non.existing;

import non.existing.Child;

public class InvokeRemovedMethod {
  public void foo() {
    Child child = new Child();
    child.removedMethod();
  }
}
