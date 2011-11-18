Overview
--------
Ply is a fun build tool that works.

Build tools suck. Make is a disaster. Maven and Ant have copious XML configurations and don't allow the developer to easily override things they want.
DSL's seem nice on the surface but end up being inflexible when one has a large complex project to work on.
Ply fixes all of this by providing a few simple paradigms:

  * Small, simple run-time written in Java
  * Complete configuration of everything by using code in any language (i.e.; not a DSL)
  * Sensible defaults so you have to work very little

Ply currently only supports Java builds, but is language agnostic. Future work is planned to make Ply build other languages.

Features
--------

* __Pretty print__ - _ply_'s output is clean and colored.  Here's the actual output of running `ply clean test` from the `ply-util` module:

![ply-util: ply clean test](https://github.com/blangel/ply/raw/master/docs/imgs/ply-util-test.png "ply-util: ply clean test")

* __Sensible defaults__ - _ply_ uses defaults which are intuitive (i.e., the default _java_ source/target for compilation is the version of the `$JAVA_HOME` jdk) and make setting up a new project extremely easy. 
* __Easily extensible__ - since _ply_ simply executes scripts (or aliases of scripts; i.e., _clean_, _install_, _test_) changing or augmenting a build lifecycle is just a matter of adding/removing/replacing scripts (or re-aliasing them).  The default scripts and aliases provided specify a best practice for development but if your project wants/needs to deviate from this approach doing so shouldn't feel like working against the grain.  And keep in mind, [scripts](ply/tree/master/docs/Scripts.md) are anything executable (bash, perl, ruby, python, ...) so even though your project's written in one language feel free to flex your polyglot-muscles and augment your build process in any language you like! 

Download/Install
----------------

[ply.tar](ply/raw/master/dist/ply.tar)

Untar the `ply.tar` package to a directory of your choosing (say `/opt/ply`) and then make sure the following properties are set as environmental variables:

* `JAVA_HOME` -> (likely already set by your distro) set to the home directory of the java installation

* `PLY_HOME` -> set to the directory of where ever you untar-ed ply (i.e., `/opt/ply`).

Finally add `${PLY_HOME}/bin` to your `$PATH`

__Bash Tab Completion__ (i.e., readline support)

Within the distribution is a file [ply_completion.bash](ply/raw/master/dist/ply/ply_completion.bash) which provides Bash tab completion.  To enable:

       $ sudo cp ply_completion.bash /etc/bash_completion.d/

Concepts
--------

* [Scripts](ply/tree/master/docs/Scripts.md)
* [Properties](ply/tree/master/docs/Properties.md)
* [Aliases](ply/tree/master/docs/Aliases.md)
* [Scopes](ply/tree/master/docs/Scopes.md)
* [Dependencies](ply/tree/master/docs/Dependencies.md)
* [Submodules](ply/tree/master/docs/Submodules.md)

At its simplest _ply_ just invokes a series of scripts. The following is a valid series of scripts for ply:

    $ ply "echo ply says:" "echo hello"

The series is space delimited so the previous example ran two scripts: `echo ply says:` and `echo hello`.

[Scripts](ply/tree/master/docs/Scripts.md) can be extended and [aliased](ply/tree/master/docs/Aliases.md).
Ply ships with property defaults and packaged scripts which allow most java projects to
build with no-to-minimal configuration.  For a list of all scripts which ply ships with see [Included Scripts](ply/tree/master/docs/IncludedScripts.md).

To enable a directory/project to use ply, simply run init from within the directory:

    $ ply init

Tutorials
--------

* [Project setup](ply/tree/master/docs/ProjectSetup.md)
* [Building](ply/tree/master/docs/BuildingProject.md)
* [Adding dependencies](ply/tree/master/docs/DependenciesTutorial.md)
* [Managing repositories](ply/tree/master/docs/Repositories.md)
* [Running tests](ply/tree/master/docs/RunningTests.md)
* [Changing log levels](ply/tree/master/docs/Logging.md)