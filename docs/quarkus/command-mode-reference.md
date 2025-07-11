Title: Command Mode Applications

URL Source: https://quarkus.io/guides/command-mode-reference

Markdown Content:
[Edit this Page](https://github.com/quarkusio/quarkus/edit/main/docs/src/main/asciidoc/command-mode-reference.adoc)

This reference covers how to write applications that run and then exit.

[](https://quarkus.io/guides/command-mode-reference#solution)Solution
---------------------------------------------------------------------

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

The solution is located in the `getting-started-command-mode`[directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/getting-started-command-mode).

[](https://quarkus.io/guides/command-mode-reference#creating-the-maven-project)Creating the Maven project
---------------------------------------------------------------------------------------------------------

First, we need to create a new Quarkus project with the following command:

For Windows users:

*   If using cmd, (don’t use backward slash `\` and put everything on the same line)

*   If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=command-mode-quickstart"`

The suggested project creation command lines disable the codestarts to avoid including a REST server. Similarly, if you use code.quarkus.io to generate a project, you need to go to **MORE OPTIONS → Starter Code** and select **No** to avoid adding the Quarkus REST (formerly RESTEasy Reactive) extension.

The Quarkus REST extension is added automatically only if you ask for codestarts and you didn’t specify any extensions.

[](https://quarkus.io/guides/command-mode-reference#writing-command-mode-applications)Writing Command Mode Applications
-----------------------------------------------------------------------------------------------------------------------

There are two different approaches that can be used to implement applications that exit.

1.   Implement `QuarkusApplication` and have Quarkus run this method automatically

2.   Implement `QuarkusApplication` and a Java main method, and use the Java main method to launch Quarkus

In this document the `QuarkusApplication` instance is referred to as the application main, and a class with a Java main method is the Java main.

The simplest possible command mode application with access to Quarkus APIs might appear as follows:

```
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain    (1)
public class HelloWorldMain implements QuarkusApplication {
  @Override
  public int run(String... args) throws Exception {   (2)
    System.out.println("Hello " + args[0]);
    return 0;
 }
}
```

**1**The `@QuarkusMain` annotation tells Quarkus that this is the main entry point.
**2**The `run` method is invoked once Quarkus starts, and the application stops when it finishes.

### [](https://quarkus.io/guides/command-mode-reference#main-method)Main method

If we want to use a Java main to run the application main it would look like:

```
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class JavaMain {

    public static void main(String... args) {
        Quarkus.run(HelloWorldMain.class, args);
    }
}
```

This is effectively the same as running the `HelloWorldMain` application main directly, but has the advantage it can be run from the IDE.

If a class that implements `QuarkusApplication` and has a Java main then the Java main will be run.

It is recommended that a Java main perform very little logic, and just launch the application main. In development mode the Java main will run in a different ClassLoader to the main application, so may not behave as you would expect.

#### [](https://quarkus.io/guides/command-mode-reference#multiple-main-methods)Multiple Main Methods

It is possible to have multiple main methods in an application, and select between them at build time. The `@QuarkusMain` annotation takes an optional 'name' parameter, and this can be used to select the main to run using the `quarkus.package.main-class` build time configuration option. If you don’t want to use annotations this can also be used to specify the fully qualified name of a main class.

By default, the `@QuarkusMain` with no name (i.e. the empty string) will be used, and if it is not present and `quarkus.package.main-class` is not specified then Quarkus will automatically generate a main class that just runs the application.

The `name` of `@QuarkusMain` must be unique (including the default of the empty string). If you have multiple `@QuarkusMain` annotations in your application the build will fail if the names are not unique.

### [](https://quarkus.io/guides/command-mode-reference#the-command-mode-lifecycle)The command mode lifecycle

When running a command mode application the basic lifecycle is as follows:

1.   Start Quarkus

2.   Run the `QuarkusApplication` main method

3.   Shut down Quarkus and exit the JVM after the main method returns

Shutdown is always initiated by the application main thread returning. If you want to run some logic on startup, and then run like a normal application (i.e. not exit) then you should call `Quarkus.waitForExit` from the main thread (A non-command mode application is essentially just running an application that just calls `waitForExit`).

If you want to shut down a running application and you are not in the main thread, then you should call `Quarkus.asyncExit` in order to unblock the main thread and initiate the shutdown process.

### [](https://quarkus.io/guides/command-mode-reference#running-the-application)Running the application

To run the command mode application on the JVM, first build it using `mvnw package` or equivalent.

Then launch it:

`java -jar target/quarkus-app/quarkus-run.jar`

You can also build a native application with `mvnw package -Dnative`, and launch it with something like:

`./target/getting-started-command-mode-1.0-SNAPSHOT-runner`

### [](https://quarkus.io/guides/command-mode-reference#development-mode)Development Mode

Also, for command mode applications, the dev mode is supported. When you start your application in dev mode, the command mode application is executed:

CLI

Maven

Gradle

`quarkus dev`

`./mvnw quarkus:dev`

`./gradlew --console=plain quarkusDev`

As command mode applications will often require arguments to be passed on the command line, this is also possible in dev mode:

CLI

Maven

Gradle

`quarkus dev '--help'`

`./mvnw quarkus:dev -Dquarkus.args='--help'`

`./gradlew quarkusDev --quarkus-args='--help'`

You should see the following down the bottom of the screen after the application is stopped:

```
--
Press [space] to restart, [e] to edit command line args (currently '-w --tags 1.0.1.Final'), [r] to resume testing, [o] Toggle test output, [h] for more options>
```

You can press the `Space bar` key and the application will be started again. You can also use the `e` hotkey to edit the command line arguments and restart your application.

[](https://quarkus.io/guides/command-mode-reference#testing-command-mode-applications)Testing Command Mode Applications
-----------------------------------------------------------------------------------------------------------------------

Command Mode applications can be tested using the `@QuarkusMainTest` and `@QuarkusMainIntegrationTest` annotations. These work in a similar way to `@QuarkusTest` and `@QuarkusIntegrationTest` where `@QuarkusMainTest` will run the CLI tests within the current JVM, while `QuarkusIntegrationTest` is used to run the generated executable (both jars and native).

We can write a simple test for our CLI application above as follows:

```
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusMainTest
public class HelloTest {

    @Test
    @Launch("World")
    public void testLaunchCommand(LaunchResult result) {
        Assertions.assertTrue(result.getOutput().contains("Hello World"));
    }

    @Test
    @Launch(value = {}, exitCode = 1)
    public void testLaunchCommandFailed() {
    }

    @Test
    public void testManualLaunch(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("Everyone");
        Assertions.assertEquals(0, result.exitCode());
        Assertions.assertTrue(result.getOutput().contains("Hello Everyone"));
    }
}
```

Each test method must be annotated with `@Launch` to automatically start the application or have a `QuarkusMainLauncher` parameter to manually launch the application.

We can then extend this with an integration test that can be used to test the native executable or runnable jar:

```
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;

@QuarkusMainIntegrationTest
public class HelloIT extends HelloTest {
}
```

### [](https://quarkus.io/guides/command-mode-reference#mocking)Mocking

CDI injection is not supported in the `@QuarkusMainTest` tests. Consequently, mocking CDI beans with `QuarkusMock` or `@InjectMock` is not supported either.

It is possible to mock CDI beans by leveraging [test profiles](https://quarkus.io/guides/getting-started-testing#testing_different_profiles) though.

For instance, in the following test, the launched application would receive a mocked singleton `CdiBean1`. The implementation `MockedCdiBean1` is provided by the test:

```
package org.acme.commandmode.test;

import java.util.Set;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.acme.commandmode.test.MyCommandModeTest.MyTestProfile;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(MyTestProfile.class)
public class MyCommandModeTest {

    @Test
    @Launch(value = {})
    public void testLaunchCommand(LaunchResult result) {
        // ... assertions ...
    }

    public static class MyTestProfile implements QuarkusTestProfile {

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(MockedCdiBean1.class); (1)
        }
    }

    @Alternative (2)
    @Singleton (3)
    public static class MockedCdiBean1 implements CdiBean1 {

        @Override
        public String myMethod() {
            return "mocked value";
        }
    }
}
```

**1**List all the CDI beans for which you want to enable an alternative mocked bean.
**2**Use `@Alternative` without a `@Priority`. Make sure you don’t use `@Mock`.
**3**The scope of the mocked bean should be consistent with the original one.

Using this pattern, you can enable specific alternatives for any given test.
