Title: Deploying to Google Cloud Platform (GCP)

URL Source: https://quarkus.io/guides/deploying-to-google-cloud

Markdown Content:
[Edit this Page](https://github.com/quarkusio/quarkus/edit/main/docs/src/main/asciidoc/deploying-to-google-cloud.adoc)

This guide covers:

*   Login to Google Cloud

*   Deploying a function to Google Cloud Functions

*   Deploying a JAR to Google App Engine Standard

*   Deploying a Docker image to Google App Engine Flexible Custom Runtimes

*   Deploying a Docker image to Google Cloud Run

*   Using Cloud SQL

[](https://quarkus.io/guides/deploying-to-google-cloud#prerequisites)Prerequisites
----------------------------------------------------------------------------------

To complete this guide, you need:

*   Roughly 1 hour for all modalities

*   An IDE

*   JDK 17+ installed with `JAVA_HOME` configured appropriately

*   Apache Maven 3.9.9

*   Optionally the [Quarkus CLI](https://quarkus.io/guides/cli-tooling) if you want to use it

*   [A Google Cloud Account](https://cloud.google.com/). Free accounts work.

*   [Cloud SDK CLI Installed](https://cloud.google.com/sdk)

[](https://quarkus.io/guides/deploying-to-google-cloud#login-to-google-cloud)Login to Google Cloud
--------------------------------------------------------------------------------------------------

Login to Google Cloud is necessary for deploying the application. It can be done as follows:

`gcloud auth login`

[](https://quarkus.io/guides/deploying-to-google-cloud#deploying-to-google-cloud-functions)Deploying to Google Cloud Functions
------------------------------------------------------------------------------------------------------------------------------

Quarkus supports deploying your application to Google Cloud Functions via the following extensions:

*   [Google Cloud Functions](https://quarkus.io/guides/gcp-functions): Build functions using the Google Cloud Functions API.

*   [Google Cloud Functions HTTP binding](https://quarkus.io/guides/gcp-functions-http): Build functions using Quarkus HTTP APIs: Quarkus REST (formerly RESTEasy Reactive, our Jakarta REST implementation), Undertow (Servlet), Vert.x Web, or [Funqy HTTP](https://quarkus.io/guides/funqy-http).

*   [Funky Google Cloud Functions](https://quarkus.io/guides/funqy-gcp-functions): Build functions using Funqy.

Each extension supports a specific kind of application development, follow the specific guides for more information on how to develop, package and deploy your applications using them.

[](https://quarkus.io/guides/deploying-to-google-cloud#deploying-to-google-app-engine-standard)Deploying to Google App Engine Standard
--------------------------------------------------------------------------------------------------------------------------------------

First, make sure to have an App Engine environment initialized for your Google Cloud project, if not, initialize one via `gcloud app create --project=[YOUR_PROJECT_ID]`.

Then, you will need to create a `src/main/appengine/app.yaml` file, let’s keep it minimalistic with only the selected engine:

`runtime: java21`

This will create a default service for your App Engine application.

You can also use another Java runtime supported by App Engine, for example, for Java 17, use `runtime: java17` instead.

App Engine Standard does not support the default Quarkus' specific packaging layout, therefore, you must set up your application to be packaged as an uber-jar via your `application.properties` file:

`quarkus.package.jar.type=uber-jar`

Then, you can choose to build the application manually or delegating that responsibility to `gcloud` or the Google Cloud Maven plugin.

### [](https://quarkus.io/guides/deploying-to-google-cloud#building-the-application-manually)Building the application manually

Use Maven to build the application using `mvn clean package`, it will generate a single JAR that contains all the classes of your application including its dependencies.

Finally, use `gcloud` to deploy your application as an App Engine service.

`gcloud app deploy target/getting-started-1.0.0-SNAPSHOT-runner.jar`

This command will upload your application jar and launch it on App Engine.

When it’s done, the output will display the URL of your application (target url), you can use it with curl or directly open it in your browser using `gcloud app browse`.

### [](https://quarkus.io/guides/deploying-to-google-cloud#building-the-application-via-gcloud)Building the application via gcloud

You can choose to let `gcloud` build your application for you, this is the simplest way to deploy to App Engine.

Then, you can just launch `gcloud app deploy` in the root of your project, it will upload all your project files (the list can be reduced via the `.gcloudignore` file), package your JAR via Maven (or Gradle) and launch it on App Engine.

When it’s done, the output will display the URL of your application (target url), you can use it with curl or directly open it in your browser using `gcloud app browse`.

### [](https://quarkus.io/guides/deploying-to-google-cloud#building-the-application-via-the-google-cloud-maven-plugin)Building the application via the Google Cloud Maven plugin

You can also let Maven control the deployment of your application using the App Engine Maven plugin.

First, add the plugin to your `pom.xml`:

```
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>appengine-maven-plugin</artifactId>
    <version>2.7.0</version>
    <configuration>
        <projectId>GCLOUD_CONFIG</projectId> (1)
        <version>gettingstarted</version>
        <artifact>${project.build.directory}/${project.artifactId}-${project.version}-runner.jar</artifact> (2)
    </configuration>
</plugin>
```

**1**Use the default `gcloud` configuration
**2**Override the default JAR name to the one generated by the Quarkus Maven plugin

Then you would be able to use Maven to build and deploy your application to App Engine via `mvn clean package appengine:deploy`.

When it’s done, the output will display the URL of your application (target URL), you can use it with curl or directly open it in your browser using `gcloud app browse`.

[](https://quarkus.io/guides/deploying-to-google-cloud#deploying-to-google-app-engine-flexible-custom-runtimes)Deploying to Google App Engine Flexible Custom Runtimes
----------------------------------------------------------------------------------------------------------------------------------------------------------------------

Before all, make sure to have an App Engine environment initialized for your Google Cloud project, if not, initialize one via `gcloud app create --project=[YOUR_PROJECT_ID]`.

App Engine Flexible Custom Runtimes uses a Docker image to run your application.

First, create an `app.yaml` file at the root of your project with the following content:

```
runtime: custom
env: flex
```

App Engine Flexible Custom Runtimes deploys your application as a Docker container, you can choose to deploy one of the Dockerfile provided inside your application.

Both JVM and native executable versions will work.

To deploy a JVM application:

*   Copy the JVM Dockerfile to the root directory of your project: `cp src/main/docker/Dockerfile.jvm Dockerfile`.

*   Build your application using `mvn clean package`.

To deploy a native application:

*   Copy the native Dockerfile to the root directory of your project: `cp src/main/docker/Dockerfile.native Dockerfile`.

*   Build your application as a native executable using `mvn clean package -Dnative`.

Finally, launch `gcloud app deploy` in the root of your project, it will upload all your project files (the list can be reduced via the `.gcloudignore` file), build your Dockerfile and launch it on App Engine Flexible custom runtime.

It uses Cloud Build to build your Docker image and deploy it to Google Container Registry (GCR).

When done, the output will display the URL of your application (target url), you can use it with curl or directly open it in your browser using `gcloud app browse`.

App Engine Flexible custom runtimes support [health checks](https://cloud.google.com/appengine/docs/flexible/reference/app-yaml?tab=java#updated_health_checks), it is strongly advised to provide them thanks to Quarkus [SmallRye Health](https://quarkus.io/guides/smallrye-health) support.

[](https://quarkus.io/guides/deploying-to-google-cloud#deploying-to-google-cloud-run)Deploying to Google Cloud Run
------------------------------------------------------------------------------------------------------------------

Google Cloud Run allows you to run your Docker containers inside Google Cloud Platform in a managed way.

By default, Quarkus listens on port 8080, and it’s also the Cloud Run default port. No need to use the `PORT` environment variable defined in Cloud Run to customize the Quarkus HTTP port.

Cloud Run will use Cloud Build to build your Docker image and deploy it to Google Container Registry (GCR).

Both JVM and native executable versions will work.

To deploy a JVM application:

*   Copy the JVM Dockerfile to the root directory of your project: `cp src/main/docker/Dockerfile.jvm Dockerfile`.

*   Build your application using `mvn clean package`.

To deploy a native application:

*   Copy the native Dockerfile to the root directory of your project: `cp src/main/docker/Dockerfile.native Dockerfile`.

*   Build your application as a native executable using `mvn clean package -Dnative`.

Then, create a `.gcloudignore` file to tell gcloud which files should be not be uploaded for Cloud Build, without it, it defaults to `.gitignore` that usually exclude the target directory where you packaged application has been created.

In this example, I only exclude the `src` directory:

`src/`

Then, use Cloud Build to build your image, it will upload to a Google Cloud Storage bucket all the files of your application (except the ones ignored by the `.gcloudignore`file), build your Docker image and push it to Google Container Registry (GCR).

`gcloud builds submit --tag gcr.io/PROJECT-ID/helloworld`

You can also build your image locally and push it to a publicly accessible Docker registry, then use this image in the next step.

Finally, use Cloud Run to launch your application.

`gcloud run deploy --image gcr.io/PROJECT-ID/helloworld`

Cloud run will ask you questions on the service name, the region and whether unauthenticated calls are allowed. After you answer to these questions, it will deploy your application.

When the deployment is done, the output will display the URL to access your application.

[](https://quarkus.io/guides/deploying-to-google-cloud#using-cloud-sql)Using Cloud SQL
--------------------------------------------------------------------------------------

Google Cloud SQL provides managed instances for MySQL, PostgreSQL and Microsoft SQL Server. Quarkus has support for all three databases.

### [](https://quarkus.io/guides/deploying-to-google-cloud#using-cloud-sql-with-a-jdbc-driver)Using Cloud SQL with a JDBC driver

To make your applications work with Cloud SQL, you first need to use the corresponding JDBC extension, for example, for PostgreSQL, add the `quarkus-jdbc-postgresql` extension.

Then you need to add to your `pom.xml` the Cloud SQL JDBC library that provides the additional connectivity to Cloud SQL. For PostgreSQL you will need to include the following dependency:

```
<dependency>
    <groupId>com.google.cloud.sql</groupId>
    <artifactId>postgres-socket-factory</artifactId>
    <version>${postgres-socket-factory.version}</version>
</dependency>
```

Finally, you need to configure your datasource specifically to use the socket factory:

```
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql:///mydatabase (1)
quarkus.datasource.username=quarkus
quarkus.datasource.password=quarkus
quarkus.datasource.jdbc.additional-jdbc-properties.cloudSqlInstance=project-id:gcp-region:instance (2)
quarkus.datasource.jdbc.additional-jdbc-properties.socketFactory=com.google.cloud.sql.postgres.SocketFactory (3)
```

**1**The JDBC URL should not include the hostname / IP of the database.
**2**We add the `cloudSqlInstance` additional JDBC property to configure the instance id.
**3**We add the `socketFactory` additional JDBC property to configure the socket factory used to connect to Cloud SQL, this one is coming from the `postgres-socket-factory` dependency.

Using a PostgreSQL socket factory is not possible in dev mode at the moment due to issue [#15782](https://github.com/quarkusio/quarkus/issues/15782).

### [](https://quarkus.io/guides/deploying-to-google-cloud#using-cloud-sql-with-a-reactive-sql-client)Using Cloud SQL with a reactive SQL client

You can also use one of our reactive SQL client instead of the JDBC client. To do so with Cloud SQL, add the following dependency (adjust the classifier depending on your platform):

```
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-transport-native-epoll</artifactId>
    <classifier>linux-x86_64</classifier>
</dependency>
```

Then configure your reactive datasource with no hostname and with the Netty native transport:

```
quarkus.datasource.reactive.url=postgresql://:5432/db-name?host=/cloudsql/project-id:zone:db-name
quarkus.vertx.prefer-native-transport=true
```

This only works when your application is running inside a Google Cloud managed runtime like App Engine.

### [](https://quarkus.io/guides/deploying-to-google-cloud#using-cloud-sql-with-native-executables)Using Cloud SQL with native executables

When generating native executables, you must mark `jnr.ffi.provider.jffi.NativeFinalizer$SingletonHolder` as runtime initialized.

`quarkus.native.additional-build-args=--initialize-at-run-time=jnr.ffi.provider.jffi.NativeFinalizer$SingletonHolder`

Additionally, starting with `com.google.cloud.sql:postgres-socket-factory:1.17.0`, you must also mark `com.kenai.jffi.internal.Cleaner` as runtime initialized.

`quarkus.native.additional-build-args=--initialize-at-run-time=jnr.ffi.provider.jffi.NativeFinalizer$SingletonHolder\\,com.kenai.jffi.internal.Cleaner`

[](https://quarkus.io/guides/deploying-to-google-cloud#going-further)Going further
----------------------------------------------------------------------------------

You can find a set of extensions to access various Google Cloud Services in the Quarkiverse (a GitHub organization for Quarkus extensions maintained by the community), including PubSub, BigQuery, Storage, Spanner, Firestore, Secret Manager (visit the repository for an accurate list of supported services).
