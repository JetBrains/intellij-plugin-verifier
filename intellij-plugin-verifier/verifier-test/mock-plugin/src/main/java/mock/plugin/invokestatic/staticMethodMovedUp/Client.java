package mock.plugin.invokestatic.staticMethodMovedUp;

import invokestatic.staticMethodMovedUp.Derived;

public class Client {
  public void client() {
    Derived.movedUp();
  }
}
