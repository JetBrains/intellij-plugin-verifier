package mock.plugin.invokeClassMethodOnInterface;

import misc.BecomeClass;
import misc.BecomeInterface;
import statics.MethodBecameStatic;

/*expected(PROBLEM)
Attempt to execute an *invokeinterface* instruction on a private method statics.MethodBecameStatic.privateInterfaceMethodTestName() : void

Method mock.plugin.invokeClassMethodOnInterface.Caller.call4(MethodBecameStatic b) : void contains an *invokeinterface* instruction referencing a private method statics.MethodBecameStatic.privateInterfaceMethodTestName() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.
 */
public class Caller {

  /*expected(PROBLEM)
  Incompatible change of class misc.BecomeInterface to interface

  Method mock.plugin.invokeClassMethodOnInterface.Caller.call(BecomeInterface b) : void has invocation *invokevirtual* instruction referencing a *class* method misc.BecomeInterface.invokeVirtualMethod() : void, but the method's host misc.BecomeInterface is an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.
 */
  public void call(BecomeInterface b) {
    b.invokeVirtualMethod();
  }

  /*expected(PROBLEM)
  Incompatible change of interface misc.BecomeClass to class

  Method mock.plugin.invokeClassMethodOnInterface.Caller.call2(BecomeClass b) : void has invocation *invokeinterface* instruction referencing an *interface* method misc.BecomeClass.invokeInterfaceOnClass() : void, but the method's host misc.BecomeClass is a *class*. This can lead to **IncompatibleClassChangeError** at runtime.
   */
  public void call2(BecomeClass b) {
    b.invokeInterfaceOnClass();
  }


  /*expected(PROBLEM)
  Attempt to execute instance instruction *invokeinterface* on a static method statics.MethodBecameStatic.becomeStatic() : void

  Method mock.plugin.invokeClassMethodOnInterface.Caller.call3(MethodBecameStatic b) : void contains an *invokeinterface* instruction referencing a static method statics.MethodBecameStatic.becomeStatic() : void, what might have been caused by incompatible change of the method to static. This can lead to **IncompatibleClassChangeError** exception at runtime.
  */
  public void call3(MethodBecameStatic b) {
    b.becomeStatic();
  }

  public void call4(MethodBecameStatic b) {
    b.privateInterfaceMethodTestName();
  }
}
