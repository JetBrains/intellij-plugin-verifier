package mock.plugin.experimental;

import experimental.ExperimentalApiClass;
import experimental.ExperimentalApiField;
import experimental.ExperimentalApiMethod;

public class ExperimentalApiUser {
  /*expected(EXPERIMENTAL)
  Experimental API field experimental.ExperimentalApiField.x access

  Experimental API field experimental.ExperimentalApiField.x : int is accessed in mock.plugin.experimental.ExperimentalApiUser.field(ExperimentalApiField) : void. This field can be changed in a future release leading to incompatibilities
  */
  public void field(ExperimentalApiField field) {
    int x = field.x;
  }

  /*expected(EXPERIMENTAL)
  Experimental API method experimental.ExperimentalApiMethod.foo(int) invocation

  Experimental API method experimental.ExperimentalApiMethod.foo(int x) : void is invoked in mock.plugin.experimental.ExperimentalApiUser.method(ExperimentalApiMethod) : void. This method can be changed in a future release leading to incompatibilities
  */
  public void method(ExperimentalApiMethod method) {
    method.foo(0);
  }

  /*expected(EXPERIMENTAL)
  Experimental API class experimental.ExperimentalApiClass reference

  Experimental API class experimental.ExperimentalApiClass is referenced in mock.plugin.experimental.ExperimentalApiUser.clazz() : void. This class can be changed in a future release leading to incompatibilities
  */
  public void clazz() {
    new ExperimentalApiClass();
  }

  /*expected(EXPERIMENTAL)
  Experimental API class experimental.ExperimentalApiClass reference

  Experimental API class experimental.ExperimentalApiClass is referenced in mock.plugin.experimental.ExperimentalApiUser.staticFunOfDeprecatedClass() : void. This class can be changed in a future release leading to incompatibilities
  */
  public void staticFunOfDeprecatedClass() {
    ExperimentalApiClass.staticFun();
  }


  /*expected(EXPERIMENTAL)
  Experimental API class experimental.ExperimentalApiClass reference

  Experimental API class experimental.ExperimentalApiClass is referenced in mock.plugin.experimental.ExperimentalApiUser.staticFieldOfDeprecatedClass() : void. This class can be changed in a future release leading to incompatibilities
  */
  public void staticFieldOfDeprecatedClass() {
    ExperimentalApiClass.staticField = "";
  }


}
