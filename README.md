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

Now the project is ready to be built:

    $ ply install

For help on ply:

    $ ply --usage

Concepts
--------

* [Scripts](ply/tree/master/docs/Scripts.md)
* [Properties](ply/tree/master/docs/Properties.md)
* [Aliases](ply/tree/master/docs/Aliases.md)
* [Scopes](ply/tree/master/docs/Scopes.md)
* [Submodules](ply/tree/master/docs/Submodules.md)
