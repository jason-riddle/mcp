Title: Quarkus and Maven

URL Source: https://quarkus.io/guides/maven-tooling

Markdown Content:
[Edit this Page](https://github.com/quarkusio/quarkus/edit/main/docs/src/main/asciidoc/maven-tooling.adoc)

Use Maven to create a new project, add or remove extensions, launch development mode, debug your application, and build your application into a jar, native executable, or container-friendly executable. Import your project into your favorite IDE using Maven project metadata.

[](https://quarkus.io/guides/maven-tooling#project-creation)Creating a new project
----------------------------------------------------------------------------------

You can scaffold a new Maven project with:

For Windows users:

*   If using cmd, (don’t use backward slash `\` and put everything on the same line)

*   If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=my-artifactId"`

If you are using the CLI, you can get the list of available options with:

`quarkus create app --help`

If you are using the Maven command, the following table lists the attributes you can pass to the `create` command:

| Attribute | Default Value | Description |
| --- | --- | --- |
| `projectGroupId` | `org.acme.sample` | The group id of the created project |
| `projectArtifactId` | _mandatory_ | The artifact id of the created project. Not passing it triggers the interactive mode. |
| `projectVersion` | `1.0.0-SNAPSHOT` | The version of the created project |
| `platformGroupId` | `io.quarkus.platform` | The group id of the target platform. |
| `platformArtifactId` | `quarkus-bom` | The artifact id of the target platform BOM. |
| `platformVersion` | The version currently recommended by the [Quarkus Extension Registry](https://quarkus.io/guides/extension-registry-user) | The version of the platform you want the project to use. It can also accept a version range, in which case the latest from the specified range will be used. |
| `javaVersion` | 17 | The version of Java you want the project to use. |
| `className` | _Not created if omitted_ | The fully qualified name of the generated resource |
| `path` | `/hello` | The resource path, only relevant if `className` is set. |
| `extensions` | _[]_ | The list of extensions to add to the project (comma-separated) |
| `quarkusRegistryClient` | `true` | Whether Quarkus should use the online registry to resolve extension catalogs. If this is set to false, the extension catalog will be narrowed to the defined (or default) platform BOM. |

By default, the command will target the `io.quarkus.platform:quarkus-bom:3.24.2` platform release (unless the coordinates of the desired platform release have been specified).

The project is generated in a directory named after the passed artifactId. If the directory already exists, the generation fails.

A pair of Dockerfiles for native and jvm mode are also generated in `src/main/docker`. Instructions to build the image and run the container are written in those Dockerfiles.

[](https://quarkus.io/guides/maven-tooling#dealing-with-extensions)Dealing with extensions
------------------------------------------------------------------------------------------

From inside a Quarkus project, you can obtain a list of the available extensions with:

CLI

Maven

`quarkus extension`

`./mvnw quarkus:list-extensions`

You can add an extension using:

CLI

Maven

`quarkus extension add hibernate-validator`

`./mvnw quarkus:add-extension -Dextensions='hibernate-validator'`

Extensions are passed using a comma-separated list.

The extension name is the GAV name of the extension: e.g., `io.quarkus:quarkus-agroal`. However, you can pass a partial name, and Quarkus will do its best to find the right extension. For example, `agroal`, `Agroal`, or `agro` will expand to `io.quarkus:quarkus-agroal`. If no extension is found or more than one extension matches, you will see a red check mark ❌ in the command result.

```
$ ./mvnw quarkus:add-extensions -Dextensions=jdbc,agroal,non-exist-ent
[...]
❌ Multiple extensions matching 'jdbc'
     * io.quarkus:quarkus-jdbc-h2
     * io.quarkus:quarkus-jdbc-mariadb
     * io.quarkus:quarkus-jdbc-postgresql
     Be more specific e.g using the exact name or the full gav.
✅ Adding extension io.quarkus:quarkus-agroal
❌ Cannot find a dependency matching 'non-exist-ent', maybe a typo?
[...]
```

You can install all extensions which match a globbing pattern :

CLI

Maven

`quarkus extension add smallrye-*`

`./mvnw quarkus:add-extension -Dextensions='smallrye-*'`

[](https://quarkus.io/guides/maven-tooling#configuring-development-mode)Configuring `javac` options
---------------------------------------------------------------------------------------------------

The Quarkus Maven plugin makes use of `javac`, and by default it picks up compiler flags to pass to `javac` from `maven-compiler-plugin`.

If you need to customize the compiler flags used by the plugin, like in [development mode](https://quarkus.io/guides/maven-tooling#dev-mode), add a `configuration` section to the `plugin` block and set the `compilerArgs` property just as you would when configuring `maven-compiler-plugin`. You can also set `source`, `target`, and `jvmArgs`. For example, to pass `--enable-preview` to both the JVM and `javac`:

```
<plugin>
  <groupId>${quarkus.platform.group-id}</groupId>
  <artifactId>quarkus-maven-plugin</artifactId>
  <version>${quarkus.platform.version}</version>

  <configuration>
    <source>${maven.compiler.source}</source>
    <target>${maven.compiler.target}</target>
    <compilerArgs>
      <arg>--enable-preview</arg>
    </compilerArgs>
    <jvmArgs>--enable-preview</jvmArgs>
  </configuration>

  ...
</plugin>
```

Because the Quarkus Maven plugin itself runs in the JVM started by Maven, and because some (rare) Quarkus extensions need to load application classes during the build, it may be necessary to pass the same flags to the JVM running Maven.

CLI

Maven

`MAVEN_OPTS='--enable-preview' quarkus build`

`MAVEN_OPTS='--enable-preview' ./mvnw install`

**Alternatively**, you can simply create the file [`.mvn/jvm.config`](https://maven.apache.org/configure.html#mvn-jvm-config-file) at the root of your project: and any options you put in that file will be picked up by Maven, without having to set `MAVEN_OPTS`.

[](https://quarkus.io/guides/maven-tooling#dev-mode)Development mode
--------------------------------------------------------------------

Quarkus comes with a built-in development mode. Run your application with:

CLI

Maven

`quarkus dev`

`./mvnw quarkus:dev`

You can then update the application sources, resources and configurations. The changes are automatically reflected in your running application. This is great to do development spanning UI and database as you see changes reflected immediately.

Dev mode enables hot deployment with background compilation, which means that when you modify your Java files or your resource files and refresh your browser these changes will automatically take effect. This works too for resource files like the configuration property file. The act of refreshing the browser triggers a scan of the workspace, and if any changes are detected the Java files are compiled, and the application is redeployed, then your request is serviced by the redeployed application. If there are any issues with compilation or deployment an error page will let you know.

Hit `CTRL+C` to stop the application.

By default, `quarkus:dev` sets the debug host to `localhost` (for security reasons). If you need to change this, for example to enable debugging on all hosts, you can use the `-DdebugHost` option like so:

CLI

Maven

`quarkus dev -DdebugHost=0.0.0.0`

`./mvnw quarkus:dev -DdebugHost=0.0.0.0`

### [](https://quarkus.io/guides/maven-tooling#remote-development-mode)Remote Development Mode

It is possible to use development mode remotely, so that you can run Quarkus in a container environment (such as OpenShift) and have changes made to your local files become immediately visible.

This allows you to develop in the same environment you will actually run your app in, and with access to the same services.

Do not use this in production. This should only be used in a development environment. You should not run production application in dev mode.

To do this you must build a mutable application, using the `mutable-jar` format. Set the following properties in `application.properties`:

```
quarkus.package.jar.type=mutable-jar (1)
quarkus.live-reload.password=changeit (2)
quarkus.live-reload.url=http://my.cluster.host.com:8080 (3)
```

**1**This tells Quarkus to use the mutable-jar format. Mutable applications also include the deployment time parts of Quarkus, so they take up a bit more disk space. If run normally they start just as fast and use the same memory as an immutable application, however they can also be started in dev mode.
**2**The password that is used to secure communication between the remote side and the local side.
**3**The URL that your app is going to be running in dev mode at. This is only needed on the local side, so you may want to leave it out of the properties file and specify it as a system property on the command line.

The `mutable-jar` is then built in the same way that a regular Quarkus jar is built, i.e. by issuing:

CLI

Maven

`quarkus build`

`./mvnw install`

Before you start Quarkus on the remote host set the environment variable `QUARKUS_LAUNCH_DEVMODE=true`. If you are on bare metal you can set it via the `export QUARKUS_LAUNCH_DEVMODE=true` command and then run the application with the proper `java -jar …​` command to run the application.

If you plan on running the application via Docker, then you’ll need to add `-e QUARKUS_LAUNCH_DEVMODE=true` to the `docker run` command. When the application starts you should now see the following line in the logs: `Profile dev activated. Live Coding activated`. You will also need to give the application the rights to update the deployment resources by adding `RUN chmod o+rw -R /deployments` after the `COPY` commands into your Dockerfile. For security reasons, this option should not be added to the production Dockerfile.

The remote side does not need to include Maven or any other development tools. The normal `fast-jar` Dockerfile that is generated with a new Quarkus application is all you need. If you are using bare metal launch the Quarkus runner jar, do not attempt to run normal dev mode.

Now you need to connect your local agent to the remote host, using the `remote-dev` command:

`./mvnw quarkus:remote-dev -Dquarkus.live-reload.url=http://my-remote-host:8080`

Now every time you refresh the browser you should see any changes you have made locally immediately visible in the remote app. This is done via an HTTP based long polling transport, that will synchronize your local workspace and the remote application via HTTP calls.

If you do not want to use the HTTP feature then you can simply run the `remote-dev` command without specifying the URL. In this mode the command will continuously rebuild the local application, so you can use an external tool such as odo or rsync to sync to the remote application.

All the config options are shown below:

Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  | Type | Default |
| --- | --- | --- |
| [`quarkus.live-reload.enabled`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-live-reload-enabled) Whether the live-reload feature should be enabled. Environment variable: `QUARKUS_LIVE_RELOAD_ENABLED` Show more | boolean | `true` |
| [`quarkus.live-reload.instrumentation`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-live-reload-instrumentation) Whether Quarkus should enable its ability to not do a full restart when changes to classes are compatible with JVM instrumentation. If this is set to true, Quarkus will perform class redefinition when possible. Environment variable: `QUARKUS_LIVE_RELOAD_INSTRUMENTATION` Show more | boolean | `false` |
| [`quarkus.live-reload.watched-resources`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-live-reload-watched-resources) The names of additional resource files to watch for changes, triggering a reload on change. Directories are _not_ supported. Environment variable: `QUARKUS_LIVE_RELOAD_WATCHED_RESOURCES` Show more | list of string |  |
| [`quarkus.live-reload.password`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-live-reload-password) Password used to use to connect to the remote dev-mode application Environment variable: `QUARKUS_LIVE_RELOAD_PASSWORD` Show more | string |  |
| [`quarkus.live-reload.url`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-live-reload-url) URL used to use to connect to the remote dev-mode application Environment variable: `QUARKUS_LIVE_RELOAD_URL` Show more | string |  |
| [`quarkus.live-reload.connect-timeout`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-live-reload-connect-timeout) The amount of time to wait for a remote dev connect or reconnect Environment variable: `QUARKUS_LIVE_RELOAD_CONNECT_TIMEOUT` Show more | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html)[](https://quarkus.io/guides/maven-tooling#duration-note-anchor-quarkus-core_quarkus-live-reload) | `30S` |
| [`quarkus.live-reload.retry-interval`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-live-reload-retry-interval) The amount of time to wait between attempts when connecting to the server side of remote dev Environment variable: `QUARKUS_LIVE_RELOAD_RETRY_INTERVAL` Show more | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html)[](https://quarkus.io/guides/maven-tooling#duration-note-anchor-quarkus-core_quarkus-live-reload) | `2S` |
| [`quarkus.live-reload.retry-max-attempts`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-live-reload-retry-max-attempts) The maximum number of attempts when connecting to the server side of remote dev Environment variable: `QUARKUS_LIVE_RELOAD_RETRY_MAX_ATTEMPTS` Show more | int | `10` |

About the Duration format

You can also use a simplified format, starting with a number:

*   If the value is only a number, it represents time in seconds.

*   If the value is a number followed by `ms`, it represents time in milliseconds.

In other cases, the simplified format is translated to the `java.time.Duration` format for parsing:

*   If the value is a number followed by `h`, `m`, or `s`, it is prefixed with `PT`.

*   If the value is a number followed by `d`, it is prefixed with `P`.

It is recommended you use SSL when using remote dev mode, however even if you are using an unencrypted connection your password is never sent directly over the wire. For the initial connection request the password is hashed with the initial state data, and subsequent requests hash it with a random session id generated by the server and any body contents for POST requests, and the path for DELETE requests, as well as an incrementing counter to prevent replay attacks.

### [](https://quarkus.io/guides/maven-tooling#extension-provided-dev-mode-java-options)Extension provided Dev mode Java options

Some extensions may provide pre-configured Java options that should be added to the command line launching an application in Dev mode.

Let’s suppose there are couple of extensions `quarkus-blue` and `quarkus-red` in an application that provide Java options for Dev mode. The logs may look something like this

```
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] (main) Adding JVM options from org.acme:quarkus-red::jar
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] (main)   add-opens: [java.base/java.io=ALL-UNNAMED, java.base/java.nio=ALL-UNNAMED]
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] (main) Adding JVM options from org.acme:quarkus-blue::jar
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] (main)   enable-native-access: [ALL-UNNAMED]
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] (main)   add-modules: [jdk.incubator.vector]
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] (main)   enable-preview: []
[DEBUG] Launching JVM with command line: /home/<username>/jdk/bin/java -Dquarkus-internal.serialized-app-model.path=/home/<username>/app/target/quarkus/bootstrap/dev-app-model.dat -javaagent:/home/<username>/.m2/repository/io/quarkus/quarkus-class-change-agent/{quarkus-version}/quarkus-class-change-agent-{quarkus-version}.jar -XX:TieredStopAtLevel=1 -agentlib:jdwp=transport=dt_socket,address=localhost:5005,server=y,suspend=n --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --enable-native-access=ALL-UNNAMED --add-modules=jdk.incubator.vector --enable-preview -Djava.util.logging.manager=org.jboss.logmanager.LogManager -jar /home/<username>/app/target/acme-app-dev.jar
```

A user may choose to disable all the Java options provided by extensions by configuring `disableAll` parameter such as

```
<plugin>
                <groupId>${quarkus.platform.group-id}</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
                <version>${quarkus.platform.version}</version>
                <configuration>
                    <extensionJvmOptions>
                        <disableAll>true</disableAll>
                    </extensionJvmOptions>
                </configuration>
            </plugin>
```

Or disable Java options provided by specific extensions by configuring Maven coordinates patterns, such as

```
<plugin>
                <groupId>${quarkus.platform.group-id}</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
                <version>${quarkus.platform.version}</version>
                <configuration>
                    <extensionJvmOptions>
                        <disableFor>
                            <extension>org.acme:quarkus-red</extension>
                        </disableFor>
                    </extensionJvmOptions>
                </configuration>
            </plugin>
```

With this configuration the logs will look like

```
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] (main) Adding JVM options from org.acme:quarkus-blue::jar
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] (main)   enable-native-access: [ALL-UNNAMED]
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] (main)   add-modules: [jdk.incubator.vector]
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] (main)   enable-preview: []
[DEBUG] Launching JVM with command line: /home/<username>/jdk/bin/java -Dquarkus-internal.serialized-app-model.path=/home/<username>/app/target/quarkus/bootstrap/dev-app-model.dat -javaagent:/home/<username>/.m2/repository/io/quarkus/quarkus-class-change-agent/{quarkus-version}/quarkus-class-change-agent-{quarkus-version}.jar -XX:TieredStopAtLevel=1 -agentlib:jdwp=transport=dt_socket,address=localhost:5005,server=y,suspend=n --enable-native-access=ALL-UNNAMED --add-modules=jdk.incubator.vector --enable-preview -Djava.util.logging.manager=org.jboss.logmanager.LogManager -jar /home/<username>/app/target/acme-app-dev.jar
```

[](https://quarkus.io/guides/maven-tooling#debugging)Debugging
--------------------------------------------------------------

In development mode, Quarkus starts by default with debug mode enabled, listening to port `5005` without suspending the JVM.

This behavior can be changed by giving the `debug` system property one of the following values:

*   `false` - the JVM will start with debug mode disabled

*   `true` - The JVM is started in debug mode and will be listening on port `5005`

*   `client` - the JVM will start in client mode and attempt to connect to `localhost:5005`

*   `{port}` - The JVM is started in debug mode and will be listening on `{port}`

An additional system property `suspend` can be used to suspend the JVM, when launched in debug mode. `suspend` supports the following values:

*   `y` or `true` - The debug mode JVM launch is suspended

*   `n` or `false` - The debug mode JVM is started without suspending

You can also run a Quarkus application in debug mode with a suspended JVM using:

CLI

Maven

`quarkus dev -Dsuspend -Ddebug`

`./mvnw quarkus:dev -Dsuspend -Ddebug`

Then, attach your debugger to `localhost:5005`.

[](https://quarkus.io/guides/maven-tooling#import-in-your-ide)Import in your IDE
--------------------------------------------------------------------------------

Once you have a [project generated](https://quarkus.io/guides/maven-tooling#project-creation), you can import it in your favorite IDE. The only requirement is the ability to import a Maven project.

**Eclipse**

In Eclipse, click on: `File → Import`. In the wizard, select: `Maven → Existing Maven Project`. On the next screen, select the root location of the project. The next screen list the found modules; select the generated project and click on `Finish`. Done!

In a separated terminal, run:

CLI

Maven

`quarkus dev`

`./mvnw quarkus:dev`

and enjoy a highly productive environment.

**IntelliJ IDEA**

In IntelliJ IDEA:

1.   From inside IntelliJ IDEA select `File → New → Project From Existing Sources…​` or, if you are on the welcome dialog, select `Import project`.

2.   Select the project root

3.   Select `Import project from external model` and `Maven`

4.   Next a few times (review the different options if needed)

5.   On the last screen click on Finish

In a separated terminal or in the embedded terminal, run:

CLI

Maven

`quarkus dev`

`./mvnw quarkus:dev`

Enjoy!

**Apache NetBeans**

In NetBeans:

1.   Select `File → Open Project`

2.   Select the project root

3.   Click on `Open Project`

In a separated terminal or the embedded terminal, go to the project root and run:

CLI

Maven

`quarkus dev`

`./mvnw quarkus:dev`

Enjoy!

**Visual Studio Code**

Open the project directory in VS Code. If you have installed the Java Extension Pack (grouping a set of Java extensions), the project is loaded as a Maven project.

[](https://quarkus.io/guides/maven-tooling#logging-quarkus-application-build-classpath-tree)Logging Quarkus application build classpath tree
--------------------------------------------------------------------------------------------------------------------------------------------

Usually, dependencies of an application (which is a Maven project) could be displayed using `mvn dependency:tree` command. In case of a Quarkus application, however, this command will list only the runtime dependencies of the application. Given that the Quarkus build process adds deployment dependencies of the extensions used in the application to the original application classpath, it could be useful to know which dependencies and which versions end up on the build classpath. Luckily, the `quarkus` Maven plugin includes the `dependency-tree` goal which displays the build dependency tree for the application.

Executing `./mvnw quarkus:dependency-tree` on your project should result in an output similar to:

```
[INFO] --- quarkus-maven-plugin:3.24.2:dependency-tree (default-cli) @ getting-started ---
[INFO] org.acme:getting-started:jar:1.0.0-SNAPSHOT
[INFO] └─ io.quarkus:quarkus-resteasy-deployment:jar:3.24.2 (compile)
[INFO]    ├─ io.quarkus:quarkus-resteasy-server-common-deployment:jar:3.24.2 (compile)
[INFO]    │  ├─ io.quarkus:quarkus-core-deployment:jar:3.24.2 (compile)
[INFO]    │  │  ├─ commons-beanutils:commons-beanutils:jar:1.9.3 (compile)
[INFO]    │  │  │  ├─ commons-logging:commons-logging:jar:1.2 (compile)
[INFO]    │  │  │  └─ commons-collections:commons-collections:jar:3.2.2 (compile)
...
```

The goal accepts the following optional parameters:

*   `mode` - the default value is `prod`, i.e. the production build dependency tree. Alternatively, it accepts values `test` to display the test dependency tree and `dev` to display the dev mode dependency tree;

*   `outputFile` - specifies the file to persist the dependency tree to;

*   `appendOutput` - the default value is `false`, indicates whether the output to the command should be appended to the file specified with the `outputFile` parameter or it should be overridden.

[](https://quarkus.io/guides/maven-tooling#downloading-maven-artifact-dependencies-for-offline-development-and-testing)Downloading Maven artifact dependencies for offline development and testing
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

Quarkus extension dependencies are divided into the runtime extension dependencies that end up on the application runtime classpath and the deployment (or build time) extension dependencies that are resolved by Quarkus only at application build time to create the build classpath. Application developers are expected to express dependencies only on the runtime artifacts of Quarkus extensions. As a consequence, the deployment extension dependencies aren’t visible to Maven plugins that aren’t aware of the Quarkus extension dependency model, such as the `maven-dependency-plugin`, `go-offline-maven-plugin`, etc. That means those plugins can not be used to pre-download all the application dependencies to be able to build and test the application later in offline mode.

To enable the use-case of building and testing a Quarkus application offline, the `quarkus-maven-plugin` includes the `go-offline` goal that could be called from the command line like this:

`./mvnw quarkus:go-offline`

This goal will resolve all the runtime, build time, test and dev mode dependencies of the application downloading them to the configured local Maven repository.

[](https://quarkus.io/guides/maven-tooling#building-a-native-executable)Building a native executable
----------------------------------------------------------------------------------------------------

Native executables make Quarkus applications ideal for containers and serverless workloads.

Make sure to have `GRAALVM_HOME` configured and pointing to the latest release of GraalVM for JDK 21. Verify that your `pom.xml` has the proper `native` profile as shown in [Maven configuration](https://quarkus.io/guides/maven-tooling#build-tool-maven).

Create a native executable using:

CLI

Maven

`quarkus build --native`

`./mvnw install -Dnative`

A native executable will be present in `target/`.

To run Integration Tests on the native executable, make sure to have the proper [Maven plugin configured](https://quarkus.io/guides/maven-tooling#build-tool-maven) and launch the `verify` goal.

```
$ ./mvnw verify -Dnative
...
[quarkus-quickstart-runner:50955]     universe:     391.96 ms
[quarkus-quickstart-runner:50955]      (parse):     904.37 ms
[quarkus-quickstart-runner:50955]     (inline):   1,143.32 ms
[quarkus-quickstart-runner:50955]    (compile):   6,228.44 ms
[quarkus-quickstart-runner:50955]      compile:   9,130.58 ms
[quarkus-quickstart-runner:50955]        image:   2,101.42 ms
[quarkus-quickstart-runner:50955]        write:     803.18 ms
[quarkus-quickstart-runner:50955]      [total]:  33,520.15 ms
[INFO]
[INFO] --- maven-failsafe-plugin:2.22.0:integration-test (default) @ quarkus-quickstart-native ---
[INFO]
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.acme.quickstart.GreetingResourceIT
Executing [/Users/starksm/Dev/JBoss/Quarkus/starksm64-quarkus-quickstarts/getting-started-native/target/quarkus-quickstart-runner, -Dquarkus.http.port=8081, -Dtest.url=http://localhost:8081, -Dquarkus.log.file.path=target/quarkus.log]
2019-02-28 16:52:42,020 INFO  [io.quarkus] (main) Quarkus started in 0.007s. Listening on: http://localhost:8080
2019-02-28 16:52:42,021 INFO  [io.quarkus] (main) Installed features: [cdi, rest]
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.081 s - in org.acme.quickstart.GreetingResourceIT
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0

...
```

### [](https://quarkus.io/guides/maven-tooling#build-a-container-friendly-executable)Build a container friendly executable

The native executable will be specific to your operating system. To create an executable that will run in a container, use the following:

CLI

Maven

`quarkus build --native -Dquarkus.native.container-build=true`

`./mvnw install -Dnative -Dquarkus.native.container-build=true`

The produced executable will be a 64-bit Linux executable, so depending on your operating system, it may no longer be runnable. However, it’s not an issue as we are going to copy it to a Docker container. Note that in this case the build itself runs in a Docker container too, so you don’t need to have GraalVM installed locally.

By default, the native executable will be generated using the `quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21` Docker image.

If you want to build a native executable with a different Docker image (for instance to use a different GraalVM version), use the `-Dquarkus.native.builder-image=<image name>` build argument.

The list of the available Docker images can be found on [quay.io](https://quay.io/repository/quarkus/ubi9-quarkus-mandrel-builder-image?tab=tags). Be aware that a given Quarkus version might not be compatible with all the images available.

Starting from Quarkus 3.19, the _builder_ image is based on UBI 9, and thus requires an UBI 9 base image if you want to run the native executable in a container. You can switch back to UBI 8, by setting the `quarkus.native.builder-image` property to one of the available image from the [quay.io repository](https://quay.io/repository/quarkus/ubi-quarkus-mandrel-builder-image?tab=tags). For example ``quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21` is using UBI 8, and so the resulting native executable will be compatible with UBI 8 base images.

[](https://quarkus.io/guides/maven-tooling#build-tool-maven)Maven configuration
-------------------------------------------------------------------------------

If you have not used [project scaffolding](https://quarkus.io/guides/maven-tooling#project-creation), add the following elements in your `pom.xml`

```
<properties>
    <skipITs>true</skipITs> (1)
</properties>

<dependencyManagement>
    <dependencies>
        <dependency> (2)
            <groupId>${quarkus.platform.group-id}</groupId>
            <artifactId>quarkus-bom</artifactId>
            <version>${quarkus.platform.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<build>
    <plugins>
        <plugin> (3)
            <groupId>${quarkus.platform.group-id}</groupId>
            <artifactId>quarkus-maven-plugin</artifactId>
            <version>${quarkus.platform.version}</version>
            <extensions>true</extensions> (4)
            <executions>
                <execution>
                    <goals>
                        <goal>build</goal>
                        <goal>generate-code</goal>
                        <goal>generate-code-tests</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        <plugin> (5)
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>${surefire-plugin.version}</version>
            <configuration>
                <systemPropertyVariables>
                    <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                    <maven.home>${maven.home}</maven.home>
                </systemPropertyVariables>
            </configuration>
        </plugin>
        <plugin> (6)
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>${surefire-plugin.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                    <configuration>
                        <systemPropertyVariables>
                            <native.image.path>${project.build.directory}/${project.build.finalName}-runner</native.image.path>
                            <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                            <maven.home>${maven.home}</maven.home>
                        </systemPropertyVariables>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>

<profiles>
    <profile> (7)
        <id>native</id>
        <properties> (8)
            <quarkus.native.enabled>true</quarkus.native.enabled>
            <skipITs>false</skipITs> (9)
        </properties>
    </profile>
</profiles>
```

**1**Disable running of integration tests (test names `*IT` and annotated with `@QuarkusIntegrationTest`) on all builds. To run these tests all the time, either remove this property, set its value to `false`, or set `-DskipITs=false` on the command line when you run the build.

 As mentioned below, this is overridden in the `native` profile.
**2**Optionally use a BOM file to omit the version of the different Quarkus dependencies.
**3**Use the Quarkus Maven plugin that will hook into the build process.
**4**Enabling Maven plugin extensions will register a Quarkus `MavenLifecycleParticipant` which will make sure the Quarkus classloaders used during the build are properly closed. During the `generate-code` and `generate-code-tests` goals the Quarkus application bootstrap is initialized and re-used in the `build` goal (which actually builds and packages a production application). The Quarkus classloaders will be properly closed in the `build` goal of the `quarkus-maven-plugin`. However, if the build fails in between the `generate-code` or `generate-code-tests` and `build` then the Quarkus augmentation classloader won’t be properly closed, which may lead to locking of JAR files that happened to be on the classpath on Windows OS.
**5**Add system properties to `maven-surefire-plugin`.

`maven.home` is only required if you have custom configuration in `${maven.home}/conf/settings.xml`.
**6**If you want to test the artifact produced by your build with Integration Tests, add the following plugin configuration. Test names `*IT` and annotated with `@QuarkusIntegrationTest` will be run against the artifact produced by the build (JAR file, container image, etc). See the [Integration Testing guide](https://quarkus.io/guides/getting-started-testing#quarkus-integration-test) for more info.

`maven.home` is only required if you have custom configuration in `${maven.home}/conf/settings.xml`.
**7**Use a specific `native` profile for native executable building.
**8**Enable the `native` package type. The build will therefore produce a native executable.
**9**Always run integration tests when building a native image (test names `*IT` and annotated with `@QuarkusIntegrationTest`).

 If you do not wish to run integration tests when building a native image, simply remove this property altogether or set its value to `true`.

### [](https://quarkus.io/guides/maven-tooling#fast-jar)Using fast-jar

`fast-jar` is the default quarkus package type.

The result of the build is a directory under `target` named `quarkus-app`.

You can run the application using: `java -jar target/quarkus-app/quarkus-run.jar`.

In order to successfully run the produced jar, you need to have the entire contents of the `quarkus-app` directory. If any of the files are missing, the application will not start or might not function correctly.

The `fast-jar` packaging results in creating an artifact that starts a little faster and consumes slightly less memory than a legacy Quarkus jar because it has indexed information about which dependency jar contains classes and resources. It can thus avoid the lookup into potentially every jar on the classpath that the legacy jar necessitates, when loading a class or resource.

### [](https://quarkus.io/guides/maven-tooling#uber-jar-maven)Uber-Jar Creation

Quarkus Maven plugin supports the generation of Uber-Jars by specifying a `quarkus.package.jar.type=uber-jar` configuration option in your `application.properties` (or `<quarkus.package.jar.type>uber-jar</quarkus.package.jar.type>` in your `pom.xml`).

The original jar will still be present in the `target` directory, but it will be renamed to contain the `.original` suffix.

When building an Uber-Jar you can specify entries that you want to exclude from the generated jar by using the `quarkus.package.ignored-entries` configuration option, this takes a comma separated list of entries to ignore.

Uber-Jar creation by default excludes [signature files](https://docs.oracle.com/javase/tutorial/deployment/jar/intro.html) that might be present in the dependencies of the application.

Uber-Jar’s final name is configurable via a Maven’s build settings `finalName` option.

#### [](https://quarkus.io/guides/maven-tooling#uber-jar-file-name-suffix)Uber-Jar file name suffix

By default the generated uber JAR file name will have the `-runner` suffix, unless it was overridden by configuring a custom one with `quarkus.package.jar.runner-suffix` configuration option. If the runner suffix is not desired, it can be disabled by setting `quarkus.package.jar.add-runner-suffix` configuration option to `false`, in which case the uber JAR will replace the original JAR file generated by `maven-jar-plugin` for the application module.

#### [](https://quarkus.io/guides/maven-tooling#attaching-uber-jar-file-as-the-main-project-artifact)Attaching Uber-Jar file as the main project artifact

As long as an Uber-Jar file name is created by appending a suffix, such as `runner`, to the original project JAR file name, the Uber-Jar file name suffix will also be used as the Maven artifact classifier for the Uber-Jar artifact. There are two ways to attach an Uber-Jar as the main project artifact (without the classifier):

1.   set `quarkus.package.jar.add-runner-suffix=false`, which will disable the addition of the file name suffix and, by doing that, will replace the original project JAR on the file system;

2.   set `attachRunnerAsMainArtifact` parameter of the `quarkus:build` goal to `true`.

### [](https://quarkus.io/guides/maven-tooling#multi-module-maven)Working with multi-module projects

By default, Quarkus will not discover CDI beans inside another module.

The best way to enable CDI bean discovery for a module in a multi-module project would be to include the `jandex-maven-plugin`, unless it is the main application module already configured with the quarkus-maven-plugin, in which case it will be indexed automatically.

```
<build>
  <plugins>
    <plugin>
      <groupId>io.smallrye</groupId>
      <artifactId>jandex-maven-plugin</artifactId>
      <version>3.3.1</version>
      <executions>
        <execution>
          <id>make-index</id>
          <goals>
            <goal>jandex</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

More information on this topic can be found on the [Bean Discovery](https://quarkus.io/guides/cdi-reference#bean_discovery) section of the CDI guide.

### [](https://quarkus.io/guides/maven-tooling#maven-test-configuration)Maven test plugin configuration

`maven-surefire-plugin` and `maven-failsafe-plugin` configurations showed above will work in most cases. However, there could be cases when extra configuration will be required.

The reason is that, Quarkus may need to re-resolve application dependencies during the test phase to set up the test classpath for the tests. The original Maven resolver used in previous build phases will not be available in the test process and, as a conseqence, Quarkus will need to initialize a new one. To make sure the new resolver is initialized correctly, the relevant configuration options will need to be passed to the test process.

#### [](https://quarkus.io/guides/maven-tooling#maven-user-settings)Maven user settings

A path to the Maven user settings file may need to be passed to test processes, for example, in case the Maven build process was not launched using the default `mvn` scripts included in the Maven distribution. It could be done in the following way:

```
<plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>${surefire-plugin.version}</version>
            <configuration>
                <systemPropertyVariables>
                    <!-- skip -->
                    <maven.settings>${session.request.userSettingsFile.absolutePath}</maven.settings>
                </systemPropertyVariables>
            </configuration>
        </plugin>
```

#### [](https://quarkus.io/guides/maven-tooling#remote-repository-access-through-authenticated-https)Remote repository access through authenticated HTTPS

In case a remote Maven repository requires [authenticated HTTPS access configuration](https://maven.apache.org/guides/mini/guide-repository-ssl.html), some or all of the following properties will need to be passed to the test plugins:

```
<plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>${surefire-plugin.version}</version>
            <configuration>
                <systemPropertyVariables>
                    <!-- skip -->
                    <javax.net.ssl.keyStoreType>${javax.net.ssl.keyStoreType}</javax.net.ssl.keyStoreType>
                    <javax.net.ssl.keyStore>${javax.net.ssl.keyStore}</javax.net.ssl.keyStore>
                    <javax.net.ssl.keyStorePassword>${javax.net.ssl.keyStorePassword}</javax.net.ssl.keyStorePassword>
                    <javax.net.ssl.trustStore>${javax.net.ssl.trustStore}</javax.net.ssl.trustStore>
                    <javax.net.ssl.trustStorePassword>${javax.net.ssl.trustStorePassword}</javax.net.ssl.trustStorePassword>
                </systemPropertyVariables>
            </configuration>
        </plugin>
```

### [](https://quarkus.io/guides/maven-tooling#maven-configuration-profile)Building with a specific configuration profile

Quarkus supports [configuration profiles](https://quarkus.io/guides/config-reference#profiles) in order to provide a specific configuration according to the target environment.

The profile can be provided directly in the Maven build’s command thanks to the system property `quarkus.profile` with a command of type:

CLI

Maven

`quarkus build quarkus deploy openshift`

`./mvnw install -Dquarkus.profile=profile-name-here`

However, it is also possible to specify the profile directly in the POM file of the project using project properties, the Quarkus Maven plugin configuration properties or system properties set in the Quarkus Maven plugin configuration.

In order of precedence (greater precedence first):

1. System properties set in the Quarkus Maven plugin configuration

```
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  ...
  <build>
    <plugins>
      ...
      <plugin>
        <groupId>${quarkus.platform.group-id}</groupId>
        <artifactId>quarkus-maven-plugin</artifactId>
        <version>${quarkus.platform.version}</version>
        <extensions>true</extensions>
        <configuration>
          <systemProperties>
            <quarkus.profile>prod-aws</quarkus.profile> (1)
          </systemProperties>
        </configuration>
     </plugin>
     ...
    </plugins>
  </build>
...
</project>
```

**1**The default configuration profile of this project is `prod-aws`.

2. Quarkus Maven plugin configuration properties

```
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  ...
  <build>
    <plugins>
      ...
      <plugin>
        <groupId>${quarkus.platform.group-id}</groupId>
        <artifactId>quarkus-maven-plugin</artifactId>
        <version>${quarkus.platform.version}</version>
        <extensions>true</extensions>
        <configuration>
          <properties>
            <quarkus.profile>prod-aws</quarkus.profile> (1)
          </properties>
        </configuration>
     </plugin>
     ...
    </plugins>
  </build>
...
</project>
```

**1**The default configuration profile of this project is `prod-aws`.

3. Project properties

```
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  ...
  <properties>
    <quarkus.profile>prod-aws</quarkus.profile> (1)
    ...
  </properties>
...
</project>
```

**1**The default configuration profile of this project is `prod-aws`.

Whatever the approach is chosen, the profile can still be overridden with the `quarkus.profile` system property or the `QUARKUS_PROFILE` environment variable.

### [](https://quarkus.io/guides/maven-tooling#maven-multi-build)Building several artifacts from a single module

In some particular use cases, it can be interesting to build several artifacts of your application from the same module. A typical example is when you want to build your application with different configuration profiles.

In that case, it is possible to add as many executions as needed to the Quarkus Maven plugin configuration.

Below is an example of a Quarkus Maven plugin configuration that will produce two builds of the same application: one using the `prod-oracle` profile and the other one using the `prod-postgresql` profile.

```
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  ...
  <build>
    <plugins>
      ...
      <plugin>
        <groupId>${quarkus.platform.group-id}</groupId>
        <artifactId>quarkus-maven-plugin</artifactId>
        <version>${quarkus.platform.version}</version>
        <extensions>true</extensions>
        <executions>
          <execution>
            <id>oracle</id>
            <goals>
              <goal>build</goal>
            </goals>
            <configuration>
              <properties>
                <quarkus.profile>prod-oracle</quarkus.profile> (1)
                <quarkus.package.output-directory>oracle-quarkus-app</quarkus.package.output-directory> (2)
              </properties>
            </configuration>
          </execution>
          <execution>
            <id>postgresql</id>
            <goals>
              <goal>build</goal>
            </goals>
            <configuration>
              <properties>
                <quarkus.profile>prod-postgresql</quarkus.profile> (3)
                <quarkus.package.output-directory>postgresql-quarkus-app</quarkus.package.output-directory> (4)
              </properties>
            </configuration>
          </execution>
        </executions>
     </plugin>
     ...
    </plugins>
  </build>
...
</project>
```

**1**The default configuration profile of the first execution of the plugin is `prod-oracle`.
**2**The output directory of the first execution of the plugin is set to `oracle-quarkus-app` instead of `quarkus-app` to have a dedicated directory.
**3**The default configuration profile of the second execution of the plugin is `prod-postgresql`.
**4**The output directory of the second execution of the plugin is set to `postgresql-quarkus-app` instead of `quarkus-app` to have a dedicated directory.

With the configuration above, both profile builds will be using the same dependencies, so if we added dependencies on the Oracle and PostgreSQL drivers to the application, both of the drivers will appear in both builds.

To isolate profile-specific dependencies from other profiles, the JDBC drivers could be added as optional dependencies to the application but configured to be included in each profile that requires them, e.g.:

```
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  ...
  <dependencies>
    ...
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>${postgresql.driver.version}</version>
      <optional>true</optional> (1)
    </dependency>
  </dependencies>
  <build>
    <plugins>
      ...
      <plugin>
        <groupId>${quarkus.platform.group-id}</groupId>
        <artifactId>quarkus-maven-plugin</artifactId>
        <version>${quarkus.platform.version}</version>
        <extensions>true</extensions>
        <executions>
          ...
          <execution>
            <id>postgresql</id>
            <goals>
              <goal>build</goal>
            </goals>
            <configuration>
              <properties>
                <quarkus.profile>prod-postgresql</quarkus.profile>
                <quarkus.package.output-directory>postgresql-quarkus-app</quarkus.package.output-directory>
                <quarkus.package.jar.filter-optional-dependencies>true</quarkus.package.jar.filter-optional-dependencies> (2)
                <quarkus.package.jar.included-optional-dependencies>org.postgresql:postgresql::jar</quarkus.package.jar.included-optional-dependencies> (3)
              </properties>
            </configuration>
          </execution>
        </executions>
     </plugin>
     ...
    </plugins>
  </build>
...
</project>
```

**1**The JDBC driver of PostgreSQL is defined as an optional dependency
**2**For backward compatibility reasons, it is necessary to explicitly indicate that the optional dependencies need to be filtered.
**3**Only the optional dependency corresponding to the JDBC driver of PostgreSQL is expected in the final artifact.

If you have more than one optional dependency to declare in the `quarkus.package.jar.included-optional-dependencies` tag, make sure they are separated with `,` (e.g. `org.postgresql:postgresql::jar,com.foo:bar::jar`).

[](https://quarkus.io/guides/maven-tooling#configuration-reference)Configuring the Project Output
-------------------------------------------------------------------------------------------------

There are a several configuration options that will define what the output of your project build will be. These are provided in `application.properties` the same as any other config property.

The properties are shown below:

Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  | Type | Default |
| --- | --- | --- |
| [`quarkus.package.jar.enabled`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-enabled) If set to false, no JAR will be produced. Environment variable: `QUARKUS_PACKAGE_JAR_ENABLED` Show more | boolean | `true` |
| [`quarkus.package.jar.type`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-type) The JAR output type to use. Environment variable: `QUARKUS_PACKAGE_JAR_TYPE` Show more | `fast-jar`The "fast JAR" packaging type., `uber-jar`The "Uber-JAR" packaging type., `mutable-jar`The "mutable JAR" packaging type (for remote development mode)., `legacy-jar`The "legacy JAR" packaging type. This corresponds to the packaging type used in Quarkus before version 1.12. | `fast-jar`The "fast JAR" packaging type. |
| [`quarkus.package.jar.compress`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-compress) Whether the created jar will be compressed. This setting is not used when building a native image Environment variable: `QUARKUS_PACKAGE_JAR_COMPRESS` Show more | boolean | `true` |
| [`quarkus.package.jar.manifest.add-implementation-entries`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-manifest-add-implementation-entries) Specify whether the `Implementation` information should be included in the runner jar’s MANIFEST.MF. Environment variable: `QUARKUS_PACKAGE_JAR_MANIFEST_ADD_IMPLEMENTATION_ENTRIES` Show more | boolean | `true` |
| [`quarkus.package.jar.manifest.attributes."attribute-name"`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-manifest-attributes-attribute-name) Custom manifest attributes to be added to the main section of the MANIFEST.MF file. An example of the user defined property: quarkus.package.jar.manifest.attributes."Entry-key1"=Value1 quarkus.package.jar.manifest.attributes."Entry-key2"=Value2 Environment variable: `QUARKUS_PACKAGE_JAR_MANIFEST_ATTRIBUTES__ATTRIBUTE_NAME_` Show more | Map<String,String> |  |
| [`quarkus.package.jar.manifest.sections."section-name"`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-manifest-sections-section-name) Custom manifest sections to be added to the MANIFEST.MF file. An example of the user defined property: quarkus.package.jar.manifest.sections."Section-Name"."Entry-Key1"=Value1 quarkus.package.jar.manifest.sections."Section-Name"."Entry-Key2"=Value2 Environment variable: `QUARKUS_PACKAGE_JAR_MANIFEST_SECTIONS__SECTION_NAME_` Show more | Map<String,Map<String,String>> |  |
| [`quarkus.package.jar.user-configured-ignored-entries`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-user-configured-ignored-entries) Files that should not be copied to the output artifact. Environment variable: `QUARKUS_PACKAGE_JAR_USER_CONFIGURED_IGNORED_ENTRIES` Show more | list of string |  |
| [`quarkus.package.jar.included-optional-dependencies`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-included-optional-dependencies) List of all the dependencies that have been defined as optional to include into the final package of the application. Each optional dependency needs to be expressed in the following format: `groupId:artifactId[:[classifier][:[type]]]` With the classifier and type being optional (note that the brackets (`[]`) denote optionality and are not a part of the syntax specification). The group ID and artifact ID must be present and non-empty. If the type is missing, the artifact is assumed to be of type `jar`. This parameter is optional; if absent, no optional dependencies will be included into the final package of the application. For backward compatibility reasons, this parameter is ignored by default and can be enabled by setting the parameter `quarkus.package.jar.filter-optional-dependencies` to `true`. This parameter is meant to be used in modules where multi-builds have been configured to avoid getting a final package with unused dependencies. Environment variable: `QUARKUS_PACKAGE_JAR_INCLUDED_OPTIONAL_DEPENDENCIES` Show more | list of GACT |  |
| [`quarkus.package.jar.filter-optional-dependencies`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-filter-optional-dependencies) Flag indicating whether the optional dependencies should be filtered out or not. This parameter is meant to be used in modules where multi-builds have been configured to avoid getting a final package with unused dependencies. Environment variable: `QUARKUS_PACKAGE_JAR_FILTER_OPTIONAL_DEPENDENCIES` Show more | boolean | `false` |
| [`quarkus.package.jar.add-runner-suffix`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-add-runner-suffix) Indicates whether the generated JAR file should have the runner suffix appended. Only applicable to the `JarType#UBER_JAR uber-JAR output type`. If disabled, the JAR built by the original build system (Maven, Gradle, etc.) will be replaced with the Quarkus-built uber-JAR. Environment variable: `QUARKUS_PACKAGE_JAR_ADD_RUNNER_SUFFIX` Show more | boolean | `true` |
| [`quarkus.package.jar.appcds.enabled`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-appcds-enabled) Whether to automate the creation of AppCDS. Care must be taken to use the same exact JVM version when building and running the application. Environment variable: `QUARKUS_PACKAGE_JAR_APPCDS_ENABLED` Show more | boolean | `false` |
| [`quarkus.package.jar.appcds.builder-image`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-appcds-builder-image) When AppCDS generation is enabled, if this property is set, then the JVM used to generate the AppCDS file will be the JVM present in the container image. The builder image is expected to have the 'java' binary on its PATH. This flag is useful when the JVM to be used at runtime is not the same exact JVM version as the one used to build the jar. Note that this property is consulted only when `quarkus.package.jar.appcds.enabled=true` and it requires having docker available during the build. Environment variable: `QUARKUS_PACKAGE_JAR_APPCDS_BUILDER_IMAGE` Show more | string |  |
| [`quarkus.package.jar.appcds.use-container`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-appcds-use-container) Whether creation of the AppCDS archive should run in a container if available. Normally, if either a suitable container image to use to create the AppCDS archive can be determined automatically or if one is explicitly set using the `quarkus.<package-type>.appcds.builder-image` setting, the AppCDS archive is generated by running the JDK contained in the image as a container. If this option is set to `false`, a container will not be used to generate the AppCDS archive. Instead, the JDK used to build the application is also used to create the archive. Note that the exact same JDK version must be used to run the application in this case. Ignored if `quarkus.package.jar.appcds.enabled` is set to `false`. Environment variable: `QUARKUS_PACKAGE_JAR_APPCDS_USE_CONTAINER` Show more | boolean | `true` |
| [`quarkus.package.jar.appcds.use-aot`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-appcds-use-aot) Whether to use [Ahead-of-Time Class Loading & Linking](https://openjdk.org/jeps/483) introduced in JDK 24. Environment variable: `QUARKUS_PACKAGE_JAR_APPCDS_USE_AOT` Show more | boolean | `false` |
| [`quarkus.package.jar.user-providers-directory`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-user-providers-directory) This is an advanced option that only takes effect for development mode. If this is specified a directory of this name will be created in the jar distribution. Users can place jar files in this directory, and when re-augmentation is performed these will be processed and added to the class-path. Note that before reaugmentation has been performed these jars will be ignored, and if they are updated the app should be reaugmented again. Environment variable: `QUARKUS_PACKAGE_JAR_USER_PROVIDERS_DIRECTORY` Show more | string |  |
| [`quarkus.package.jar.include-dependency-list`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-include-dependency-list) If this option is true then a list of all the coordinates of the artifacts that made up this image will be included in the quarkus-app directory. This list can be used by vulnerability scanners to determine if your application has any vulnerable dependencies. Only supported for the `JarType#FAST_JAR fast JAR` and `JarType#MUTABLE_JAR mutable JAR` output types. Environment variable: `QUARKUS_PACKAGE_JAR_INCLUDE_DEPENDENCY_LIST` Show more | boolean | `true` |
| [`quarkus.package.jar.decompiler.enabled`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-decompiler-enabled) Enable decompilation of generated and transformed bytecode into a filesystem. Environment variable: `QUARKUS_PACKAGE_JAR_DECOMPILER_ENABLED` Show more | boolean | `false` |
| [`quarkus.package.jar.decompiler.output-directory`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-decompiler-output-directory) The directory into which to save the decompilation output. A relative path is understood as relative to the build directory. Environment variable: `QUARKUS_PACKAGE_JAR_DECOMPILER_OUTPUT_DIRECTORY` Show more | string | `decompiler` |
| [`quarkus.package.jar.decompiler.jar-directory`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-jar-decompiler-jar-directory) The directory into which to save the decompilation tool if it doesn’t exist locally. Environment variable: `QUARKUS_PACKAGE_JAR_DECOMPILER_JAR_DIRECTORY` Show more | string | `${user.home}/.quarkus` |
| [`quarkus.package.main-class`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-main-class) The entry point of the application. This can either be a fully qualified name of a standard Java class with a main method, or `io.quarkus.runtime.QuarkusApplication`. If your application has main classes annotated with `io.quarkus.runtime.annotations.QuarkusMain` then this can also reference the name given in the annotation, to avoid the need to specify fully qualified names in the config. Environment variable: `QUARKUS_PACKAGE_MAIN_CLASS` Show more | string |  |
| [`quarkus.package.output-directory`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-output-directory) The directory into which the output package(s) should be written. Relative paths are resolved from the build systems target directory. Environment variable: `QUARKUS_PACKAGE_OUTPUT_DIRECTORY` Show more | path |  |
| [`quarkus.package.output-name`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-output-name) The name of the final artifact, excluding the suffix and file extension. Environment variable: `QUARKUS_PACKAGE_OUTPUT_NAME` Show more | string |  |
| [`quarkus.package.write-transformed-bytecode-to-build-output`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-write-transformed-bytecode-to-build-output) Setting this switch to `true` will cause Quarkus to write the transformed application bytecode to the build tool’s output directory. This is useful for post-build tools that need to scan the application bytecode (for example, offline code-coverage tools). For example, if using Maven, enabling this feature will result in the classes in `target/classes` being replaced with classes that have been transformed by Quarkus. Setting this to `true`, however, should be done with a lot of caution and only if subsequent builds are done in a clean environment (i.e. the build tool’s output directory has been completely cleaned). Environment variable: `QUARKUS_PACKAGE_WRITE_TRANSFORMED_BYTECODE_TO_BUILD_OUTPUT` Show more | boolean | `false` |
| [`quarkus.package.runner-suffix`](https://quarkus.io/guides/maven-tooling#quarkus-core_quarkus-package-runner-suffix) The suffix that is applied to the runner artifact’s base file name. Environment variable: `QUARKUS_PACKAGE_RUNNER_SUFFIX` Show more | string | `-runner` |

### [](https://quarkus.io/guides/maven-tooling#custom-test-configuration-profile)Custom test configuration profile in JVM mode

By default, Quarkus tests in JVM mode are run using the `test` configuration profile. If you are not familiar with Quarkus configuration profiles, everything you need to know is explained in the [Configuration Profiles Documentation](https://quarkus.io/guides/config-reference#profiles).

It is however possible to use a custom configuration profile for your tests with the Maven Surefire and Maven Failsafe configurations shown below. This can be useful if you need for example to run some tests using a specific database which is not your default testing database.

```
<project>
  [...]
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>${surefire-plugin.version}</version>
        <configuration>
          <systemPropertyVariables>
            <quarkus.test.profile>foo</quarkus.test.profile> (1)
            <buildDirectory>${project.build.directory}</buildDirectory>
            [...]
          </systemPropertyVariables>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-failsafe-plugin</artifactId>
        <version>${failsafe-plugin.version}</version>
        <configuration>
          <systemPropertyVariables>
            <quarkus.test.profile>foo</quarkus.test.profile> (1)
            <buildDirectory>${project.build.directory}</buildDirectory>
            [...]
          </systemPropertyVariables>
        </configuration>
      </plugin>
    </plugins>
  </build>
  [...]
</project>
```

**1**The `foo` configuration profile will be used to run the tests.

It is not possible to use a custom test configuration profile in native mode for now. Native tests are always run using the `prod` profile.

### [](https://quarkus.io/guides/maven-tooling#bootstrap-maven-properties)Bootstrap Maven properties

Quarkus bootstrap includes a Maven resolver implementation that is used to resolve application runtime and build time dependencies. The Quarkus Maven resolver is initialized from the same Maven command line that launched the build, test or dev mode. Typically, there is no need to add any extra configuration for it. However, there could be cases where an extra configuration option may be necessary to properly resolve application dependencies in test or dev modes, or IDEs.

Maven test plugins (such as `surefire` and `failsafe`), for example, are not propagating build system properties to the running tests by default. Which means some system properties set by the Maven CLI aren’t available for the Quarkus Maven resolver initialized for the tests, which may result in test dependencies being resolved using different settings than the main Maven build.

Here is a list of system properties the Quarkus bootstrap Maven resolver checks during its initialization.

| Property name | Default Value | Description |
| --- | --- | --- |
| `maven.home` | `MAVEN_HOME` envvar | The Maven home dir is used to resolve the global settings file unless it was explicitly provided on the command line with the `-gs` argument |
| `maven.settings` | `~/.m2/settings.xml` | Unless the custom settings file has been provided with the `-s` argument, this property can be used to point the resolver to a custom Maven settings file |
| `maven.repo.local` | `~/.m2/repository` | This property could be used to configure a custom local Maven repository directory, if it is different from the default one and the one specified in the `settings.xml` |
| `maven.top-level-basedir` | none | This property may be useful to help the Maven resolver identify the top-level Maven project in the workspace. By default, the Maven resolver will be discovering a project’s workspace by navigating the parent-module POM relationship. However, there could be project layouts that are using an aggregator module which isn’t appearing as the parent for its modules. In this case, this property will help the Quarkus Maven resolver to properly discover the workspace. |
| `quarkus.bootstrap.effective-model-builder` | `false` | By default, the Quarkus Maven resolver is reading project’s POMs directly when discovering the project’s layout. While in most cases it works well enough and relatively fast, reading raw POMs has its limitation. E.g. if a POM includes modules in a profile, these modules will not be discovered. This system property enables project’s layout discovery based on the effective POM models, that are properly interpolated, instead of the raw ones. The reason this option is not enabled by default is it may appear to be significantly more time-consuming that could increase, e.g. CI testing times. Until there is a better approach found that could be used by default, projects that require it should enable this option. |
| `quarkus.bootstrap.legacy-model-resolver` | `false` | This **system** or **POM** property can be used to enable the legacy `ApplicationModel` resolver implementation. The property was introduced in Quarkus 3.19.0 and will be removed once the legacy implementation is known to be not in demand. |

These system properties above could be added to, e.g., a `surefire` and/or `failsafe` plugin configuration as

```
<project>
  [...]
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>${surefire-plugin.version}</version>
        <configuration>
          <systemPropertyVariables>
            <maven.home>${maven.home}</maven.home> (1)
            <maven.repo.local>${settings.localRepository}</maven.repo.local> (2)
            <maven.settings>${session.request.userSettingsFile.path}</maven.settings> (3)
            <maven.top-level-basedir>${session.topLevelProject.basedir.absolutePath}</maven.top-level-basedir> (4)
            <quarkus.bootstrap.effective-model-builder>true</quarkus.bootstrap.effective-model-builder> (5)
          </systemPropertyVariables>
        </configuration>
      </plugin>
    </plugins>
  </build>
  [...]
</project>
```

**1**Propagate `maven.home` system property set by the Maven CLI to the tests
**2**Set the Maven local repository directory for the tests
**3**Set the Maven settings file the tests
**4**Point to the top-level project directory for the tests
**5**Enable effective POM-based project layout discovery

#### [](https://quarkus.io/guides/maven-tooling#top-level-vs-multi-module-project-directory)Top-level vs Multi-module project directory

In Maven there appears to be a notion of the top-level project (that is exposed as a project property `${session.topLevelProject.basedir.absolutePath}`) and the multi-module project directory (that is available as property `${maven.multiModuleProjectDirectory}`). These directories might not always match!

`maven.multiModuleProjectDirectory` is meant to be consulted by the Maven code itself and not something to be relied upon by user code. So, if you find it useful, use it at your own risk!

The `${maven.multiModuleProjectDirectory}` will be resolved to the first directory that contains `.mvn` directory as its child going up the workspace file system tree starting from the current directory (or the one specified with the `-f` argument) from which the `mvn` command was launched. If the `.mvn` directory was not found, however, the `${maven.multiModuleProjectDirectory}` will be pointing to the directory from which the `mvn` command was launched (or the one targeted with the `-f` argument).

The `${session.topLevelProject.basedir.absolutePath}` will be pointing either to the directory from which the `mvn` command was launched or to the directory targeted with the `-f` argument, if it was specified.

[](https://quarkus.io/guides/maven-tooling#project-info)Quarkus project info
----------------------------------------------------------------------------

The Quarkus Maven plugin includes a goal called `info` (currently marked as 'experimental') that logs Quarkus-specific information about the project, such as: the imported Quarkus platform BOMs and the Quarkus extensions found among the project dependencies. In a multi-module project `quarkus:info` will assume that the current module, in which it is executed, is the main module of the application.

The report generated by `quarkus:info` is not currently including the Quarkus Maven plugin information, however it’s planned to be added in the future releases.

Here is an example `info` output for a simple project:

```
[aloubyansky@localhost code-with-quarkus]$ mvn quarkus:info
[INFO] Scanning for projects...
[INFO]
[INFO] ---------------------< org.acme:code-with-quarkus >---------------------
[INFO] Building code-with-quarkus 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- quarkus-maven-plugin:3.24.2:info (default-cli) @ code-with-quarkus ---
[WARNING] quarkus:info goal is experimental, its options and output may change in future versions
[INFO] Quarkus platform BOMs: (1)
[INFO]   io.quarkus.platform:quarkus-bom:pom:3.24.2
[INFO]   io.quarkus.platform:quarkus-camel-bom:pom:3.24.2
[INFO]
[INFO] Extensions from io.quarkus.platform:quarkus-bom: (2)
[INFO]   io.quarkus:quarkus-rest
[INFO]
[INFO] Extensions from io.quarkus.platform:quarkus-camel-bom: (3)
[INFO]   org.apache.camel.quarkus:camel-quarkus-rabbitmq
[INFO]
[INFO] Extensions from registry.quarkus.io: (4)
[INFO]   io.quarkiverse.prettytime:quarkus-prettytime:2.0.1
```

**1**Quarkus platform BOMs imported in the project (BOMs imported by parent POMs will also be reported)
**2**Direct Quarkus extension dependencies managed by the `quarkus-bom`
**3**Direct Quarkus extension dependencies managed by the `quarkus-camel-bom`
**4**Direct Quarkus extensions dependencies that aren’t managed by Quarkus BOMs but found in the Quarkus extension registry

`quarkus:info` will also report Quarkus extensions that aren’t found in the Quarkus extension registries if those are present among the project dependencies, indicating they have an 'unknown origin'.

### [](https://quarkus.io/guides/maven-tooling#project-info-misaligned)Highlighting misaligned versions

`quarkus:info` will also highlight basic Quarkus dependency version misalignments, in case they are detected. For example, if we modify the project mentioned above by removing the `camel-quarkus-rabbitmq` extension from the dependencies and adding a `2.6.3.Final``<version>` element to the `quarkus-rest` dependency that is managed by the `quarkus-bom` and then run `quarkus:info` again, we’ll see something like:

```
[INFO] --- quarkus-maven-plugin:3.24.2:info (default-cli) @ code-with-quarkus ---
[WARNING] quarkus:info goal is experimental, its options and output may change in future versions
[INFO] Quarkus platform BOMs:
[INFO]   io.quarkus.platform:quarkus-bom:pom:3.24.2
[INFO]   io.quarkus.platform:quarkus-camel-bom:pom:3.24.2 | unnecessary (1)
[INFO]
[INFO] Extensions from io.quarkus.platform:quarkus-bom:
[INFO]   io.quarkus:quarkus-resteasy-reactive:2.6.3.Final | misaligned (2)
[INFO]
[INFO] Extensions from io.quarkus.platform:quarkus-camel-bom:
[INFO]   org.apache.camel.quarkus:camel-quarkus-rabbitmq
[INFO]
[INFO] Extensions from registry.quarkus.io:
[INFO]   io.quarkiverse.prettytime:quarkus-prettytime:2.0.1
[INFO]
[WARNING] Non-recommended Quarkus platform BOM and/or extension versions were found. For more details, please, execute 'mvn quarkus:update -Drectify'
```

**1**The `quarkus-camel-bom` import is now reported as 'unnecessary' since none of the Quarkus extensions it includes are found among the project dependencies
**2**The version `2.6.3.Final` of the `quarkus-resteasy-reactive` is now reported as being misaligned with the version managed by the Quarkus platform BOM imported in the project, which is 3.24.2

[](https://quarkus.io/guides/maven-tooling#project-update)Quarkus project update
--------------------------------------------------------------------------------

The `quarkus:update` goal (currently marked as 'experimental') provided by the Quarkus Maven plugin can be used to check whether there are Quarkus-related updates available for a project, such as: new releases of the relevant Quarkus platform BOMs and non-platform Quarkus extensions present in the project. In a multi-module project the `update` goal is meant to be executed from the main Quarkus application module.

At this point, the `quarkus:update` goal does not actually apply the recommended updates but simply reports what they are and how to apply them manually.

The Quarkus Maven plugin version isn’t currently included in the update report, however it’s planned to be added in the future releases.

The way `quarkus:update` works, first, all the direct Quarkus extension dependencies of the project are collected (those that are managed by the Quarkus platform BOMs and those that aren’t but found in the Quarkus extension registries). Then the configured Quarkus extension registries (typically the `registry.quarkus.io`) will be queried for the latest recommended/supported Quarkus platform versions and non-platform Quarkus extensions compatible with them. The algorithm will then select the latest compatible combination of all the extensions found in the project, assuming such a combination actually exists. Otherwise, no updates will be suggested.

Assuming we have a project including Kogito, Camel and core Quarkus extensions available in the Quarkus platform based on Quarkus `2.7.1.Final`, the output of the `quarkus:update` would look like:

```
[aloubyansky@localhost code-with-quarkus]$ mvn quarkus:update
[INFO] Scanning for projects...
[INFO]
[INFO] ---------------------< org.acme:code-with-quarkus >---------------------
[INFO] Building code-with-quarkus 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- quarkus-maven-plugin:3.24.2:update (default-cli) @ code-with-quarkus ---
[WARNING] quarkus:update goal is experimental, its options and output might change in future versions
[INFO]
[INFO] Recommended Quarkus platform BOM updates: (1)
[INFO] Update: io.quarkus.platform:quarkus-bom:pom:2.7.1.Final -> 3.24.2
[INFO] Update: io.quarkus.platform:quarkus-camel-bom:pom:2.7.1.Final -> 3.24.2
```

**1**A list of currently recommended Quarkus platform BOM updates

Typically, a single project property will be used to manage all the Quarkus platform BOMs but the implementation isn’t currently smart enough to point that out and will report updates for each BOM individually.

If we modify the project to remove all the Camel Quarkus extensions from the project, change the version of the `quarkus-resteasy-reactive` extension to `2.6.3.Final` and downgrade `quarkus-prettytime` which is not included in the Quarkus platform BOMs to `0.2.0`, `quarkus:update` will report something like:

```
[INFO] Recommended Quarkus platform BOM updates: (1)
[INFO] Update: io.quarkus.platform:quarkus-bom:pom:2.7.1.Final -> 3.24.2
[INFO] Remove: io.quarkus.platform:quarkus-camel-bom:pom:2.7.1.Final (2)
[INFO]
[INFO] Extensions from io.quarkus.platform:quarkus-bom:
[INFO] Update: io.quarkus:quarkus-resteasy-reactive:2.6.3.Final -> remove version (managed) (3)
[INFO]
[INFO] Extensions from registry.quarkus.io:
[INFO] Update: io.quarkiverse.prettytime:quarkus-prettytime:0.2.0 -> 0.2.1 (4)
```

**1**A list of the currently recommended Quarkus platform BOM updates for the project
**2**Given that the project does not include any Camel Quarkus extensions, the BOM import is recommended to be removed
**3**An outdated version of the `quarkus-resteasy-reactive` is recommended to be removed in favor of the one managed by the `quarkus-bom`
**4**The latest compatible version of the `quarkus-prettytime` extension

### [](https://quarkus.io/guides/maven-tooling#quarkus-project-rectify)Quarkus project rectify

As was mentioned above, `quarkus:info`, besides reporting Quarkus platform and extension versions, performs a quick version alignment check, to make sure the extension versions used in the project are compatible with the imported Quarkus platform BOMs. If misalignments are detected, the following warning message will be logged:

`[WARNING] Non-recommended Quarkus platform BOM and/or extension versions were found. For more details, please, execute 'mvn quarkus:update -Drectify'`

When the `rectify` option is enabled, `quarkus:update`, instead of suggesting the latest recommended Quarkus version updates, will log update instructions to simply align the extension dependency versions found in the project with the currently imported Quarkus platform BOMs.
