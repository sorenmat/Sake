package com.sake.build.plugins

import scala.collection.JavaConversions._
import java.io.File
import java.net.URLClassLoader
import com.sake.build.ivy.JarDependency
import org.testng._

trait TestNG {

  def testJarName: String

  def jarName: String

  def classpath: Set[JarDependency]

  def test {
      test("test_resources/generaladvlife-db-test.xml")
  }

  def test(suiteFile: String) = {
    try {
      val urls = classpath.map(file => file.getJarFile.toURI.toURL)
      val testng = new org.testng.TestNG();
      testng.setVerbose(1)
      // ClassLoader.getSystemClassLoader()

      addSoftwareLibrary(new File(jarName), testng.getClass.getClassLoader)
      classpath.foreach(file => addSoftwareLibrary(file.getJarFile, testng.getClass.getClassLoader))
      //testng.setServiceLoaderClassLoader(ClassLoader.getSystemClassLoader().asInstanceOf[URLClassLoader])

      val cl = new URLClassLoader(urls.toArray, ClassLoader.getSystemClassLoader())
      testng.addClassLoader(cl)
      println("Settings testing jar in TestNG to " + testJarName)
      //testng.setTestJar(testJarName)
      testng.setTestSuites(List(suiteFile))
      testng.addListener(new ISuiteListener {
        def onFinish(p1: ISuite) {
          println("Finishing...")
        }

        def onStart(p1: ISuite) {
          println("Starting...")
        }
      })
      val result = testng.run

    } catch {
      case e: Throwable => e.printStackTrace()
    }

  }

  def addSoftwareLibrary(file: File, classLoader: ClassLoader) {
    try {
      val method = classOf[URLClassLoader].getDeclaredMethods.filter(f => f.getName == "addURL").head
      //val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
      method.setAccessible(true);
      println(file.toURI().toURL() + " was added to system classloader")
      method.invoke(classLoader, file.toURI().toURL())

    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
}