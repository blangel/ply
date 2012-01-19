Repositories
------------

Repositories in Ply are nearly identical to those in __maven__.  They are a hierarchical collection of dependencies organized by _namespace_, _name_ and _version_.  Ply downloads dependencies to your project's local repository (which is controlled by `depmngr`'s property named `localRepo`).  Besides the local repository (specified by _depmngr.localRepo_) repositories are found within property file named `repositories.properties`.  Repositories can be added/removed like any other property by interacting with the `repositories.properties` file either directly or via `ply set/remove/append/prepend in repositories`.  One can also use the `dep` script's `add-repo` or `remove-repo` options.  For instance, to add __JBoss__'s __maven__ repository to your project's repositories:

      $ ply dep add-repo maven:https://repository.jboss.org/

Note, the `maven:` prefix to the URL.  This is to tell Ply that the repository is a __maven__ repository and not a Ply repository.  Ply repositories clearly do not need or have `pom.xml` files and instead use a project's `dependencies.properties` file when figuring out transitive dependencies.  If you are adding a Ply repository you can simply add the repository without a prefix.  For instance:

      $ ply dep add-repo http://repository.ocheyedan.net/

Using Your Local Maven Repository
---------------------------------

For those who have used __maven__ and have built a large local repository, you can add your local __maven__ repository to your list of Ply repositories, thus circumventing re-downloading of copious amounts of _jar_ files.  To do so issue the following command from within your project:

     $ ply dep add-repo maven:~/.m2/repository

Where `~/.m2/repository` points to your local __maven__ repository.

This will add your local __maven__ repository to the list of repositories _ply_ uses for your project.  If you want to add your local __maven__ repository globally so that each _ply_ project automatically utilizes it, add the following to the `$PLY_HOME/config/repositories.properties` file:

     ~/.m2/repository=maven

Where, again, `~/.m2/repository` points to your local __maven__ repository.
