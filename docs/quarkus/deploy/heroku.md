Title: Deploying to Heroku

URL Source: https://quarkus.io/guides/deploying-to-heroku

Markdown Content:
[Edit this Page](https://github.com/quarkusio/quarkus/edit/main/docs/src/main/asciidoc/deploying-to-heroku.adoc)

In this guide you will learn how to deploy a Quarkus based web application as a web-dyno to Heroku.

This guide covers:

*   Update Quarkus HTTP Port

*   Install the Heroku CLI

*   Deploy the application to Heroku

*   Deploy the application as container image to Heroku

    *   Using Docker

    *   Using Podman

*   Deploy the native application as container image to Heroku

[](https://quarkus.io/guides/deploying-to-heroku#prerequisites)Prerequisites
----------------------------------------------------------------------------

To complete this guide, you need:

*   Roughly 1 hour for all modalities

*   An IDE

*   JDK 17+ installed with `JAVA_HOME` configured appropriately

*   Apache Maven 3.9.9

*   Optionally the [Quarkus CLI](https://quarkus.io/guides/cli-tooling) if you want to use it

*   [A Heroku Account](https://www.heroku.com/). You need at least an Eco account to deploy an application.

*   [Heroku CLI installed](https://devcenter.heroku.com/articles/heroku-cli)

[](https://quarkus.io/guides/deploying-to-heroku#introduction)Introduction
--------------------------------------------------------------------------

Heroku is a platform as a service (PaaS) that enables developers to build, run, and operate applications entirely in the cloud. It supports several languages like Java, Ruby, Node.js, Scala, Clojure, Python, PHP, and Go. In addition, it offers a container registry that can be used to deploy prebuilt container images.

Heroku can be used in different ways to run a Quarkus application:

*   As a plain Java program running in a container defined by Heroku’s environment

*   As a containerized Java program running in a container defined by the Quarkus build process

*   As a containerized native program running in a container defined by the Quarkus build process

All three approaches need to be aware of the port that Heroku assigns to it to handle traffic. Luckily, there’s a dynamic configuration property for it.

[](https://quarkus.io/guides/deploying-to-heroku#common-project-setup)Common project setup
------------------------------------------------------------------------------------------

This guide will take as input a simple application created with the Quarkus tooling:

For Windows users:

*   If using cmd, (don’t use backward slash `\` and put everything on the same line)

*   If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=getting-started-with-heroku"`

This command will create a new REST application in the `getting-started-with-heroku` directory.

Let’s make this application a Git repository:

1.   Change to the application directory: `cd getting-started-with-heroku`.

2.   Initialize a new Git repository: `git init -b main`.

3.   Add all files to the repository: `git add .`.

4.   Commit the files: `git commit -a -m 'Initial copy of getting-started'`.

Heroku can react on changes in your repository, run CI and redeploy your application when your code changes. Therefore, we start with a valid repository already.

Also, make sure your Heroku CLI is working:

```
heroku --version
heroku login
```

[](https://quarkus.io/guides/deploying-to-heroku#prepare-the-quarkus-http-port)Prepare the Quarkus HTTP Port
------------------------------------------------------------------------------------------------------------

Heroku picks a random port and assigns it to the container that is eventually running your Quarkus application. That port is available as an environment variable under `$PORT`. The easiest way to make Quarkus in all deployment scenarios aware of it is using the following configuration:

`quarkus.http.port=${PORT:8080}`

This reads as: "Listen on `$PORT` if this is a defined variable, otherwise listen on 8080 as usual." Run the following to add this to your `application.properties`:

```
echo "quarkus.http.port=\${PORT:8080}" >> src/main/resources/application.properties
git commit -am "Configure the HTTP Port."
```

[](https://quarkus.io/guides/deploying-to-heroku#deploy-the-repository-and-build-on-heroku)Deploy the repository and build on Heroku
------------------------------------------------------------------------------------------------------------------------------------

The first variant uses the Quarkus Maven build to create the _quarkus-app_ application structure containing the runnable "fast-jar" as well as all libraries needed inside Heroku’s build infrastructure and then deploying that result, the other one uses a local build process to create an optimized container.

For the first variant, two additional files are needed in your application’s root directory:

*   `system.properties` to configure the Java version

*   `Procfile` to configure how Heroku starts your application

Quarkus needs JDK 17, so we specify that first:

```
echo "java.runtime.version=17" >> system.properties
git add system.properties
git commit -am "Configure the Java version for Heroku."
```

We will deploy a web application so we need to configure the type `web` in the Heroku `Procfile` like this:

```
echo "web: java \$JAVA_OPTS -jar target/quarkus-app/quarkus-run.jar" >> Procfile
git add Procfile
git commit -am "Add a Procfile."
```

Your application should already be runnable via `heroku local web` from the repository root directory. You need to have run `mvn package` before to create the runnable jar for this to succeed.

Now let’s create an application in your account and deploy that repository to it:

`heroku create`

This will create a remote repository in your Heroku account, and it should have also added a heroku remote url to your local repository which you can view using `git remote -v`:

```
starksm@Scotts-Mac-Studio getting-started % git remote -v
heroku	https://git.heroku.com/young-shelf-58876.git (fetch)
heroku	https://git.heroku.com/young-shelf-58876.git (push)
```

Now you can push your application to Heroku and open it in your browser.

```
git push heroku main
heroku open hello
```

The application will have a generated URL and the terminal should output that. `heroku open hello` opens your default browser to access your new application using the '/hello' context. That page should output the text 'hello'.

To access the REST endpoint via curl, get the app URL from the heroku info command:

```
heroku info | grep  "Web URL:"
APP_NAME=<https url info>
curl $APP_NAME/hello
```

Of course, you can use the Heroku CLI to connect this repo to your GitHub account, too, but this is out of scope for this guide.

[](https://quarkus.io/guides/deploying-to-heroku#deploy-as-container)Deploy as container
----------------------------------------------------------------------------------------

The advantage of pushing a whole container is that we are in complete control over its content and maybe even choose to deploy a container with a native executable running on GraalVM.

First, login to Heroku’s container registry:

`heroku container:login`

We need to add an extension to our project to add the capability to build container images:

CLI

Maven

Gradle

`quarkus extension add container-image-docker`

`./mvnw quarkus:add-extension -Dextensions='container-image-docker'`

`./gradlew addExtension --extensions='container-image-docker'`

Then, let’s commit this change:

```
git add pom.xml
git commit -am "Add container-image-docker extension."
```

The image we are going to build needs to be named accordingly to work with Heroku’s registry and deployment. We get the generated name via `heroku info` and pass it on to the (local) build:

```
APP_NAME=`heroku info | grep  "=== .*" |sed "s/=== //"`
./mvnw clean package\
  -Dquarkus.container-image.build=true\
  -Dquarkus.container-image.group=registry.heroku.com/$APP_NAME\
  -Dquarkus.container-image.name=web\
  -Dquarkus.container-image.tag=latest
```

[](https://quarkus.io/guides/deploying-to-heroku#push-and-release-the-image)Push and release the image
------------------------------------------------------------------------------------------------------

You can now push the image and release it.

The initial push is rather big, as all layers of the image need to be transferred. The following pushes will be smaller.

### [](https://quarkus.io/guides/deploying-to-heroku#pushing-through-docker)Pushing through Docker

With Docker installed, these steps are simple:

```
docker push registry.heroku.com/$APP_NAME/web
heroku stack:set container
heroku container:release web --app $APP_NAME
```

### [](https://quarkus.io/guides/deploying-to-heroku#pushing-through-podman)Pushing through Podman

When you want to use Podman as a drop-in-replacement for Docker, you will have some problems because the Heroku CLI depends on Docker and doesn’t support the OCI format. But there are possible solutions for these problems.

Cannot find docker, please ensure docker is installed.

The problem is obviously that the heroku-cli can’t find docker. This is quite easy to resolve, because the podman cli is docker-compatible. We just need to create a symlink from podman to docker:

`sudo ln -s $(which podman) /usr/local/bin/docker`

Error writing manifest: Error uploading manifest latest to registry.heroku.com/$APP_NAME/web: unsupported

Instead of doing a normal podman push (OCI format) we must use a workaround in order to push and release our app through Podman and the Heroku CLI in the desired format (v2s2 - Docker Image Manifest Version 2, Schema 2). Also [skopeo](https://github.com/containers/skopeo) is needed.

```
CONTAINER_DIR="target/container-dir"
mkdir $CONTAINER_DIR
podman push --format=v2s2 "registry.heroku.com/$APP_NAME/web" dir:$CONTAINER_DIR
skopeo --debug copy dir:$CONTAINER_DIR "docker://registry.heroku.com/$APP_NAME/web:latest"
heroku container:release web --app "$APP_NAME"
rm -rf $CONTAINER_DIR
```

*   [Source of solutions and workarounds](https://urhengulas.github.io/blog/podman_heroku.html)

[](https://quarkus.io/guides/deploying-to-heroku#check-the-logs)Check the logs
------------------------------------------------------------------------------

You can and should check the logs to see if your application is now indeed running from the container:

`heroku logs --app $APP_NAME --tail`

[](https://quarkus.io/guides/deploying-to-heroku#deploy-as-native-application-inside-a-container)Deploy as native application inside a container
------------------------------------------------------------------------------------------------------------------------------------------------

The biggest advantage we take when deploying our app as a container is to deploy a container with the natively compiled application. Why? Because Heroku will stop or sleep the application when there’s no incoming traffic. A native application will wake up much faster from its sleep.

The process is pretty much the same. We opt in to compiling a native image inside a local container, so that we don’t have to deal with installing GraalVM locally:

```
APP_NAME=`heroku info | grep  "=== .*" |sed "s/=== //"`
./mvnw clean package \
  -Dquarkus.container-image.build=true \
  -Dquarkus.container-image.group=registry.heroku.com/$APP_NAME \
  -Dquarkus.container-image.name=web \
  -Dquarkus.container-image.tag=latest \
  -Dnative \
  -Dquarkus.native.container-build=true
```

After that, push and release again using Docker or Podman (see above) and check the logs.
