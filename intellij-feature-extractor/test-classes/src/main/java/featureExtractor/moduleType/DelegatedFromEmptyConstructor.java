package featureExtractor.moduleType;

import com.intellij.openapi.module.ModuleType;

/**
 * @author Sergey Patrikeev
 */
public class DelegatedFromEmptyConstructor extends ModuleType {

  public DelegatedFromEmptyConstructor() {
    this("MODULE_ID");
  }

  private DelegatedFromEmptyConstructor(String id) {
    super(id);
  }
}
