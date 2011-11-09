Overview
--------
Ply is a build tool.  At its simplest it just invokes a series of scripts. The following is a valid series of scripts for ply:

    $ ply "echo ply says:" "echo hello"

The series is space delimited so the previous example ran two scripts: `echo ply says:` and `echo hello`.

[Scripts](ply/tree/master/docs/Scripts.md) can be extended and [aliased](ply/tree/master/docs/Aliases.md).
Ply ships with property defaults and packaged scripts which allow most java projects to
build with no-to-minimal configuration.  For a list of all scripts which ply ships with see [Included Scripts](ply/tree/master/docs/IncludedScripts.md).

To enable a directory/project to use ply, simply run init from within the directory:

    $ ply init

Features
--------

* __Pretty print__ - _ply_'s output is clean and colored.  Here's the actual output of running `ply clean test` from the `ply-util` module:

![ply-util: ply clean test](https://github.com/blangel/ply/raw/master/docs/imgs/ply-util-test.png "ply-util: ply clean test")

* __Sensible defaults__ - _ply_ uses defaults which are intuitive (i.e., the default _java_ source/target for compilation is the version of the `$JAVA_HOME` jdk) and make setting up a new project extremely easy. 
* __Easily extensible__ - since _ply_ simply executes scripts (or aliases of scripts; i.e., _clean_, _install_, _test_) changing or augmenting a build lifecycle is just a matter of adding/removing/replacing scripts (or re-aliasing them).  The default scripts and aliases provided specify a best practice for development but if your project wants/needs to deviate from this approach doing so shouldn't feel like working against the grain.  And keep in mind, [scripts](ply/tree/master/docs/Scripts.md) are anything executable (bash, perl, ruby, python, ...) so even though your project's written in one language feel free to flex your polyglot-muscles and augment your build process in any language you like! 

Download/Install
----------------

[ply.tar](ply/raw/master/ply.tar)

Untar the `ply.tar` package to a directory of your choosing (say `/opt/ply`) and then make sure the following properties are set as environmental variables:

* `JAVA_HOME` -> (likely already set by your distro) set to the home directory of the java installation

* `PLY_HOME` -> set to the directory of where ever you untar-ed ply (i.e., `/opt/ply`).

Finally add `${PLY_HOME}/bin` to your `$PATH`

Concepts
--------

* [Scripts](ply/tree/master/docs/Scripts.md)
* [Properties](ply/tree/master/docs/Properties.md)
* [Aliases](ply/tree/master/docs/Aliases.md)
* [Scopes](ply/tree/master/docs/Scopes.md)
* [Submodules](ply/tree/master/docs/Submodules.md)

Tutorials
--------

* [Project setup](TODO)
* [Add dependencies](TODO)
* [Add submodules](TODO)
* [Running tests](TODO)
* [Changing log levels](TODO)