package featureExtractor;

public class ConstantHolder {

  public static final String STATIC_CONSTANT = "I_am_constant";

  private static final ConstantHolder INSTANCE = new ConstantHolder();

  public String myFunction() {
    return ".constantValue";
  }

  public String myRefFunction() {
    return myFunction();
  }

  public String staticConstant() {
    return STATIC_CONSTANT;
  }

  public String concat() {
    return myFunction() + "Concat";
  }

  public String concat2() {
    return "prefix" + myFunction() + myRefFunction();
  }

  public String instance() {
    return INSTANCE.myRefFunction();
  }

}