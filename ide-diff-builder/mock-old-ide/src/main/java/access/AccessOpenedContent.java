package access;

public class AccessOpenedContent {
  private int privateFieldBecamePublic;

  private int privateFieldBecameProtected;

  private void privateMethodBecamePublic() {
  }

  private void privateMethodBecameProtected() {
  }

  private static class PrivateNestedBecamePublic {
  }

  private static class PrivateNestedBecameProtected {
  }

  private class PrivateInnerBecamePublic {
  }

  private class PrivateInnerBecameProtected {
  }
}
