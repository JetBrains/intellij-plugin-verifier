package mock.plugin.noproblems.private_inner;

/**
 * @author Sergey Patrikeev
 */
public class SomeClass {

  private int x;

  private void foo() {

  }

  protected void protFoo() {

  }

  public void bar() {
    Runnable runnable = new Runnable() {

      @Override
      public void run() {
        foo();

      }
    };
  }

  class InstanceSubClass {
    public void bar() {
      int x = SomeClass.this.x;

      protFoo();
    }
  }



}
