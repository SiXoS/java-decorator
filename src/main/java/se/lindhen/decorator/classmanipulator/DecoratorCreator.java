package se.lindhen.decorator.classmanipulator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class DecoratorCreator {

  private static final String DELEGATE_FIELD_NAME = "delegate";
  private static final String DELEGATE_CONSTRUCTOR_PARAMETER_NAME = "delegate";
  private static final String DELEGATE_FETCHER_NAME = "getDelegate";

  private final ClassInfoReader.ClassInfo classInfo;
  private final String classToDecorateSimpleName;
  private final Class<?> classToDecorate;

  public DecoratorCreator(ClassInfoReader.ClassInfo classInfo, Class<?> classToDecorate) {
    this.classInfo = classInfo;
    this.classToDecorateSimpleName = classInfo.getDecoratesClass().substring(classInfo.getDecoratesClass().lastIndexOf('.') + 1);
    this.classToDecorate = classToDecorate;
  }

  public String generateDecorator(){
    CompilationUnit compilationUnit = new CompilationUnit();
    setPackageName(compilationUnit);
    addImports(compilationUnit);
    createClass(compilationUnit);
    return compilationUnit.toString();
  }

  private void setPackageName(CompilationUnit compilationUnit) {
    compilationUnit.setPackageDeclaration("decorator." + classInfo.getPackageName());
  }

  private void addImports(CompilationUnit compilationUnit) {
    compilationUnit.addImport(classInfo.getDecoratesClass());
    compilationUnit.addImport(classInfo.getPackageName() + "." + classInfo.getClassName());
  }

  private void createClass(CompilationUnit compilationUnit) {
    ClassOrInterfaceDeclaration classDecl = compilationUnit.addClass(classInfo.getClassName() + "Impl", Modifier.PUBLIC);
    classDecl.addExtendedType(classInfo.getClassName());
    classDecl.addField(classToDecorateSimpleName, DELEGATE_FIELD_NAME, Modifier.PRIVATE, Modifier.FINAL);
    generateConstructorCode(classDecl.addConstructor(Modifier.PUBLIC));
    generateDelegateGetter(classDecl);
    generateDecoratorMethods(classDecl);
  }

  private void generateConstructorCode(ConstructorDeclaration constructor) {
    constructor.addAndGetParameter(classToDecorateSimpleName, DELEGATE_CONSTRUCTOR_PARAMETER_NAME);
    AssignExpr assignExpr = new AssignExpr(new FieldAccessExpr(new ThisExpr(), DELEGATE_FIELD_NAME), new NameExpr(DELEGATE_CONSTRUCTOR_PARAMETER_NAME), AssignExpr.Operator.ASSIGN);
    NodeList<Statement> statements = new NodeList<>();
    statements.add(new ExpressionStmt(assignExpr));
    constructor.setBody(new BlockStmt(statements));
  }

  private void generateDelegateGetter(ClassOrInterfaceDeclaration classDecl) {
    MethodDeclaration methodDecl = classDecl.addMethod(DELEGATE_FETCHER_NAME, Modifier.PUBLIC);
    NodeList<Statement> statements = new NodeList<>();
    statements.add(new ReturnStmt(new NameExpr(DELEGATE_FIELD_NAME)));
    methodDecl.setBody(new BlockStmt(statements));
  }

  private void generateDecoratorMethods(ClassOrInterfaceDeclaration classDecl) {
    Arrays.stream(classToDecorate.getMethods())
        .filter(this::isOverridableMethod)
        .forEach(method -> generateDecoratorMethod(classDecl, method));
  }

  /* non-javadoc
   * If the method is static or final it can't be overridden: return false
   * If the method is public or protected it can be overridden: return true
   * Otherwise it can't be overridden: return false
   */
  private boolean isOverridableMethod(Method method) {
    if((method.getModifiers() & (java.lang.reflect.Modifier.STATIC | java.lang.reflect.Modifier.FINAL)) > 0){
      return false;
    }
    if((method.getModifiers() & (java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.PROTECTED)) > 0) {
      return true;
    }
    return false;
  }

  private void generateDecoratorMethod(ClassOrInterfaceDeclaration classDecl, Method method) {

    MethodDeclaration methodDecl = classDecl.addMethod(method.getName(), javaReflectionAccessModifiersToAstModifiers(method.getModifiers()));
    Arrays.stream(method.getParameters())
        .forEach(parameter -> crateAndAddParameterFromReflection(methodDecl, parameter));
    generateDelegateInvoker(methodDecl, method.getReturnType());
  }


  private Modifier javaReflectionAccessModifiersToAstModifiers(int modifiers) {
    // we only need to check for public or protected modifiers.
    // Modifiers like private, abstract, final, native or static can't be used on the generated methods
    // Modifiers like synchronized or strictfp still has effect even if not used on the delegate methods.
    if((modifiers & java.lang.reflect.Modifier.PROTECTED) > 0){
      return Modifier.PROTECTED;
    } else if ((modifiers & java.lang.reflect.Modifier.PUBLIC) > 0) {
      return Modifier.PUBLIC;
    } else {
      throw new IllegalArgumentException("Tried to translate modifier that were neither public nor protected but: '" +
          java.lang.reflect.Modifier.toString(modifiers) + "'.");
    }
  }

  private void crateAndAddParameterFromReflection(MethodDeclaration methodDecl, java.lang.reflect.Parameter parameter) {
    methodDecl.addAndGetParameter(parameter.getType(), parameter.getName());
  }

  private void generateDelegateInvoker(MethodDeclaration methodDecl, Class<?> returnType) {
    NodeList<Expression> arguments = methodDecl.getParameters()
        .stream()
        .map(Parameter::getNameAsString)
        .map(NameExpr::new)
        .collect(NodeList.toNodeList());
    Expression methodCallExpr = new MethodCallExpr(new FieldAccessExpr(new ThisExpr(), DELEGATE_FIELD_NAME), methodDecl.getName(), arguments);
    // if the return type is the same as the class to decorate we want to wrap it to keep the decoration
    if(returnType.isAssignableFrom(classToDecorate)) {
      ClassOrInterfaceType decoratorType = new ClassOrInterfaceType(null, classInfo.getClassName());
      methodCallExpr = new ObjectCreationExpr(null, decoratorType, NodeList.nodeList(methodCallExpr));
      methodDecl.setType(decoratorType);
    } else {
      methodDecl.setType(returnType);
    }
    methodDecl.setBody(new BlockStmt(NodeList.nodeList(new ReturnStmt(methodCallExpr))));
  }

}
