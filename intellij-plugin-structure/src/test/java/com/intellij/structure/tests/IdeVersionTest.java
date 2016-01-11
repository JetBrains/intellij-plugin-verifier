package com.intellij.structure.tests;


import com.intellij.structure.domain.IdeVersion;
import org.junit.Assert;
import org.junit.Test;

public class IdeVersionTest {
  @Test
  public void testTypicalBuild() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("IU-138.1042");
    Assert.assertEquals(138, ideVersion.getBranch());
    Assert.assertEquals(1042, ideVersion.getBuild());
    Assert.assertEquals(0, ideVersion.getAttempt());
    Assert.assertEquals("IU", ideVersion.getProductCode());
    Assert.assertEquals("idea", ideVersion.getProductName());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals(true, ideVersion.isOk());
  }

  @Test
  public void testBuildWithAttempt() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("IU-138.1042.1");
    Assert.assertEquals(138, ideVersion.getBranch());
    Assert.assertEquals(1042, ideVersion.getBuild());
    Assert.assertEquals(1, ideVersion.getAttempt());
    Assert.assertEquals("IU", ideVersion.getProductCode());
    Assert.assertEquals("idea", ideVersion.getProductName());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals(true, ideVersion.isOk());
  }

  @Test
  public void testSnapshotBuild() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("PS-136.SNAPSHOT");
    Assert.assertEquals(136, ideVersion.getBranch());
    Assert.assertEquals(Integer.MAX_VALUE, ideVersion.getBuild());
    Assert.assertEquals(0, ideVersion.getAttempt());
    Assert.assertEquals("PS", ideVersion.getProductCode());
    Assert.assertEquals("phpStorm", ideVersion.getProductName());
    Assert.assertEquals(true, ideVersion.isSnapshot());
    Assert.assertEquals(true, ideVersion.isOk());
  }

  @Test
  public void testUnsupportedProduct() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("XX-138.SNAPSHOT");
    Assert.assertEquals(0, ideVersion.getBranch());
    Assert.assertEquals(0, ideVersion.getBuild());
    Assert.assertEquals("", ideVersion.getProductCode());
    Assert.assertNull(ideVersion.getProductName());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals(false, ideVersion.isOk());
  }

  @Test
  public void testCLionTypicalBuild() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("CL-140.1197");
    Assert.assertEquals(140, ideVersion.getBranch());
    Assert.assertEquals(1197, ideVersion.getBuild());
    Assert.assertEquals("CL", ideVersion.getProductCode());
    Assert.assertEquals("clion", ideVersion.getProductName());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals(true, ideVersion.isOk());
  }

  @Test
  public void testOneNumberActualBuild() {
    IdeVersion updateBuild = IdeVersion.createIdeVersion("133");
    Assert.assertEquals(133, updateBuild.getBranch());
    Assert.assertEquals(0, updateBuild.getBuild());
    Assert.assertEquals("", updateBuild.getProductCode());
    Assert.assertNull(updateBuild.getProductName());
    Assert.assertEquals(false, updateBuild.isSnapshot());
    Assert.assertEquals(true, updateBuild.isOk());
  }

  @Test
  public void testLegacyBuild() {
    IdeVersion updateBuild = IdeVersion.createIdeVersion("8987");
    Assert.assertEquals(80, updateBuild.getBranch());
    Assert.assertEquals(8987, updateBuild.getBuild());
    Assert.assertEquals("", updateBuild.getProductCode());
    Assert.assertNull(updateBuild.getProductName());
    Assert.assertEquals(false, updateBuild.isSnapshot());
    Assert.assertEquals(true, updateBuild.isOk());
  }
}