Title: Getting Started on Heroku with Java | Heroku Dev Center

URL Source: https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true

Markdown Content:
*   [Language Support](https://devcenter.heroku.com/categories/language-support)
*   [Java](https://devcenter.heroku.com/categories/java-support)
*   [Getting Started on Heroku with Java](https://devcenter.heroku.com/articles/getting-started-with-java)

English — [日本語に切り替える](https://devcenter.heroku.com/ja/articles/getting-started-with-java)
Last updated March 21, 2025

Table of Contents
-----------------

*   [Introduction](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#introduction)
*   [Set Up](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#set-up)
*   [Prepare the App](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#prepare-the-app)
*   [Create Your App](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#create-your-app)
*   [Define a Procfile](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#define-a-procfile)
*   [Provision a Database](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#provision-a-database)
*   [Deploy the App](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#deploy-the-app)
*   [Scale the App](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#scale-the-app)
*   [View Logs](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#view-logs)
*   [Provision a Logging Add-on](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#provision-a-logging-add-on)
*   [Use a Database](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#use-a-database)
*   [Prepare the Local Environment](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#prepare-the-local-environment)
*   [Run the App Locally](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#run-the-app-locally)
*   [Push Local Changes](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#push-local-changes)
*   [Define Config Vars](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#define-config-vars)
*   [Start a One-off Dyno](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#start-a-one-off-dyno)
*   [Next Steps](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#next-steps)
*   [Delete Your App and Add-on](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#delete-your-app-and-add-on)

[Introduction](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#introduction)
------------------------------------------------------------------------------------------------------------

Complete this tutorial to deploy a sample Java app to [Cedar](https://devcenter.heroku.com/articles/generations#cedar), the legacy generation of the Heroku platform. To deploy the app to the [Fir](https://devcenter.heroku.com/articles/generations#fir) generation, only available to [Heroku Private Spaces](https://devcenter.heroku.com/articles/private-spaces), follow this [guide](https://devcenter.heroku.com/articles/getting-started-with-java-maven-fir) instead.

The tutorial assumes that you have:

*   A [verified Heroku Account](https://devcenter.heroku.com/articles/account-verification)
*   [OpenJDK 17](https://www.azul.com/downloads/?version=java-17-lts&package=jdk#zulu) (or newer) installed locally
*   [Postgres](https://devcenter.heroku.com/articles/heroku-postgresql#local-setup) installed locally
*   An [Eco dynos plan](https://devcenter.heroku.com/articles/eco-dyno-hours) subscription (recommended)

If you prefer to use Gradle instead of Maven, see the [Getting Started with Gradle on Heroku](https://devcenter.heroku.com/articles/getting-started-with-gradle-on-heroku) guide.

Using dynos and databases to complete this tutorial counts towards your usage. We recommend using our [low-cost plans](https://blog.heroku.com/new-low-cost-plans) to complete this tutorial. Eligible students can apply for platform credits through our new [Heroku for GitHub Students program](https://blog.heroku.com/github-student-developer-program).

[Set Up](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#set-up)
------------------------------------------------------------------------------------------------

Install the [Heroku Command Line Interface](https://devcenter.heroku.com/articles/heroku-cli) (CLI). Use the CLI to manage and scale your app, provision add-ons, view your logs, and run your app locally.

Download and run the installer for your platform:

You can find more installation options for the Heroku CLI [here](https://devcenter.heroku.com/articles/heroku-cli).

After installation, you can use the `heroku` command from your command shell.

To log in to the Heroku CLI, use the `heroku login` command:

```
$ heroku login
heroku: Press any key to open up the browser to login or q to exit:
Opening browser to https://cli-auth.heroku.com/auth/cli/browser/***
heroku: Waiting for login...
Logging in... done
Logged in as me@example.com
```

This command opens your web browser to the Heroku login page. If your browser is already logged in to Heroku, click the **`Log In`** button on the page.

This authentication is required for the `heroku` and `git` commands to work correctly.

If you have any problems installing or using the Heroku CLI, see the main [Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli) article for advice and troubleshooting steps.

[Prepare the App](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#prepare-the-app)
------------------------------------------------------------------------------------------------------------------

If you’re new to Heroku, it’s recommended to complete this tutorial using the Heroku-provided sample application.

If you have your own application that you want to deploy instead, see [Preparing a Codebase for Heroku Deployment](https://devcenter.heroku.com/articles/preparing-a-codebase-for-heroku-deployment).

Create a local copy of the sample app by executing the following commands in your local command shell or terminal:

```
$ git clone https://github.com/heroku/java-getting-started
$ cd java-getting-started
```

This functioning Git repository contains a sample Java application. It includes a `pom.xml` file, which is used by Maven, a Java build tool.

[Create Your App](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#create-your-app)
------------------------------------------------------------------------------------------------------------------

Using a dyno and a database to complete this tutorial counts towards your usage. [Delete your app](https://devcenter.heroku.com/articles/heroku-cli-commands#heroku-apps-destroy), and [database](https://devcenter.heroku.com/articles/heroku-postgresql#removing-the-add-on) as soon as you’re done to control costs.

By default, apps use Eco dynos if you’re subscribed to Eco. Otherwise, it defaults to Basic dynos. The Eco dynos plan is shared across all Eco dynos in your account and is recommended if you plan on deploying many small apps to Heroku. Learn more [here](https://blog.heroku.com/new-low-cost-plans). Eligible students can apply for platform credits through our [Heroku for GitHub Students program](https://blog.heroku.com/github-student-developer-program).

To prepare Heroku to receive your source code, create an app:

```
$ heroku create
Creating app... done, ⬢ peaceful-inlet-84135
http://peaceful-inlet-84135.herokuapp.com/ | https://git.heroku.com/peaceful-inlet-84135.git
```

When you create an app, a git remote called `heroku` is also created and associated with your local git repository. Git remotes are versions of your repository that live on other servers. You deploy your app by pushing its code to that special Heroku-hosted remote associated with your app.

Heroku generates a random name for your app, in this case, `peaceful-inlet-84135`. You can [specify your own app name](https://devcenter.heroku.com/articles/heroku-cli-commands#heroku-apps-create-app).

[Define a Procfile](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#define-a-procfile)
----------------------------------------------------------------------------------------------------------------------

Use a [Procfile](https://devcenter.heroku.com/articles/procfile), a text file in the root directory of your application, to explicitly declare what command to execute to start your app.

The `Procfile` in the example app looks like this:

```
web: java -jar target/java-getting-started-1.0.0-SNAPSHOT.jar
```

This Procfile declares a single process type, `web`, and the command needed to run it. The name `web` is important here. It declares that this process type is attached to Heroku’s [HTTP routing](https://devcenter.heroku.com/articles/http-routing) stack and receives web traffic when deployed.

A Procfile can contain additional process types. For example, you can declare a [background worker process](https://devcenter.heroku.com/articles/background-jobs-queueing#process-model) that processes items off a queue.

[Provision a Database](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#provision-a-database)
----------------------------------------------------------------------------------------------------------------------------

The sample app requires a database. Provision a [Heroku Postgres](https://devcenter.heroku.com/articles/heroku-postgresql) database, one of the add-ons available through the [Elements Marketplace](https://www.heroku.com/elements/addons). Add-ons are cloud services that provide out-of-the-box additional services for your application, such as logging, monitoring, databases, and more.

```
$ heroku addons:create heroku-postgresql:essential-0
Creating heroku-postgresql:essential-0 on ⬢ peaceful-inlet-84135... ~$0.007/hour (max $5/month)
Database should be available soon
postgresql-fitted-70383 is being created in the background. The app will restart when complete...
Use heroku addons:info postgresql-fitted-70383 to check creation progress
Use heroku addons:docs heroku-postgresql to view documentation
```

Your Heroku app now has access to a Postgres database. The `DATABASE_URL` environment variable stores the credentials. Heroku also automatically makes a `JDBC_DATABASE_URL` environment variable available for Java applications. It contains a JDBC-compatible version of `DATABASE_URL`.

You can see all the add-ons provisioned with the `addons` command:

```
$ heroku addons
Add-on                                       Plan         Price     State
───────────────────────────────────────────  ───────────  ────────  ───────
heroku-postgresql (postgresql-fitted-70383)  essential-0  $5/month  created
 └─ as DATABASE

The table above shows add-ons and the attachments to the current app (peaceful-inlet-84135) or other apps.
```

[Deploy the App](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#deploy-the-app)
----------------------------------------------------------------------------------------------------------------

Deploy your code. This command pushes the `main` branch of the sample repo to your `heroku` remote, which then deploys to Heroku:

```
$ git push heroku main
remote: Compressing source files... done.
remote: Building source:
remote:
remote: -----> Building on the Heroku-22 stack
remote: -----> Determining which buildpack to use for this app
remote: -----> Java app detected
remote: -----> Installing OpenJDK 17... done
remote: -----> Executing Maven
remote:        $ ./mvnw -DskipTests clean dependency:list install
...
remote:        [INFO] ------------------------------------------------------------------------
remote:        [INFO] BUILD SUCCESS
remote:        [INFO] ------------------------------------------------------------------------
remote:        [INFO] Total time:  10.733 s
remote:        [INFO] Finished at: 2023-03-09T16:00:12Z
remote:        [INFO] ------------------------------------------------------------------------
remote: -----> Discovering process types
remote:        Procfile declares types -> web
remote:
remote: -----> Compressing...
remote:        Done: 86.4M
remote: -----> Launching...
remote:        Released v5
remote:        https://peaceful-inlet-84135.herokuapp.com/ deployed to Heroku
remote:
remote: Verifying deploy... done.
To https://git.heroku.com/peaceful-inlet-84135.git
 * [new branch]      main -> main
```

By default, apps use Eco dynos if you’re subscribed to [Eco](https://devcenter.heroku.com/articles/eco-dyno-hours). A dyno is a lightweight Linux container that runs the command specified in your `Procfile`. After deployment, ensure that you have one `web`[dyno](https://devcenter.heroku.com/articles/dynos) running the app. You can check how many dynos are running using the `heroku ps` command:

```
$ heroku ps
Eco dyno hours quota remaining this month: 1000h 0m (100%)
Eco dyno usage for this app: 0h 0m (0%)
For more information on Eco dyno hours, see:
https://devcenter.heroku.com/articles/eco-dyno-hours

=== web (Eco): java -jar target/java-getting-started-1.0.0-SNAPSHOT.jar (1)
web.1: up 2023/03/09 17:00:28 +0100 (~ 1m ago)
```

The running `web` dyno serves requests. Visit the app at the URL shown in the logs. As a handy shortcut, you can open the website with:

```
$ heroku open
```

The Eco dynos plan is shared across all Eco dynos in your account and is recommended if you plan on deploying many small apps to Heroku. Eco dynos sleep if they don’t receive any traffic for half an hour. This sleep behavior causes a few seconds delay for the first request upon waking. Eco dynos consume from a monthly, account-level quota of [eco dyno hours](https://devcenter.heroku.com/articles/eco-dyno-hours). As long as you haven’t exhausted the quota, your apps can continue to run.

To avoid dyno sleeping, upgrade to a Basic or Professional dyno type as described in [Dyno Types](https://devcenter.heroku.com/articles/dyno-types).

[Scale the App](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#scale-the-app)
--------------------------------------------------------------------------------------------------------------

Horizontal scaling an application on Heroku is equivalent to changing the number of running dynos.

Scale the number of web dynos to zero:

```
$ heroku ps:scale web=0
```

Access the app again by refreshing your browser or running `heroku open`. You get an error message because your app no longer has any web dynos available to serve requests.

Scale it up again:

```
$ heroku ps:scale web=1
```

[View Logs](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#view-logs)
------------------------------------------------------------------------------------------------------

Heroku treats logs as streams of time-ordered events, aggregated from the output streams of all your app and Heroku components. Heroku provides a single stream for all events.

View information about your running app by using one of the [logging commands](https://devcenter.heroku.com/articles/logging), `heroku logs --tail`:

```
$ heroku logs --tail
2023-03-09T16:03:23.130513+00:00 app[web.1]: 2023-03-09T16:03:23.130Z  INFO 2 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 41040 (http)
2023-03-09T16:03:23.141029+00:00 app[web.1]: 2023-03-09T16:03:23.140Z  INFO 2 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2023-03-09T16:03:23.141265+00:00 app[web.1]: 2023-03-09T16:03:23.141Z  INFO 2 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.5]
2023-03-09T16:03:23.218914+00:00 app[web.1]: 2023-03-09T16:03:23.218Z  INFO 2 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2023-03-09T16:03:23.220624+00:00 app[web.1]: 2023-03-09T16:03:23.220Z  INFO 2 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 976 ms
2023-03-09T16:03:23.469655+00:00 app[web.1]: 2023-03-09T16:03:23.469Z  INFO 2 --- [           main] o.s.b.a.w.s.WelcomePageHandlerMapping    : Adding welcome page template: index
2023-03-09T16:03:23.692938+00:00 app[web.1]: 2023-03-09T16:03:23.692Z  INFO 2 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 41040 (http) with context path ''
2023-03-09T16:03:23.707694+00:00 app[web.1]: 2023-03-09T16:03:23.707Z  INFO 2 --- [           main] c.heroku.java.GettingStartedApplication  : Started GettingStartedApplication in 1.927 seconds (process running for 2.33)
2023-03-09T16:03:23.940755+00:00 heroku[web.1]: State changed from starting to up
2023-03-09T16:03:24.673236+00:00 app[web.1]: 2023-03-09T16:03:24.672Z  INFO 2 --- [io-41040-exec-3] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2023-03-09T16:03:24.673549+00:00 app[web.1]: 2023-03-09T16:03:24.673Z  INFO 2 --- [io-41040-exec-3] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2023-03-09T16:03:24.674938+00:00 app[web.1]: 2023-03-09T16:03:24.674Z  INFO 2 --- [io-41040-exec-3] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
2023-03-09T16:03:24.948408+00:00 heroku[router]: at=info method=GET path="/" host=peaceful-inlet-84135.herokuapp.com request_id=e010a5e5-f212-4e0b-a624-c96f7421c98f fwd="85.222.134.1" dyno=web.1 connect=0ms service=304ms status=200 bytes=8917 protocol=https
2023-03-09T16:03:25.162539+00:00 heroku[router]: at=info method=GET path="/stylesheets/main.css" host=peaceful-inlet-84135.herokuapp.com request_id=dcbe440f-82c5-4442-80bc-be7be0b5cd61 fwd="85.222.134.1" dyno=web.1 connect=0ms service=10ms status=304 bytes=208 protocol=https
2023-03-09T16:03:25.335819+00:00 heroku[router]: at=info method=GET path="/lang-logo.png" host=peaceful-inlet-84135.herokuapp.com request_id=071b67a6-1e22-4cfe-b57b-c49db6b5af19 fwd="85.222.134.1" dyno=web.1 connect=0ms service=3ms status=304 bytes=208 protocol=https
2023-03-09T16:03:25.534061+00:00 heroku[router]: at=info method=GET path="/favicon.ico" host=peaceful-inlet-84135.herokuapp.com request_id=cc651ead-23ce-4c43-a3ce-4edc93450b14 fwd="85.222.134.1" dyno=web.1 connect=0ms service=82ms status=404 bytes=333 protocol=https
```

To see more log messages generate, visit your application in the browser first.

To stop streaming the logs, press `Control+C`.

[Provision a Logging Add-on](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#provision-a-logging-add-on)
----------------------------------------------------------------------------------------------------------------------------------------

Add-ons are third-party cloud services that provide out-of-the-box additional services for your application, from persistence through logging to monitoring and more.

By default, Heroku stores 1500 lines of logs from your application, but the full log stream is available as a service. Several add-on providers have logging services that provide things such as log persistence, search, and email and SMS alerts.

In this step, you provision one of these logging add-ons, [Papertrail](https://devcenter.heroku.com/articles/papertrail).

Provision the Papertrail logging add-on:

```
$ heroku addons:create papertrail
Creating papertrail on ⬢ peaceful-inlet-84135... free
Welcome to Papertrail. Questions and ideas are welcome (technicalsupport@solarwinds.com). Happy logging!
Created papertrail-slippery-84785 as PAPERTRAIL_API_TOKEN
Use heroku addons:docs papertrail to view documentation
```

The add-on is now deployed and configured for your application. You can list add-ons for your app with this command:

```
$ heroku addons
```

To see this particular add-on in action, visit your application’s Heroku URL a few times. Each visit generates more log messages, which get routed to the Papertrail add-on. Visit the Papertrail console to see the log messages:

```
$ heroku addons:open papertrail
```

Your browser opens up a Papertrail web console that shows the latest log events. The interface lets you search and set up alerts.

[Use a Database](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#use-a-database)
----------------------------------------------------------------------------------------------------------------

Heroku provides managed data services for Postgres and Redis, and the [add-on marketplace](https://elements.heroku.com/addons/categories/data-stores) provides additional data services, including MongoDB and MySQL.

Use the `heroku addons` command for an overview of the database provisioned for your app:

```
$ heroku addons

Add-on                                       Plan         Price     State
───────────────────────────────────────────  ───────────  ────────  ───────
heroku-postgresql (postgresql-fitted-70383)  essential-0  $5/month  created
 └─ as DATABASE

papertrail (papertrail-slippery-84785)       choklad      free      created
 └─ as PAPERTRAIL
```

Listing the config vars for your app displays the URL that your app uses to connect to the database, `DATABASE_URL`:

```
$ heroku config
=== peaceful-inlet-84135 Config Vars
DATABASE_URL:         postgres://avhrhofbiyvpct:3ab23026d0fc225bde4544cedabc356904980e6a02a2418ca44d7fd19dad8e03@ec2-23-21-4-7.compute-1.amazonaws.com:5432/d8e8ojni26668k
PAPERTRAIL_API_TOKEN: [REDACTED]
```

The `heroku pg` command provides more in-depth information on your app’s Heroku Postgres databases:

```
$ heroku pg
=== DATABASE_URL
Plan:                  Essential 0
Status:                Available
Connections:           0/20
PG Version:            15.5
Created:               2024-05-01 13:22 UTC
Data Size:             8.6 MB/1.00 GB (0.84%) (In compliance)
Tables:                0
Fork/Follow:           Unsupported
Rollback:              Unsupported
Continuous Protection: Off
Add-on:                postgresql-fitted-70383
```

Running this command for your app indicates that the app has an `essential-0` Postgres database running Postgres 15.5, with no tables.

The example app you deployed already has database functionality, which you can reach by visiting your app’s `/database` path.

```
$ heroku open /database
```

You see something like this:

```
Database Output

* Read from DB: 2023-03-09 16:58:55.816605
* Read from DB: 2023-03-09 16:58:56.728701
* Read from DB: 2023-03-09 16:58:57.064755
```

Assuming that you have [Postgres installed locally](https://devcenter.heroku.com/articles/heroku-postgresql#local-setup), use the `heroku pg:psql` command to connect to the remote database and see all the rows:

```
$ heroku pg:psql
--> Connecting to postgresql-fitted-70383
psql (15.2, server 14.7 (Ubuntu 14.7-1.pgdg20.04+1))
SSL connection (protocol: TLSv1.3, cipher: TLS_AES_256_GCM_SHA384, compression: off)
Type "help" for help.

peaceful-inlet-84135::DATABASE=> SELECT * FROM ticks;
            tick
----------------------------
 2023-03-09 16:58:55.816605
 2023-03-09 16:58:56.728701
 2023-03-09 16:58:57.064755
(3 rows)

peaceful-inlet-84135::DATABASE=> \q
```

The following info illustrates how the example app implements its database functionality. Don’t change your example app code in this step

The code in the example app looks like this:

```
private final DataSource dataSource;

@Autowired
public GettingStartedApplication(DataSource dataSource) {
    this.dataSource = dataSource;
}

@GetMapping("/database")
String database(Map<String, Object> model) {
    try (Connection connection = dataSource.getConnection()) {
        final var statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
        statement.executeUpdate("INSERT INTO ticks VALUES (now())");

        final var resultSet = statement.executeQuery("SELECT tick FROM ticks");
        final var output = new ArrayList<>();
        while (resultSet.next()) {
            output.add("Read from DB: " + resultSet.getTimestamp("tick"));
        }

        model.put("records", output);
        return "database";

    } catch (Throwable t) {
        model.put("message", t.getMessage());
        return "error";
    }
}
```

The `database` method adds a row to the `tick` table when you access your app using the `/database` route. It then returns all rows to render in the output.

The `DataSource` shown in the example app code is automatically configured and injected by the Spring Boot framework. It refers to the values in the `src/main/resources/application.properties` file for the database connection configuration.

The example app has `spring.datasource.url` set to the value in the `JDBC_DATABASE_URL` environment variable to establish a pool of connections to the database:

```
spring.datasource.url: ${JDBC_DATABASE_URL}
```

The official Heroku Java buildpack that’s automatically added to your app [sets this `JDBC_DATABASE_URL`](https://devcenter.heroku.com/articles/connecting-to-relational-databases-on-heroku-with-java#using-the-jdbc_database_url) environment variable when a dyno starts up. This variable is dynamic and doesn’t appear in your list of configuration variables when running `heroku config`. You can view it by running the following command:

```
$ heroku run echo \$JDBC_DATABASE_URL
```

Read more about [Heroku PostgreSQL](https://devcenter.heroku.com/articles/heroku-postgresql). You can also install [Redis or other data add-ons](https://elements.heroku.com/addons/categories/data-stores) via `heroku addons:create`.

[Prepare the Local Environment](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#prepare-the-local-environment)
----------------------------------------------------------------------------------------------------------------------------------------------

You must install your app’s dependencies before you can run your app locally.

Run `./mvnw clean install` in your local directory. This command installs the dependencies to your local environment, preparing your system to run the app locally.

```
$ ./mvnw clean install
...
[INFO] Installing /Users/example-user/java-getting-started/pom.xml to /Users/example-user/.m2/repository/com/heroku/java-getting-started/1.0.0-SNAPSHOT/java-getting-started-1.0.0-SNAPSHOT.pom
[INFO] Installing /Users/example-user/java-getting-started/target/java-getting-started-1.0.0-SNAPSHOT.jar to /Users/example-user/.m2/repository/com/heroku/java-getting-started/1.0.0-SNAPSHOT/java-getting-started-1.0.0-SNAPSHOT.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.125 s
[INFO] Finished at: 2023-03-09T17:24:39+01:00
[INFO] ------------------------------------------------------------------------
```

The Maven process compiles and builds a JAR, with dependencies, placing it into your application’s `target` directory. The `spring-boot-maven-plugin` in the `pom.xml` provides this process.

After installing dependencies, you can run your app locally but it still requires a Postgres database. Create a local Postgres database and update your local `.env` file.

`heroku local`, the command used to run apps locally, automatically sets up your environment based on the `.env` file in your app’s root directory. Set the `JDBC_DATABASE_URL` environment variable with your local Postgres database’s connection string:

```
JDBC_DATABASE_URL=jdbc:postgresql://localhost:5432/java_database_name
```

Your local environment is now ready to run your app and connect to the database.

[Run the App Locally](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#run-the-app-locally)
--------------------------------------------------------------------------------------------------------------------------

Ensure you’ve already run `./mvnw clean install` before running your app locally.

Start your application locally with the [`heroku local` CLI command](https://devcenter.heroku.com/articles/heroku-local):

```
$ heroku local --port 5001
...
5:26:58 PM web.1 |  2023-03-09T17:26:58.009+01:00  INFO 39665 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 5001 (http)
5:26:58 PM web.1 |  2023-03-09T17:26:58.014+01:00  INFO 39665 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
5:26:58 PM web.1 |  2023-03-09T17:26:58.014+01:00  INFO 39665 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.5]
5:26:58 PM web.1 |  2023-03-09T17:26:58.055+01:00  INFO 39665 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
5:26:58 PM web.1 |  2023-03-09T17:26:58.056+01:00  INFO 39665 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 497 ms
5:26:58 PM web.1 |  2023-03-09T17:26:58.175+01:00  INFO 39665 --- [           main] o.s.b.a.w.s.WelcomePageHandlerMapping    : Adding welcome page template: index
5:26:58 PM web.1 |  2023-03-09T17:26:58.278+01:00  INFO 39665 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 5001 (http) with context path ''
5:26:58 PM web.1 |  2023-03-09T17:26:58.288+01:00  INFO 39665 --- [           main] c.heroku.java.GettingStartedApplication  : Started GettingStartedApplication in 0.931 seconds (process running for 1.119)
```

Just like the Heroku platform, `heroku local` examines your `Procfile` to determine what command to run.

To see your app running locally, open [http://localhost:5001](http://localhost:5001/) with your web browser

If you want to access the app’s `/database` route locally, ensure that your local Postgres database is running before you visit the URL.

To stop the app from running locally, go back to your terminal window and press `Control+C` to exit.

[Push Local Changes](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#push-local-changes)
------------------------------------------------------------------------------------------------------------------------

In this step, you make local changes to your app and deploy them to Heroku.

Modify `pom.xml` to include a dependency for `jscience` by adding the following code inside the `<dependencies>` element:

In the file `pom.xml`, add the following dependency to the `<dependencies>` element:

```
<dependency>
  <groupId>org.jscience</groupId>
  <artifactId>jscience</artifactId>
  <version>4.3.1</version>
</dependency>
```

In the file `src/main/java/com/heroku/java/GettingStartedApplication.java`, add the following `import` statements for the library:

```
import org.jscience.physics.amount.Amount;
import org.jscience.physics.model.RelativisticModel;
import javax.measure.unit.SI;
```

In the file `GettingStartedApplication.java`, add the following `convert` method:

```
@GetMapping("/convert")
String convert(Map<String, Object> model) {
    RelativisticModel.select();
    var energy = Amount.valueOf("12 GeV");

    model.put("result", "E=mc^2: " + energy + " = " + energy.to(SI.KILOGRAM));
    return "convert";
}
```

Finally, create a `src/main/resources/templates/convert.html` file with these contents:

```
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" th:replace="~{fragments/layout :: layout (~{::body},'hello')}">
<body>

<div class="container">
    <p th:text="${result}"/>
</div>

</body>
</html>
```

[Here’s the final source code](https://github.com/heroku/java-getting-started/blob/localchanges/src/main/java/com/heroku/java/GettingStartedApplication.java) for `GettingStartedApplication.java`. Ensure that your changes look similar. [Here’s a diff](https://github.com/heroku/java-getting-started/compare/localchanges) of all the local changes made.

Test your changes locally:

```
$ ./mvnw clean install
...
[INFO] Installing /Users/example-user/java-getting-started/pom.xml to /Users/example-user/.m2/repository/com/heroku/java-getting-started/1.0.0-SNAPSHOT/java-getting-started-1.0.0-SNAPSHOT.pom
[INFO] Installing /Users/example-user/java-getting-started/target/java-getting-started-1.0.0-SNAPSHOT.jar to /Users/example-user/.m2/repository/com/heroku/java-getting-started/1.0.0-SNAPSHOT/java-getting-started-1.0.0-SNAPSHOT.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.211 s
[INFO] Finished at: 2023-03-09T18:04:10+01:00
[INFO] ------------------------------------------------------------------------

$ heroku local --port 5001
...
6:05:29 PM web.1 |  2023-03-09T18:05:29.514+01:00  INFO 69174 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 492 ms
6:05:29 PM web.1 |  2023-03-09T18:05:29.628+01:00  INFO 69174 --- [           main] o.s.b.a.w.s.WelcomePageHandlerMapping    : Adding welcome page template: index
6:05:29 PM web.1 |  2023-03-09T18:05:29.726+01:00  INFO 69174 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 5001 (http) with context path ''
6:05:29 PM web.1 |  2023-03-09T18:05:29.736+01:00  INFO 69174 --- [           main] c.heroku.java.GettingStartedApplication  : Started GettingStartedApplication in 0.911 seconds (process running for 1.099)
```

Visit your application’s `/convert` path at [http://localhost:5001/convert](http://localhost:5001/convert), which displays some scientific conversions:

```
E=mc^2: 12 GeV = (2.139194076302506E-26 ± 1.4E-42) kg
```

After testing, deploy your changes. Almost every Heroku deployment follows this same pattern. First, use the `git add` command to stage your modified files for commit:

```
$ git add .
```

Next, commit the changes to the repository:

```
$ git commit -m "Add convert endpoint"
```

Now deploy as you did before:

```
$ git push heroku main
```

Finally, check that your updated code successfully deployed by opening your browser to that route:

```
$ heroku open /convert
```

[Define Config Vars](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#define-config-vars)
------------------------------------------------------------------------------------------------------------------------

Heroku lets you externalize your app’s configuration by storing data such as encryption keys or external resource addresses in [config vars](https://devcenter.heroku.com/articles/config-vars).

At runtime, config vars are exposed to your app as environment variables. For example, modify `GettingStartedApplication.java` so that the method obtains an energy value from the `ENERGY` environment variable:

In the file `src/main/java/com/heroku/java/GettingStartedApplication.java`, change the `convert` method:

```
@GetMapping("/convert")
String convert(Map<String, Object> model) {
    RelativisticModel.select();

    final var result = java.util.Optional
            .ofNullable(System.getenv().get("ENERGY"))
            .map(Amount::valueOf)
            .map(energy -> "E=mc^2: " + energy + " = " + energy.to(SI.KILOGRAM))
            .orElse("ENERGY environment variable is not set!");

    model.put("result", result);
    return "convert";
}
```

Recompile the app to integrate this change by running `./mvnw clean install`.

`heroku local` automatically sets up your local environment based on the `.env` file in your app’s root directory. Your sample app already includes a `.env` file with the following contents:

```
ENERGY=20 GeV
```

Your local `.env` file also includes the `JDBC_DATABASE_URL` variable if you set it during the [Run the App Locally](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#run-the-app-locally) step.

Don’t commit the `.env` file to version control as it often includes secure credentials. Include `.env` in your repo’s `.gitignore` file. The sample app repo only includes a `.env` file as an example for this tutorial step.

Run the app with `heroku local --port 5001` and visit [http://localhost:5001/convert](http://localhost:5001/convert) to see the conversion value for 20 GeV.

Now that you know it works as expected locally, set this variable as a config var on your app running on Heroku. Execute the following:

```
$ heroku config:set ENERGY="20 GeV"
Setting ENERGY and restarting ⬢ peaceful-inlet-84135... done, v9
ENERGY: 20 GeV
```

View the app’s config vars using `heroku config` to verify it’s set correctly:

```
$ heroku config
=== peaceful-inlet-84135 Config Vars
DATABASE_URL:         postgres://avhrhofbiyvpct:3ab23026d0fc225bde4544cedabc356904980e6a02a2418ca44d7fd19dad8e03@ec2-23-21-4-7.compute-1.amazonaws.com:5432/d8e8ojni26668k
ENERGY:               20 GeV
PAPERTRAIL_API_TOKEN: [REDACTED]
```

To see your changes in action, deploy your local changes to Heroku and visit the `/convert` route :

```
$ git add .
$ git commit -m "Use ENERGY environment variable"
$ git push heroku main
$ heroku open /convert
```

[Start a One-off Dyno](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#start-a-one-off-dyno)
----------------------------------------------------------------------------------------------------------------------------

The `heroku run` command lets you run maintenance and administrative tasks on your app in a [one-off dyno](https://devcenter.heroku.com/articles/one-off-dynos). It also lets you launch a REPL process attached to your local terminal for experimenting in your app’s environment or your deployed application code:

```
$ heroku run java -version
Running java -version on ⬢ peaceful-inlet-84135... up, run.4406 (Eco)
openjdk version "17.0.6" 2023-01-17 LTS
OpenJDK Runtime Environment Zulu17.40+19-CA (build 17.0.6+10-LTS)
OpenJDK 64-Bit Server VM Zulu17.40+19-CA (build 17.0.6+10-LTS, mixed mode, sharing)
```

If you receive an error, `Error connecting to process`, [configure your firewall](https://devcenter.heroku.com/articles/one-off-dynos#timeout-awaiting-process).

Remember to type `exit` to exit the shell and terminate the dyno.

[Next Steps](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#next-steps)
--------------------------------------------------------------------------------------------------------

Congratulations! You now know how to deploy an app, change its configuration, scale it, view logs, attach add-ons, and run it locally.

Here’s some recommended reading to continue your Heroku journey:

*   [How Heroku Works](https://devcenter.heroku.com/articles/how-heroku-works) provides a technical overview of the concepts encountered while writing, configuring, deploying, and running apps.
*   The [Java category](https://devcenter.heroku.com/categories/java-support) provides more in-depth information on developing and deploying Java apps.
*   The [Deployment category](https://devcenter.heroku.com/categories/deployment) provides a variety of powerful integrations and features to help streamline and simplify your deployments.

[Delete Your App and Add-on](https://devcenter.heroku.com/articles/getting-started-with-java?singlepage=true#delete-your-app-and-add-on)
----------------------------------------------------------------------------------------------------------------------------------------

Remove the app and database from your account. You’re only charged for the resources you used.

This action removes your add-on and any data saved in the database.

```
$ heroku addons:destroy heroku-postgresql
 ▸    WARNING: Destructive Action
 ▸    This command will affect the app peaceful-inlet-84135
 ▸    To proceed, type peaceful-inlet-84135 or re-run this command with
 ▸    --confirm peaceful-inlet-84135

>
```

This action permanently deletes your application

```
$ heroku apps:destroy
 ▸    WARNING: This will delete ⬢ peaceful-inlet-84135 including all add-ons.
 ▸    To proceed, type peaceful-inlet-84135 or re-run this command with
 ▸    --confirm peaceful-inlet-84135

>
```

You can confirm that your add-on and app are gone with these commands:

```
$ heroku addons --all
$ heroku apps --all
```
