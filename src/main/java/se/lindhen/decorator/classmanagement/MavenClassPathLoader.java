package se.lindhen.decorator.classmanagement;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MavenClassPathLoader {

    private ClassLoader classLoader;

    public MavenClassPathLoader(List<String> classPath) {
        init(classPath);
    }

    private void init(List<String> classPath) {
        classLoader = URLClassLoader.newInstance(
                classPath.stream()
                        .map(this::appendSlashesForDirectories)
                        .map(this::makeUrl)
                        .collect(Collectors.toList())
                .toArray(new URL[classPath.size()]));
    }

    private String appendSlashesForDirectories(String path) {
        return isJar(path) ? "jar:file:" + path + "!/" : "file:" + path + "/";
    }

    private boolean isJar(String path) {
        return path.endsWith(".jar");
    }

    private URL makeUrl(String path) {
        try {
            return new URL(path);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Classpath '" + path + "' is not a valid url");
        }
    }

    public Class<?> loadClass(String qualifiedClassName) throws ClassNotFoundException {
        return classLoader.loadClass(qualifiedClassName);
    }

}
