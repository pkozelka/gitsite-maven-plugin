package net.kozelka.gitsite.mojo;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Pushes staged site into special branch of the same git repository where the project resides.
 *
 * @author Petr Kozelka
 */
@Mojo(name = "gitsite-deploy", defaultPhase = LifecyclePhase.SITE_DEPLOY, requiresProject = true, aggregator = true)
public class SiteDeployGitMojo extends AbstractMojo {

    /**
     * The site directory to be deployed. It must be already completely generated.
     */
    @Parameter(defaultValue = "${project.build.directory}/staging", property = "gitsite.inputDirectory")
    File inputDirectory;

    /**
     * Which branch to put the generated site into.
     * <p><b>DANGEROUS</b> this branch will be replaced, with no history left - make sure you won't loose any data</p>
     */
    @Parameter(defaultValue = "site", property = "gitsite.branch")
    String gitBranch;

    /**
     * The SCM URL where the site will be deployed. By default we reuse the current project's scm.
     */
    @Parameter(defaultValue = "${project.scm.developerConnection}")
    String gitScmUrl;

    /**
     * Whether to keep previous commits in the site branch. If false, all previous commits will be deleted with each update.
     */
    @Parameter(defaultValue = "true", property = "gitsite.keepHistory")
    boolean keepHistory;

    @Parameter(defaultValue = "${project.build.directory}/gitsite-deploy.log")
    File logfile;

    private static final String SCM_PREFIX = "scm:git:";

    private void validate() throws MojoExecutionException {
        if (!gitScmUrl.startsWith(SCM_PREFIX)) {
            throw new MojoExecutionException(String.format("gitScmUrl must start with prefix '%s'", SCM_PREFIX));
        }
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        validate();
        final String gitRemoteUrl = gitScmUrl.substring(SCM_PREFIX.length());
        try {
            FileUtils.deleteDirectory(new File(inputDirectory, ".git"));
            FileUtils.fileWrite(new File(inputDirectory, ".gitattributes").getAbsolutePath(), "* text=auto\n");
            final int fileCount = FileUtils.getFiles(inputDirectory, null, null, false).size();
            if (keepHistory) {
                // branch must already exist! TODO: detect that
                final String origDir = inputDirectory.getAbsolutePath() + ".orig";
                git("clone", "--branch", gitBranch, "--single-branch", gitRemoteUrl, origDir);
                FileUtils.rename(new File(origDir, ".git"), new File(inputDirectory, ".git"));
                git("add", "-A", ".");
                git("commit", "-am", String.format("Updating site with %d files", fileCount));
                git("push", gitRemoteUrl);
            } else {
                git("init");
                git("add", "-A", ".");
                git("commit", "-am", String.format("Creating site with %d files", fileCount));
                git("push", gitRemoteUrl, "+master:" + gitBranch);
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("git publishing error", e);
        } catch (IOException e) {
            throw new MojoExecutionException("git publishing error", e);
        }
    }

    private void git(String... args) throws CommandLineException, MojoFailureException {
        final Commandline cl = new Commandline();
        cl.setWorkingDirectory(inputDirectory);
        cl.setExecutable("git");
        cl.addArguments(args);
        final StreamConsumer stdout = new StreamConsumer() {
            public void consumeLine(String line) {
                debug(line);
            }
        };
        final StreamConsumer stderr = new StreamConsumer() {
            public void consumeLine(String line) {
                warn(line);
            }
        };
        info(String.format("Executing: %s", cl));
        final int exitCode = CommandLineUtils.executeCommandLine(cl, stdout, stderr);
        if (exitCode != 0) {
            throw new MojoFailureException(String.format("git returned with exit code '%d'", exitCode));
        }
    }

    private void filelog(String severity, String line) {
        try {
            FileUtils.fileAppend(logfile.getAbsolutePath(), String.format("%6s %s%n", severity, line));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void debug(String line) {
        getLog().debug(line);
        filelog("DEBUG", line);
    }

    private void warn(String line) {
        getLog().warn(line);
        filelog("WARN", line);
    }

    private void info(String line) {
        getLog().info(line);
        filelog("INFO", line);
    }
}
