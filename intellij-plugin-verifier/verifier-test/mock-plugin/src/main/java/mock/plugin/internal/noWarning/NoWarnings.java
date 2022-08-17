package mock.plugin.internal.noWarning;

import internal.hierarchy.PublicConcreteClass;
import internal.hierarchy.PublicInterface;
import internal.noWarning.NonInternalOverridden;

public class NoWarnings extends NonInternalOverridden {
  public void noWarning() {
  /*
   * No warnings should be produced here because {@link OwnInternalClass} is declared in the same module.
   */
    new OwnInternalClass();
  }

  // No warning here because the NonInternalOverridden.someMethod() is not internal itself.
  @Override
  public void someMethod() {
    super.someMethod();
  }

  public static void foo() {
    // No warning here because the someMethod() is not internal itself.
    new NonInternalOverridden().someMethod();
  }

  public static void bar() {
    // No warning here because the doWork() is not internal itself.
    new PublicConcreteClass().doWork();
  }

  public static void baz() {
    // No warning here because the doWork() is not internal itself.
    ((PublicInterface) new PublicConcreteClass()).doWork();
  }
}

