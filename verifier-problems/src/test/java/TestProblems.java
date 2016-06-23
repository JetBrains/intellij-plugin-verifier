import com.intellij.structure.impl.utils.StringUtil;
import com.jetbrains.pluginverifier.persistence.GsonHolder;
import com.jetbrains.pluginverifier.problems.AccessType;
import com.jetbrains.pluginverifier.problems.IncompatibleClassChangeProblem;
import com.jetbrains.pluginverifier.problems.Problem;
import kotlin.Pair;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

  @Test
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
  }

  @Test
  public void checkConversions() throws Exception {
    for (String name : myProblemsClasses) {
      Class<?> aClass = Class.forName(name);
      if ((aClass.getModifiers() & Modifier.ABSTRACT) != 0) {
        continue;
      }
      Method serialize = null;
      Method deserialize = null;
      for (Method method : aClass.getMethods()) {
        if (method.getName().equals("serialize")) {
          serialize = method;
        }
        if (method.getName().equals("deserialize")) {
          deserialize = method;
        }
      }
      Assert.assertNotNull(serialize);
      Assert.assertNotNull(deserialize);

      checkConversion(aClass);
    }

  }

  private void checkConversion(Class<?> aClass) throws Exception {
    Problem factory = (Problem) aClass.newInstance();

    List<Pair<String, String>> pairs = factory.serialize();
    int size = pairs.size();
    String[] names = new String[size];
    String[] params = new String[size];
    for (int i = 0; i < size; i++) {
      names[i] = pairs.get(i).getFirst();
      if ("access".equals(names[i])) { //guess enum constant name
        params[i] = AccessType.PACKAGE_PRIVATE.name();
      } else if ("change".equals(names[i])) {
        params[i] = IncompatibleClassChangeProblem.Change.CLASS_TO_INTERFACE.name();
      } else {
        params[i] = "TEST_PARAMETER_" + i;
      }
    }

    Problem instance = (Problem) factory.deserialize(params);

    String json = GsonHolder.INSTANCE.getGSON().toJson(instance);
    Problem problem = GsonHolder.INSTANCE.getGSON().fromJson(json, instance.getClass());

    Assert.assertEquals(instance, problem);
  }
}
