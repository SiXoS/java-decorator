package se.lindhen.decorator;

import se.lindhen.decorator.classmanipulator.ClassInfoReaderTest;

import java.util.Scanner;

public class TestUtils {

  public static String readResource(String resource) {
    return new Scanner(ClassInfoReaderTest.class.getResourceAsStream(resource)).useDelimiter("\\Z").next();
  }

}
