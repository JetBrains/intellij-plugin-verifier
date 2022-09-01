package mock.plugin.internal.otherPackage;

import internal.otherPackage.NonInternalOverriddenOtherPackage;

public class NoWarningsOtherPackage extends NonInternalOverriddenOtherPackage {
  // No warning here because the NonInternalOverridden.someMethod() is not internal itself.
  @Override
  public void someMethod() {
    super.someMethod();
  }

}

