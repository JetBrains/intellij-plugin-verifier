package internal.noWarning;

public class NonInternalOverridden implements InternalInterface {
  // This method overrides an internal method but it is not internal itself.
  @Override
  public void someMethod() {
  }
}
