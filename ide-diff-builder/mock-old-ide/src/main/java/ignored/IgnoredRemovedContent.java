package ignored;

public class IgnoredRemovedContent {

  static {
    //static blocks are ignored.
  }

  int packagePrivateField;
  private int privateField;

  private static void privateStaticMethod() {
  }

  static void packagePrivateStaticMethod() {
  }

  private void privateMethod() {
  }

  void packagePrivateMethod() {
  }

}
