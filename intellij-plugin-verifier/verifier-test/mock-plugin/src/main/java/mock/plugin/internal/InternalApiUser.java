package mock.plugin.internal;

import idea.kotlin.internal.InternalFileKt;
import idea.kotlin.internal.NonInternalClass;
import internal.intellijInternalApi.IntellijInternalApiClass;
import internal.intellijInternalApi.IntellijInternalApiMethod;
import internal.internalApi.InternalApiClass;
import internal.internalApi.InternalApiField;
import internal.internalApi.InternalApiMethod;
import internal.internalPackage.ClassFromInternalPackage;

public class InternalApiUser {
  /*expected(INTERNAL)
  Internal field internal.internalApi.InternalApiField.x access

  Internal field internal.internalApi.InternalApiField.x : int is accessed in mock.plugin.internal.InternalApiUser.field(InternalApiField) : void. This field is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the field is not supposed to be used in client code.
  */
  public void field(InternalApiField field) {
    int x = field.x;
  }

  /*expected(INTERNAL)
  Internal method internal.internalApi.InternalApiMethod.foo(int) invocation

  Internal method internal.internalApi.InternalApiMethod.foo(int x) : void is invoked in mock.plugin.internal.InternalApiUser.method(InternalApiMethod) : void. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.
  */
  public void method(InternalApiMethod method) {
    method.foo(0);
  }

  /*expected(INTERNAL)
  Internal class internal.internalApi.InternalApiClass reference

  Internal class internal.internalApi.InternalApiClass is referenced in mock.plugin.internal.InternalApiUser.clazz() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal constructor internal.internalApi.InternalApiClass.<init>() invocation

  Internal constructor internal.internalApi.InternalApiClass.<init>() is invoked in mock.plugin.internal.InternalApiUser.clazz() : void. This constructor is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.
  */
  public void clazz() {
    new InternalApiClass();
  }

  /*expected(INTERNAL)
  Internal class internal.internalPackage.ClassFromInternalPackage reference

  Internal class internal.internalPackage.ClassFromInternalPackage is referenced in mock.plugin.internal.InternalApiUser.clazzFromInternalPackage() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal method internal.internalPackage.ClassFromInternalPackage.method() invocation

  Internal method internal.internalPackage.ClassFromInternalPackage.method() : void is invoked in mock.plugin.internal.InternalApiUser.clazzFromInternalPackage() : void. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal constructor internal.internalPackage.ClassFromInternalPackage.<init>() invocation

  Internal constructor internal.internalPackage.ClassFromInternalPackage.<init>() is invoked in mock.plugin.internal.InternalApiUser.clazzFromInternalPackage() : void. This constructor is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.
  */
  public void clazzFromInternalPackage() {
    ClassFromInternalPackage aPackage = new ClassFromInternalPackage();

    aPackage.method();
  }

  /*expected(INTERNAL)
  Internal class internal.internalApi.InternalApiClass reference

  Internal class internal.internalApi.InternalApiClass is referenced in mock.plugin.internal.InternalApiUser.staticFunOfInternalClass() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal method internal.internalApi.InternalApiClass.staticFun() invocation

  Internal method internal.internalApi.InternalApiClass.staticFun() : void is invoked in mock.plugin.internal.InternalApiUser.staticFunOfInternalClass() : void. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.
  */
  public void staticFunOfInternalClass() {
    InternalApiClass.staticFun();
  }


  /*expected(INTERNAL)
  Internal class internal.internalApi.InternalApiClass reference

  Internal class internal.internalApi.InternalApiClass is referenced in mock.plugin.internal.InternalApiUser.staticFieldOfInternalClass() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal field internal.internalApi.InternalApiClass.staticField access

  Internal field internal.internalApi.InternalApiClass.staticField : java.lang.String is accessed in mock.plugin.internal.InternalApiUser.staticFieldOfInternalClass() : void. This field is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the field is not supposed to be used in client code.
  */
  public void staticFieldOfInternalClass() {
    InternalApiClass.staticField = "";
  }

  public void nonInternalKotlinClassUser() {
    // This is not an effectively @Internal class because the @Internal annotation is applied to @file: rather than to a package,
    // and Kotlin applies this annotation only to top-leve declarations.
    new NonInternalClass();
  }

  /*expected(INTERNAL)
  Internal class idea.kotlin.internal.InternalFileKt reference

  Internal class idea.kotlin.internal.InternalFileKt is referenced in mock.plugin.internal.InternalApiUser.internalKotlinTopLevelDeclarations() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal method idea.kotlin.internal.InternalFileKt.internalTopLevelFunction() invocation

  Internal method idea.kotlin.internal.InternalFileKt.internalTopLevelFunction() : void is invoked in mock.plugin.internal.InternalApiUser.internalKotlinTopLevelDeclarations() : void. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.
  */
  public void internalKotlinTopLevelDeclarations() {
    InternalFileKt.internalTopLevelFunction();
  }

  /*expected(INTERNAL)
  Internal method internal.intellijInternalApi.IntellijInternalApiMethod.foo(int) invocation

  Internal method internal.intellijInternalApi.IntellijInternalApiMethod.foo(int x) : void is invoked in mock.plugin.internal.InternalApiUser.intellijInternalMethod(IntellijInternalApiMethod) : void. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.
  */
  public void intellijInternalMethod(IntellijInternalApiMethod method) {
    method.foo(0);
  }

  /*expected(INTERNAL)
  Internal class internal.intellijInternalApi.IntellijInternalApiClass reference

  Internal class internal.intellijInternalApi.IntellijInternalApiClass is referenced in mock.plugin.internal.InternalApiUser.intellijInternalClass() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal constructor internal.intellijInternalApi.IntellijInternalApiClass.<init>() invocation

  Internal constructor internal.intellijInternalApi.IntellijInternalApiClass.<init>() is invoked in mock.plugin.internal.InternalApiUser.intellijInternalClass() : void. This constructor is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.
  */
  public void intellijInternalClass() {
    new IntellijInternalApiClass();
  }

  /*expected(INTERNAL)
  Internal method internal.intellijInternalApi.IntellijInternalApiClass.Companion.staticFun() invocation

  Internal method internal.intellijInternalApi.IntellijInternalApiClass.Companion.staticFun() : void is invoked in mock.plugin.internal.InternalApiUser.staticFunOfIntellijInternalClass() : void. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal field internal.intellijInternalApi.IntellijInternalApiClass.Companion access

  Internal field internal.intellijInternalApi.IntellijInternalApiClass.Companion : internal.intellijInternalApi.IntellijInternalApiClass.Companion is accessed in mock.plugin.internal.InternalApiUser.staticFunOfIntellijInternalClass() : void. This field is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the field is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal class internal.intellijInternalApi.IntellijInternalApiClass.Companion reference

  Internal class internal.intellijInternalApi.IntellijInternalApiClass.Companion is referenced in mock.plugin.internal.InternalApiUser.staticFunOfIntellijInternalClass() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal class internal.intellijInternalApi.IntellijInternalApiClass reference

  Internal class internal.intellijInternalApi.IntellijInternalApiClass is referenced in mock.plugin.internal.InternalApiUser.staticFunOfIntellijInternalClass() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.
  */
  public void staticFunOfIntellijInternalClass() {
    IntellijInternalApiClass.Companion.staticFun();
  }

  /*expected(INTERNAL)
  Internal class internal.intellijInternalApi.IntellijInternalApiClass.Companion reference

  Internal class internal.intellijInternalApi.IntellijInternalApiClass.Companion is referenced in mock.plugin.internal.InternalApiUser.staticFieldOfIntellijInternalClass() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal method internal.intellijInternalApi.IntellijInternalApiClass.Companion.setStaticField(String) invocation

  Internal method internal.intellijInternalApi.IntellijInternalApiClass.Companion.setStaticField(java.lang.String <set-?>) : void is invoked in mock.plugin.internal.InternalApiUser.staticFieldOfIntellijInternalClass() : void. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal field internal.intellijInternalApi.IntellijInternalApiClass.Companion access

  Internal field internal.intellijInternalApi.IntellijInternalApiClass.Companion : internal.intellijInternalApi.IntellijInternalApiClass.Companion is accessed in mock.plugin.internal.InternalApiUser.staticFieldOfIntellijInternalClass() : void. This field is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the field is not supposed to be used in client code.
  */

  /*expected(INTERNAL)
  Internal class internal.intellijInternalApi.IntellijInternalApiClass reference

  Internal class internal.intellijInternalApi.IntellijInternalApiClass is referenced in mock.plugin.internal.InternalApiUser.staticFieldOfIntellijInternalClass() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.
  */
  public void staticFieldOfIntellijInternalClass() {
    IntellijInternalApiClass.Companion.setStaticField("");
  }
}
