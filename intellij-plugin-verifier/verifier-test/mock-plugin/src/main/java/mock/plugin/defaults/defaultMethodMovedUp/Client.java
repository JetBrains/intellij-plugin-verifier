package mock.plugin.defaults.defaultMethodMovedUp;

public class Client {
  public static void main(String[] args) {
    //Must not report AbstractMethodError because foo() was moved upward in hierarchy to the BaseInterface.
    new ClientImplementor().foo();
  }
}
