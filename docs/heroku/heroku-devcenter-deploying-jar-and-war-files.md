Title: Deploying JAR and WAR Files | Heroku Dev Center

URL Source: https://devcenter.heroku.com/articles/deploying-jar-and-war-files

Markdown Content:
*   [Language Support](https://devcenter.heroku.com/categories/language-support)
*   [Java](https://devcenter.heroku.com/categories/java-support)
*   [Working with Java](https://devcenter.heroku.com/categories/working-with-java)
*   [Deploying JAR and WAR Files](https://devcenter.heroku.com/articles/deploying-jar-and-war-files)

English — [日本語に切り替える](https://devcenter.heroku.com/ja/articles/deploying-jar-and-war-files)
Last updated February 24, 2025

Table of Contents
-----------------

*   [Requirements and Installation](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#requirements-and-installation)
*   [Authentication](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#authentication)
*   [Deploying an Executable JAR file](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#deploying-an-executable-jar-file)
*   [Deploying a WAR File](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#deploying-a-war-file)
*   [Configuring OpenJDK Version](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#configuring-openjdk-version)
*   [Automatically Included Files and Directories](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#automatically-included-files-and-directories)

You can package many JVM applications into a self-contained executable JAR file or a WAR file that includes application code, configuration, and dependencies. As an alternative to [Git deployments](https://devcenter.heroku.com/articles/git), Heroku offers a command-line tool to deploy JAR and WAR files directly.

This deployment strategy can be useful when Git deployments result in apps that exceed the size limit. You can also use this deployment to build and test your app on existing continuous integration (CI) infrastructure such as Jenkins before deploying to Heroku.

[Requirements and Installation](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#requirements-and-installation)
--------------------------------------------------------------------------------------------------------------------------------

The Heroku JVM Application Deployer requires [OpenJDK 8](https://devcenter.heroku.com/articles/java-support#specifying-a-java-version) or later installed on the machine deploying the app.

Download the Heroku JVM Application Deployer JAR file from the [latest release on GitHub](https://github.com/heroku/heroku-jvm-application-deployer/releases/latest).

[Authentication](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#authentication)
--------------------------------------------------------------------------------------------------

The Heroku JVM Application Deployer uses [Heroku’s Platform API](https://devcenter.heroku.com/articles/platform-api-reference) and needs an API key to function. If you installed the [Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli) and are logged in with `heroku login`, the deployer automatically picks up your API key.

If you haven’t installed the Heroku CLI, the deployer attempts to read the Heroku API key from the `HEROKU_API_KEY` environment variable.

[Deploying an Executable JAR file](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#deploying-an-executable-jar-file)
--------------------------------------------------------------------------------------------------------------------------------------

The following command will deploys the JAR file to Heroku. The command generates and deploys a [Procfile](https://devcenter.heroku.com/articles/procfile) that runs the deployed JAR file with `java -jar`.

`java -jar heroku-jvm-application-deployer.jar --app=nameless-dusk-88303 /path/to/app.jar`

You can only run JAR files with a `Main-Class` attribute in the JAR manifest with `java -jar` . If you can’t run your JAR file via `java -jar`, you must provide a custom Procfile.

```
-----> Packaging application...
       - including: Procfile (hidden)
       - including: test-1.0-SNAPSHOT-jar-with-dependencies.jar
-----> Creating build...
       - file: /tmp/heroku-deploy3032832562328358957source-blob.tgz
       - size: 3MB
-----> Uploading build...
       Uploaded 4 KB/3 MB (0.1%, 4 KB/s)
       Uploaded 1 MB/3 MB (50.6%, 1 MB/s)
       Uploaded 2 MB/3 MB (90.3%, 1 MB/s)
       - success (3.1 seconds)
-----> Deploying...
remote:
remote: -----> Building on the Heroku-24 stack
remote: -----> Using buildpack: heroku/jvm
remote: -----> JVM Common app detected
remote: -----> Installing OpenJDK 21... done
remote: -----> Discovering process types
remote:        Procfile declares types -> web
remote:
remote: -----> Compressing...
remote:        Done: 76.4M
remote: -----> Launching...
remote:        Released v6
remote:        https://nameless-dusk-88303-9d9b2a3590bf.herokuapp.com/ deployed to Heroku
remote:
-----> Done
```

[Deploying a WAR File](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#deploying-a-war-file)
--------------------------------------------------------------------------------------------------------------

Deploying a WAR file works the same way as deploying an executable JAR file. The deployer downloads the latest release of Heroku’s Webapp Runner for you and deploys it with your WAR file.

The deployer also generates and deploys a Procfile that uses Webapp Runner to run your WAR file.

`java -jar heroku-jvm-application-deployer.jar --app=nameless-dusk-88303 /path/to/app.war`

```
-----> Downloading webapp-runner 10.1.28.0...
-----> Packaging application...
       - including: Procfile (hidden)
       - including: test-1.0-SNAPSHOT.war
       - including: .heroku/webapp-runner.jar
-----> Creating build...
       - file: /tmp/heroku-deploy7572390222443658138source-blob.tgz
       - size: 24MB
-----> Uploading build...
       Uploaded 4 KB/24 MB (0.0%, 4 KB/s)
       Uploaded 23 MB/24 MB (96.0%, 24 MB/s)
       - success (1.2 seconds)
-----> Deploying...
remote:
remote: -----> Building on the Heroku-24 stack
remote: -----> Using buildpack: heroku/jvm
remote: -----> JVM Common app detected
remote: -----> Installing OpenJDK 21... done
remote: -----> Discovering process types
remote:        Procfile declares types -> web
remote:
remote: -----> Compressing...
remote:        Done: 97.6M
remote: -----> Launching...
remote:        Released v8
remote:        https://nameless-dusk-88303-9d9b2a3590bf.herokuapp.com/ deployed to Heroku
remote:
-----> Done
```

[Configuring OpenJDK Version](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#configuring-openjdk-version)
----------------------------------------------------------------------------------------------------------------------------

The deployer [supports the same OpenJDK versions](https://devcenter.heroku.com/articles/java-support#supported-java-versions) as defined in [Heroku Java Support Reference](https://devcenter.heroku.com/articles/java-support#specifying-a-java-version). It also uses the same default version. To configure the OpenJDK version, use the `--jdk` flag:

`java -jar heroku-jvm-application-deployer.jar --jdk 21 --app=nameless-dusk-88303`

This command generates and deploys a `system.properties` file with the configured OpenJDK version. If a `system.properties` file exists in the current working directory, the deployer deploys and uses this file instead.

[Automatically Included Files and Directories](https://devcenter.heroku.com/articles/deploying-jar-and-war-files#automatically-included-files-and-directories)
--------------------------------------------------------------------------------------------------------------------------------------------------------------

The Heroku JVM Application Deployer automatically includes certain files and directories from the current working directory if they’re present. These files are:

*   `Procfile` (See: [Procfile](https://devcenter.heroku.com/articles/procfile))
*   `system.properties` (See: [Heroku Java Support](https://devcenter.heroku.com/articles/java-support#specifying-a-java-version))
*   `.jdk-overlay` (See: [Customizing the JDK](https://devcenter.heroku.com/articles/customizing-the-jdk))

To disable the automatic inclusion of these files, pass the `--disable-auto-includes` flag.
