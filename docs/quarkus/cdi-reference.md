Title: Contexts and Dependency Injection

URL Source: https://quarkus.io/guides/cdi-reference

Markdown Content:
[Edit this Page](https://github.com/quarkusio/quarkus/edit/main/docs/src/main/asciidoc/cdi-reference.adoc)

Quarkus DI solution (also called ArC) is based on the [Jakarta Contexts and Dependency Injection 4.1](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html) specification. It implements the CDI Lite specification, with selected improvements on top, and passes the CDI Lite TCK. It does not implement CDI Full. See also [the list of supported features and limitations](https://quarkus.io/guides/cdi-reference#supported_features_and_limitations). Most of the existing CDI code should work just fine but there are some small differences which follow from the Quarkus architecture and goals.

The [CDI integration guide](https://quarkus.io/guides/cdi-integration) has more detail on common CDI-related integration use cases, and example code for solutions.

[](https://quarkus.io/guides/cdi-reference#bean_discovery)1. Bean Discovery
---------------------------------------------------------------------------

Bean discovery in CDI is a complex process which involves legacy deployment structures and accessibility requirements of the underlying module architecture. However, Quarkus is using a **simplified bean discovery**. There is only single bean archive with the [bean discovery mode `annotated`](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#default_bean_discovery) and no visibility boundaries.

The bean archive is synthesized from:

*   the application classes,

*   dependencies that contain a `beans.xml` descriptor (content is ignored),

*   dependencies that contain a Jandex index - `META-INF/jandex.idx`,

*   dependencies referenced by `quarkus.index-dependency` in `application.properties`,

*   and Quarkus integration code.

Bean classes that don’t have a [bean defining annotation](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#bean_defining_annotations) are not discovered. This behavior is defined by CDI. But producer methods and fields and observer methods are discovered even if the declaring class is not annotated with a bean defining annotation (this behavior is different to what is defined in CDI). In fact, the declaring bean classes are considered annotated with `@Dependent`.

Quarkus extensions may declare additional discovery rules. For example, `@Scheduled` business methods are registered even if the declaring class is not annotated with a bean defining annotation.

### [](https://quarkus.io/guides/cdi-reference#how-to-generate-a-jandex-index)1.1. How to Generate a Jandex Index

A dependency with a Jandex index is automatically scanned for beans. To generate the index just add the following plugin to your build file:

If you can’t modify the dependency, you can still index it by adding `quarkus.index-dependency` entries to your `application.properties`:

```
quarkus.index-dependency.<name>.group-id=
quarkus.index-dependency.<name>.artifact-id=(this one is optional)
quarkus.index-dependency.<name>.classifier=(this one is optional)
```

If no `artifact-id` is specified then all dependencies with the specified `group-id` are indexed.

For example, the following entries ensure that the `org.acme:acme-api` dependency is indexed:

Example application.properties

```
quarkus.index-dependency.acme.group-id=org.acme (1)
quarkus.index-dependency.acme.artifact-id=acme-api (2)
```

**1**Value is a group id for a dependency identified by name `acme`.
**2**Value is an artifact id for a dependency identified by name `acme`.

### [](https://quarkus.io/guides/cdi-reference#how-to-exclude-types-and-dependencies-from-discovery)1.2. How To Exclude Types and Dependencies from Discovery

It may happen that some beans from third-party libraries do not work correctly in Quarkus. A typical example is a bean injecting a portable extension. In such case, it’s possible to exclude types and dependencies from the bean discovery. The `quarkus.arc.exclude-types` property accepts a list of string values that are used to match classes that should be excluded.

Table 1. Value Examples Value Description
`org.acme.Foo`Match the fully qualified name of the class
`org.acme.*`Match classes with package `org.acme`
`org.acme.**`Match classes where the package starts with `org.acme`
`Bar`Match the simple name of the class

Example application.properties

`quarkus.arc.exclude-types=org.acme.Foo,org.acme.*,Bar (1)(2)(3)`

**1**Exclude the type `org.acme.Foo`.
**2**Exclude all types from the `org.acme` package.
**3**Exclude all types whose simple name is `Bar`

It is also possible to exclude a dependency artifact that would be otherwise scanned for beans. For example, because it contains a `beans.xml` descriptor.

Example application.properties

```
quarkus.arc.exclude-dependency.acme.group-id=org.acme (1)
quarkus.arc.exclude-dependency.acme.artifact-id=acme-services (2)
```

**1**Value is a group id for a dependency identified by name `acme`.
**2**Value is an artifact id for a dependency identified by name `acme`.

[](https://quarkus.io/guides/cdi-reference#string-based-qualifiers)2. String-Based Qualifiers
---------------------------------------------------------------------------------------------

The `@Named` qualifier, which you might be familiar with, is a _string-based qualifier_. That is, it’s the string value of the qualifier annotation who determines whether the qualifier matches or not. This is not type-safe and should not be the norm in CDI applications. Specific qualifier types should be preferred.

However, sometimes string-based qualifiers are necessary. In that case, avoid the `@Named` qualifier, because in CDI, it works differently to all other qualifiers.

Specifically: if the only qualifier a bean has is `@Named`, it also automatically gets `@Default`. This means that if multiple beans of the same type exist, one of them without qualifiers and the others with `@Named`, they _all_ get the `@Default` qualifier and bean resolution will error with ambiguity. For example:

```
@ApplicationScoped
public class Producers {
    @Produces
    MyBean produce() {
        ...
    }

    @Produces
    @Named("foo")
    MyBean produceFoo() {
        ...
    }
}

@ApplicationScoped
public class Consumer {
    @Inject
    MyBean bean;
}
```

In this case, the `Consumer#bean` injection point will cause ambiguity error, because both `MyBean` producers will have the `@Default` qualifier.

Instead of `@Named`, use `@io.smallrye.common.annotation.Identifier`. This is a regular qualifier that works like all others. So if we rewrite the example to use `@Identifier`:

```
@ApplicationScoped
public class Producers {
    @Produces
    MyBean produce() {
        ...
    }

    @Produces
    @Identifier("foo")
    MyBean produceFoo() {
        ...
    }
}

@ApplicationScoped
public class Consumer {
    @Inject
    MyBean bean;
}
```

Only the first producer will get the `@Default` qualifier, the second will not. Hence, there will be no error and everything will work as expected.

### [](https://quarkus.io/guides/cdi-reference#when-to-use-named)2.1. When To Use `@Named`?

There is one case where `@Named` is the right thing to use: specifying an external identifier for a different language that doesn’t support dependency injection directly.

For example:

```
@ApplicationScoped
@Named("myBean")
public class MyBean {
    public String getValue() {
        ...
    }
}

@ApplicationScoped
public class Consumer {
    @Inject
    MyBean bean;
}
```

As you can see, in the application code, the bean is injected without a qualifier. The bean name is only used to refer to the bean in the other language.

Historically, the most common external language that used bean names was JSF. In Quarkus, we have [Qute](https://quarkus.io/guides/qute-reference#injecting-beans-directly-in-templates). In a Qute template, one would refer to the bean using its name:

`The current value is {inject:myBean.value}.`

Outside of this use-case, just use `@Identifier`.

[](https://quarkus.io/guides/cdi-reference#native-executables-and-private-members)3. Native Executables and Private Members
---------------------------------------------------------------------------------------------------------------------------

Quarkus is using GraalVM to build a native executable. One of the limitations of GraalVM is the usage of [Reflection](https://www.graalvm.org/jdk21/reference-manual/native-image/Reflection/). Reflective operations are supported but all relevant members must be registered for reflection explicitly. Those registrations result in a bigger native executable.

And if Quarkus DI needs to access a private member it **has to use reflection**. That’s why Quarkus users are encouraged _not to use private members_ in their beans. This involves injection fields, constructors and initializers, observer methods, producer methods and fields, disposers and interceptor methods.

How to avoid using private members? You can use package-private modifiers:

```
@ApplicationScoped
public class CounterBean {

    @Inject
    CounterService counterService; (1)

    void onMessage(@Observes Event msg) { (2)
    }
}
```

**1**A package-private injection field.
**2**A package-private observer method.

Or constructor injection:

```
@ApplicationScoped
public class CounterBean {

    private CounterService service;

    CounterBean(CounterService service) { (1)
      this.service = service;
    }
}
```

**1**A package-private constructor injection. `@Inject` is optional in this particular case.

[](https://quarkus.io/guides/cdi-reference#supported_features_and_limitations)4.  Supported Features and Limitations
--------------------------------------------------------------------------------------------------------------------

The CDI Lite specification is fully supported. The following features from CDI Full are also supported:

*   Decorators

    *   Decoration of built-in beans, such as `Event`, is not supported

*   `BeanManager`

    *   In addition to the `BeanContainer` methods, the following methods are supported: `getInjectableReference()`, `resolveDecorators()`

*   `@SessionScoped`

    *   Only with the Undertow extension; see [here](https://quarkus.io/guides/cdi#bean-scope-available) for details

The _method invokers_ implementation supports asynchronous methods. The following methods are considered asynchronous and `@Dependent` instances are only destroyed when the asynchronous action completes:

*   methods that declare a return type of `CompletionStage`, `Uni`, or `Multi`

These additional features are not covered by the CDI Lite TCK.

[](https://quarkus.io/guides/cdi-reference#nonstandard_features)5. Non-standard Features
----------------------------------------------------------------------------------------

### [](https://quarkus.io/guides/cdi-reference#eager-instantiation-of-beans)5.1. Eager Instantiation of Beans

#### [](https://quarkus.io/guides/cdi-reference#lazy_by_default)5.1.1. Lazy By Default

By default, CDI beans are created lazily, when needed. What exactly "needed" means depends on the scope of a bean.

*   A **normal scoped bean** (`@ApplicationScoped`, `@RequestScoped`, etc.) is needed when a method is invoked upon an injected instance (contextual reference per the specification).

In other words, injecting a normal scoped bean will not suffice because a _client proxy_ is injected instead of a contextual instance of the bean.

*   A **bean with a pseudo-scope** (`@Dependent` and `@Singleton` ) is created when injected.

Lazy Instantiation Example

```
@Singleton // => pseudo-scope
class AmazingService {
  String ping() {
    return "amazing";
  }
}

@ApplicationScoped // => normal scope
class CoolService {
  String ping() {
    return "cool";
  }
}

@Path("/ping")
public class PingResource {

  @Inject
  AmazingService s1; (1)

  @Inject
  CoolService s2; (2)

  @GET
  public String ping() {
    return s1.ping() + s2.ping(); (3)
  }
}
```

**1**Injection triggers the instantiation of `AmazingService`.
**2**Injection itself does not result in the instantiation of `CoolService`. A client proxy is injected.
**3**The first invocation upon the injected proxy triggers the instantiation of `CoolService`.

#### [](https://quarkus.io/guides/cdi-reference#startup_event)5.1.2. Startup Event

However, if you really need to instantiate a bean eagerly you can:

*   Declare an observer of the `StartupEvent` - the scope of the bean does not matter in this case:

```
@ApplicationScoped
class CoolService {
  void startup(@Observes StartupEvent event) { (1)
  }
}
``` **1**A `CoolService` is created during startup to service the observer method invocation.
*   Use the bean in an observer of the `StartupEvent` - normal scoped beans must be used as described in [Lazy By Default](https://quarkus.io/guides/cdi-reference#lazy_by_default):

```
@Dependent
class MyBeanStarter {

  void startup(@Observes StartupEvent event, AmazingService amazing, CoolService cool) { (1)
    cool.toString(); (2)
  }
}
``` **1**The `AmazingService` is created during injection.
**2**The `CoolService` is a normal scoped bean, so we have to invoke a method upon the injected proxy to force the instantiation.
*   Annotate the bean with `@io.quarkus.runtime.Startup` as described in [Startup annotation](https://quarkus.io/guides/lifecycle#startup_annotation):

```
@Startup (1)
@ApplicationScoped
public class EagerAppBean {

   private final String name;

   EagerAppBean(NameGenerator generator) { (2)
     this.name = generator.createName();
   }
}
``` **1**For each bean annotated with `@Startup` a synthetic observer of `StartupEvent` is generated. The default priority is used.
**2**The bean constructor is called when the application starts and the resulting contextual instance is stored in the application context.

### [](https://quarkus.io/guides/cdi-reference#request-context-lifecycle)5.2. Request Context Lifecycle

The request context is also active:

*   during notification of a synchronous observer method.

The request context is destroyed:

*   after the observer notification completes for an event, if it was not already active when the notification started.

An event with qualifier `@Initialized(RequestScoped.class)` is fired when the request context is initialized for an observer notification. Moreover, the events with qualifiers `@BeforeDestroyed(RequestScoped.class)` and `@Destroyed(RequestScoped.class)` are fired when the request context is destroyed.

#### [](https://quarkus.io/guides/cdi-reference#how-to-enable-trace-logging-for-request-context-activation)5.2.1. How to Enable Trace Logging for Request Context Activation

You can set the `TRACE` level for the logger `io.quarkus.arc.requestContext` and try to analyze the log output afterwards.

`application.properties` Example

```
quarkus.log.category."io.quarkus.arc.requestContext".min-level=TRACE (1)
quarkus.log.category."io.quarkus.arc.requestContext".level=TRACE
```

**1**You also need to adjust the minimum log level for the relevant category.

### [](https://quarkus.io/guides/cdi-reference#qualified-injected-fields)5.3. Qualified Injected Fields

In CDI, if you declare a field injection point you need to use `@Inject` and optionally a set of qualifiers.

```
@Inject
  @ConfigProperty(name = "cool")
  String coolProperty;
```

In Quarkus, you can skip the `@Inject` annotation completely if the injected field declares at least one qualifier.

```
@ConfigProperty(name = "cool")
  String coolProperty;
```

With the notable exception of one special case discussed below, `@Inject` is still required for constructor and method injection.

### [](https://quarkus.io/guides/cdi-reference#simplified-constructor-injection)5.4. Simplified Constructor Injection

In CDI, a normal scoped bean must always declare a no-args constructor (this constructor is normally generated by the compiler unless you declare any other constructor). However, this requirement complicates constructor injection - you need to provide a dummy no-args constructor to make things work in CDI.

```
@ApplicationScoped
public class MyCoolService {

  private SimpleProcessor processor;

  MyCoolService() { // dummy constructor needed
  }

  @Inject // constructor injection
  MyCoolService(SimpleProcessor processor) {
    this.processor = processor;
  }
}
```

There is no need to declare dummy constructors for normal scoped bean in Quarkus - they are generated automatically. Also, if there’s only one constructor there is no need for `@Inject`.

```
@ApplicationScoped
public class MyCoolService {

  private SimpleProcessor processor;

  MyCoolService(SimpleProcessor processor) {
    this.processor = processor;
  }
}
```

We don’t generate a no-args constructor automatically if a bean class extends a class that does not declare a no-args constructor.

### [](https://quarkus.io/guides/cdi-reference#remove_unused_beans)5.5. Removing Unused Beans

The container attempts to remove all unused beans, interceptors and decorators during build by default. This optimization helps to minimize the amount of generated classes, thus conserving memory. However, Quarkus can’t detect the programmatic lookup performed via the `CDI.current()` static method. Therefore, it is possible that a removal results in a false positive error, i.e. a bean is removed although it’s actually used. In such cases, you’ll notice a big warning in the log. Users and extension authors have several options [how to eliminate false positives](https://quarkus.io/guides/cdi-reference#eliminate_false_positives).

The optimization can be disabled by setting `quarkus.arc.remove-unused-beans` to `none` or `false`. Quarkus also provides a middle ground where application beans are never removed whether or not they are unused, while the optimization proceeds normally for non application classes. To use this mode, set `quarkus.arc.remove-unused-beans` to `fwk` or `framework`.

#### [](https://quarkus.io/guides/cdi-reference#whats-removed)5.5.1. What’s Removed?

Quarkus first identifies so-called _unremovable_ beans that form the roots in the dependency tree. A good example is a Jakarta REST resource class or a bean which declares a `@Scheduled` method.

An _unremovable_ bean:

*   is [excluded from removal](https://quarkus.io/guides/cdi-reference#eliminate_false_positives), or

*   has a name designated via `@Named`, or

*   declares an observer method.

An _unused_ bean:

*   is not _unremovable_, and

*   is not eligible for injection to any injection point in the dependency tree of _unremovable_ beans, and

*   does not declare any producer which is eligible for injection to any injection point in the dependency tree, and

*   is not eligible for injection into any `jakarta.enterprise.inject.Instance` or `jakarta.inject.Provider` injection point, and

*   is not eligible for injection into any [`@Inject @All List<>`](https://quarkus.io/guides/cdi-reference#injecting-multiple-bean-instances-intuitively) injection point.

Unused interceptors and decorators are not associated with any bean.

When using the dev mode (running `./mvnw quarkus:dev`), you can see more information about which beans are being removed:

1.   In the console - just enable the DEBUG level in your `application.properties`, i.e. `quarkus.log.category."io.quarkus.arc.processor".level=DEBUG`

2.   In the relevant Dev UI page

#### [](https://quarkus.io/guides/cdi-reference#eliminate_false_positives)5.5.2. How To Eliminate False Positives

Users can instruct the container to not remove any of their specific beans (even if they satisfy all the rules specified above) by annotating them with `@io.quarkus.arc.Unremovable`. This annotation can be declared on a class, a producer method or field.

Since this is not always possible, there is an option to achieve the same via `application.properties`. The `quarkus.arc.unremovable-types` property accepts a list of string values that are used to match beans based on their name or package.

Table 2. Value Examples Value Description
`org.acme.Foo`Match the fully qualified name of the bean class
`org.acme.*`Match beans where the package of the bean class is `org.acme`
`org.acme.**`Match beans where the package of the bean class starts with `org.acme`
`Bar`Match the simple name of the bean class

Example application.properties

`quarkus.arc.unremovable-types=org.acme.Foo,org.acme.*,Bar`

Furthermore, extensions can eliminate false positives by producing an `UnremovableBeanBuildItem`.

### [](https://quarkus.io/guides/cdi-reference#default_beans)5.6. Default Beans

Quarkus adds a capability that CDI currently does not support which is to conditionally declare a bean if no other bean with equal types and qualifiers was declared by any available means (bean class, producer, synthetic bean, …​) This is done using the `@io.quarkus.arc.DefaultBean` annotation and is best explained with an example.

Say there is a Quarkus extension that among other things declares a few CDI beans like the following code does:

```
@Dependent
public class TracerConfiguration {

    @Produces
    public Tracer tracer(Reporter reporter, Configuration configuration) {
        return new Tracer(reporter, configuration);
    }

    @Produces
    @DefaultBean
    public Configuration configuration() {
        // create a Configuration
    }

    @Produces
    @DefaultBean
    public Reporter reporter(){
        // create a Reporter
    }
}
```

The idea is that the extension autoconfigures things for the user, eliminating a lot of boilerplate - we can just `@Inject` a `Tracer` wherever it is needed. Now imagine that in our application we would like to utilize the configured `Tracer`, but we need to customize it a little, for example by providing a custom `Reporter`. The only thing that would be needed in our application would be something like the following:

```
@Dependent
public class CustomTracerConfiguration {

    @Produces
    public Reporter reporter(){
        // create a custom Reporter
    }
}
```

`@DefaultBean` allows extensions (or any other code for that matter) to provide defaults while backing off if beans of that type are supplied in any way Quarkus supports.

Default beans can optionally declare `@jakarta.annotation.Priority`. If there is no priority defined, `@Priority(0)` is assumed. Priority value is used for bean ordering and during typesafe resolution to disambiguate multiple matching default beans.

```
@Dependent
public class CustomizedDefaultConfiguration {

    @Produces
    @DefaultBean
    @Priority(100)
    public Configuration customizedConfiguration(){
        // create a customized default Configuration
        // this will have priority over previously defined default bean
    }
}
```

### [](https://quarkus.io/guides/cdi-reference#enable_build_profile)5.7. Enabling Beans for Quarkus Build Profile

Quarkus adds a capability that CDI currently does not support which is to conditionally enable a bean when a Quarkus build time profile is enabled, via the `@io.quarkus.arc.profile.IfBuildProfile` and `@io.quarkus.arc.profile.UnlessBuildProfile` annotations. When used in conjunction with `@io.quarkus.arc.DefaultBean`, these annotations allow for the creation of different bean configurations for different build profiles.

Imagine for instance that an application contains a bean named `Tracer`, which needs to do nothing when in tests or in dev mode, but works in its normal capacity for the production artifact. An elegant way to create such beans is the following:

```
@Dependent
public class TracerConfiguration {

    @Produces
    @IfBuildProfile("prod")
    public Tracer realTracer(Reporter reporter, Configuration configuration) {
        return new RealTracer(reporter, configuration);
    }

    @Produces
    @DefaultBean
    public Tracer noopTracer() {
        return new NoopTracer();
    }
}
```

If instead, it is required that the `Tracer` bean also works in dev mode and only default to doing nothing for tests, then `@UnlessBuildProfile` would be ideal. The code would look like:

```
@Dependent
public class TracerConfiguration {

    @Produces
    @UnlessBuildProfile("test") // this will be enabled for both prod and dev build time profiles
    public Tracer realTracer(Reporter reporter, Configuration configuration) {
        return new RealTracer(reporter, configuration);
    }

    @Produces
    @DefaultBean
    public Tracer noopTracer() {
        return new NoopTracer();
    }
}
```

The runtime profile has absolutely no effect on the bean resolution using `@IfBuildProfile` and `@UnlessBuildProfile`.

It is also possible to use `@IfBuildProfile` and `@UnlessBuildProfile` on stereotypes.

### [](https://quarkus.io/guides/cdi-reference#enable_build_properties)5.8. Enabling Beans for Quarkus Build Properties

Quarkus adds a capability that CDI currently does not support which is to conditionally enable a bean when a Quarkus build time property has or does not have a specific value, via the `@io.quarkus.arc.properties.IfBuildProperty` and `@io.quarkus.arc.properties.UnlessBuildProperty` annotations. When used in conjunction with `@io.quarkus.arc.DefaultBean`, these annotations allow for the creation of different bean configurations for different build properties.

The scenario we mentioned above with `Tracer` could also be implemented in the following way:

```
@Dependent
public class TracerConfiguration {

    @Produces
    @IfBuildProperty(name = "some.tracer.enabled", stringValue = "true")
    public Tracer realTracer(Reporter reporter, Configuration configuration) {
        return new RealTracer(reporter, configuration);
    }

    @Produces
    @DefaultBean
    public Tracer noopTracer() {
        return new NoopTracer();
    }
}
```

`@IfBuildProperty` and `@UnlessBuildProperty` are repeatable annotations, i.e. a bean will only be enabled if **all** the conditions defined by these annotations are satisfied.

If instead, it is required that the `RealTracer` bean is only used if the `some.tracer.enabled` property is not `false`, then `@UnlessBuildProperty` would be ideal. The code would look like:

```
@Dependent
public class TracerConfiguration {

    @Produces
    @UnlessBuildProperty(name = "some.tracer.enabled", stringValue = "false")
    public Tracer realTracer(Reporter reporter, Configuration configuration) {
        return new RealTracer(reporter, configuration);
    }

    @Produces
    @DefaultBean
    public Tracer noopTracer() {
        return new NoopTracer();
    }
}
```

Properties set at runtime have absolutely no effect on the bean resolution using `@IfBuildProperty`.

It is also possible to use `@IfBuildProperty` and `@UnlessBuildProperty` on stereotypes.

### [](https://quarkus.io/guides/cdi-reference#declaring-selected-alternatives)5.9. Declaring Selected Alternatives

In CDI, an alternative bean may be selected either globally for an application by means of `@Priority`, or for a bean archive using a `beans.xml` descriptor. Quarkus has a simplified bean discovery and the content of `beans.xml` is ignored.

However, it is also possible to select alternatives for an application using the unified configuration. The `quarkus.arc.selected-alternatives` property accepts a list of string values that are used to match alternative beans. If any value matches then the priority of `Integer#MAX_VALUE` is used for the relevant bean. The priority declared via `@Priority` or inherited from a stereotype is overridden.

Table 3. Value Examples Value Description
`org.acme.Foo`Match the fully qualified name of the bean class or the bean class of the bean that declares the producer
`org.acme.*`Match beans where the package of the bean class is `org.acme`
`org.acme.**`Match beans where the package of the bean class starts with `org.acme`
`Bar`Match the simple name of the bean class or the bean class of the bean that declares the producer

Example application.properties

`quarkus.arc.selected-alternatives=org.acme.Foo,org.acme.*,Bar`

### [](https://quarkus.io/guides/cdi-reference#simplified-producer-method-declaration)5.10. Simplified Producer Method Declaration

In CDI, a producer method must be always annotated with `@Produces`.

```
class Producers {

  @Inject
  @ConfigProperty(name = "cool")
  String coolProperty;

  @Produces
  @ApplicationScoped
  MyService produceService() {
    return new MyService(coolProperty);
  }
}
```

In Quarkus, you can skip the `@Produces` annotation completely if the producer method is annotated with a scope annotation, a stereotype or a qualifier.

```
class Producers {

  @ConfigProperty(name = "cool")
  String coolProperty;

  @ApplicationScoped
  MyService produceService() {
    return new MyService(coolProperty);
  }
}
```

### [](https://quarkus.io/guides/cdi-reference#interception_of_static_methods)5.11. Interception of Static Methods

The Interceptors specification is clear that _around-invoke_ methods must not be declared static. However, this restriction was driven mostly by technical limitations. And since Quarkus is a build-time oriented stack that allows for additional class transformations, those limitations don’t apply anymore. It’s possible to annotate a non-private static method with an interceptor binding:

```
class Services {

  @Logged (1)
  static BigDecimal computePrice(long amount) { (2)
    BigDecimal price;
    // Perform computations...
    return price;
  }
}
```

**1**`Logged` is an interceptor binding.
**2**Each method invocation is intercepted if there is an interceptor associated with `Logged`.

#### [](https://quarkus.io/guides/cdi-reference#limitations)5.11.1. Limitations

*   Only **method-level bindings** are considered for backward compatibility reasons (otherwise static methods of bean classes that declare class-level bindings would be suddenly intercepted)

*   Private static methods are never intercepted

*   `InvocationContext#getTarget()` returns `null` for obvious reasons; therefore not all existing interceptors may behave correctly when intercepting static methods

Interceptors can use `InvocationContext.getMethod()` to detect static methods and adjust the behavior accordingly.

### [](https://quarkus.io/guides/cdi-reference#unproxyable_classes_transformation)5.12. Ability to handle 'final' classes and methods

In normal CDI, classes that are marked as `final` and / or have `final` methods are not eligible for proxy creation, which in turn means that interceptors and normal scoped beans don’t work properly. This situation is very common when trying to use CDI with alternative JVM languages like Kotlin where classes and methods are `final` by default.

Quarkus however, can overcome these limitations when `quarkus.arc.transform-unproxyable-classes` is set to `true` (which is the default value).

### [](https://quarkus.io/guides/cdi-reference#container-managed-concurrency)5.13. Container-managed Concurrency

There is no standard concurrency control mechanism for CDI beans. Nevertheless, a bean instance can be shared and accessed concurrently from multiple threads. In that case it should be thread-safe. You can use standard Java constructs (`volatile`, `synchronized`, `ReadWriteLock`, etc.) or let the container control the concurrent access. Quarkus provides `@io.quarkus.arc.Lock` and a built-in interceptor for this interceptor binding. Each interceptor instance associated with a contextual instance of an intercepted bean holds a separate `ReadWriteLock` with non-fair ordering policy.

`io.quarkus.arc.Lock` is a regular interceptor binding and as such can be used for any bean with any scope. However, it is especially useful for "shared" scopes, e.g. `@Singleton` and `@ApplicationScoped`.

Container-managed Concurrency Example

```
import io.quarkus.arc.Lock;

@Lock (1)
@ApplicationScoped
class SharedService {

  void addAmount(BigDecimal amount) {
    // ...changes some internal state of the bean
  }

  @Lock(value = Lock.Type.READ, time = 1, unit = TimeUnit.SECONDS) (2) (3)
  BigDecimal getAmount() {
    // ...it is safe to read the value concurrently
  }
}
```

**1**`@Lock` (which maps to `@Lock(Lock.Type.WRITE)`) declared on the class instructs the container to lock the bean instance for any invocation of any business method, i.e. the client has "exclusive access" and no concurrent invocations will be allowed.
**2**`@Lock(Lock.Type.READ)` overrides the value specified at class level. It means that any number of clients can invoke the method concurrently, unless the bean instance is locked by `@Lock(Lock.Type.WRITE)`.
**3**You can also specify the "wait time". If it’s not possible to acquire the lock in the given time a `LockException` is thrown.

### [](https://quarkus.io/guides/cdi-reference#repeatable-interceptor-bindings)5.14. Repeatable interceptor bindings

Quarkus has limited support for `@Repeatable` interceptor binding annotations.

When binding an interceptor to a component, you can declare multiple `@Repeatable` annotations on methods. Repeatable interceptor bindings declared on classes and stereotypes are not supported, because there are some open questions around interactions with the Interceptors specification. This might be added in the future.

As an example, suppose we have an interceptor that clears a cache. The corresponding interceptor binding would be called `@CacheInvalidateAll` and would be declared as `@Repeatable`. If we wanted to clear two caches at the same time, we would add `@CacheInvalidateAll` twice:

```
@ApplicationScoped
class CachingService {
  @CacheInvalidateAll(cacheName = "foo")
  @CacheInvalidateAll(cacheName = "bar")
  void heavyComputation() {
    // ...
    // some computation that updates a lot of data
    // and requires 2 caches to be invalidated
    // ...
  }
}
```

This is how interceptors are used. What about creating an interceptor?

When declaring interceptor bindings of an interceptor, you can add multiple `@Repeatable` annotations to the interceptor class as usual. This is useless when the annotation members are `@Nonbinding`, as would be the case for the `@Cached` annotation, but is important otherwise.

For example, suppose we have an interceptor that can automatically log method invocations to certain targets. The interceptor binding annotation `@Logged` would have a member called `target`, which specifies where to store the log. Our implementation could be restricted to console logging and file logging:

```
@Interceptor
@Logged(target = "console")
@Logged(target = "file")
class NaiveLoggingInterceptor {
  // ...
}
```

Other interceptors could be provided to log method invocations to different targets.

### [](https://quarkus.io/guides/cdi-reference#caching-the-result-of-programmatic-lookup)5.15. Caching the Result of Programmatic Lookup

In certain situations, it is practical to obtain a bean instance programmatically via an injected `jakarta.enterprise.inject.Instance` and `Instance.get()`. However, according to the specification the `get()` method must identify the matching bean and obtain a contextual reference. As a consequence, a new instance of a `@Dependent` bean is returned from each invocation of `get()`. Moreover, this instance is a dependent object of the injected `Instance`. This behavior is well-defined, but it may lead to unexpected errors and memory leaks. Therefore, Quarkus comes with the `io.quarkus.arc.WithCaching` annotation. An injected `Instance` annotated with this annotation will cache the result of the `Instance#get()` operation. The result is computed on the first call and the same value is returned for all subsequent calls, even for `@Dependent` beans.

```
class Producer {

  AtomicLong nextLong = new AtomicLong();
  AtomicInteger nextInt = new AtomicInteger();

   @Dependent
   @Produces
   Integer produceInt() {
     return nextInt.incrementAndGet();
   }

   @Dependent
   @Produces
   Long produceLong() {
     return nextLong.incrementAndGet();
   }
}

class Consumer {

  @Inject
  Instance<Long> longInstance;

  @Inject
  @WithCaching
  Instance<Integer> intInstance;

  // this method should always return true
  // Producer#produceInt() is only called once
  boolean pingInt() {
    return intInstance.get().equals(intInstance.get());
  }

  // this method should always return false
  // Producer#produceLong() is called twice per each pingLong() invocation
  boolean pingLong() {
    return longInstance.get().equals(longInstance.get());
  }
}
```

It is also possible to clear the cached value via `io.quarkus.arc.InjectableInstance.clearCache()`. In this case, you’ll need to inject the Quarkus-specific `io.quarkus.arc.InjectableInstance` instead of `jakarta.enterprise.inject.Instance`.

### [](https://quarkus.io/guides/cdi-reference#declaratively-choose-beans-that-can-be-obtained-by-programmatic-lookup)5.16. Declaratively Choose Beans That Can Be Obtained by Programmatic Lookup

It is sometimes useful to narrow down the set of beans that can be obtained by programmatic lookup via `jakarta.enterprise.inject.Instance`. Typically, a user needs to choose the appropriate implementation of an interface based on a runtime configuration property.

Imagine that we have two beans implementing the interface `org.acme.Service`. You can’t inject the `org.acme.Service` directly unless your implementations declare a CDI qualifier. However, you can inject the `Instance<Service>` instead, then iterate over all implementations and choose the correct one manually. Alternatively, you can use the `@LookupIfProperty` and `@LookupUnlessProperty` annotations. `@LookupIfProperty` indicates that a bean should only be obtained if a runtime configuration property matches the provided value. `@LookupUnlessProperty`, on the other hand, indicates that a bean should only be obtained if a runtime configuration property does not match the provided value.

`@LookupIfProperty` Example

```
interface Service {
    String name();
 }

 @LookupIfProperty(name = "service.foo.enabled", stringValue = "true")
 @ApplicationScoped
 class ServiceFoo implements Service {

    public String name() {
       return "foo";
    }
 }

 @ApplicationScoped
 class ServiceBar implements Service {

    public String name() {
       return "bar";
    }
 }

 @ApplicationScoped
 class Client {

    @Inject
    Instance<Service> service;

    void printServiceName() {
       // This will print "bar" if the property "service.foo.enabled" is NOT set to "true"
       // If "service.foo.enabled" is set to "true" then service.get() would result in an AmbiguousResolutionException
       System.out.println(service.get().name());
    }
 }
```

### [](https://quarkus.io/guides/cdi-reference#sorting-beans-obtained-with-programmatic-lookup)5.17. Sorting beans obtained with programmatic lookup

If there is more than one bean that matches the required type and qualifiers and is eligible for injection, it is possible to iterate (or stream) available bean instances. Beans returned by both stream and iterator methods are sorted by priority as defined by `io.quarkus.arc.InjectableBean#getPriority()`. Higher priority goes first. If no priority is explicitly declared, 0 is assumed.

```
interface Service {

}

@Priority(100)
@ApplicationScoped
class FirstService implements Service {

}

@Priority(10)
@ApplicationScoped
class SecondService implements Service {

}

@ApplicationScoped
class ThirdService implements Service {

}

@ApplicationScoped
class Client {

   @Inject
   Instance<Service> serviceInstance;

   void printServiceName() {
       if(service.isAmbiguous()){
           for (Service service : serviceInstance) {
                // FirstService, SecondService, ThirdService
           }
       }
   }
}
```

### [](https://quarkus.io/guides/cdi-reference#injecting-multiple-bean-instances-intuitively)5.18. Injecting Multiple Bean Instances Intuitively

In CDI, it’s possible to inject multiple bean instances (aka contextual references) via the `jakarta.enterprise.inject.Instance` which implements `java.lang.Iterable`. However, it’s not exactly intuitive. Therefore, a new way was introduced in Quarkus - you can inject a `java.util.List` annotated with the `io.quarkus.arc.All` qualifier. The type of elements in the list is used as the required type when performing the lookup.

```
@ApplicationScoped
public class Processor {

     @Inject
     @All
     List<Service> services; (1) (2)
}
```

**1**The injected instance is an _immutable list_ of the contextual references of the _disambiguated_ beans.
**2**For this injection point the required type is `Service` and no additional qualifiers are declared.

The list is sorted by priority as defined by `io.quarkus.arc.InjectableBean#getPriority()`. Higher priority goes first. In general, the `@jakarta.annotation.Priority` annotation can be used to assign the priority to a class bean, producer method or producer field.

If an injection point declares no other qualifier than `@All` then `@Any` is used, i.e. the behavior is equivalent to `@Inject @Any Instance<Service>`.

You can also inject a list of bean instances wrapped in `io.quarkus.arc.InstanceHandle`. This can be useful if you need to inspect the related bean metadata.

```
@ApplicationScoped
public class Processor {

     @Inject
     @All
     List<InstanceHandle<Service>> services;

     public void doSomething() {
       for (InstanceHandle<Service> handle : services) {
         if (handle.getBean().getScope().equals(Dependent.class)) {
           handle.get().process();
           break;
         }
       }
     }
}
```

Neither a type variable nor a wildcard is a legal type parameter for an `@All List<>` injection point, i.e. `@Inject @All List<?> all` is not supported and results in a deployment error.

It is also possible to obtain the list of all bean instance handles programmatically via the `Arc.container().listAll()` methods.

### [](https://quarkus.io/guides/cdi-reference#ignoring-class-level-interceptor-bindings-for-methods-and-constructors)5.19. Ignoring Class-Level Interceptor Bindings for Methods and Constructors

If a managed bean declares interceptor binding annotations on the class level, the corresponding `@AroundInvoke` interceptors will apply to all business methods. Similarly, the corresponding `@AroundConstruct` interceptors will apply to the bean constructor.

For example, suppose we have a logging interceptor with the `@Logged` binding annotation and a tracing interceptor with the `@Traced` binding annotation:

```
@ApplicationScoped
@Logged
public class MyService {
    public void doSomething() {
        ...
    }

    @Traced
    public void doSomethingElse() {
        ...
    }
}
```

In this example, both `doSomething` and `doSomethingElse` will be intercepted by the hypothetical logging interceptor. Additionally, the `doSomethingElse` method will be intercepted by the hypothetical tracing interceptor.

Now, if that `@Traced` interceptor also performed all the necessary logging, we’d like to skip the `@Logged` interceptor for this method, but keep it for all other methods. To achieve that, you can annotate the method with `@NoClassInterceptors`:

```
@Traced
@NoClassInterceptors
public void doSomethingElse() {
    ...
}
```

The `@NoClassInterceptors` annotation may be put on methods and constructors and means that all class-level interceptors are ignored for these methods and constructors. In other words, if a method/constructor is annotated `@NoClassInterceptors`, then the only interceptors that will apply to this method/constructor are interceptors declared directly on the method/constructor.

This annotation affects only business method interceptors (`@AroundInvoke`) and constructor lifecycle callback interceptors (`@AroundConstruct`).

### [](https://quarkus.io/guides/cdi-reference#exceptions-thrown-by-an-asynchronous-observer-method)5.20. Exceptions Thrown By An Asynchronous Observer Method

If an exception is thrown by an asynchronous observer then the `CompletionStage` returned by the `fireAsync()` method completes exceptionally so that the event producer may react appropriately. However, if the event producer does not care then the exception is ignored silently. Therefore, Quarkus logs an error message by default. It is also possible to implement a custom `AsyncObserverExceptionHandler`. A bean that implements this interface should be `@jakarta.inject.Singleton` or `@jakarta.enterprise.context.ApplicationScoped`.

`NoopAsyncObserverExceptionHandler`

```
@Singleton
public class NoopAsyncObserverExceptionHandler implements AsyncObserverExceptionHandler {

  void handle(Throwable throwable, ObserverMethod<?> observerMethod, EventContext<?> eventContext) {
    // do nothing
  }

}
```

### [](https://quarkus.io/guides/cdi-reference#intercepted-self-invocation)5.21. Intercepted self-invocation

Quarkus supports what is known as intercepted self-invocation or just self-interception - a scenario where CDI bean invokes its own intercepted method from within another method while triggering any associated interceptors. This is a non-standard feature as CDI specification doesn’t define whether self-interception should work or not.

Suppose we have a CDI bean with two methods, one of which has the `@Transactional` interceptor binding associated with it:

```
@ApplicationScoped
public class MyService {

  @Transactional (1)
  void doSomething() {
    // some application logic
  }

  void doSomethingElse() {
    doSomething();(2)
  }

}
```

**1**One or more interceptor bindings; `@Transactional` is just an example.
**2**Non-intercepted method invoking another method from the same bean that has associated binding(s); this will trigger interception.

In the above example, any code calling the `doSomething()` method triggers interception - in this case, the method becomes transactional. This is regardless of whether the invocation originated directly from the `MyService` bean (such as `MyService#doSomethingElse`) or from some other bean.

### [](https://quarkus.io/guides/cdi-reference#intercepting-producer-methods-and-synthetic-beans)5.22. Intercepting Producer Methods and Synthetic Beans

By default, interception is only supported for managed beans (also known as class-based beans). To support interception of producer methods and synthetic beans, the CDI specification includes an `InterceptionFactory`, which is a runtime oriented concept and therefore cannot be supported in Quarkus.

Instead, Quarkus has its own API: `InterceptionProxy` and `@BindingsSource`. The `InterceptionProxy` is very similar to `InterceptionFactory`: it creates a proxy that applies `@AroundInvoke` interceptors before forwarding the method call to the target instance. The `@BindingsSource` annotation allows setting interceptor bindings in case the intercepted class is external and cannot be changed.

```
import io.quarkus.arc.InterceptionProxy;

@ApplicationScoped
class MyProducer {
    @Produces
    MyClass produce(InterceptionProxy<MyClass> proxy) { (1)
        return proxy.create(new MyClass()); (2)
    }
}
```

**1**Declares an injection point of type `InterceptionProxy<MyClass>`. This means that at build time, a subclass of `MyClass` is generated that does the interception and forwarding. Note that the type argument must be identical to the return type of the producer method.
**2**Creates an instance of the interception proxy for the given instance of `MyClass`. The method calls will be forwarded to this target instance after all interceptors run.

In this example, interceptor bindings are read from the `MyClass` class.

Note that `InterceptionProxy` only supports `@AroundInvoke` interceptors declared on interceptor classes. Other kinds of interception, as well as `@AroundInvoke` interceptors declared on the target class and its superclasses, are not supported.

The intercepted class should be [proxyable](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1#unproxyable) and therefore should not be `final`, should not have non-private `final` methods, and should have a non-private zero-parameter constructor. If it isn’t, a bytecode transformation will attempt to fix it if [enabled](https://quarkus.io/guides/cdi-reference#unproxyable_classes_transformation), but note that adding a zero-parameter constructor is not always possible.

It is often the case that the produced classes come from external libraries and don’t contain interceptor binding annotations at all. To support such cases, the `@BindingsSource` annotation may be declared on the `InterceptionProxy` parameter:

```
import io.quarkus.arc.BindingsSource;
import io.quarkus.arc.InterceptionProxy;

abstract class MyClassBindings { (1)
    @MyInterceptorBinding
    abstract String doSomething();
}

@ApplicationScoped
class MyProducer {
    @Produces
    MyClass produce(@BindingsSource(MyClassBindings.class) InterceptionProxy<MyClass> proxy) { (2)
        return proxy.create(new MyClass());
    }
}
```

**1**A class that mirrors the `MyClass` structure and contains interceptor bindings.
**2**The `@BindingsSource` annotation says that interceptor bindings for `MyClass` should be read from `MyClassBindings`.

The concept of _bindings source_ is a build-time friendly equivalent of `InterceptionFactory.configure()`.

Producer method interception and synthetic bean interception only works for instance methods. [Interception of Static Methods](https://quarkus.io/guides/cdi-reference#interception_of_static_methods) is not supported for producer methods and synthetic beans.

#### [](https://quarkus.io/guides/cdi-reference#declaring-bindingssource)5.22.1. Declaring `@BindingsSource`

The `@BindingsSource` annotation specifies a class that mirrors the structure of the intercepted class. Interceptor bindings are then read from that class and treated as if they were declared on the intercepted class.

Specifically: class-level interceptor bindings declared on the bindings source class are treated as class-level bindings of the intercepted class. Method-level interceptor bindings declared on the bindings source class are treated as method-level bindings of a method with the same name, return type, parameter types and `static` flag of the intercepted class.

It is common to make the bindings source class and methods `abstract` so that you don’t have to write method bodies:

```
abstract class MyClassBindings {
    @MyInterceptorBinding
    abstract String doSomething();
}
```

Since this class is never instantiated and its method are never invoked, this is okay, but it’s also possible to create a non-`abstract` class:

```
class MyClassBindings {
    @MyInterceptorBinding
    String doSomething() {
        return null; (1)
    }
}
```

**1**The method body does not matter.

Note that for generic classes, the type variable names must also be identical. For example, for the following class:

```
class MyClass<T> {
    T doSomething() {
        ...
    }

    void doSomethingElse(T param) {
        ...
    }
}
```

the bindings source class must also use `T` as the name of the type variable:

```
abstract class MyClassBindings<T> {
    @MyInterceptorBinding
    abstract T doSomething();
}
```

You don’t need to declare methods that are not annotated simply because they exist on the intercepted class. If you want to add method-level bindings to a subset of methods, you only have to declare the methods that are supposed to have an interceptor binding. If you only want to add class-level bindings, you don’t have to declare any methods at all.

These annotations can be present on a bindings source class:

*   _interceptor bindings_: on the class and on the methods

*   _stereotypes_: on the class

*   `@NoClassInterceptors`: on the methods

Any other annotation present on a bindings source class is ignored.

#### [](https://quarkus.io/guides/cdi-reference#synthetic-beans)5.22.2. Synthetic Beans

Using `InterceptionProxy` in synthetic beans is similar.

First, you have to declare that your synthetic bean injects the `InterceptionProxy`:

```
public void register(RegistrationContext context) {
    context.configure(MyClass.class)
            .types(MyClass.class)
            .injectInterceptionProxy() (1)
            .creator(MyClassCreator.class)
            .done();
}
```

**1**Once again, this means that at build time, a subclass of `MyClass` is generated that does the interception and forwarding.

Second, you have to obtain the `InterceptionProxy` from the `SyntheticCreationalContext` in the `BeanCreator` and use it:

```
public MyClass create(SyntheticCreationalContext<MyClass> context) {
    InterceptionProxy<MyClass> proxy = context.getInterceptionProxy(); (1)
    return proxy.create(new MyClass());
}
```

**1**Obtains the `InterceptionProxy` for `MyClass`, as declared above. It would also be possible to use the `getInjectedReference()` method, passing a `TypeLiteral`, but `getInterceptionProxy()` is easier.

There’s also an equivalent of `@BindingsSource`. The `injectInterceptionProxy()` method has an overload with a parameter:

```
public void register(RegistrationContext context) {
    context.configure(MyClass.class)
            .types(MyClass.class)
            .injectInterceptionProxy(MyClassBindings.class) (1)
            .creator(MyClassCreator.class)
            .done();
}
```

**1**The argument is the bindings source class.

### [](https://quarkus.io/guides/cdi-reference#instance-handle-close-behavior)5.23. `Instance.Handle.close()` Behavior

Per the CDI specification, the `Instance.Handle.close()` method always delegates to `destroy()`. In ArC, this is only true in the [Strict Mode](https://quarkus.io/guides/cdi-reference#strict_mode).

In the default mode, the `close()` method only delegates to `destroy()` when the bean is `@Dependent` (or when the instance handle does not represent a CDI contextual object). When the instance handle represents a bean of any other scope, the `close()` method does nothing; the bean is left as is and will be destroyed whenever its context is destroyed.

This is to make the following code behave as one would naively expect:

```
Instance<T> instance = ...;
try (Instance.Handle<T> handle : instance.getHandle()) {
   T value = handle.get();
   ... use value ...
}
```

The `@Dependent` beans are destroyed immediately, while other beans are not destroyed at all. This is important when multiple beans of different scopes might be returned by the `Instance`.

[](https://quarkus.io/guides/cdi-reference#reactive_pitfalls)6. Pitfalls with Reactive Programming
--------------------------------------------------------------------------------------------------

CDI is a purely synchronous framework. Its notion of asynchrony is very limited and based solely on thread pools and thread offloading. Therefore, there is a number of pitfalls when using CDI together with reactive programming.

### [](https://quarkus.io/guides/cdi-reference#detecting-when-blocking-is-allowed)6.1. Detecting When Blocking Is Allowed

The `io.quarkus.runtime.BlockingOperationControl#isBlockingAllowed()` method can be used to detect whether blocking is allowed on the current thread. When it is not, and you need to perform a blocking operation, you have to offload it to another thread. The easiest way is to use the `Vertx.executeBlocking()` method:

```
import io.quarkus.runtime.BlockingOperationControl;

@ApplicationScoped
public class MyBean {
    @Inject
    Vertx vertx;

    @PostConstruct
    void init() {
        if (BlockingOperationControl.isBlockingAllowed()) {
            somethingThatBlocks();
        } else {
            vertx.executeBlocking(() -> {
                somethingThatBlocks();
                return null;
            });
        }
    }

    void somethingThatBlocks() {
        // use the file system or JDBC, call a REST service, etc.
        Thread.sleep(5000);
    }
}
```

### [](https://quarkus.io/guides/cdi-reference#asynchronous-observers)6.2. Asynchronous Observers

CDI asynchronous observers (`@ObservesAsync`) are not aware of reactive programming and are not meant to be used as part of reactive pipelines. The observer methods are meant to be synchronous, they are just offloaded to a thread pool.

The `Event.fireAsync()` method returns a `CompletionStage` that completes when all observers are notified. If all observers were notified successfully, the `CompletionStage` completes with the event payload. If some observers have thrown an exception, the `CompletionStage` completes exceptionally with a `CompletionException`.

The return type of the observer _does not matter_. The return value of the observer is _ignored_.

You may declare an observer method that has a return type of `CompletionStage<>` or `Uni<>`, but neither the return type nor the actual return value affect the result of `Event.fireAsync()`. Further, if the observer declares a return type of `Uni<>`, the returned `Uni` will not be subscribed to, so it is quite possible that part of the observer logic will not even execute.

Therefore, it is recommended that observer methods, both synchronous and asynchronous, are always declared `void`.

[](https://quarkus.io/guides/cdi-reference#build_time_apis)7. Build Time Extensions
-----------------------------------------------------------------------------------

Quarkus incorporates build-time optimizations in order to provide instant startup and low memory footprint. The downside of this approach is that CDI Portable Extensions cannot be supported. Nevertheless, most of the functionality can be achieved using Quarkus [extensions](https://quarkus.io/guides/writing-extensions). See the [integration guide](https://quarkus.io/guides/cdi-integration) for more information.

[](https://quarkus.io/guides/cdi-reference#dev_mode)8.  Dev mode
----------------------------------------------------------------

In dev mode, two special endpoints are registered automatically to provide some basic debug info in the JSON format:

*   HTTP GET `/q/arc` - returns the summary; number of beans, config properties, etc.

*   HTTP GET `/q/arc/beans` - returns the list of all beans

    *   You can use query params to filter the output:

        *   `scope` - include beans with scope that ends with the given value, i.e. `http://localhost:8080/q/arc/beans?scope=ApplicationScoped`

        *   `beanClass` - include beans with bean class that starts with the given value, i.e. `http://localhost:8080/q/arc/beans?beanClass=org.acme.Foo`

        *   `kind` - include beans of the specified kind (`CLASS`, `PRODUCER_FIELD`, `PRODUCER_METHOD`, `INTERCEPTOR` or `SYNTHETIC`), i.e. `http://localhost:8080/q/arc/beans?kind=PRODUCER_METHOD`

*   HTTP GET `/q/arc/removed-beans` - returns the list of unused beans removed during build

*   HTTP GET `/q/arc/observers` - returns the list of all observer methods

These endpoints are only available in dev mode, i.e. when you run your application via `mvn quarkus:dev` (or `./gradlew quarkusDev`).

### [](https://quarkus.io/guides/cdi-reference#monitoring-business-method-invocations-and-events)8.1. Monitoring Business Method Invocations and Events

In dev mode, it is also possible to enable monitoring of business method invocations and fired events. Simply set the `quarkus.arc.dev-mode.monitoring-enabled` configuration property to `true` and explore the relevant Dev UI pages.

[](https://quarkus.io/guides/cdi-reference#strict_mode)9. Strict Mode
---------------------------------------------------------------------

By default, ArC does not perform all validations required by the CDI specification. It also improves CDI usability in many ways, some of them being directly against the specification.

To pass the CDI Lite TCK, ArC also has a _strict_ mode. This mode enables additional validations and disables certain improvements that conflict with the specification.

To enable the strict mode, use the following configuration:

`quarkus.arc.strict-compatibility=true`

Some other features affect specification compatibility as well:

*   [Transformation of unproxyable classes](https://quarkus.io/guides/cdi-reference#unproxyable_classes_transformation)

*   [Unused beans removal](https://quarkus.io/guides/cdi-reference#remove_unused_beans)

To get a behavior closer to the specification, these features should be disabled.

Applications are recommended to use the default, non-strict mode, which makes CDI more convenient to use. The "strictness" of the strict mode (the set of additional validations and the set of disabled improvements on top of the CDI specification) may change over time.

[](https://quarkus.io/guides/cdi-reference#arc-configuration-reference)10. ArC Configuration Reference
------------------------------------------------------------------------------------------------------

Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  | Type | Default |
| --- | --- | --- |
| [`quarkus.arc.remove-unused-beans`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-remove-unused-beans) * If set to `all` (or `true`) the container will attempt to remove all unused beans. * If set to `none` (or `false`) no beans will ever be removed even if they are unused (according to the criteria set out below) * If set to `fwk`, then all unused beans will be removed, except the unused beans whose classes are declared in the application code An unused bean: * is not a built-in bean or interceptor, * is not eligible for injection to any injection point, * is not excluded by any extension, * does not have a name, * does not declare an observer, * does not declare any producer which is eligible for injection to any injection point, * is not directly eligible for injection into any `jakarta.enterprise.inject.Instance` injection point Environment variable: `QUARKUS_ARC_REMOVE_UNUSED_BEANS` Show more | string | `all` |
| [`quarkus.arc.auto-inject-fields`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-auto-inject-fields) If set to true `@Inject` is automatically added to all non-static non-final fields that are annotated with one of the annotations defined by `AutoInjectAnnotationBuildItem`. Environment variable: `QUARKUS_ARC_AUTO_INJECT_FIELDS` Show more | boolean | `true` |
| [`quarkus.arc.transform-unproxyable-classes`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-transform-unproxyable-classes) If set to true, the bytecode of unproxyable beans will be transformed. This ensures that a proxy/subclass can be created properly. If the value is set to false, then an exception is thrown at build time indicating that a subclass/proxy could not be created. Quarkus performs the following transformations when this setting is enabled: * Remove 'final' modifier from classes and methods when a proxy is required. * Create a no-args constructor if needed. * Makes private no-args constructors package-private if necessary. Environment variable: `QUARKUS_ARC_TRANSFORM_UNPROXYABLE_CLASSES` Show more | boolean | `true` |
| [`quarkus.arc.transform-private-injected-fields`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-transform-private-injected-fields) If set to true, the bytecode of private fields that are injection points will be transformed to package private. This ensures that field injection can be performed completely reflection-free. If the value is set to false, then a reflection fallback is used to perform the injection. Environment variable: `QUARKUS_ARC_TRANSFORM_PRIVATE_INJECTED_FIELDS` Show more | boolean | `true` |
| [`quarkus.arc.fail-on-intercepted-private-method`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-fail-on-intercepted-private-method) If set to true (the default), the build fails if a private method that is neither an observer nor a producer, is annotated with an interceptor binding. An example of this is the use of `Transactional` on a private method of a bean. If set to false, Quarkus simply logs a warning that the annotation will be ignored. Environment variable: `QUARKUS_ARC_FAIL_ON_INTERCEPTED_PRIVATE_METHOD` Show more | boolean | `true` |
| [`quarkus.arc.selected-alternatives`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-selected-alternatives) The list of selected alternatives for an application. An element value can be: * a fully qualified class name, i.e. `org.acme.Foo` * a simple class name as defined by `Class#getSimpleName()`, i.e. `Foo` * a package name with suffix `.*`, i.e. `org.acme.*`, matches a package * a package name with suffix `.**`, i.e. `org.acme.**`, matches a package that starts with the value Each element value is used to match an alternative bean class, an alternative stereotype annotation type or a bean class that declares an alternative producer. If any value matches then the priority of `Integer#MAX_VALUE` is used for the relevant bean. The priority declared via `jakarta.annotation.Priority` is overridden. Environment variable: `QUARKUS_ARC_SELECTED_ALTERNATIVES` Show more | list of string |  |
| [`quarkus.arc.auto-producer-methods`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-auto-producer-methods) If set to true then `jakarta.enterprise.inject.Produces` is automatically added to all non-void methods that are annotated with a scope annotation, a stereotype or a qualifier, and are not annotated with `Inject` or `Produces`, and no parameter is annotated with `Disposes`, `Observes` or `ObservesAsync`. Environment variable: `QUARKUS_ARC_AUTO_PRODUCER_METHODS` Show more | boolean | `true` |
| [`quarkus.arc.exclude-types`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-exclude-types) The list of types that should be excluded from discovery. An element value can be: * a fully qualified class name, i.e. `org.acme.Foo` * a simple class name as defined by `Class#getSimpleName()`, i.e. `Foo` * a package name with suffix `.*`, i.e. `org.acme.*`, matches a package * a package name with suffix `.**`, i.e. `org.acme.**`, matches a package that starts with the value If any element value matches a discovered type then the type is excluded from discovery, i.e. no beans and observer methods are created from this type. Environment variable: `QUARKUS_ARC_EXCLUDE_TYPES` Show more | list of string |  |
| [`quarkus.arc.unremovable-types`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-unremovable-types) List of types that should be considered unremovable regardless of whether they are directly used or not. This is a configuration option equivalent to using `io.quarkus.arc.Unremovable` annotation. An element value can be: * a fully qualified class name, i.e. `org.acme.Foo` * a simple class name as defined by `Class#getSimpleName()`, i.e. `Foo` * a package name with suffix `.*`, i.e. `org.acme.*`, matches a package * a package name with suffix `.**`, i.e. `org.acme.**`, matches a package that starts with the value If any element value matches a discovered bean, then such a bean is considered unremovable. Environment variable: `QUARKUS_ARC_UNREMOVABLE_TYPES` Show more | list of string |  |
| [Artifacts that should be excluded from discovery](https://quarkus.io/guides/cdi-reference#quarkus-arc_section_quarkus-arc-exclude-dependency) | Type | Default |
| [`quarkus.arc.exclude-dependency."dependency-name".group-id`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-exclude-dependency-dependency-name-group-id) The maven groupId of the artifact. Environment variable: `QUARKUS_ARC_EXCLUDE_DEPENDENCY__DEPENDENCY_NAME__GROUP_ID` Show more | string | required |
| [`quarkus.arc.exclude-dependency."dependency-name".artifact-id`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-exclude-dependency-dependency-name-artifact-id) The maven artifactId of the artifact (optional). Environment variable: `QUARKUS_ARC_EXCLUDE_DEPENDENCY__DEPENDENCY_NAME__ARTIFACT_ID` Show more | string |  |
| [`quarkus.arc.exclude-dependency."dependency-name".classifier`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-exclude-dependency-dependency-name-classifier) The maven classifier of the artifact (optional). Environment variable: `QUARKUS_ARC_EXCLUDE_DEPENDENCY__DEPENDENCY_NAME__CLASSIFIER` Show more | string |  |
| [`quarkus.arc.detect-unused-false-positives`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-detect-unused-false-positives) If set to true then the container attempts to detect "unused removed beans" false positives during programmatic lookup at runtime. You can disable this feature to conserve some memory when running your application in production. Environment variable: `QUARKUS_ARC_DETECT_UNUSED_FALSE_POSITIVES` Show more | boolean | `true` |
| [`quarkus.arc.detect-wrong-annotations`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-detect-wrong-annotations) If set to true then the container attempts to detect _wrong_ usages of annotations and eventually fails the build to prevent unexpected behavior of a Quarkus application. A typical example is `@jakarta.ejb.Singleton` which is often confused with `@jakarta.inject.Singleton`. As a result a component annotated with `@jakarta.ejb.Singleton` would be completely ignored. Another example is an inner class annotated with a scope annotation - this component would be again completely ignored. Environment variable: `QUARKUS_ARC_DETECT_WRONG_ANNOTATIONS` Show more | boolean | `true` |
| [`quarkus.arc.strict-compatibility`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-strict-compatibility) If set to `true`, the container will perform additional validations mandated by the CDI specification. Some improvements on top of the CDI specification may be disabled. Applications that work as expected in the strict mode should work without a change in the default, non-strict mode. The strict mode is mainly introduced to allow passing the CDI Lite TCK. Applications are recommended to use the default, non-strict mode, which makes CDI more convenient to use. The "strictness" of the strict mode (the set of additional validations and the set of disabled improvements on top of the CDI specification) may change over time. Note that `transform-unproxyable-classes` and `remove-unused-beans` also has effect on specification compatibility. You may want to disable these features to get behavior closer to the specification. Environment variable: `QUARKUS_ARC_STRICT_COMPATIBILITY` Show more | boolean | `false` |
| [`quarkus.arc.dev-mode.monitoring-enabled`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-dev-mode-monitoring-enabled) If set to true then the container monitors business method invocations and fired events during the development mode. This config property should not be changed in the development mode as it requires a full rebuild of the application Environment variable: `QUARKUS_ARC_DEV_MODE_MONITORING_ENABLED` Show more | boolean | `false` |
| [`quarkus.arc.dev-mode.generate-dependency-graphs`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-dev-mode-generate-dependency-graphs) If set to `true` then the dependency graphs are generated and available in the Dev UI. If set to `auto` then the dependency graphs are generated if there’s less than 1000 beans in the application. If set to `false` the dependency graphs are not generated. Environment variable: `QUARKUS_ARC_DEV_MODE_GENERATE_DEPENDENCY_GRAPHS` Show more | `true`, `false`, `auto` | `auto` |
| [`quarkus.arc.test.disable-application-lifecycle-observers`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-test-disable-application-lifecycle-observers) If set to true then disable `StartupEvent` and `ShutdownEvent` observers declared on application bean classes during the tests. Environment variable: `QUARKUS_ARC_TEST_DISABLE_APPLICATION_LIFECYCLE_OBSERVERS` Show more | boolean | `false` |
| [`quarkus.arc.ignored-split-packages`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-ignored-split-packages) The list of packages that will not be checked for split package issues. A package string representation can be: * a full name of the package, i.e. `org.acme.foo` * a package name with suffix `.*`, i.e. `org.acme.*`, which matches a package that starts with provided value Environment variable: `QUARKUS_ARC_IGNORED_SPLIT_PACKAGES` Show more | list of string |  |
| [`quarkus.arc.context-propagation.enabled`](https://quarkus.io/guides/cdi-reference#quarkus-arc_quarkus-arc-context-propagation-enabled) If set to true and the SmallRye Context Propagation extension is present then the CDI contexts will be propagated by means of the MicroProfile Context Propagation API. Specifically, a `org.eclipse.microprofile.context.spi.ThreadContextProvider` implementation is registered. On the other hand, if set to false then the MicroProfile Context Propagation API will never be used to propagate the CDI contexts. Note that the CDI contexts may be propagated in a different way though. For example with the Vertx duplicated context. Environment variable: `QUARKUS_ARC_CONTEXT_PROPAGATION_ENABLED` Show more | boolean | `true` |
