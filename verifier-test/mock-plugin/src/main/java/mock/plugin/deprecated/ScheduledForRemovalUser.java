package mock.plugin.deprecated;

import deprecated.ScheduledForRemovalClass;
import deprecated.ScheduledForRemovalField;
import deprecated.ScheduledForRemovalMethod;

public class ScheduledForRemovalUser {
  public void clazz() {
    new ScheduledForRemovalClass();
  }

  public void method() {
    //Usage of the scheduled for removal method directly.
    ScheduledForRemovalMethod method = new ScheduledForRemovalMethod();
    method.foo(1);

    //Usage of the method of the deprecated class.
    ScheduledForRemovalClass deprecatedClass = new ScheduledForRemovalClass();
    deprecatedClass.foo();
  }

  public void field() {
    //Usage of the deprecated field directly.
    ScheduledForRemovalField deprecatedField = new ScheduledForRemovalField();
    int x = deprecatedField.x;

    //Usage of the field of the deprecated class.
    ScheduledForRemovalClass deprecatedClass = new ScheduledForRemovalClass();
    int x1 = deprecatedClass.x;
  }

  /**
   * Usage of deprecated {@link ScheduledForRemovalClass} must not be reported here,
   * because `x` is accessed through inheritor.
   */
  public void fieldOfInheritor(ScheduledForRemovalClassInheritor inheritor) {
    int x = inheritor.x;
  }

  /**
   * Usage of deprecated {@link ScheduledForRemovalClass} must not be reported here,
   * because `foo` is accessed through inheritor.
   */
  public void methodOfInheritor(ScheduledForRemovalClassInheritor inheritor) {
    inheritor.foo();
  }

  /**
   * Verifier should report {@link ScheduledForRemovalClass} usage
   */
  public void staticFunOfDeprecatedClass() {
    ScheduledForRemovalClass.staticFun();
  }

  /**
   * Verifier should report {@link ScheduledForRemovalClass} usage
   */
  public void staticFieldOfDeprecatedClass() {
    ScheduledForRemovalClass.staticField = "";
  }

}
