Dependencies
-------------

Ply handles dependencies in much the same way as __maven__.  Dependencies are installed into repositories (which can be local or remote) and then downloaded when necessary.  In fact, Ply piggy-backs off of the central __maven__ repository by default (see `ply get-all from repositories`).  Dependencies are described in an atom format:

      namespace:name:version[:artifactName]

where the `artifactName` is optionally specified and defaults to be `${name}-${version}.${packaging}`.
  
To add a dependency to a project you can add the dependency into the `dependencies.properties` file, however, this will not resolve the dependency (i.e., download the dependency thus ensuring that it is available) until the next time the `dep` alias is run (e.g., during compilation).  You can use the `dep` script itself to add a dependency and force resolution immediately: 

     $ ply dep add namespace:name:version

Just like __maven__, if you add a dependency to a project which itself has dependencies, these transitive dependencies are incorporated where necessary.  To see all transitive and direct dependencies of your project, run the following:

     $ ply dep tree

Scoped Dependencies
-------------------

If you haven't already, read [Scopes](Scopes.md) for a general description of what scopes are in Ply.  With respect to dependencies, we use scopes to indicate to Ply that certain dependencies are only applicable in certain circumstances.  The most familiar example would be test scoped dependencies.  For instance, suppose we want to add a dependency to our project but only want that dependency utilized when we are running within the _test_ scope.  We can accomplish that by adding the dependency to the `dependencies.test.properties` file or adding via `dep` run within scope _test_; e.g., to add a _test_ scoped dependency to the [Junit](http://www.junit.org/) project we would:

     $ ply test:dep add junit:junit:4.10

The junit dependency has now been added but only within the _test_ scope.  To visualize our _test_ dependencies we issue the same _tree_ command but scope it with _test_:

     $ ply test:dep tree

Transient Dependencies
----------------------

Transient dependencies let others know that when they use your project the dependency is not necessary in order to work correctly.  Transient dependencies are available on the compilation classpath but are not exported into packages (war/uber-jar/etc) or pulled in as transitive dependencies.  One example of where this is necessary is when your project provides two implementations of the same feature.  For instance, suppose your project provides caching support and can work with either the [memcache](http://memcached.org/) or [ehcache](http://ehcache.org/) libraries.  Your project will need to compile with both but dependent projects will not.  Those dependent projects would depend upon your project and then, directly, on whichever cache library they choose. 

