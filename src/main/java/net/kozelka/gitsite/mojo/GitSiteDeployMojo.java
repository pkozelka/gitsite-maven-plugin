package net.kozelka.gitsite.mojo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import net.kozelka.gitsite.utils.ShellExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Pushes prepared static site into a special git branch.
 * Can be used to simplify maven site publication on GitHub Pages.
 *
 * @author Petr Kozelka
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.SITE_DEPLOY, requiresProject = true)
public class GitSiteDeployMojo extends AbstractMultiModuleMojo {

    /**
     * The site directory to be deployed. It must be already completely generated.
     */
    @Parameter(defaultValue = "${project.build.directory}/staging", property = "gitsite.inputDirectory")
    File inputDirectory;

    /**
     * Which branch to put the generated site into.
     */
    @Parameter(defaultValue = "gitsite", property = "gitsite.branch")
    String gitBranch;

    /**
     * The SCM URL where the site will be deployed. By default we reuse the current project's scm.
     */
    @Parameter(defaultValue = "${project.scm.developerConnection}", property = "gitsite.gitScmUrl")
    String gitScmUrl;

    /**
     * Whether to keep previous commits in the site branch. If false, all previous commits will be deleted with each update.
     * <p><b>DANGEROUS</b>: when false, the <code>gitBranch</code> will be replaced with single commit, and all its history will be dropped.
     * While this is useful for saving space, you should really make sure that you use a dedicated branch name (and not a code branch)!!!</p>
     */
    @Parameter(defaultValue = "true", property = "gitsite.keepHistory")
    boolean keepHistory;

    /**
     * Commit message to be used for publishing.
     * This is passed to {@link String#format(String, Object...)} where first parameter is number of published files; therefore, you can use '%d' placeholder to reference it.
     */
    @Parameter(defaultValue = "Publishing ${project.artifactId} site with %d files", property = "gitsite.commitMessage")
    String commitMessage;

    /**
     * Where to store log of git operations.
     */
    @Parameter(defaultValue = "${project.build.directory}/gitsite-deploy.log")
    File logfile;

    private static final String SCM_PREFIX = "scm:git:";

    private void validate() throws MojoExecutionException {
        if (!gitScmUrl.startsWith(SCM_PREFIX)) {
            throw new MojoExecutionException(String.format("gitScmUrl must start with prefix '%s'", SCM_PREFIX));
        }
    }

    @Override
    protected void executeInRootModule() throws MojoExecutionException, MojoFailureException {
        getLog().debug("ROOT MODULE - executionRootDirectory = " + executionRootDirectory);
        validate();
        saveParameters("inputDirectory", "gitBranch", "gitScmUrl", "keepHistory", "logfile", "commitMessage");
    }

    @Override
    protected void executeInLastModule() throws MojoExecutionException, MojoFailureException {
        getLog().debug("LAST MODULE - executionRootDirectory = " + executionRootDirectory);
        final File paramFile = loadParameters();
        gitSiteDeploy();
        if (!paramFile.delete()) {
            paramFile.deleteOnExit();
        }
    }

    private void gitSiteDeploy() throws MojoExecutionException, MojoFailureException {
        final String subdir = ".";
        final String workDir = inputDirectory.getAbsolutePath() + ".work";
        final ShellExecutor shell = getShellExecutor();
        final String gitRemoteUrl = gitScmUrl.substring(SCM_PREFIX.length());

        try {
            boolean pushForce = !keepHistory;

            // clone or init site.wc
            // - clone only if keepHistory or subdir
            boolean needClone = keepHistory || !subdir.equals(".");
            if (needClone) {
                final ShellExecutor.Result cloneResult = new ShellExecutor.Result();
                shell.execWithResult(cloneResult, "git", "clone", "--branch", gitBranch, "--single-branch", gitRemoteUrl, workDir);
                if (cloneResult.getExitCode() != 0) {
                    for (String line : cloneResult.getStderrLines()) {
                        final String lline = line.toLowerCase();
                        if (lline.contains("Could not find remote branch")
                            || lline.contains(" not found in upstream ")) {
                            getLog().info(String.format("Branch '%s' does not exist in '%s' - will be created",
                                gitBranch, gitRemoteUrl));
                            pushForce = true;
                            break;
                        }
                    }
                    if (!pushForce) {
                        throw new MojoExecutionException(String.format("git exited with code %d", cloneResult.getExitCode()));
                    }
                }
            } else {
                shell.exec("git", "init", workDir);
                shell.exec("git", "branch", "-M", "master", gitBranch);
                shell.exec("git", "remote", "add", "origin", gitRemoteUrl);
            }

            // move site to the subdir
            final File targetArea = new File(workDir, subdir).getCanonicalFile();
            final String excludes = ".git/**";
            final List<File> filesToDelete = FileUtils.getFiles(targetArea, null, excludes, true);
            for (File file : filesToDelete) {
                FileUtils.fileDelete(file.getAbsolutePath());
            }
            FileUtils.copyDirectoryStructure(inputDirectory, targetArea);

            // commit
            FileUtils.fileWrite(new File(workDir, ".gitattributes").getAbsolutePath(), "* text=auto\n");
            shell.exec("git", "add", "-A", ".");
            final int fileCount = FileUtils.getFiles(inputDirectory, null, null, false).size();
            shell.exec("git", "commit", "-am", String.format(commitMessage, fileCount));

            // push or push-force
            if (keepHistory) {
                shell.exec("git", "push");
            } else {
                shell.exec("git", "push", "origin", gitBranch, "--force", "--set-upstream");
            }

        } catch (CommandLineException e) {
            throw new MojoExecutionException("git publishing error", e);
        } catch (IOException e) {
            throw new MojoExecutionException("git publishing error", e);
        }
    }

    private void gitSiteDeployOld() throws MojoExecutionException, MojoFailureException {
        try {
            // perform the push
            final String gitRemoteUrl = gitScmUrl.substring(SCM_PREFIX.length());
            FileUtils.deleteDirectory(new File(inputDirectory, ".git"));
            FileUtils.fileWrite(new File(inputDirectory, ".gitattributes").getAbsolutePath(), "* text=auto\n");
            final int fileCount = FileUtils.getFiles(inputDirectory, null, null, false).size();
            final ShellExecutor shell = getShellExecutor();
            boolean pushForce = !keepHistory;
            final String origDir = inputDirectory.getAbsolutePath() + ".orig";
            if (keepHistory) {
                final ShellExecutor.Result cloneResult = new ShellExecutor.Result();
                shell.execWithResult(cloneResult, "git", "clone", "--branch", gitBranch, "--single-branch", gitRemoteUrl, origDir);
                if (cloneResult.getExitCode() != 0) {
                    for (String line : cloneResult.getStderrLines()) {
                        final String lline = line.toLowerCase();
                        if (lline.contains("Could not find remote branch")
                            || lline.contains(" not found in upstream ")) {
                            getLog().info(String.format("Branch '%s' does not exist in '%s' - will be created",
                                gitBranch, gitRemoteUrl));
                            pushForce = true;
                            break;
                        }
                    }
                    if (!pushForce) {
                        throw new MojoExecutionException(String.format("git exited with code %d", cloneResult.getExitCode()));
                    }
                }
            }
            if (!pushForce) {
                FileUtils.rename(new File(origDir, ".git"), new File(inputDirectory, ".git"));
                shell.exec("git", "add", "-A", ".");
                shell.exec("git", "commit", "-am", String.format(commitMessage, fileCount));
                shell.exec("git", "push", gitRemoteUrl);
            } else {
                shell.exec("git", "init");
                shell.exec("git", "remote", "add", "origin", gitRemoteUrl);
                shell.exec("git", "add", "-A", ".");
                shell.exec("git", "commit", "-am", String.format(commitMessage, fileCount));
                shell.exec("git", "push", "origin", "+master:" + gitBranch, "--set-upstream");
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("git publishing error", e);
        } catch (IOException e) {
            throw new MojoExecutionException("git publishing error", e);
        }
    }

    private ShellExecutor getShellExecutor() {
        final ShellExecutor shell = new ShellExecutor();
        shell.setInfo(new StreamConsumer() {
            public void consumeLine(String line) {
                getLog().info(line);
                filelog("INFO", line);
            }
        });
        shell.setStderr(new StreamConsumer() {
            public void consumeLine(String line) {
                getLog().warn(line);
                filelog("WARN", line);
            }
        });
        shell.setStdout(new StreamConsumer() {
            public void consumeLine(String line) {
                getLog().debug(line);
                filelog("DEBUG", line);
            }
        });
        shell.setWorkingDirectory(inputDirectory);
        return shell;
    }

    private void filelog(String severity, String line) {
        try {
            FileUtils.fileAppend(logfile.getAbsolutePath(), String.format("%6s %s%n", severity, line));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
