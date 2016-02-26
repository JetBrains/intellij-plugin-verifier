package mock.plugin.noproblems.private_inner;

/**
 * @author Sergey Patrikeev
 */
public class SomeClass {

  private void foo() {

  }

  public void bar() {
    Runnable runnable = new Runnable() {

      @Override
      public void run() {
        foo();

      }
    };
  }

}
