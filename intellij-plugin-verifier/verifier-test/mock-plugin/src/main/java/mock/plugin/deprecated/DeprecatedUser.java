package mock.plugin.deprecated;

import deprecated.*;

public class DeprecatedUser {
  /*expected(DEPRECATED)
  Deprecated class deprecated.DeprecatedClass reference

  Deprecated class deprecated.DeprecatedClass is referenced in mock.plugin.deprecated.DeprecatedUser.clazz() : void
  */
  public void clazz() {
    new DeprecatedClass();
  }

  /*expected(DEPRECATED)
  Deprecated class deprecated.KotlinDeprecatedClass reference

  Deprecated class deprecated.KotlinDeprecatedClass is referenced in mock.plugin.deprecated.DeprecatedUser.kotlinClazz() : void
  */
  public void kotlinClazz() {
    new KotlinDeprecatedClass();
  }

  /*expected(DEPRECATED)
  Deprecated class deprecated.DeprecatedWithCommentClass reference

  Deprecated class deprecated.DeprecatedWithCommentClass is referenced in mock.plugin.deprecated.DeprecatedUser.clazzWithComment() : void
  */
  public void clazzWithComment() {
    new DeprecatedWithCommentClass();
  }

  /*expected(DEPRECATED)
  Deprecated class deprecated.DeprecatedClass reference

  Deprecated class deprecated.DeprecatedClass is referenced in mock.plugin.deprecated.DeprecatedUser.method() : void
   */

  /*expected(DEPRECATED)
  Deprecated method deprecated.DeprecatedMethod.foo(int) invocation

  Deprecated method deprecated.DeprecatedMethod.foo(int x) : void is invoked in mock.plugin.deprecated.DeprecatedUser.method() : void
   */

  /*expected(DEPRECATED)
  Deprecated class deprecated.DeprecatedClass reference

  Deprecated class deprecated.DeprecatedClass is referenced in mock.plugin.deprecated.DeprecatedUser.method() : void
   */

  /*expected(DEPRECATED)
  Deprecated constructor deprecated.DeprecatedMethod.<init>() invocation

  Deprecated constructor deprecated.DeprecatedMethod.<init>() is invoked in mock.plugin.deprecated.DeprecatedUser.method() : void
   */
  public void method() {
    //Usage of the deprecated method directly.
    DeprecatedMethod method = new DeprecatedMethod();
    method.foo(1);

    //Usage of the method of the deprecated class.
    DeprecatedClass deprecatedClass = new DeprecatedClass();
    deprecatedClass.foo();
  }

  /*expected(DEPRECATED)
  Deprecated class deprecated.DeprecatedClass reference

  Deprecated class deprecated.DeprecatedClass is referenced in mock.plugin.deprecated.DeprecatedUser.field() : void
  */
  public void field() {
    /*expected(DEPRECATED)
    Deprecated field deprecated.DeprecatedField.x access

    Deprecated field deprecated.DeprecatedField.x : int is accessed in mock.plugin.deprecated.DeprecatedUser.field() : void
    */
    DeprecatedField deprecatedField = new DeprecatedField();
    int x = deprecatedField.x;

    /*expected(DEPRECATED)
    Deprecated class deprecated.DeprecatedClass reference

    Deprecated class deprecated.DeprecatedClass is referenced in mock.plugin.deprecated.DeprecatedUser.field() : void
    */
    //Usage of the field of the deprecated class.
    DeprecatedClass deprecatedClass = new DeprecatedClass();
    int x1 = deprecatedClass.x;
  }
}
