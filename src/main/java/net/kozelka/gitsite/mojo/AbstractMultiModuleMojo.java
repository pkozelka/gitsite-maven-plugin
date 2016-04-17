package net.kozelka.gitsite.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Dispatches execution to method based on module position within the reactor
 */
public abstract class AbstractMultiModuleMojo extends AbstractMojo {
    @Parameter(property = "reactorProjects", readonly = true)
    protected List<MavenProject> reactorProjects;
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;
    @Parameter(property = "session", required = true, readonly = true)
    protected MavenSession mavenSession;

    protected String executionRootDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        // http://lorands.com/2011/08/how-to-find-out-if-the-given-maven-project-is-a-root-of-a-multi-module-execution/
        executionRootDirectory = mavenSession.getExecutionRootDirectory();
        if (project == reactorProjects.get(0)) {
            executeInFirstModule();
        }
        if (project.isExecutionRoot()) {
            executeInRootModule();
        }
        final int size = reactorProjects.size();
        if (project == reactorProjects.get(size - 1)) {
            executeInLastModule();
        }
        executeInEachModule();
    }

    private File getParamFile() {
        return new File(executionRootDirectory, "." + getClass().getName() + ".properties");
    }

    protected File saveParameters(String... fieldNames) throws MojoExecutionException {
        final File paramFile = getParamFile();
        try {
            final Properties properties = new Properties();
            for (String fieldName : fieldNames) {
                final Field field = this.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                final Object value = field.get(this);
                if (value != null) {
                    properties.put(fieldName, value + "");
                }
            }
            final OutputStream os = new FileOutputStream(paramFile);
            try {
                properties.store(os, getClass().getName());
            } finally {
                os.close();
            }
        } catch (NoSuchFieldException e) {
            throw new MojoExecutionException("Cannot save parameters", e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Cannot save parameters", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot save parameters", e);
        }
        return paramFile;
    }

    protected File loadParameters() throws MojoExecutionException {
        final File paramFile = getParamFile();
        try {
            final Properties properties = new Properties();
            final FileInputStream is = new FileInputStream(paramFile);
            try {
                properties.load(is);
            } finally {
                is.close();
            }
            final Set<Object> keys = properties.keySet();
            for (Object key : keys) {
                final String fieldName = (String) key;
                final Field field = this.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                final String value = properties.getProperty(fieldName);
                field.set(this, fromString(value, field.getType()));
            }
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Cannot load parameters", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot load parameters", e);
        } catch (NoSuchFieldException e) {
            throw new MojoExecutionException("Cannot load parameters", e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Cannot load parameters", e);
        }
        return paramFile;
    }

    private Object fromString(String s, Class<?> toType) {
        if (toType.equals(Boolean.class) || toType.equals(boolean.class)) {
            return Boolean.valueOf(s);
        }
        if (toType.equals(File.class)) {
            return new File(s);
        }
        // more types comes here if needed
        return s;
    }

    protected void executeInEachModule() throws MojoExecutionException, MojoFailureException {
        //
    }

    protected void executeInFirstModule() throws MojoExecutionException, MojoFailureException {
        //
    }

    protected void executeInRootModule() throws MojoExecutionException, MojoFailureException {
        //
    }

    protected void executeInLastModule() throws MojoExecutionException, MojoFailureException {
        //
    }
}
