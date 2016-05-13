package mock.plugin.field;

import fields.FieldsContainer;
import fields.otherPackage.OtherFieldsContainer;
import non.existing.NonExistingClass;

/**
 * @author Sergey Patrikeev
 */
public class FieldProblemsContainer {

  public void accessDeletedField() {
    int deletedField = new FieldsContainer().deletedField;
  }

  public void accessPrivateField() {
    int privateField = new FieldsContainer().privateField;
  }

  public void accessProtectedField() {
    new OtherFieldsContainer().protectedField = 10;
  }

  public void accessPackageField() {
    new OtherFieldsContainer().packageField = 10;
  }

  public void instanceAccessOnStatic() {
    int instanceField = new FieldsContainer().staticField;
  }

  public void staticAccessOnInstance() {
    int instanceField = FieldsContainer.instanceField;
  }

  public void accessUnknownClass() {
    int nonExistingField = NonExistingClass.nonExistingField;
  }

  public void accessUnknownClassOfArray() {
//    int size = new NonExistingClass[2].length; //TODO
  }

  public void setOnFinalFieldFromNotInitMethod() {
    new FieldsContainer().finalField = 10;
  }

  public void setOnStaticFinalFieldFromNotClinitMethod() {
    FieldsContainer.staticFinalField = 10;
  }
}
