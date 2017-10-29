package se.lindhen.decorator;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import se.lindhen.decorator.classmanagement.MavenClassPathLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Goal which touches a timestamp file.
 *
 */
@Mojo(name = "decorate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class MyMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     */
    @Parameter(property = "javadecorator.target.dir", defaultValue = "${project.build.directory}")
    private File outputDirectory;

    @Parameter(property = "javadecorator.sources.dir", defaultValue = "${project.build.sourceDirectory}")
    private File sourceDirectory;

    @Parameter(property = "d", defaultValue = "${project.runtimeClasspathElements}")
    private List<String> classPath;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            File searchPath = new File(sourceDirectory, "se/lindhen/decorators");
            for (File file : searchPath.listFiles()) {
                String content = new Scanner(file).useDelimiter("\\Z").next();
                String classImplementsDecorator = "([a-zA-Z]|\\s)*class([a-zA-Z0-9<>,]|\\s)+implements([a-zA-Z0-9,<>]|\\s)+Decorator\\s*<([a-zA-Z0-9,<>]|\\s)+>\\s*\\{";
                if (Pattern.compile(classImplementsDecorator).matcher(content).find()) {
                    generateDecorator(file);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void generateDecorator(File file) throws MojoExecutionException {
        MavenClassPathLoader classLoader = new MavenClassPathLoader(classPath);
        String classPath = file.getAbsolutePath().substring(sourceDirectory.getAbsolutePath().length() + 1);
        classPath = classPath.replace('/','.');
        classPath = classPath.substring(0, classPath.length() - 5);
        Class<?> decoratorClass = null;
        try {
            decoratorClass = classLoader.loadClass("java.util.Map");
            System.out.println("success");
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("class not found", e);
        }
        Type[] genericInterfaces = decoratorClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if(genericInterface instanceof ParameterizedType) {
                System.out.println(((ParameterizedType)genericInterface).getTypeName() + ", " + Arrays.toString(((ParameterizedType) genericInterface).getActualTypeArguments()));
            }
        }
    }




}
