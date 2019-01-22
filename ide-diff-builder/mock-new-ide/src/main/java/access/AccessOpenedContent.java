package access;

public class AccessOpenedContent {
  public int privateFieldBecamePublic;

  protected int privateFieldBecameProtected;

  public void privateMethodBecamePublic() {
  }

  protected void privateMethodBecameProtected() {
  }

  public static class PrivateNestedBecamePublic {
  }

  protected static class PrivateNestedBecameProtected {
  }

  public class PrivateInnerBecamePublic {
  }

  protected class PrivateInnerBecameProtected {
  }
}
