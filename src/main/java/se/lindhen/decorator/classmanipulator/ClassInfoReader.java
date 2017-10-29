package se.lindhen.decorator.classmanipulator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import se.lindhen.decorator.util.Try;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClassInfoReader {

    private static final Pattern decoratorDetector = Pattern
        .compile("([a-zA-Z]|\\s)*class([a-zA-Z0-9<>,]|\\s)+implements([a-zA-Z0-9,<>]|\\s)+Decorator\\s*<([a-zA-Z0-9,<>]|\\s)+>\\s*\\{");

    public static boolean isDecoratableClass(File classDefinition) throws FileNotFoundException {
        return new Scanner(classDefinition)
            .findWithinHorizon(decoratorDetector, 0) != null;
    }

    public static Try<ClassInfo> read(String classDefinition) {
        CompilationUnit compilationUnit = JavaParser.parse(classDefinition);
        ClassInfo classInfo = new ClassInfo();

        Set<String> imports = compilationUnit.getImports().stream()
            .map(imp -> imp.getName().asString())
            .collect(Collectors.toSet());

        Try<ClassOrInterfaceDeclaration> mainClass = findTopLevelClass(compilationUnit)
            .map(decl -> (ClassOrInterfaceDeclaration) decl);

        mainClass.ifPresent(node -> classInfo.setClassName(node.getNameAsString()));
        return mainClass
            .flatMap(mainClass1 -> findPackage(compilationUnit).runIfPresent(classInfo::setPackageName).map(dontCare -> mainClass1))
            .flatMap(mainClass1 -> findClassToDecorate(imports, mainClass1).runIfPresent(classInfo::setDecoratesClass))
            .map(dontCare -> classInfo);
    }

    private static Try<TypeDeclaration<?>> findTopLevelClass(CompilationUnit compilationUnit) {
        return Try.ofOptional(compilationUnit.getTypes().stream()
            .filter(TypeDeclaration::isTopLevelType)
            .filter(TypeDeclaration::isClassOrInterfaceDeclaration)
            .findFirst())
            .failOnEmpty("Could not find any top-level class.");
    }

    private static Try<String> findPackage(CompilationUnit compilationUnit) {
        return Try.ofOptional(compilationUnit.getPackageDeclaration())
            .map(PackageDeclaration::getNameAsString)
            .failOnEmpty("no package declaration specified");
    }

    private static Try<String> findClassToDecorate(Set<String> imports, ClassOrInterfaceDeclaration mainClass) {
        Try<String> classToDecorate = getDecoratorExtensionDefinitionFromClassExtensions(mainClass)
            .failOnEmpty("The class " + mainClass.getNameAsString() + " did not extend Decorator.")
            .flatMapOptional(ClassInfoReader::getClassToDecorateFromTypeArguments)
            .failOnEmpty("There were no type arguments for the Decorator");
        return classToDecorate.flatMap(ctd -> getQualifiedClassFromImports(ctd, imports));
    }

    private static Try<ClassOrInterfaceType> getDecoratorExtensionDefinitionFromClassExtensions(ClassOrInterfaceDeclaration mainClass) {
        return Try.ofOptional(mainClass.getImplementedTypes()
                .stream()
                .filter(def -> def.getName().asString().equals("Decorator"))
                .findFirst());
    }

    private static Optional<String> getClassToDecorateFromTypeArguments(ClassOrInterfaceType decoratorExtension) {
        return decoratorExtension.getTypeArguments()
            .map(args -> args.isEmpty() ? null : args.get(0))
            .filter(Type::isClassOrInterfaceType)
            .map(t -> ((ClassOrInterfaceType) t).getName().asString());
    }

    private static Try<String> getQualifiedClassFromImports(String decoratesClass,Set<String> imports) {
        return Try.ofOptional(imports.stream()
            .filter(i -> i.endsWith("." + decoratesClass))
            .findFirst())
            .failOnEmpty("Could not determine import for the class to decorate (" + decoratesClass + "). Note that the import has to be fully qualified to be resolved.");
    }

    public static class ClassInfo {
        private String decoratesClass;
        private String packageName;
        private String className;

        public ClassInfo setClassName(String className) {
            this.className = className;
            return this;
        }

        public ClassInfo setDecoratesClass(String decoratesClass) {
            this.decoratesClass = decoratesClass;
            return this;
        }

        public ClassInfo setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public String getClassName() {
            return className;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getDecoratesClass() {
            return decoratesClass;
        }
    }

}
