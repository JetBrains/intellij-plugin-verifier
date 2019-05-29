package mock.plugin.constructors;

import constructors.DeletedDefaultConstructor;

/*expected(PROBLEM)
Invocation of unresolved constructor constructors.DeletedDefaultConstructor.<init>()

Constructor mock.plugin.constructors.MissingDefaultConstructor.<init>() contains an *invokespecial* instruction referencing an unresolved constructor constructors.DeletedDefaultConstructor.<init>(). This can lead to **NoSuchMethodError** exception at runtime.
*/
public class MissingDefaultConstructor extends DeletedDefaultConstructor {

  /*
  Here it is:

  public <init>()V
  L0
   LINENUMBER 5 L0
   ALOAD 0
   INVOKESPECIAL constructors/DeletedDefaultConstructor.<init> ()V
   RETURN
  L1
   LOCALVARIABLE this Lmock/plugin/constructors/MissingDefaultConstructor; L0 L1 0
   MAXSTACK = 1
   MAXLOCALS = 1
  */
}
