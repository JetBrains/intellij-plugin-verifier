package mock.plugin.non.existing;

import interfaces.SomeInterface;
import interfaces.SomeInterface2;
import invokevirtual.Parent;

public class InheritMethod extends Parent implements SomeInterface, SomeInterface2 {
  //inherits removedMethod()
  //inherits removedStaticMethod()
  @Override
  public void someFun() {

  }

  @Override
  public void someFun2() {

  }
}
