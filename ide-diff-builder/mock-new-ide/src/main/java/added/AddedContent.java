package added;

public class AddedContent extends AddedOverrideHelperBase<Number> {

  public int publicField;
  protected int protectedField;

  public AddedContent() {
  }

  public AddedContent(int x) {
  }

  public static void addedPublicStaticMethod() {
  }

  protected static void addedProtectedStaticMethod() {
  }

  public void addedPublicMethod() {
  }

  protected void addedProtectedMethod() {
  }

  //This overriding method must be registered because it has generified parameter
  //and in byte-code it has signature "name(Number)" rather than overridden method's "name(Object)".
  @Override
  public void methodWithGenericParameter(Number number) {
  }

  //This overriding method must be registered because it has generified return type
  //and in byte-code it has signature "name(): Number" rather than overridden method's "name(): Object".
  @Override
  public Number methodWithGenericReturnType() {
    return null;
  }

  public static class PublicNested {
  }

  public static class ProtectedNested {
  }

  public class PublicInner {
    //Added with Inner => not registered directly.
    public class InnerInner {
    }
  }

  protected class ProtectedInner {
    //Added with Inner => not registered directly.
    public class InnerInner {
    }
  }

}
