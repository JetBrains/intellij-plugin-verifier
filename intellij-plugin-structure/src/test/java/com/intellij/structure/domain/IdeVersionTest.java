package com.intellij.structure.domain;


import org.junit.Assert;
import org.junit.Test;

public class IdeVersionTest {

  @Test
  public void testWithoutProductCode() throws Exception {
    //test that no exception
    IdeVersion.createIdeVersion("144.2608.2");
    IdeVersion.createIdeVersion("139.144");
    IdeVersion.createIdeVersion("139.SNAPSHOT");
    IdeVersion.createIdeVersion("139.SNAPSHOT");
    IdeVersion.createIdeVersion("6.0");
  }

  @Test
  public void testTypicalBuild() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("IU-138.1042");
    Assert.assertEquals(138, ideVersion.getBranch());
    Assert.assertEquals(1042, ideVersion.getBuild());
    Assert.assertEquals(0, ideVersion.getAttempt());
    Assert.assertEquals("IU", ideVersion.getProductCode());
    Assert.assertEquals("idea", ideVersion.getProductName());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals("IU-138.1042", ideVersion.toString());
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
    Assert.assertEquals("IU-138.1042.1", ideVersion.toString());
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
    Assert.assertEquals("PS-136.SNAPSHOT", ideVersion.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnsupportedProduct() {
    IdeVersion.createIdeVersion("XX-138.SNAPSHOT");
  }

  @Test
  public void testCLionTypicalBuild() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("CL-140.1197");
    Assert.assertEquals(140, ideVersion.getBranch());
    Assert.assertEquals(1197, ideVersion.getBuild());
    Assert.assertEquals("CL", ideVersion.getProductCode());
    Assert.assertEquals("clion", ideVersion.getProductName());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals("CL-140.1197", ideVersion.toString());
  }

  @Test
  public void testOneNumberActualBuild() {
    IdeVersion updateBuild = IdeVersion.createIdeVersion("133");
    Assert.assertEquals(133, updateBuild.getBranch());
    Assert.assertEquals(0, updateBuild.getBuild());
    Assert.assertEquals("", updateBuild.getProductCode());
    Assert.assertEquals("", updateBuild.getProductName());
    Assert.assertEquals(false, updateBuild.isSnapshot());
    Assert.assertEquals("133", updateBuild.toString());
  }

  @Test
  public void testLegacyBuild() {
    IdeVersion updateBuild = IdeVersion.createIdeVersion("8987");
    Assert.assertEquals(80, updateBuild.getBranch());
    Assert.assertEquals(8987, updateBuild.getBuild());
    Assert.assertEquals("", updateBuild.getProductCode());
    Assert.assertEquals("", updateBuild.getProductName());
    Assert.assertEquals(false, updateBuild.isSnapshot());
    Assert.assertEquals("80.8987", updateBuild.toString());
  }

  @Test
  public void getProductIdByCode() throws Exception {
    Assert.assertEquals("idea", IdeVersion.getProductIdByCode("IU"));
  }

  @Test
  public void testEqualsAndHashCode() throws Exception {
    IdeVersion ic1 = IdeVersion.createIdeVersion("IC-144.1532.2");
    IdeVersion ic2 = IdeVersion.createIdeVersion("IC-144.1532.2");
    IdeVersion iu1 = IdeVersion.createIdeVersion("IU-144.1532.2");
    Assert.assertEquals(ic1.hashCode(), ic2.hashCode());
    Assert.assertEquals(ic1, ic2);
    Assert.assertNotEquals(ic1.hashCode(), iu1.hashCode());
    Assert.assertNotEquals(ic1, iu1);
  }

  @Test
  public void isCorrectVersion() throws Exception {
    Assert.assertTrue(IdeVersion.isCorrectVersion("IC-144.123.11"));
    Assert.assertTrue(IdeVersion.isCorrectVersion("2031"));
    Assert.assertFalse(IdeVersion.isCorrectVersion("ABACABA"));
  }
}