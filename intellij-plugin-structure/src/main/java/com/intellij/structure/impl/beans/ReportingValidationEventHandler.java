package com.intellij.structure.impl.beans;

import com.intellij.structure.impl.utils.validators.Validator;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

public class ReportingValidationEventHandler implements ValidationEventHandler {
  private final Validator myValidator;
  private final String myFileName;

  public ReportingValidationEventHandler(Validator validator, String fileName){
    myValidator = validator;
    myFileName = fileName;
  }

  @Override
  public boolean handleEvent(ValidationEvent event) {
    if (event.getMessage().contains("unexpected element")) return true;
    myValidator.onIncorrectStructure("Failed to parse plugin.xml " + myFileName + ": " + event.getMessage());
    return true;
  }
}
