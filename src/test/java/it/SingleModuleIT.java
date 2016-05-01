package it;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.codehaus.plexus.util.cli.CommandLineException;

/**
 * Created by Petr Kozelka.
 */
public class SingleModuleIT {
    public static boolean prepare(File basedir, File localRepositoryPath, Map<String,String> context) throws CommandLineException, IOException {
        final File gitwc = new File(basedir, "target/gitsiterepo");
        IntegrationTestUtils.createGitRepo(gitwc, "singlemodule/gitsite");
        return true;
    }

    public static boolean verify(File basedir, File localRepositoryPath, Map<String,String> context) {
        return true;
    }
}
