package se.lindhen.decorator.classmanipulator;

import org.junit.Test;
import se.lindhen.decorator.util.Try;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.lindhen.decorator.TestUtils.readResource;

public class ClassInfoReaderTest {

  private static String exampleDecorator = readResource("/ExampleDecorator.java");
  private static String exampleDecoratorNoImplements = readResource("/ExampleDecoratorNoImplement.java");
  private static String emptyClass = readResource("/EmptyClass.java");
  private static String noPackage = readResource("/NoPackage.java");
  private static String unqualifiedDecoratorName = readResource("/ExampleDecoratorUnqualifiedDecorator.java");
  private static String noTypeArgument = readResource("/ExampleDecoratorNoTypeArgument.java");

  @Test
  public void testExpectedInput() {
    Try<ClassInfoReader.ClassInfo> read = ClassInfoReader.read(exampleDecorator);
    read.ifFailed(System.out::println);
    assertTrue(read.isPresent());
    ClassInfoReader.ClassInfo result = read.get();
    assertEquals("MapOptional", result.getClassName());
    assertEquals("se.lindhen.decorators", result.getPackageName());
    assertEquals("java.util.Map", result.getDecoratesClass());
  }

  @Test
  public void testClassDoesNotImplementDecorator() {
    testFailure(exampleDecoratorNoImplements, "The class MapOptional did not extend Decorator.");
  }

  @Test
  public void testEmptyClass() {
    testFailure(emptyClass, "Could not find any top-level class.");
  }

  @Test
  public void testNoPackage() {
    testFailure(noPackage, "no package declaration specified");
  }

  @Test
  public void testUnqualifiedDecorator() {
    testFailure(unqualifiedDecoratorName, "Could not determine import for the class to decorate (Map). Note that the import has to be fully qualified to be resolved.");
  }

  @Test
  public void testMissingTypeArguments() {
    testFailure(noTypeArgument, "There were no type arguments for the Decorator");
  }

  private void testFailure(String exampleDecoratorNoImplements, String expected) {
    Try<ClassInfoReader.ClassInfo> read = ClassInfoReader.read(exampleDecoratorNoImplements);
    assertTrue(read.hasFailed());
    read.ifFailed(reason -> assertEquals(expected, reason));
  }

}