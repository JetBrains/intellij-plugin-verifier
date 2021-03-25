package mock.plugin.internal;

import idea.kotlin.internal.InternalFileKt;
import idea.kotlin.internal.NonInternalClass;
import internal.InternalApiClass;
import internal.InternalApiField;
import internal.InternalApiMethod;
import internal.internalPackage.ClassFromInternalPackage;

public class InternalApiUser {
  /*expected(INTERNAL)
  Internal field internal.InternalApiField.x access

  Internal field internal.InternalApiField.x : int is accessed in mock.plugin.internal.InternalApiUser.field(InternalApiField) : void. This field is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the field is not supposed to be used in client code.
  */
  public void field(InternalApiField field) {
    int x = field.x;
  }

  /*expected(INTERNAL)
  Internal method internal.InternalApiMethod.foo(int) invocation

  Internal method internal.InternalApiMethod.foo(int x) : void is invoked in mock.plugin.internal.InternalApiUser.method(InternalApiMethod) : void. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the method is not supposed to be used in client code.
  */
  public void method(InternalApiMethod method) {
    method.foo(0);
  }

  /*expected(INTERNAL)
  Internal class internal.InternalApiClass reference

  Internal class internal.InternalApiClass is referenced in mock.plugin.internal.InternalApiUser.clazz() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal constructor internal.InternalApiClass.<init>() invocation

  Internal constructor internal.InternalApiClass.<init>() is invoked in mock.plugin.internal.InternalApiUser.clazz() : void. This constructor is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the method is not supposed to be used in client code.
  */
  public void clazz() {
    new InternalApiClass();
  }

  /*expected(INTERNAL)
  Internal class internal.internalPackage.ClassFromInternalPackage reference

  Internal class internal.internalPackage.ClassFromInternalPackage is referenced in mock.plugin.internal.InternalApiUser.clazzFromInternalPackage() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal method internal.internalPackage.ClassFromInternalPackage.method() invocation

  Internal method internal.internalPackage.ClassFromInternalPackage.method() : void is invoked in mock.plugin.internal.InternalApiUser.clazzFromInternalPackage() : void. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the method is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal constructor internal.internalPackage.ClassFromInternalPackage.<init>() invocation

  Internal constructor internal.internalPackage.ClassFromInternalPackage.<init>() is invoked in mock.plugin.internal.InternalApiUser.clazzFromInternalPackage() : void. This constructor is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the method is not supposed to be used in client code.
  */
  public void clazzFromInternalPackage() {
    ClassFromInternalPackage aPackage = new ClassFromInternalPackage();

    aPackage.method();
  }

  /*expected(INTERNAL)
  Internal class internal.InternalApiClass reference

  Internal class internal.InternalApiClass is referenced in mock.plugin.internal.InternalApiUser.staticFieldOfDeprecatedClass() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal method internal.InternalApiClass.staticFun() invocation

  Internal method internal.InternalApiClass.staticFun() : void is invoked in mock.plugin.internal.InternalApiUser.staticFunOfDeprecatedClass() : void. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the method is not supposed to be used in client code.
  */
  public void staticFunOfDeprecatedClass() {
    InternalApiClass.staticFun();
  }


  /*expected(INTERNAL)
  Internal class internal.InternalApiClass reference

  Internal class internal.InternalApiClass is referenced in mock.plugin.internal.InternalApiUser.staticFunOfDeprecatedClass() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal field internal.InternalApiClass.staticField access

  Internal field internal.InternalApiClass.staticField : java.lang.String is accessed in mock.plugin.internal.InternalApiUser.staticFieldOfDeprecatedClass() : void. This field is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the field is not supposed to be used in client code.
  */
  public void staticFieldOfDeprecatedClass() {
    InternalApiClass.staticField = "";
  }

  public void nonInternalKotlinClassUser() {
    // This is not an effectively @Internal class because the @Internal annotation is applied to @file: rather than to a package,
    // and Kotlin applies this annotation only to top-leve declarations.
    new NonInternalClass();
  }

  /*expected(INTERNAL)
  Internal class idea.kotlin.internal.InternalFileKt reference

  Internal class idea.kotlin.internal.InternalFileKt is referenced in mock.plugin.internal.InternalApiUser.internalKotlinTopLevelDeclarations() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal method idea.kotlin.internal.InternalFileKt.internalTopLevelFunction() invocation

  Internal method idea.kotlin.internal.InternalFileKt.internalTopLevelFunction() : void is invoked in mock.plugin.internal.InternalApiUser.internalKotlinTopLevelDeclarations() : void. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the method is not supposed to be used in client code.
  */
  public void internalKotlinTopLevelDeclarations() {
    InternalFileKt.internalTopLevelFunction();
  }

}
