package net.kozelka.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

public class IntegrationTestUtils {
    public static void run(File workdir, String executable, String... args) throws CommandLineException {
        final Commandline cl = new Commandline();
        if (workdir != null) {
            cl.setWorkingDirectory(workdir);
        }
        cl.setExecutable(executable);
        cl.addArguments(args);
        final StreamConsumer stdout = new DefaultConsumer();
        final StreamConsumer stderr = stdout;
        System.out.printf("Executing: %s%n", cl);
        final int exitCode = CommandLineUtils.executeCommandLine(cl, stdout, stderr);
        System.out.printf("Exit code: %d%n", exitCode);
        if (exitCode != 0) {
            throw new RuntimeException(String.format("git returned with exit code '%d'", exitCode));
        }
    }

    public static void createGitRepo(File gitwc) throws CommandLineException, IOException {
        run(null, "git", "init", gitwc.getAbsolutePath());
        FileUtils.fileWrite(new File(gitwc, "Hello.txt"), "Hello world");
        run(gitwc, "git", "add", "-A");
        run(gitwc, "git", "commit", "-am", "first commit");
    }
}
