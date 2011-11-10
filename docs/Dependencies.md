Dependencies
-------------

Ply handles dependencies in much the same way as __maven__.  Dependencies are installed into repositories (which can be local or remote) and then downloaded when necessary.  In fact, _ply_ piggy-backs off of the central __maven__ repository by default (see `ply config --repositories get-all`).  Dependencies are described in an atom format:

      namespace:name:version[:artifactName]

where the `artifactName` is optionally specified and defaults to be `${name}-${version}.${packaging}`.
  
To add a dependency to a project you can add the dependency into the `dependencies.properties` file, however, this will not resolve the dependency (i.e., download the dependency thus ensuring that it is available) until the next time the `dep` alias is run (e.g., during compilation).  You can use the `dep` script itself to add a dependency and force resolution immediately: 

     $ ply "dep add namespace:name:version"

Just like __maven__, if you add a dependency to a project which itself has dependencies, these transitive dependencies are incorporated where necessary.  To see all transitive and direct dependencies of your project, run the following:

     $ ply "dep tree"

Utilizing the `electric-chilly` project made in [Project Setup](ProjectSetup.md), let's add a dependency to the [Google Guava](http://code.google.com/p/guava-libraries/) project.

      $ ply "dep add com.google.guava:guava-collections:r03" -Pply.log.levels=info

The `-Pply.log.levels=info` property override is unnecessary but added to give more detailed feedback about what transitive dependencies are being downloaded, see [Changing Log Levels](Logging.md) for details about available log levels.  Your output should be similar to the following:

![dep guava](https://github.com/blangel/ply/raw/master/docs/imgs/ply-dep-guava.png "dep guava")

We can see that four dependencies were downloaded even though we only added one.  Let's issue the `ply "dep tree"` command to visualize the dependencies of `electric-chilly`:

      $ ply "dep tree"

![dep tree](https://github.com/blangel/ply/raw/master/docs/imgs/ply-dep-tree.png "dep tree")

It's now apparent that `electric-chilly` has the one direct dependency which we added `com.google.guava:guava-collections:r03` but that the `guava-collections` dependency itself depends upon two other projects: `com.google.guava:guava-primitives:r03` and `com.google.guava:guava-annotations:r03`.  The `guava-primitives` itself depends upon `com.google.guava:guava-base:r03`.

Scoped Dependencies
-------------------

If you haven't already, read [Scopes](Scopes.md) for a general description of what scopes are in _ply_.  With respect to dependencies, we use scopes to indicate to _ply_ that certain dependencies are only applicable in certain circumstances.  The most familiar example would be test scoped dependencies.  For instance, suppose we want to add a dependency to our project but only want that dependency utilized when we are running within the _test_ scope.  We can accomplish that by adding the dependency to the `dependencies.test.properties` file or adding via `dep` run within scope _test_; e.g., to add a _test_ scoped dependency to the [Junit](http://www.junit.org/) project we would:

     $ ply "test:dep add junit:junit:4.10"

The junit dependency has now been added but only within the _test_ scope.  To visualize our _test_ dependencies we issue the same _tree_ command but scope it with _test_:

     $ ply "test:dep tree"

Using Your Local Maven Repository
---------------------------------

For those who have used __maven__ and have built a large local repository, you can add your local __maven__ repository to your list of ply repositories, thus circumventing re-downloading of copious amounts of _jar_ files.  To do so issue the following command from within your project:

     $ ply "dep add-repo maven:~/.m2/repository"

Where `~/.m2/repository` points to your local __maven__ repository.

This will add your local __maven__ repository to the list of repositories _ply_ uses for your project.  If you want to add your local __maven__ repository globally so that each _ply_ project automatically utilizes it, add the following to the `$PLY_HOME/config/repositories.properties` file:

     ~/.m2/repository=maven

Where, again, `~/.m2/repository` points to your local __maven__ repository.




