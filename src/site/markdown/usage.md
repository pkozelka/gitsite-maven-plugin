# Usage

You can use this plugin to publish your site on GitHub pages, by simply running the `site-deploy` phase.

In all cases, we need to configure our project so that normal deployment (using `project.distributionManagement.site` configuration) is skipped:

```
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-site-plugin</artifactId>
    <version>3.5</version>
    <configuration>
        <skipDeploy>true</skipDeploy>
    </configuration>
</plugin>
```


## Single-module project

Attach the gitsite's mojo into the `site-deploy` phase so that publishing is performed:

```
<plugin>
    <groupId>net.kozelka.maven</groupId>
    <artifactId>gitsite-maven-plugin</artifactId>
    <version>0.1.0</version>
    <executions>
        <execution>
            <id>gitsite-deploy</id>
            <phase>site-deploy</phase>
            <goals>
                <goal>deploy</goal>
            </goals>
            <configuration>
                <inputDirectory>${project.reporting.outputDirectory}</inputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Note the configuration of the `inputDirectory` parameter. It is necessary for

Now we can run the site deployment:

```
mvn site-deploy
```

### Usage for Github Pages

When publishing with Github Pages, remember to set the target branch to `gh-pages` in the plugin's configuration:

```
<configuration>
    ...
    <gitBranch>gh-pages</gitBranch>
    ...
</configuration>
```

## Multi-module project with docs on root only

can be configured the same way as single-module, but it's better to run its site deployment only on the top module:

```
mvn site-deploy --non-recursive
```

## Multi-module project

With multi-pom projects, we need to stage all the sites into one directory first.

By default, `maven-site-plugin:stage` uses the `target/staging` which is also the default for `gitsite:deploy`.

So we have to generate and stage the site first:

```
mvn site site:stage
```

and then deploy; the pom.xml configuration is very similar (no need to specify `inputDirectory` in this case):

```
<plugin>
    <groupId>net.kozelka.maven</groupId>
    <artifactId>gitsite-maven-plugin</artifactId>
    <version>0.1.0</version>
    <executions>
        <execution>
            <id>gitsite-deploy</id>
            <phase>site-deploy</phase>
            <goals>
                <goal>deploy</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Now you can deploy the site:

```
mvn site-deploy
```
