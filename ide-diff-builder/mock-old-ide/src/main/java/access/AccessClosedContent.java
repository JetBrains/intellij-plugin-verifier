package access;

public class AccessClosedContent {
  public int publicFieldBecamePrivate;

  protected int protectedFieldBecamePrivate;

  public void publicMethodBecamePrivate() {
  }

  protected void protectedMethodBecamePrivate() {
  }

  public static class PublicNestedBecamePrivate {
  }

  protected static class ProtectedNestedBecamePrivate {
  }

  public class PublicInnerBecamePrivate {
  }

  protected class ProtectedInnerBecamePrivate {
  }
}
