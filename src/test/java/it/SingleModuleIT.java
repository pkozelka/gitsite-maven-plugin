package it;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import net.kozelka.utils.IntegrationTestUtils;
import org.codehaus.plexus.util.cli.CommandLineException;

/**
 * Created by Petr Kozelka.
 */
public class SingleModuleIT {
    public static boolean prepare(File basedir, File localRepositoryPath, Map<String,String> context, Map<String,String> scriptVariables) throws CommandLineException, IOException {
        IntegrationTestUtils.createGitRepo(new File(basedir, "target/gitsiterepo"));
        return true;
    }

    public static boolean verify(File basedir, File localRepositoryPath, Map<String,String> context, Map<String,String> scriptVariables) {
        return true;
    }
}
