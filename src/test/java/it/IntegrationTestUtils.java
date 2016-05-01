package it;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import net.kozelka.gitsite.utils.ShellExecutor;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;

class IntegrationTestUtils {

    static void createGitRepo(File gitwc, String branchToPrefill) throws CommandLineException, IOException {
        final ShellExecutor shell = new ShellExecutor();
        shell.exec("git", "init", gitwc.getAbsolutePath());
        final File helloTxtFile = new File(gitwc, "Hello.txt");
        FileUtils.fileWrite(helloTxtFile, "Hello world\n");
        shell.setWorkingDirectory(gitwc);
        shell.exec("git", "add", "-A");
        shell.exec("git", "commit", "-am", "initial commit");
        shell.exec("git", "branch", branchToPrefill);
        for (int i=1; i<=5; i++) {
            FileUtils.fileAppend(helloTxtFile.getAbsolutePath(),
                String.format("%s commit %d into %s%n",
                    new Date(), i, branchToPrefill));
        }
    }
}
