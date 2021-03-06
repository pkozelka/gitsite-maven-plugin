package net.kozelka.gitsite.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

public class ShellExecutor {
    private File workingDirectory;
    private StreamConsumer info = new DefaultConsumer();
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
        if (workingDirectory != null) {
            cl.setWorkingDirectory(workingDirectory);
        }
        cl.setExecutable(executable);
        cl.addArguments(args);
        final int clHash = cl.toString().hashCode();
        info.consumeLine(String.format("Executing: %s [#%x]", cl.toString(), clHash));
        final int exitCode = CommandLineUtils.executeCommandLine(cl, stdout, stderr);
        if (exitCode != 0) {
            throw new CommandLineException(String.format("%s [#%x] returned with exit code '%d'", executable, clHash, exitCode));
        }
    }

    public Result execWithResult(String executable, String... args) throws CommandLineException {
        final Result result = new Result();
        execWithResult(result, executable, args);
        return result;
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

        public boolean stdoutContains(Pattern message) {
            return contains(stdoutLines, message);
        }

        public boolean stderrContains(Pattern message) {
            return contains(stderrLines, message);
        }

        private boolean contains(List<String> lines, Pattern message) {
            for (String line : lines) {
                final String lline = line.toLowerCase();
                final Matcher matcher = message.matcher(lline);
                if (matcher.matches()) {
                    return true;
                }
            }
            return false;
        }

    }
}
