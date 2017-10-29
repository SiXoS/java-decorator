package se.lindhen.decorator;

import com.github.javaparser.ast.CompilationUnit;
import com.google.inject.internal.util.Lists;
import se.lindhen.decorator.classmanipulator.ClassInfoReader;
import se.lindhen.decorator.util.Try;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DecorateManager {


  public void performDecoration(File searchPath) {
    List<File> allDecorators = findAllDecorators(searchPath);
    List<Try<ClassInfoReader.ClassInfo>> classInfos = allDecorators.stream()
        .map(file -> ClassInfoReader.read(readFile(file)))
        .collect(Collectors.toList());
    Try.join(classInfos)
      .flatMap(this::generateClasses);

  }

  private List<File> findAllDecorators(File searchPath) {
    if(searchPath.isDirectory()) {
      return Arrays.stream(searchPath.listFiles())
          .map(this::findAllDecorators)
          .reduce(this::mergeLists)
          .orElse(Collections.emptyList());
    } else {
      try {
        if(ClassInfoReader.isDecoratableClass(searchPath)) {
          return Lists.newArrayList(searchPath);
        }
      } catch (FileNotFoundException e) {
        System.err.println("Could not find file '" + searchPath.getAbsolutePath() + "'.");
      }
      return Lists.newArrayList();
    }
  }

  private List<File> mergeLists(List<File> accumulator, List<File> nextList) {
    accumulator.addAll(nextList);
    return accumulator;
  }

  private Try<CompilationUnit> generateClasses(List<ClassInfoReader.ClassInfo> classInfos) {
    return null;
  }

  private String readFile(File file) {
    try {
      return new Scanner(file).useDelimiter("\\Z").next();
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Tried to parse file (" + file.getAbsolutePath() + ") that doesn't exist.", e);
    }
  }

}
