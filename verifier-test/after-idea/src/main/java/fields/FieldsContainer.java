package fields;

public class FieldsContainer {

  public final static int staticFinalField = 10;
  public static int staticField;
  public final int finalField;
  public int instanceField;
  private int privateField;

  public FieldsContainer(int finalField) {
    this.finalField = finalField;
  }

  public FieldsContainer() {
    finalField = 1;
  }
}
