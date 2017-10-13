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
    DeprecatedMethod method = new DeprecatedMethod();
    method.foo(1);
  }

  public void field() {
    DeprecatedField deprecatedField = new DeprecatedField();
    int x = deprecatedField.x;
  }
}
