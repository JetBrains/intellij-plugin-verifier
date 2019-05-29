package mock.plugin.deprecated;

import deprecated.ScheduledForRemovalClass;
import deprecated.ScheduledForRemovalField;
import deprecated.ScheduledForRemovalMethod;

public class ScheduledForRemovalUser {
  /*expected(DEPRECATED)
  Deprecated class deprecated.ScheduledForRemovalClass reference

  Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalUser.clazz() : void. This class will be removed in 2018.1
  */
  public void clazz() {
    new ScheduledForRemovalClass();
  }

  /*expected(DEPRECATED)
  Deprecated constructor deprecated.ScheduledForRemovalMethod.<init>() invocation

  Deprecated constructor deprecated.ScheduledForRemovalMethod.<init>() is invoked in mock.plugin.deprecated.ScheduledForRemovalUser.method() : void. This constructor will be removed in 2018.1
  */

  /*expected(DEPRECATED)
  Deprecated method deprecated.ScheduledForRemovalMethod.foo(int) invocation

  Deprecated method deprecated.ScheduledForRemovalMethod.foo(int x) : void is invoked in mock.plugin.deprecated.ScheduledForRemovalUser.method() : void. This method will be removed in 2018.1
  */

  /*expected(DEPRECATED)
  Deprecated class deprecated.ScheduledForRemovalClass reference

  Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalUser.method() : void. This class will be removed in 2018.1
  */
  public void method() {
    //Usage of the scheduled for removal method directly.
    ScheduledForRemovalMethod method = new ScheduledForRemovalMethod();
    method.foo(1);

    //Usage of the method of the deprecated class.
    ScheduledForRemovalClass deprecatedClass = new ScheduledForRemovalClass();
    deprecatedClass.foo();
  }

  /*expected(DEPRECATED)
  Deprecated field deprecated.ScheduledForRemovalField.x access

  Deprecated field deprecated.ScheduledForRemovalField.x : int is accessed in mock.plugin.deprecated.ScheduledForRemovalUser.field() : void. This field will be removed in 2018.1
  */

  /*expected(DEPRECATED)
  Deprecated class deprecated.ScheduledForRemovalClass reference

  Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalUser.field() : void. This class will be removed in 2018.1
  */
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

  /*expected(DEPRECATED)
    Deprecated class deprecated.ScheduledForRemovalClass reference

    Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalUser.staticFunOfDeprecatedClass() : void. This class will be removed in 2018.1
  */

  public void staticFunOfDeprecatedClass() {
    ScheduledForRemovalClass.staticFun();
  }

  /*expected(DEPRECATED)
    Deprecated class deprecated.ScheduledForRemovalClass reference

    Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalUser.staticFieldOfDeprecatedClass() : void. This class will be removed in 2018.1
  */
  public void staticFieldOfDeprecatedClass() {
    ScheduledForRemovalClass.staticField = "";
  }

}
