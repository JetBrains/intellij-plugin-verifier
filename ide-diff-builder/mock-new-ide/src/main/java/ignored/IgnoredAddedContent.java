package ignored;

//Contains contents that must be ignored (private, package private and other).
public class IgnoredAddedContent extends IgnoredOverrideHelperBase {

  static {
    //static blocks are ignored.
  }

  int packagePrivateField;
  private int privateField;

  //This constructor is not changed.
  public IgnoredAddedContent() {
  }

  IgnoredAddedContent(String x) {
  }

  private static void privateStaticMethod() {
  }

  static void packagePrivateStaticMethod() {
  }

  private void privateMethod() {
  }

  void packagePrivateMethod() {
  }

  //Overriding method must be ignored.
  @Override
  public void bar() {
    super.bar();
  }

  //Method overriding Object's methods must be ignored.
  @Override
  public String toString() {
    return super.toString();
  }
}
