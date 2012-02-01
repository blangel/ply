Dependencies
-------------

First, make sure you understand dependencies and their role in Ply by reading [Dependencies](Dependencies.md).

Utilizing the `electric-chilly` project made in [Project Setup](ProjectSetup.md), let's add a dependency to the [Google Guava](http://code.google.com/p/guava-libraries/) project.

      $ ply dep add com.google.guava:guava-collections:r03 -Pply.log.levels=info

The `-Pply.log.levels=info` property override is unnecessary but added to give more detailed feedback about what transitive dependencies are being downloaded, see [Changing Log Levels](Logging.md) for details about available log levels.  Your output should be similar to the following:

![dep guava](https://github.com/blangel/ply/raw/master/docs/imgs/ply-dep-guava.png "dep guava")

We can see that four dependencies were downloaded even though we only added one.  Let's issue the `ply dep tree` command to visualize the dependencies of `electric-chilly`:

      $ ply dep tree

![dep tree](https://github.com/blangel/ply/raw/master/docs/imgs/ply-dep-tree.png "dep tree")

It's now apparent that `electric-chilly` has the one direct dependency which we added `com.google.guava:guava-collections:r03` but that the `guava-collections` dependency itself depends upon two other projects: `com.google.guava:guava-primitives:r03` and `com.google.guava:guava-annotations:r03`.  The `guava-primitives` itself depends upon `com.google.guava:guava-base:r03`.

Test Scoped Dependency
---------------------

Let's now add `junit` to our `electric-chilly` project as a test dependency.

      $ ply test:dep add junit:junit:4.10

Note, adding a test dependency is the same except the `dep` script is run within scope _test_ (i.e., `test:dep`).

Continue on to [Repositories](Repositories.md)