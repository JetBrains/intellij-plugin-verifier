import com.intellij.structure.impl.utils.StringUtil;
import com.jetbrains.pluginverifier.persistence.GsonHolder;
import com.jetbrains.pluginverifier.problems.AccessType;
import com.jetbrains.pluginverifier.problems.IncompatibleClassChangeProblem;
import com.jetbrains.pluginverifier.problems.Problem;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public class TestProblems {

  private Set<String> myProblemsClasses = new HashSet<String>();

  @Before
  public void setUp() throws Exception {
    //collect all Problem derived classes

    File dir = new File(StringUtil.toSystemDependentName("src/main/java/com/jetbrains/pluginverifier/problems"));
    if (!dir.isDirectory()) {
      throw new IllegalArgumentException(dir + " is not a problems containing directory");
    }
    for (File aFile : FileUtils.listFiles(dir, null, true)) {
      String name = aFile.getAbsolutePath();
      name = name.substring(0, name.lastIndexOf('.'));
      String s = StringUtil.substringAfter(name, StringUtil.toSystemDependentName("src/main/java/"));
      if (s.endsWith("Problem")) {
        myProblemsClasses.add(s.replace(File.separatorChar, '.'));
      }
    }
    if (myProblemsClasses.isEmpty()) {
      throw new IllegalArgumentException("No problem classes found");
    }
//    System.out.println("All found problem classes: " + myProblemsClasses);
  }

  @Test
  public void checkHasDefaultPublicConstructor() throws Exception {
    for (String name : myProblemsClasses) {
      Class<?> aClass = Class.forName(name);
      if ((aClass.getModifiers() & Modifier.ABSTRACT) != 0) {
        continue;
      }
      aClass.getConstructor(); //NoSuchMethod if no public constructor
    }
  }

  /*@Test
  public void checkFailForNullFields() throws Exception {
    for (String name : myProblemsClasses) {
      Class<?> aClass = Class.forName(name);
      if ((aClass.getModifiers() & Modifier.ABSTRACT) == 0) {
        //create an instance with empty (null) fields
        Problem instance = ((Problem) aClass.newInstance());

        String json = GsonHolder.INSTANCE.getGSON().toJson(instance);
        boolean ok = false;
        try {
          GsonHolder.INSTANCE.getGSON().fromJson(json, Problem.class);
        } catch (Exception e) {
          ok = true;
        }
        if (!ok) {
          throw new AssertionError("Not OK for " + name);
        }
      }
    }
  }*/

  @Test
  public void checkConversions() throws Exception {
    for (String name : myProblemsClasses) {
      Class<?> aClass = Class.forName(name);
      if ((aClass.getModifiers() & Modifier.ABSTRACT) != 0) {
        continue;
      }
      checkConversion(aClass);
    }

  }

  private void checkConversion(Class<?> aClass) throws Exception {
    Constructor<?>[] constructors = aClass.getConstructors();

    int i = 0;
    List<Object> pairs = new ArrayList<Object>();

    for (Constructor<?> constructor : constructors) {
      if (constructor.getParameterTypes().length > 0) {
        for (Class<?> aClass1 : constructor.getParameterTypes()) {
          if (aClass1.equals(AccessType.class)) {
            pairs.add(AccessType.PACKAGE_PRIVATE);
          } else if (aClass1.equals(IncompatibleClassChangeProblem.Change.class)) {
            pairs.add(IncompatibleClassChangeProblem.Change.CLASS_TO_INTERFACE);
          } else {
            pairs.add("TEST_PARAM_" + (++i));
          }
        }
        Object instance = constructor.newInstance(pairs.toArray(new Object[0]));
        String json = GsonHolder.INSTANCE.getGSON().toJson(instance, Problem.class);
        Problem problem = (Problem) GsonHolder.INSTANCE.getGSON().fromJson(json, instance.getClass());

        Assert.assertEquals(instance, problem);
        return;
      }
    }



  }
}
