package mock.plugin.non.existing;

public class AccessRemovedField {
  public void foo() {
    int removedField = new InheritField().removedField;
  }
}
