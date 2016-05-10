package mock.plugin;

import com.intellij.execution.filters.Filter;

/**
 * @author Sergey Patrikeev
 */
public class FilterImpl implements Filter {
  public FilterImpl() {
  }


  @Override
  public Result applyFilter(String line, int entireLength) {
    return null;
  }
}
