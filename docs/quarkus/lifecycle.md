Title: Application Initialization and Termination

URL Source: https://quarkus.io/guides/lifecycle

Markdown Content:
Application Initialization and Termination - Quarkus

===============

Application Initialization and Termination
==========================================

You often need to execute custom actions when the application starts and clean up everything when the application stops. This guide explains how to:

*   Write a Quarkus application with a main method

*   Write command mode applications that run a task and then terminate

*   Be notified when the application starts

*   Be notified when the application stops

[](https://quarkus.io/guides/lifecycle#prerequisites)Prerequisites
------------------------------------------------------------------

To complete this guide, you need:

*   Roughly 15 minutes

*   An IDE

*   JDK 17+ installed with `JAVA_HOME` configured appropriately

*   Apache Maven 3.9.9

*   Optionally the [Quarkus CLI](https://quarkus.io/guides/cli-tooling) if you want to use it

*   Optionally Mandrel or GraalVM installed and [configured appropriately](https://quarkus.io/guides/building-native-image#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

[](https://quarkus.io/guides/lifecycle#solution)Solution
--------------------------------------------------------

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `lifecycle-quickstart`[directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/lifecycle-quickstart).

[](https://quarkus.io/guides/lifecycle#creating-the-maven-project)Creating the Maven project
--------------------------------------------------------------------------------------------

First, we need a new project. Create a new project with the following command:

CLI

Maven

```bash
quarkus create app org.acme:lifecycle-quickstart \
    --no-code
cd lifecycle-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](https://quarkus.io/guides/cli-tooling) guide.

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.24.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=lifecycle-quickstart \
    -DnoCode
cd lifecycle-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

*   If using cmd, (don’t use backward slash `\` and put everything on the same line)

*   If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=lifecycle-quickstart"`

It generates:

*   the Maven structure

*   example `Dockerfile` files for both `native` and `jvm` modes

*   the application configuration file

[](https://quarkus.io/guides/lifecycle#the-main-method)The main method
----------------------------------------------------------------------

By default, Quarkus will automatically generate a main method, that will bootstrap Quarkus and then just wait for shutdown to be initiated. Let’s provide our own main method:

```java
package com.acme;

import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.Quarkus;

@QuarkusMain  (1)
public class Main {

    public static void main(String ... args) {
        System.out.println("Running main method");
        Quarkus.run(args); (2)
    }
}
```

**1**This annotation tells Quarkus to use this as the main method, unless it is overridden in the config
**2**This launches Quarkus

This main class will bootstrap Quarkus and run it until it stops. This is no different to the automatically generated main class, but has the advantage that you can just launch it directly from the IDE without needing to run a Maven or Gradle command.

It is not recommenced to do any business logic in this main method, as Quarkus has not been set up yet, and Quarkus may run in a different ClassLoader. If you want to perform logic on startup use an `io.quarkus.runtime.QuarkusApplication` as described below.

If we want to actually perform business logic on startup (or write applications that complete a task and then exit) we need to supply a `io.quarkus.runtime.QuarkusApplication` class to the run method. After Quarkus has been started the `run` method of the application will be invoked. When this method returns the Quarkus application will exit.

If you want to perform logic on startup you should call `Quarkus.waitForExit()`, this method will wait until a shutdown is requested (either from an external signal like when you press `Ctrl+C` or because a thread has called `Quarkus.asyncExit()`).

An example of what this looks like is below:

```java
package com.acme;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main {
    public static void main(String... args) {
        Quarkus.run(MyApp.class, args);
    }

    public static class MyApp implements QuarkusApplication {

        @Override
        public int run(String... args) throws Exception {
            System.out.println("Do startup logic here");
            Quarkus.waitForExit();
            return 0;
        }
    }
}
```

`Quarkus.run` also provides a version that allows the code to handle errors. For example:

```java
package com.acme;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main {
    public static void main(String... args) {
        Quarkus.run(MyApp.class,
        (exitCode, exception) -> {
            // do whatever
        },
        args);
    }

    public static class MyApp implements QuarkusApplication {

        @Override
        public int run(String... args) throws Exception {
            System.out.println("Do startup logic here");
            Quarkus.waitForExit();
            return 0;
        }
    }
}
```

### [](https://quarkus.io/guides/lifecycle#injecting-the-command-line-arguments)Injecting the command line arguments

It is possible to inject the arguments that were passed in on the command line:

```java
@Inject
@CommandLineArguments
String[] args;
```

Command line arguments can be passed to the application through the `-D` flag with the property `quarkus.args`:

*   For Quarkus dev mode:

CLI Maven Gradle
```bash
quarkus dev -Dquarkus.args=cmd-args
```
```bash
./mvnw quarkus:dev -Dquarkus.args=cmd-args
```
```bash
./gradlew --console=plain quarkusDev -Dquarkus.args=cmd-args
```
*   For a runner jar: `java -Dquarkus.args=<cmd-args> -jar target/quarkus-app/quarkus-run.jar`

*   For a native executable: `./target/lifecycle-quickstart-1.0-SNAPSHOT-runner -Dquarkus.args=<cmd-args>`

[](https://quarkus.io/guides/lifecycle#listening-for-startup-and-shutdown-events)Listening for startup and shutdown events
--------------------------------------------------------------------------------------------------------------------------

Create a new class named `AppLifecycleBean` (or pick another name) in the `org.acme.lifecycle` package, and copy the following content:

```java
package org.acme.lifecycle;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AppLifecycleBean {

    private static final Logger LOGGER = Logger.getLogger("ListenerBean");

    void onStart(@Observes StartupEvent ev) {               (1)
        LOGGER.info("The application is starting...");
    }

    void onStop(@Observes ShutdownEvent ev) {               (2)
        LOGGER.info("The application is stopping...");
    }

}
```

**1**Method called when the application is starting
**2**Method called when the application is terminating

The events are also called in _dev mode_ between each redeployment.

The methods can access injected beans. Check the [AppLifecycleBean.java](https://github.com/quarkusio/quarkus-quickstarts/blob/main/lifecycle-quickstart/src/main/java/org/acme/lifecycle/AppLifecycleBean.java) class for details.

### [](https://quarkus.io/guides/lifecycle#what-is-the-difference-from-initializedapplicationscoped-class-and-destroyedapplicationscoped-class)What is the difference from `@Initialized(ApplicationScoped.class)` and `@Destroyed(ApplicationScoped.class)`

In the JVM mode, there is no real difference, except that `StartupEvent` is always fired **after**`@Initialized(ApplicationScoped.class)` and `ShutdownEvent` is fired **before**`@Destroyed(ApplicationScoped.class)`. For a native executable build, however, `@Initialized(ApplicationScoped.class)` is fired as **part of the native build process**, whereas `StartupEvent` is fired when the native image is executed. See [Three Phases of Bootstrap and Quarkus Philosophy](https://quarkus.io/guides/writing-extensions#bootstrap-three-phases) for more details.

In CDI applications, an event with qualifier `@Initialized(ApplicationScoped.class)` is fired when the application context is initialized. See [the spec](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#application_context) for more info.

### [](https://quarkus.io/guides/lifecycle#startup_annotation)Using `@Startup` to initialize a CDI bean at application startup

A bean represented by a class, producer method or field annotated with `@Startup` is initialized at application startup:

```java
package org.acme.lifecycle;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

@Startup (1)
@ApplicationScoped
public class EagerAppBean {

   private final String name;

   EagerAppBean(NameGenerator generator) { (2)
     this.name = generator.createName();
   }
}
```

**1**For each bean annotated with `@Startup` a synthetic observer of `StartupEvent` is generated. The default priority is used.
**2**The bean constructor is called when the application starts and the resulting contextual instance is stored in the application context.

`@Dependent` beans are destroyed immediately afterwards to follow the behavior of observers declared on `@Dependent` beans.

If a class is annotated with `@Startup` but with no scope annotation then `@ApplicationScoped` is added automatically.

The `@Startup` annotation can be also declared on a non-static non-producer no-args method:

```java
package org.acme.lifecycle;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EagerAppBean {

   @Startup
   void init() { (1)
     doSomeCoolInit();
   }
}
```

**1**The bean is created and the `init()` method is invoked upon the contextual instance when the application starts.

### [](https://quarkus.io/guides/lifecycle#shutdown_annotation)Using `@Shutdown` to execute a business method of a CDI bean during application shutdown

The `@io.quarkus.runtime.Shutdown` annotation is used to mark a business method of a CDI bean that should be executed during application shutdown. The annotated method must be non-private and non-static and declare no arguments. The behavior is similar to a declaration of a `ShutdownEvent` observer. The following examples are functionally equivalent.

```java
import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class Bean1 {
   void onShutdown(@Observes ShutdownEvent event) {
      // place the logic here
   }
}

@ApplicationScoped
class Bean2 {

   @Shutdown
   void shutdown() {
      // place the logic here
   }
}
```

[](https://quarkus.io/guides/lifecycle#package-and-run-the-application)Package and run the application
------------------------------------------------------------------------------------------------------

Run the application with:

CLI

Maven

Gradle

```bash
quarkus dev
```

```bash
./mvnw quarkus:dev
```

```bash
./gradlew --console=plain quarkusDev
```

The logged message is printed. When the application is stopped, the second log message is printed.

As usual, the application can be packaged using:

CLI

Maven

Gradle

```bash
quarkus build
```

```bash
./mvnw install
```

```bash
./gradlew build
```

and executed using `java -jar target/quarkus-app/quarkus-run.jar`.

You can also generate the native executable using:

CLI

Maven

Gradle

```bash
quarkus build --native
```

```bash
./mvnw install -Dnative
```

```bash
./gradlew build -Dquarkus.native.enabled=true
```

[](https://quarkus.io/guides/lifecycle#launch-modes)Launch Modes
----------------------------------------------------------------

Quarkus has 3 different launch modes, `NORMAL` (i.e. production), `DEVELOPMENT` and `TEST`. If you are running `quarkus:dev` then the mode will be `DEVELOPMENT`, if you are running a JUnit test it will be `TEST`, otherwise it will be `NORMAL`.

Your application can get the launch mode by injecting the `io.quarkus.runtime.LaunchMode` enum into a CDI bean, or by invoking the static method `io.quarkus.runtime.LaunchMode.current()`.

[](https://quarkus.io/guides/lifecycle#graceful-shutdown)Graceful Shutdown
--------------------------------------------------------------------------

Quarkus includes support for graceful shutdown, this allows Quarkus to wait for running requests to finish, up till a set timeout. By default, this is disabled, however you can configure this by setting the `quarkus.shutdown.timeout` config property. When this is set shutdown will not happen until all running requests have completed, or until this timeout has elapsed.

Extensions that accept requests need to add support for this on an individual basis. At the moment only the HTTP extension supports this, so shutdown may still happen when messaging requests are active.

Quarkus supports a delay time, where the application instance still responds to requests, but the readiness probe fails. This gives the infrastructure time to recognize that the instance is shutting down and stop routing traffic to the instance. This feature can be enabled by setting the build-time property `quarkus.shutdown.delay-enabled` to `true`. The delay can then be configured by setting the runtime property `quarkus.shutdown.delay`. It is not set by default, thus no delay is applied.

To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() javadoc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.

You can also use a simplified format, starting with a number:

*   If the value is only a number, it represents time in seconds.

*   If the value is a number followed by `ms`, it represents time in milliseconds.

In other cases, the simplified format is translated to the `java.time.Duration` format for parsing:

*   If the value is a number followed by `h`, `m`, or `s`, it is prefixed with `PT`.

*   If the value is a number followed by `d`, it is prefixed with `P`.

*   [Prerequisites](https://quarkus.io/guides/lifecycle#prerequisites)
*   [Solution](https://quarkus.io/guides/lifecycle#solution)
*   [Creating the Maven project](https://quarkus.io/guides/lifecycle#creating-the-maven-project)
*   [The main method](https://quarkus.io/guides/lifecycle#the-main-method)
    *   [Injecting the command line arguments](https://quarkus.io/guides/lifecycle#injecting-the-command-line-arguments)

*   [Listening for startup and shutdown events](https://quarkus.io/guides/lifecycle#listening-for-startup-and-shutdown-events)
    *   [What is the difference from `@Initialized(ApplicationScoped.class)` and `@Destroyed(ApplicationScoped.class)`](https://quarkus.io/guides/lifecycle#what-is-the-difference-from-initializedapplicationscoped-class-and-destroyedapplicationscoped-class)
    *   [Using `@Startup` to initialize a CDI bean at application startup](https://quarkus.io/guides/lifecycle#startup_annotation)
    *   [Using `@Shutdown` to execute a business method of a CDI bean during application shutdown](https://quarkus.io/guides/lifecycle#shutdown_annotation)

*   [Package and run the application](https://quarkus.io/guides/lifecycle#package-and-run-the-application)
*   [Launch Modes](https://quarkus.io/guides/lifecycle#launch-modes)
*   [Graceful Shutdown](https://quarkus.io/guides/lifecycle#graceful-shutdown)
