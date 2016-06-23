package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

@XmlRootElement
public class CyclicDependenciesProblem extends Problem {

  private String myCycle;

  public CyclicDependenciesProblem() {

  }

  public CyclicDependenciesProblem(@NotNull String cycle) {
    Preconditions.checkNotNull(cycle);
    myCycle = cycle;
  }

  public String getCycle() {
    return myCycle;
  }

  public void setCycle(String cycle) {
    myCycle = cycle;
  }

  @NotNull
  public String getDescription() {
    return "cyclic plugin dependencies" + (myCycle != null ? " " + myCycle : "");
  }


  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new CyclicDependenciesProblem(params[0]);
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(new Pair<String, String>("cycle", myCycle));
  }


}
