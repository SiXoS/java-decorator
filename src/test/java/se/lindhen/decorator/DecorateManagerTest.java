package se.lindhen.decorator;


import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DecorateManagerTest {

  private static File searchPath = getDecoratorSearchPath();

  private static File getDecoratorSearchPath() {
    try {
      return new File(DecorateManagerTest.class.getResource("/decoratorSearchPath").toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testFindsCorrectFiles() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method findAllDecorators = DecorateManager.class.getDeclaredMethod("findAllDecorators", File.class);
    findAllDecorators.setAccessible(true);
    List<File> files = (List<File>) findAllDecorators.invoke(new DecorateManager(), searchPath);
    findAllDecorators.setAccessible(false);
    assertEquals(3, files.size());
    files.forEach(file -> assertEquals("ExampleDecorator.java", file.getName()));
  }

}