package statics;

public interface MethodBecameStatic {

  static void becomeStatic() {

  }

  //  will be "private" at runtime.
  void privateInterfaceMethodTestName();

}
