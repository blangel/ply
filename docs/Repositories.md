Repositories
------------

Repositories in Ply are nearly identical to those in __maven__.  They are a hierarchical collection of dependencies organized by _namespace_, _name_ and _version_.  Ply downloads dependencies to your project's local repository (which is controlled by `depmngr`'s property named `localRepo`).  Besides the local repository, repositories are found within property file named `repositories.properties`.  Repositories can be added/removed like any other property by interacting with the `repositories.properties` file either directly or via `ply set/rm/append/prepend in repositories`.  One can also use the `repo` script's `add` or `rm` options.  For instance, to add __JBoss__'s __maven__ repository to your project's repositories:

      $ ply repo add maven:https://repository.jboss.org/

Note, the `maven:` prefix to the URL.  This is to tell Ply that the repository is a __maven__ repository and not a Ply repository.  Ply repositories clearly do not need or have `pom.xml` files and instead use a project's `dependencies.properties` file when figuring out transitive dependencies.  If you are adding a Ply repository you can simply add the repository without a prefix.  For instance:

      $ ply repo add http://repository.ocheyedan.net/

Using Your Local Maven Repository
---------------------------------

For those who have used __maven__ and have built a large local repository, you can add your local __maven__ repository to your list of Ply repositories, thus circumventing re-downloading of copious amounts of _jar_ files.  To do so issue the following command from within your project:

     $ ply repo add maven:~/.m2/repository

Where `~/.m2/repository` points to your local __maven__ repository.

This will add your local __maven__ repository to the list of repositories _ply_ uses for your project.  If you want to add your local __maven__ repository globally so that each _ply_ project automatically utilizes it, add the following to the `$PLY_HOME/config/repositories.properties` file:

     ~/.m2/repository=maven

Where, again, `~/.m2/repository` points to your local __maven__ repository.

Adding Authentication Information for Repositories
-------------------------------------------------

Ply supports two authentication types for secured repositories.  One is __basic__ authentication (for http/https) repositories. This is the default authentication mechanism for __maven__ repositories like [Artifactory](http://www.jfrog.com/home/v_artifactory_opensource_overview) and [Nexus](http://www.sonatype.org/nexus/). The other type is __git__ which allows users to leverage git repositories as their dependency repositories. In conjunction with __GitHub__ this is very useful as teams can leverage existing Team/User management of __GitHub__ without replicating the logic within another tool.

To add username/password for a repository do the following.

     $ ply repo auth REPO_URL AUTH_TYPE USERNAME

Where REPO_URL points to your repository URL.  AUTH_TYPE is either `basic` or `git` and USERNAME is the user's login name to the REPO_URL.  For instance, to add __basic__ authentication information to a __maven__ repository located at *http://mycompany.com/maven/repo* for the user *blangel* do the following:

     $ ply repo auth http://mycompany.com/maven/repo basic blangel
     
After invoking, Ply will prompt for password. It then encrypts this value and stores for use when resolving dependencies from the added repository.  This command will add the information globally (i.e., you do not need to do this per project). If however, you wanted to add authentication information just within a project you can use the `repo auth-local` variant to do so.

As a complete example, here is how one would add authentication information (globally) and then add the repository into a project's list of known repositories.

     $ echo "Adding basic authentication information for user blangel for repository http://mycompany.com/maven/repo; this only needs to be done once as it'll be added globally"
     $ ply repo auth http://mycompany.com/maven/repo basic blangel
     $ echo "For each ply project needing access to http://mycompany.com/maven/repo do the following (or edit the global $PLY_HOME/config/repositories.properties once adding entry 'http://mycompany.com/maven/repo=maven')"
     $ ply repo add maven:http://mycompany.com/maven/repo

Continue on to [Running tests](RunningTests.md)