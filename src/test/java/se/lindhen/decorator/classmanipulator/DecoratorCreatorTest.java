package se.lindhen.decorator.classmanipulator;

import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static se.lindhen.decorator.TestUtils.readResource;

public class DecoratorCreatorTest {

  @Test
  public void testSimpleClass() {
    String decorateDecleration = readResource("/ExampleDecorator.java");
    ClassInfoReader.ClassInfo classInfo = ClassInfoReader.read(decorateDecleration).get();
    System.out.println(new DecoratorCreator(classInfo, Map.class).generateDecorator());
  }

  @Test
  public void testChainFunctionsAreWrapped() {
    String decorateDecleration = readResource("/DecoratedFuture.java");
    ClassInfoReader.ClassInfo classInfo = ClassInfoReader.read(decorateDecleration).get();
    System.out.println(new DecoratorCreator(classInfo, CompletableFuture.class).generateDecorator());
  }

}