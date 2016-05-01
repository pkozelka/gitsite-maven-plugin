package net.kozelka.gitsite.mojo;

import java.io.File;
import java.util.List;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

/**
 * Created by Petr Kozelka.
 */
public class GitSiteDeployMojoTest {
    @Test
    public void test() throws Exception {
        final String excludes = "target/**,.idea/**,.git/**";
        final List<File> files = FileUtils.getFiles(new File("."), null, excludes);
        System.out.println("files.size() = " + files.size());
        for (File file : files) {
            System.out.println("file = " + file);
        }
    }
}
