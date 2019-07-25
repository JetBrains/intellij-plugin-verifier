package mock.plugin.experimental;

import experimental.ExperimentalApiClass;
import experimental.ExperimentalApiEnclosingClass;
import experimental.ExperimentalApiField;
import experimental.ExperimentalApiMethod;
import experimental.experimentalPackage.ClassFromExperimentalPackage;

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

  /*expected(EXPERIMENTAL)
  Experimental API constructor experimental.ExperimentalApiClass.<init>() invocation

  Experimental API constructor experimental.ExperimentalApiClass.<init>() is invoked in mock.plugin.experimental.ExperimentalApiUser.clazz() : void. This constructor can be changed in a future release leading to incompatibilities
  */
  public void clazz() {
    new ExperimentalApiClass();
  }


  /*expected(EXPERIMENTAL)
  Experimental API class experimental.experimentalPackage.ClassFromExperimentalPackage reference

  Experimental API class experimental.experimentalPackage.ClassFromExperimentalPackage is referenced in mock.plugin.experimental.ExperimentalApiUser.clazzFromExperimentalPackage() : void. This class can be changed in a future release leading to incompatibilities
  */

  /*expected(EXPERIMENTAL)
  Experimental API constructor experimental.experimentalPackage.ClassFromExperimentalPackage.<init>() invocation

  Experimental API constructor experimental.experimentalPackage.ClassFromExperimentalPackage.<init>() is invoked in mock.plugin.experimental.ExperimentalApiUser.clazzFromExperimentalPackage() : void. This constructor can be changed in a future release leading to incompatibilities
  */

  /*expected(EXPERIMENTAL)
  Experimental API method experimental.experimentalPackage.ClassFromExperimentalPackage.method() invocation

  Experimental API method experimental.experimentalPackage.ClassFromExperimentalPackage.method() : void is invoked in mock.plugin.experimental.ExperimentalApiUser.clazzFromExperimentalPackage() : void. This method can be changed in a future release leading to incompatibilities
  */
  public void clazzFromExperimentalPackage() {
    ClassFromExperimentalPackage aPackage = new ClassFromExperimentalPackage();

    aPackage.method();
  }

  /*expected(EXPERIMENTAL)
  Experimental API class experimental.ExperimentalApiClass reference

  Experimental API class experimental.ExperimentalApiClass is referenced in mock.plugin.experimental.ExperimentalApiUser.staticFunOfDeprecatedClass() : void. This class can be changed in a future release leading to incompatibilities
  */

  /*expected(EXPERIMENTAL)
  Experimental API method experimental.ExperimentalApiClass.staticFun() invocation

  Experimental API method experimental.ExperimentalApiClass.staticFun() : void is invoked in mock.plugin.experimental.ExperimentalApiUser.staticFunOfDeprecatedClass() : void. This method can be changed in a future release leading to incompatibilities
  */
  public void staticFunOfDeprecatedClass() {
    ExperimentalApiClass.staticFun();
  }


  /*expected(EXPERIMENTAL)
  Experimental API class experimental.ExperimentalApiClass reference

  Experimental API class experimental.ExperimentalApiClass is referenced in mock.plugin.experimental.ExperimentalApiUser.staticFieldOfDeprecatedClass() : void. This class can be changed in a future release leading to incompatibilities
  */

  /*expected(EXPERIMENTAL)
  Experimental API field experimental.ExperimentalApiClass.staticField access

  Experimental API field experimental.ExperimentalApiClass.staticField : java.lang.String is accessed in mock.plugin.experimental.ExperimentalApiUser.staticFieldOfDeprecatedClass() : void. This field can be changed in a future release leading to incompatibilities
  */
  public void staticFieldOfDeprecatedClass() {
    ExperimentalApiClass.staticField = "";
  }

  /*expected(EXPERIMENTAL)
  Experimental API constructor experimental.ExperimentalApiEnclosingClass.NestedClass.<init>() invocation

  Experimental API constructor experimental.ExperimentalApiEnclosingClass.NestedClass.<init>() is invoked in mock.plugin.experimental.ExperimentalApiUser.nestedClass() : void. This constructor can be changed in a future release leading to incompatibilities
  */

  /*expected(EXPERIMENTAL)
  Experimental API class experimental.ExperimentalApiEnclosingClass.NestedClass reference

  Experimental API class experimental.ExperimentalApiEnclosingClass.NestedClass is referenced in mock.plugin.experimental.ExperimentalApiUser.nestedClass() : void. This class can be changed in a future release leading to incompatibilities
  */
  public void nestedClass() {
    new ExperimentalApiEnclosingClass.NestedClass();
  }

}
