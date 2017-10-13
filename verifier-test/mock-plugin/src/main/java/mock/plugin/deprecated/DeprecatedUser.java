package mock.plugin.deprecated;

import deprecated.DeprecatedClass;
import deprecated.DeprecatedField;
import deprecated.DeprecatedMethod;
import deprecated.DeprecatedWithCommentClass;

/**
 * @author Sergey Patrikeev
 */
public class DeprecatedUser {
  public void clazz() {
    new DeprecatedClass();
  }

  public void clazzWithComment() {
    new DeprecatedWithCommentClass();
  }

  public void method() {
    //Usage of the deprecated method directly.
    DeprecatedMethod method = new DeprecatedMethod();
    method.foo(1);

    //Usage of the method of the deprecated class.
    DeprecatedClass deprecatedClass = new DeprecatedClass();
    deprecatedClass.foo();
  }

  public void field() {
    //Usage of the deprecated field directly.
    DeprecatedField deprecatedField = new DeprecatedField();
    int x = deprecatedField.x;

    //Usage of the field of the deprecated class.
    DeprecatedClass deprecatedClass = new DeprecatedClass();
    int x1 = deprecatedClass.x;
  }
}
