package mock.plugin.noproblems.bridgeMethod;

public abstract class A implements I {

  @Override
  public abstract A m();

  public abstract static class B extends A {
    /*
    Here the following method will be generated to fulfill interface's I contract:

    public synthetic bridge m()Lmock/plugin/noproblems/bridgeMethod/I;
     L0
      LINENUMBER 8 L0
      ALOAD 0
      INVOKESPECIAL mock/plugin/noproblems/bridgeMethod/A.m ()Lmock/plugin/noproblems/bridgeMethod/A;
      ARETURN
     L1
      LOCALVARIABLE this Lmock/plugin/noproblems/bridgeMethod/A$B; L0 L1 0
      MAXSTACK = 1
      MAXLOCALS = 1

     It has an instruction INVOKESPECIAL pointing to the abstract method A.m().
     The rules for INVOKESPECIAL in such case mandate JVM to throw an AbstractMethodException.

     But actually this method will never be used: the eventual implementor of A.B will provide both
      methods A.m() : A and A.m() : I
     */
  }

}