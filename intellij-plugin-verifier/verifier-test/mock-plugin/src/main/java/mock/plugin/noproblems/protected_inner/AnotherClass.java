package mock.plugin.noproblems.protected_inner;

public class AnotherClass {
  public void f() {
    b(new Runnable() {
      @Override
      public void run() {
        //here problem shouldn't be found

        new SomeClass();
      }
    });
  }

  public void b(Runnable dummyRunnable) {

  }
}
