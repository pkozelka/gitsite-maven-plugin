package net.kozelka.gitsite.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

public class ShellExecutor {
    private File workingDirectory;
    private StreamConsumer info;
    private StreamConsumer stdout = new DefaultConsumer();
    private StreamConsumer stderr = new DefaultConsumer();

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setInfo(StreamConsumer info) {
        this.info = info;
    }

    public void setStdout(StreamConsumer stdout) {
        this.stdout = stdout;
    }

    public void setStderr(StreamConsumer stderr) {
        this.stderr = stderr;
    }

    public void exec(String executable, String... args) throws CommandLineException {
        final Commandline cl = new Commandline();
        cl.setWorkingDirectory(workingDirectory);
        cl.setExecutable(executable);
        cl.addArguments(args);
        info.consumeLine(String.format("Executing: %s", cl));
        final int exitCode = CommandLineUtils.executeCommandLine(cl, stdout, stderr);
        if (exitCode != 0) {
            throw new CommandLineException(String.format("%s returned with exit code '%d'", executable, exitCode));
        }
    }

    public void execWithResult(final Result result, String executable, String... args) throws CommandLineException {
        final Commandline cl = new Commandline();
        cl.setWorkingDirectory(workingDirectory);
        cl.setExecutable(executable);
        cl.addArguments(args);
        info.consumeLine(String.format("Executing: %s", cl));
        result.exitCode = CommandLineUtils.executeCommandLine(cl,
            new StreamConsumer() {
                public void consumeLine(String line) {
                    stdout.consumeLine(line);
                    result.stdoutLines.add(line);
                }
            },
            new StreamConsumer() {
                public void consumeLine(String line) {
                    stderr.consumeLine(line);
                    result.stderrLines.add(line);
                }
            });
        info.consumeLine(String.format("%s exited with code %d", executable, result.exitCode));
    }

    public static class Result {
        private int exitCode;
        private final List<String> stdoutLines = new ArrayList<String>();
        private final List<String> stderrLines = new ArrayList<String>();

        public int getExitCode() {
            return exitCode;
        }

        public List<String> getStdoutLines() {
            return stdoutLines;
        }

        public List<String> getStderrLines() {
            return stderrLines;
        }
    }
}
