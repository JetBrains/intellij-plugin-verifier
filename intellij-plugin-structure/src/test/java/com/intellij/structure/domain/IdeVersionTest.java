package com.intellij.structure.domain;


import org.junit.Assert;
import org.junit.Test;

public class IdeVersionTest {

  @Test
  public void testWithoutProductCode() {
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
    Assert.assertEquals(138, ideVersion.getBaselineVersion());
    Assert.assertEquals(1042, ideVersion.getBuild());
    Assert.assertNull(ideVersion.getAttempt());
    Assert.assertEquals("IU", ideVersion.getProductCode());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals("IU-138.1042", ideVersion.asString());
  }

  @Test
  public void testBuildWithAttempt() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("IU-138.1042.1");
    Assert.assertEquals(138, ideVersion.getBaselineVersion());
    Assert.assertEquals(1042, ideVersion.getBuild());
    Assert.assertEquals("1", ideVersion.getAttempt());
    Assert.assertEquals("IU", ideVersion.getProductCode());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals("IU-138.1042.1", ideVersion.asString(true, true));
  }

  @Test
  public void testRiderTypicalBuild() {
    IdeVersion updateBuild = IdeVersion.createIdeVersion("RS-144.4713");
    Assert.assertEquals(144, updateBuild.getBaselineVersion());
    Assert.assertEquals(4713, updateBuild.getBuild());
    Assert.assertEquals("RS", updateBuild.getProductCode());
//    Assert.assertEquals("rider", updateBuild.getProductName());
    Assert.assertEquals(false, updateBuild.isSnapshot());
  }

  @Test
  public void testSnapshotBuild() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("PS-136.SNAPSHOT");
    Assert.assertEquals(136, ideVersion.getBaselineVersion());
    Assert.assertEquals(Integer.MAX_VALUE, ideVersion.getBuild());
    Assert.assertNull(ideVersion.getAttempt());
    Assert.assertEquals("PS", ideVersion.getProductCode());
    Assert.assertEquals(true, ideVersion.isSnapshot());
    Assert.assertEquals("PS-136.SNAPSHOT", ideVersion.asString());
  }

  /*@Test(expected = IllegalArgumentException.class)
  public void testUnsupportedProduct() {
    IdeVersion.createIdeVersion("XX-138.SNAPSHOT");
  }*/

  @Test
  public void testCLionTypicalBuild() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("CL-140.1197");
    Assert.assertEquals(140, ideVersion.getBaselineVersion());
    Assert.assertEquals(1197, ideVersion.getBuild());
    Assert.assertEquals("CL", ideVersion.getProductCode());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals("CL-140.1197", ideVersion.asString());
  }

  @Test
  public void testOneNumberActualBuild() {
    IdeVersion updateBuild = IdeVersion.createIdeVersion("133");
    Assert.assertEquals(133, updateBuild.getBaselineVersion());
    Assert.assertEquals(0, updateBuild.getBuild());
    Assert.assertEquals("", updateBuild.getProductCode());
    Assert.assertEquals(false, updateBuild.isSnapshot());
    Assert.assertEquals("133.0", updateBuild.asString());
  }

  @Test
  public void testLegacyBuild() {
    IdeVersion updateBuild = IdeVersion.createIdeVersion("8987");
    Assert.assertEquals(80, updateBuild.getBaselineVersion());
    Assert.assertEquals(8987, updateBuild.getBuild());
    Assert.assertEquals("", updateBuild.getProductCode());
    Assert.assertEquals(false, updateBuild.isSnapshot());
    Assert.assertEquals("80.8987", updateBuild.asString());
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

}