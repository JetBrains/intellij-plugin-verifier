package mock.plugin.internal;

import internal.InternalApiClass;
import internal.InternalApiField;
import internal.InternalApiMethod;

public class InternalApiUser {
  /*expected(INTERNAL)
  Internal field 'internal.InternalApiField.x' access

  Internal field 'internal.InternalApiField.x : int' is accessed in 'mock.plugin.internal.InternalApiUser.field(InternalApiField) : void'. This field is marked with '@org.jetbrains.annotations.ApiStatus.Internal' annotation and indicates that the field is not supposed to be used in client code.
  */
  public void field(InternalApiField field) {
    int x = field.x;
  }

  /*expected(INTERNAL)
  Internal method 'internal.InternalApiMethod.foo(int)' invocation

  Internal method 'internal.InternalApiMethod.foo(int x) : void' is invoked in 'mock.plugin.internal.InternalApiUser.method(InternalApiMethod) : void'. This method is marked with '@org.jetbrains.annotations.ApiStatus.Internal' annotation and indicates that the method is not supposed to be used in client code.
  */
  public void method(InternalApiMethod method) {
    method.foo(0);
  }

  /*expected(INTERNAL)
  Internal class 'internal.InternalApiClass' reference

  Internal class 'internal.InternalApiClass' is referenced in 'mock.plugin.internal.InternalApiUser.clazz() : void'. This class is marked with '@org.jetbrains.annotations.ApiStatus.Internal' annotation and indicates that the class is not supposed to be used in client code.
  */
  public void clazz() {
    new InternalApiClass();
  }

  /*expected(INTERNAL)
  Internal class 'internal.InternalApiClass' reference

  Internal class 'internal.InternalApiClass' is referenced in 'mock.plugin.internal.InternalApiUser.staticFieldOfDeprecatedClass() : void'. This class is marked with '@org.jetbrains.annotations.ApiStatus.Internal' annotation and indicates that the class is not supposed to be used in client code.
  */
  public void staticFunOfDeprecatedClass() {
    InternalApiClass.staticFun();
  }


  /*expected(INTERNAL)
  Internal class 'internal.InternalApiClass' reference

  Internal class 'internal.InternalApiClass' is referenced in 'mock.plugin.internal.InternalApiUser.staticFunOfDeprecatedClass() : void'. This class is marked with '@org.jetbrains.annotations.ApiStatus.Internal' annotation and indicates that the class is not supposed to be used in client code.
  */
  public void staticFieldOfDeprecatedClass() {
    InternalApiClass.staticField = "";
  }


}
