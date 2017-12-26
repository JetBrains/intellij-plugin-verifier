package inheritance;

public abstract class AImpl implements A {

  private AImpl() {

  }

  public static AImpl createAImpl() {
    return new AImpl() {
      @Override
      public void foo() {

      }
    };
  }

}
