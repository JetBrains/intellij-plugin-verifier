package removed;

public class RemovedContent {

  public int publicField;

  protected int protectedField;

  public void publicMethod() {
  }

  public void publicStaticMethod() {
  }

  public static class PublicNested {
  }

  protected static class ProtectedNested {
  }

  public class PublicInner {
    //Removed with PublicInner => not registered directly.
    public class InnerInner {
    }
  }

  protected class ProtectedInner {
  }

}
