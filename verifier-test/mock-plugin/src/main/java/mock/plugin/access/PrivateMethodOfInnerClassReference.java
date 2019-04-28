package mock.plugin.access;

public class PrivateMethodOfInnerClassReference {

  private static void boo() {
    //Must not report "illegal private method invocation" because the inner class provides synthetic "access$000()V" method
    // that invokes the private method of the Inner class.
    Inner.foo();
  }

  private static class Inner {
    private static void foo() {
    }
  }
}
