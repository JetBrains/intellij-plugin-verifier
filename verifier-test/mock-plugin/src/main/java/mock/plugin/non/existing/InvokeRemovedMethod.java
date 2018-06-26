package mock.plugin.non.existing;

import invokevirtual.Child;

public class InvokeRemovedMethod {
  public void foo() {
    Child child = new Child();
    child.removedMethod();
  }

  public void invokeVirtual() {
    //we must report that the method could have been found in the IDE's classes non.existing.Parent or interfaces.SomeInterface or interfaces.SomeInterface2
    new InheritMethod().removedMethod();
  }

  public void invokeStatic() {
    //we must report that the method could have been found in the IDE's classes non.existing.Parent or interfaces.SomeInterface or interfaces.SomeInterface2
    new InheritMethod().removedMethod();
  }

}
