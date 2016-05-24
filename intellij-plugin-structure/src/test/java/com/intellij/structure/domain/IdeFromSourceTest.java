package com.intellij.structure.domain;

import com.intellij.structure.resolvers.Resolver;
import junit.framework.AssertionFailedError;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * TODO: rewrite test. construct the directories structure manually.
 *
 * Created by Sergey Patrikeev
 */
public class IdeFromSourceTest {
  @Test
  public void tryCreate() {
    List<File> idePaths = Arrays.asList(new File("/home/user/Documents/ultimate"), new File("/home/sergey/Documents/work/ultimate"));
    for (File idePath : idePaths) {
      if (idePath.isDirectory()) {
        try {
          Ide ide = IdeManager.getInstance().createIde(idePath);
          checkIde(ide);
        } catch (Exception e) {
          e.printStackTrace();
          throw new AssertionFailedError("Some exception");
        }
      }
    }

  }

  private void checkIde(Ide ide) {
    try {
      Resolver ideResolver = Resolver.createIdeResolver(ide);
      ideResolver.getAllClasses(); //it should not throw an exception
    } catch (Exception e) {
      e.printStackTrace();
      throw new AssertionFailedError("Some exception");
    }
  }
}
