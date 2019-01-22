package access;

public class AccessClosedContent {
  private int publicFieldBecamePrivate;

  private int protectedFieldBecamePrivate;

  private void publicMethodBecamePrivate() {
  }

  private void protectedMethodBecamePrivate() {
  }

  private static class PublicNestedBecamePrivate {
  }

  private static class ProtectedNestedBecamePrivate {
  }

  private class PublicInnerBecamePrivate {
  }

  private class ProtectedInnerBecamePrivate {
  }
}
