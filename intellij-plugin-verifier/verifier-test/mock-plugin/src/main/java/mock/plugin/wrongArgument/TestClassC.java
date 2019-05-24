package mock.plugin.wrongArgument;

import wrongArgument.TestClassA;
import wrongArgument.TestClassB;

public class TestClassC {

  private static void _print_(TestClassA a) {
    a.print();
  }

  /*
  Here expected:

  java.lang.VerifyError: Bad type on operand stack

  Exception Details:
  Location:
    TestClassC.main([Ljava/lang/String;)V @9: invokestatic
  Reason:
    Type 'TestClassB' (current frame, stack[0]) is not assignable to 'TestClassA'
   */
  public static void main(String[] args) {
    TestClassB b = new TestClassB();
    TestClassC._print_(b);
  }

}
