Overview
--------

Ply is a build tool.  It is sensible, fast and a joy to use. 

Features
--------

* __Pretty print__ - ply's output is clean and colored.  Here's the actual output of running `ply clean test` from the `ply-util` module:

![ply-util: ply clean test](https://github.com/blangel/ply/raw/master/docs/imgs/ply-util-test.png "ply-util: ply clean test")

* __No xml__ - Ply has no xml configuration. Its configuration is simple and familiar; java-style [properties](ply/tree/master/docs/Properties.md) files. Ply even has tools built-in to help you manage your project's properties. And yes, just properties files. Good ol' simple key-value pairs, no complex syntax or DSL to learn.
* __Sensible defaults__ - ply uses defaults which are intuitive (e.g., the default _java_ source/target for compilation is the version of the `$JAVA_HOME` jdk). And because ply has sensible and intuitive defaults starting a new project is as simple as running `ply init`.
* __Easily extensible__ - since ply simply executes scripts (or aliases of scripts; i.e., _clean_, _install_, _test_) changing or augmenting a build lifecycle is just a matter of adding/removing/replacing scripts (or re-aliasing them).  The default scripts and aliases provided specify a best practice for development but if your project wants/needs to deviate from this approach doing so shouldn't feel like working against the grain.  And keep in mind, [scripts](ply/tree/master/docs/Scripts.md) are anything executable (bash, perl, ruby, python, ...) so even though your project's written in one language feel free to flex your polyglot-muscles and augment your build process in any language you like! 

Why Another Build Tool?
-----------------------

Other build tools make you work harder than you should have to.  How hard? Stupidly hard:

  * __Copious XML configuration__ - many build tools use xml to configure their execution.  XML is verbose and maintaining it is arduous, err prone and often mandates IDE features/plugins to prevent developer insanity.
  * __Rigid extensions__ - extending the functionality of build tools is hard and often forces developers to conform to rigid APIs (i.e., implementing an interface in a particular langauge) or in the best case forces developers to write their extensions in a set language (i.e., an extension is anything _provided_ it is written in Ruby).
  * __Boiler-plate configuration for new projects__ - starting a new project, developers are eager to get working.  Having to copy and paste configuration files from existing projects and then do find-replace operations is annoying and not productive.

Concepts
--------

* [Scripts](ply/tree/master/docs/Scripts.md)
* [Properties](ply/tree/master/docs/Properties.md)
* [Aliases](ply/tree/master/docs/Aliases.md)
* [Scopes](ply/tree/master/docs/Scopes.md)
* [Dependencies](ply/tree/master/docs/Dependencies.md)
* [Submodules](ply/tree/master/docs/Submodules.md)

At its simplest _ply_ just invokes a series of scripts. The following is a valid series of scripts for ply:

    $ ply clean compile

The series is space delimited so the previous example ran two scripts: `clean` and `compile`

[Scripts](ply/tree/master/docs/Scripts.md) can be extended and [aliased](ply/tree/master/docs/Aliases.md).
Ply ships with property defaults and packaged scripts which allow most java projects to
build with no configuration.  For a list of all scripts which ply ships with see [Included Scripts](ply/tree/master/docs/IncludedScripts.md).

To enable a directory/project to use ply, simply run `init` from within the directory:

    $ ply init

Ok, I Want It!
--------------

Download the tar file [ply.tar](https://s3.amazonaws.com/ply-buildtool/ply.tar) or the zip file [ply.zip](https://s3.amazonaws.com/ply-buildtool/ply.zip)

Un-package the tar/zip file to a directory of your choosing (say `/opt/ply`) and then make sure the following properties are set as environmental variables:

* `JAVA_HOME` -> (likely already set by your distro) set to the home directory of the java installation

* `PLY_HOME` -> set to the directory of where ever you untar-ed ply (i.e., `/opt/ply`).

Finally add `${PLY_HOME}/bin` to your `$PATH`

__Bash Tab Completion__ (i.e., readline support)

Within the distribution is a file [ply_completion.bash](ply/raw/master/dist/ply/ply_completion.bash) which provides Bash tab completion.  To enable:

       $ sudo cp ply_completion.bash /etc/bash_completion.d/

Updating Ply
------------

Once installed, ply can update itself to the most recent version.  Simply run:

       $ ply update

Tutorials
--------

* [Project setup](ply/tree/master/docs/ProjectSetup.md)
* [Building](ply/tree/master/docs/BuildingProject.md)
* [Adding dependencies](ply/tree/master/docs/DependenciesTutorial.md)
* [Managing repositories](ply/tree/master/docs/Repositories.md)
* [Running tests](ply/tree/master/docs/RunningTests.md)
* [Changing log levels](ply/tree/master/docs/Logging.md)