package com.sake.build.plugins
import org.testng._
import org.testng.xml.XmlSuite
import scala.collection.JavaConversions._

trait TestNG {

  def testJar: String
  def suiteFile: String
  
  def test = {
    val testng = new org.testng.TestNG();
    testng.setTestJar(testJar)
    val suite = new XmlSuite
    suite.setFileName(suiteFile)

    testng.setXmlSuites(List(suite))
    testng.runSuitesLocally
    //testng.setTestClasses(Array( Run2.class ));
    //testng.addListener(tla);
    //testng.run();

  }
}