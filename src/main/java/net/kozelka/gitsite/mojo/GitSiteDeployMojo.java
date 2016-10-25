package net.kozelka.gitsite.mojo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
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
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.SITE_DEPLOY)
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
     * Where to store log of shell executions.
     */
    @Parameter(defaultValue = "${project.build.directory}/gitsite-deploy.log")
    File logfile;

    /**
     * Subdirectory for placing the site's content.
     * <p>
     * Typically, we place content on the root. However, in some cases, we need to keep multiple static sites under one git-based hosting.
     * Sample use-cases are <b>multiple versions</b> and <b>Bitbucket hosting</b>.
     * </p>
     */
    @Parameter(defaultValue = "", property = "gitsite.subcontext")
    String subcontext;

    /**
     * Name of the file that will contain simple listing of subcontexts.
     * The listing is alphabetically sorted, line separated and each subcontext is listed only once.
     */
    @Parameter(defaultValue = ".gitsite.index.txt", property = "gitsite.index")
    String index;

    /**
     * Comma-separated list of roots for subdirs.
     */
    @Parameter(defaultValue = "VERSION,BRANCH", property = "gitsite.roots")
    String roots;

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
        final File workDir = new File(inputDirectory.getAbsolutePath() + ".work");
        workDir.mkdirs();
        final ShellExecutor shell = getShellExecutor();
        shell.setWorkingDirectory(workDir);
        final String gitRemoteUrl = gitScmUrl.substring(SCM_PREFIX.length());

        try {
            boolean pushForce = !keepHistory;
            subcontext = subcontext == null ? "" : subcontext;

            // clone or init site.wc
            // - clone only if keepHistory or subcontext
            boolean needClone = keepHistory || !subcontext.equals("");
            final String localBranch;
            if (needClone) {
                final ShellExecutor.Result cloneResult = new ShellExecutor.Result();
                shell.execWithResult(cloneResult, "git", "clone", "--branch", gitBranch, "--single-branch", gitRemoteUrl, ".");
                if (cloneResult.getExitCode() == 0) {
                    localBranch = gitBranch;
                } else {
                    localBranch = "master";
                    boolean networkProblem = true;
                    for (String line : cloneResult.getStderrLines()) {
                        final String lline = line.toLowerCase();
                        if (lline.contains("Could not find remote branch")
                            || lline.contains(" not found in upstream ")) {
                            getLog().info(String.format("Branch '%s' does not exist in '%s' - will be created",
                                gitBranch, gitRemoteUrl));
                            pushForce = true;
                            networkProblem = false;
                            break;
                        }
                    }
                    if (networkProblem) {
                        throw new MojoExecutionException(String.format("git exited with code %d", cloneResult.getExitCode()));
                    }
                }
            } else {
                shell.exec("git", "init");
                shell.exec("git", "remote", "add", "origin", gitRemoteUrl);
                localBranch = "master";
            }


            // move site to the subcontext
            final File targetArea = new File(workDir, subcontext).getCanonicalFile();
            targetArea.mkdirs();
            getLog().debug("Moving site into " + targetArea);
            final StringBuilder excludes = new StringBuilder(".git/**");
            //TODO: should we care about exclussions only when rendering the root site?
            for (StringTokenizer tok = new StringTokenizer(roots, ","); tok.hasMoreTokens(); ) {
                final String root = tok.nextToken();
                excludes.append(",");
                excludes.append(root);
                excludes.append("/**");
            }
            final List<File> filesToDelete = FileUtils.getFiles(targetArea.getAbsoluteFile(), null, excludes.toString());
            getLog().info(String.format("Deleting %d files - excluded '%s'", filesToDelete.size(), excludes));
            for (File file : filesToDelete) {
                getLog().debug("Deleting file: " + file);
                FileUtils.fileDelete(file.getAbsolutePath());
            }
            FileUtils.copyDirectoryStructure(inputDirectory, targetArea);

            // update subcontext index
            updateIndex(new File(workDir, index), subcontext);

            // commit
            FileUtils.fileWrite(new File(workDir, ".gitattributes").getAbsolutePath(), "* text=auto\n");
            shell.exec("git", "add", "-A", ".");
            final int fileCount = FileUtils.getFiles(inputDirectory, null, null, false).size();
            shell.exec("git", "commit", "-am", String.format(commitMessage, fileCount));

            // push or push-force
            if (pushForce) {
                shell.exec("git", "push", "origin", localBranch + ":" + gitBranch, "--force", "--set-upstream");
            } else {
                shell.exec("git", "push", "origin", localBranch + ":" + gitBranch);
            }

        } catch (CommandLineException e) {
            throw new MojoExecutionException("git publishing error", e);
        } catch (IOException e) {
            throw new MojoExecutionException("git publishing error", e);
        }
    }

    private static void updateIndex(File indexFile, String newSubcontext) throws IOException {
        final String content = indexFile.exists() ? FileUtils.fileRead(indexFile, "UTF-8") : "";
        final List<String> subdirIndex = new ArrayList<String>(Arrays.asList(content.split("\n")));
        subdirIndex.add(newSubcontext);
        System.out.println("newSubcontext = " + newSubcontext);
        System.out.println("subdirIndex = " + subdirIndex);
        Collections.sort(subdirIndex);
        final BufferedWriter wr = new BufferedWriter(new FileWriter(indexFile));
        try {
            for (String subcontext : subdirIndex) {
                if(subcontext.trim().length() == 0) continue;
                final File subdirVerify = new File(indexFile.getParentFile(), "." + subcontext).getCanonicalFile();
                if (subdirVerify.isDirectory()) {
                    wr.write(subcontext);
                    wr.newLine();
                } else {
                    System.err.printf("WARN: removing %s from index%n", subcontext);
                }
            }
        } finally {
            wr.close();
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
