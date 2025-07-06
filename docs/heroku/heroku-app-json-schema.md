Title: app.json Schema | Heroku Dev Center

URL Source: https://devcenter.heroku.com/articles/app-json-schema

Last updated May 27, 2025

`app.json` is a manifest format for describing web apps. It declares environment variables, add-ons, and other information required to run an app on Heroku. This document describes the schema in detail.

`The app.json` will only be processed and analyzed when creating a new app, not when re-deploying one.

See the [Setting Up Apps using the Platform API](https://devcenter.heroku.com/articles/setting-up-apps-using-the-heroku-platform-api) for details on how to use `app.json` to set up apps on Heroku.

[Example app.json](https://devcenter.heroku.com/articles/app-json-schema#example-app-json)
------------------------------------------------------------------------------------------

```
{
  "name": "Small Sharp Tool",
  "description": "This app does one little thing, and does it well.",
  "keywords": [
    "productivity",
    "HTML5",
    "scalpel"
  ],
  "website": "https://small-sharp-tool.com/",
  "repository": "https://github.com/jane-doe/small-sharp-tool",
  "logo": "https://small-sharp-tool.com/logo.svg",
  "success_url": "/welcome",
  "scripts": {
    "postdeploy": "bundle exec rake bootstrap"
  },
  "env": {
    "SECRET_TOKEN": {
      "description": "A secret key for verifying the integrity of signed cookies.",
      "generator": "secret"
    },
    "WEB_CONCURRENCY": {
      "description": "The number of processes to run.",
      "value": "5"
    }
  },
  "formation": {
    "web": {
      "quantity": 1,
      "size": "standard-1x"
    }
  },
  "image": "heroku/ruby",
  "addons": [
    "openredis",
    {
      "plan": "mongolab:shared-single-small",
      "as": "MONGO"
    },
    {
      "plan": "heroku-postgresql",
      "options": {
        "version": "9.5"
      }
    }
  ],
  "buildpacks": [
    {
      "url": "https://github.com/stomita/heroku-buildpack-phantomjs"
    }
  ],
  "environments": {
    "test": {
      "scripts": {
        "test": "bundle exec rake test"
      }
    }
  }
}
```

app.json Schema Reference
-------------------------

[Add-ons](https://devcenter.heroku.com/articles/app-json-schema#add-ons)
------------------------------------------------------------------------

_(array, optional)_ An array of strings or objects specifying Heroku add-ons to provision on the app before deploying.

If an add-on plan is given as an object, the following properties configure the add-on:

*   `plan`: _(string, required)_ The add-on and plan to provision. This string should be in the format `addon:plan` or `addon`. If plan is omitted, that addon’s default plan will be provisioned.
*   `as`: _(string, optional)_ The [attachment name](https://devcenter.heroku.com/articles/add-ons#attachment-names-aliases) for the new add-on. If the attachment name is omitted, the add-on will be attached using its default name.
*   `options`: _(object, optional)_ Any add-on-specific options (for example, the database version to provision with [Heroku PostgreSQL](https://devcenter.heroku.com/articles/heroku-postgresql#version-support)). Keys correspond to option names from the add-on’s documentation. Values correspond to add-on option values, and should be strings. For options that do not take a value, use the value `true`.

If an add-on is given as a string, the string should be in the format `addon:plan` or `addon`. The add-on will be provisioned with its default attachment name and options. The object form is preferred.

Ephemeral apps, such as Review and CI apps, override the plan if specified. Instead they use an ephemeral default as specified by the add-on provider. We are working to evolve this model, but in the mean time it helps to provide more appropriate default plans and mitigate churn for providers.

```
{
  "addons": [
    "openredis",
    {
      "plan": "mongolab:shared-single-small",
      "as": "MONGO"
    },
    {
      "plan": "heroku-postgresql",
      "options": {
        "version": "9.5"
      }
    }
  ]
}
```

[Buildpacks](https://devcenter.heroku.com/articles/app-json-schema#buildpacks)
------------------------------------------------------------------------------

_(array, optional)_ An array of objects specifying the buildpacks needed to build the app. Each `buildpack` object must contain a `url` field where the buildpack can be downloaded or cloned from. The last buildpack in the list will be used to determine the [process types](https://devcenter.heroku.com/articles/procfile) for the application.

The [shorthand names for officially supported buildpacks](https://devcenter.heroku.com/articles/officially-supported-buildpacks) can also be used as a URL.

```
{
  "buildpacks": [
    {
      "url": "https://github.com/heroku/heroku-buildpack-pgbouncer"
    },
    {
      "url": "heroku/ruby"
    }
  ]
}
```

The `buildpacks` section of `app.json` is supported on [Cedar](https://devcenter.heroku.com/articles/generations#cedar)-generation apps only. You must set Cloud Native Buildpacks for [Fir](https://devcenter.heroku.com/articles/generations#fir)-generation apps [via project.toml](https://devcenter.heroku.com/articles/managing-buildpacks#set-a-cloud-native-buildpack).

[Description](https://devcenter.heroku.com/articles/app-json-schema#description)
--------------------------------------------------------------------------------

_(string, optional)_ A brief summary of the app: what it does, who it’s for, why it exists, etc.

```
{
  "description": "This app does one little thing, and does it well."
}
```

[env](https://devcenter.heroku.com/articles/app-json-schema#env)
----------------------------------------------------------------

_(object, optional)_ A key-value object for [config variables](https://devcenter.heroku.com/articles/config-vars) to add to the app’s runtime environment. Keys are the names of the config variables. Values can be strings or objects. If the value is a string, it will be used. If the value is an object, it defines specific requirements for that variable:

*   `description`: a human-friendly blurb about what the value is for and how to determine what it should be
*   `value`: a default value to use. This should always be a string.
*   `required`: A boolean indicating whether the given value is required for the app to function (default: `true`).
*   `generator`: a string representing a function to call to generate the value. Currently the only supported generator is `secret`, which generates a pseudo-random string of characters.

```
{
  "env": {
    "SECRET_TOKEN": {
      "description": "A secret key for verifying the integrity of signed cookies.",
      "generator": "secret"
    },
    "WEB_CONCURRENCY": {
      "description": "The number of processes to run.",
      "value": "5"
    }
  }
}
```

[Environments](https://devcenter.heroku.com/articles/app-json-schema#environments)
----------------------------------------------------------------------------------

_(object, optional)_ A key-value object holding environment-specific overrides for app.json keys.

Each key in the object is the name of an environment. The following environment names are understood by the platform:

*   `test`: applied to continuous integration environments created by Heroku CI.
*   `review`: applied to Review Apps environments created by [Heroku Review Apps](https://devcenter.heroku.com/articles/github-integration-review-apps).

Only `test` and `review` are permitted environment names under the `environments` object.

Each value in the `environments` object is an object holding any valid `app.json` keys. When the platform applies `app.json` to an app, the values for keys defined in the applicable environment _completely replace_ the values defined in the base manifest. For example, given the manifest

```
{
  "env": {
    "SECRET_TOKEN": {
      "description": "A secret key for verifying the integrity of signed cookies.",
      "generator": "secret"
    },
    "WEB_CONCURRENCY": "5"
  },
  "environments": {
    "test": {
      "env": {
        "SECRET_TOKEN": "test-secret"
      }
    }
  }
}
```

apps created using the base manifest will have both a `SECRET_TOKEN` config variable (with a generated value) and a `WEB_CONCURRENCY` config variable (with the value `5`), while apps created using the `test` environment will have _only_ a `SECRET_TOKEN` config variable (with a fixed value). The `WEB_CONCURRENCY` config variable will not be set in the `test` environment.

[Formation](https://devcenter.heroku.com/articles/app-json-schema#formation)
----------------------------------------------------------------------------

_(object, optional)_ A key-value object for process type configuration. Keys are the names of the process types. Values are objects like [the formation resource](https://devcenter.heroku.com/articles/platform-api-reference#formation):

*   `quantity`: number of processes to maintain
*   `size`: dyno size according to [this list](https://devcenter.heroku.com/articles/dyno-types)

You incur billing charges for dynos used based on the formation requested.

If the size isn’t specified, apps use the default dyno size for that runtime. If you have multiple processes, to avoid mixing dyno types, it’s highly recommended to explicitly set your dyno sizes with the `formation` key.

```
{
  "formation": {
    "web": {
      "quantity": 1,
      "size": "basic"
    }
  }
}
```

[Image](https://devcenter.heroku.com/articles/app-json-schema#image)
--------------------------------------------------------------------

_(string, optional)_ The Docker image that can be used to build the app. This key is used by [Heroku-Docker](https://github.com/heroku/heroku-docker) to provision local Docker containers.

```
{
  "image": "heroku/nodejs"
}
```

Official, maintained Docker images can be found on [Heroku’s Docker Hub](https://hub.docker.com/u/heroku).

This key is deprecated.

[keywords](https://devcenter.heroku.com/articles/app-json-schema#keywords)
--------------------------------------------------------------------------

_(array, optional)_ An array of strings describing the app.

```
{
  "keywords": [
    "productivity",
    "HTML5",
    "scalpel"
  ]
}
```

[logo (URL)](https://devcenter.heroku.com/articles/app-json-schema#logo-url)
----------------------------------------------------------------------------

_(string, optional)_ The URL of the application’s logo image. Dimensions should be square. Format can be SVG, PNG, or JPG.

```
{
  "logo": "https://small-sharp-tool.com/logo.svg"
}
```

[Name](https://devcenter.heroku.com/articles/app-json-schema#name)
------------------------------------------------------------------

_(string, optional)_ A clean and simple name to identify the template (30 characters max).

```
{
  "name": "Small Sharp Tool"
}
```

[Repository](https://devcenter.heroku.com/articles/app-json-schema#repository)
------------------------------------------------------------------------------

_(string, optional)_ The location of the application’s source code, such as a Git URL, GitHub URL, Subversion URL, or Mercurial URL.

```
{
  "repository": "https://github.com/jane-doe/small-sharp-tool"
}
```

[Scripts](https://devcenter.heroku.com/articles/app-json-schema#scripts)
------------------------------------------------------------------------

_(object, optional)_ A key-value object specifying scripts or shell commands to execute at different stages in the build/release process.

*   `postdeploy`: _(string or object, optional)_ The script or shell command to run one-time setup tasks after the app is created. The value can be a string or an object. If the value is an object, the following properties are accepted:
    *   `command`: _(string, required)_ The script or shell command to run.
    *   `size`: _(string, optional)_ The size of the dyno on which the command will be run. Valid sizes can be found [here](https://devcenter.heroku.com/articles/dyno-types).

The `postdeploy` script is run _once_, after the app is created and not on subsequent deploys to the app

```
{
  "scripts": {
    "postdeploy": "bundle exec rake bootstrap",
  }
}
```

*   `pr-predestroy`: _(string or object, optional)_ The script or shell command to run a cleanup task after review apps are destroyed. The value can be a string or an object. If the value is an object, the following properties are accepted:
    *   `command`: _(string, required)_ The script or shell command to run.
    *   `size`: _(string, optional)_ The size of the dyno on which the command will be run. Valid sizes can be found [here](https://devcenter.heroku.com/articles/dyno-types).

The `pr-predestroy` script is run when review apps are destroyed by merging or closing the associated pull request.

```
{
  "scripts": {
    "pr-predestroy": "bundle exec rake cleanup",
  }
}
```

You will incur billing charges for dynos used based on the formation requested.

[Stack](https://devcenter.heroku.com/articles/app-json-schema#stack)
--------------------------------------------------------------------

_(string, optional)_ The [Heroku stack](https://devcenter.heroku.com/articles/stack) on which the app will run. If this is not specified, the [default stack](https://devcenter.heroku.com/articles/stack#default-stack) will be used.

```
{
  "stack": "heroku-24"
}
```

[success_url](https://devcenter.heroku.com/articles/app-json-schema#success_url)
--------------------------------------------------------------------------------

_(string, optional)_ A URL specifying where to redirect the user once their new app is deployed. If value is a fully-qualified URL, the user should be redirected to that URL. If value begins with a slash `/`, the user should be redirected to that path in their newly deployed app.

```
{
  "success_url": "/welcome"
}
```

[Website](https://devcenter.heroku.com/articles/app-json-schema#website)
------------------------------------------------------------------------

_(string, optional)_ The project’s website.

```
{
  "website": "https://small-sharp-tool.com/"
}
```
