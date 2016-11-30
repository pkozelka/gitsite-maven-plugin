# Site with multiple versions



## How to configure multiversion site

Following configuration uses some placeholders:

* **MyProjectContext**
* **MyProjectName**
* **MyDocServerUrl**

### Changes in `pom.xml`

This is essential for proper computation of relative paths in resulting site:

```
 <url>${docserver.url}/MyProjectContext${gitsite.subcontext}</url>
```

By default, we assume generating the site on "default" url.
And `docserver.url` needs to be defined.

```
    <properties>
        <docserver.url>MyDocServerUrl</docserver.url>
        <gitsite.subcontext/>
    </properties>
```


Configure the `subcontext` param to do the static site overlaying properly:

```
    <plugin>
        <groupId>net.kozelka.maven</groupId>
        <artifactId>gitsite-maven-plugin</artifactId>
        <configuration>
            <!-- ... here put your other config params ... -->
            <subcontext>${gitsite.subcontext}</subcontext>
        </configuration>
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


### Changes in `src/site/site.xml`

Links to all known (or published) major versions:

```
    <links>
        <item name="v1.1" title="Version 1.1" href="${docserver.url}/MyProjectContext/VERSION/1.1/"/>
        <item name="v1.2" title="Version 1.2" href="${docserver.url}/MyProjectContext/VERSION/1.2/"/>
        <item name="v1.3" title="Version 1.3" href="${docserver.url}/MyProjectContext/VERSION/1.3/"/>
        <item name="v2.0" title="Version 2.0" href="${docserver.url}/MyProjectContext/VERSION/2.0/"/>
    </links>
```

Note that the `/VERSION/` is a hardcoded prefix for a version subcontext; it's currently hardcoded (this may change later if needed).
It is treated specially in the mojo code, to avoid deleting versions by default version site. So, arbitrarily choosing different subcontext uri may have undesirable impact.

Contribute to breadcrumbs with project name and current version

```
    <breadcrumbs>
        <item name="MyProjectName" href="${docserver.url}/MyProjectContext/"/>
        <item name="${project.version}" href="index.html"/>
    </breadcrumbs>
```

### Branches

Here we assume that each major version is

- present in its own branch
- merged up (sooner or later) to the _newer_ major version

Therefore, we should make the above changes in the _oldest_ branch (version) that we want to support, and merge it up (with any adjustments if needed).

### Execution on CI

Eventually, the site needs to be generated and published. Here we employ the `gitsite.subcontext` parameter:

```
mvn clean site-deploy --non-recursive -Dgitsite.subcontext=/VERSION/1.1
```

Note that this builds *one* of the branches; must be executed on each branch in order to build them all, with the parameter value changed accordingly.

> TODO: More automation will be probably arriving here as creating separate build job for each major version is a bit messy.

### Default version

Even worse, we need to run a job that renders one of the major versions in the "default" context, i.e. without the `/VERSION/XXX` part in it.

