Project Setup
-------------

To enable a directory/project to use ply, simply run init from within the directory:

    $ ply init

This will create the `.ply/config` directory and also a `project.properties` file with the following properties set

* __namespace__ - will default to be the name of the project directory.  The _namespace_ is analogous to _groupId_ in __maven__.
* __name__ - will default to be the name of the project directory.  The _name_ is analogous to _artifactId_ in __maven__.
* __version__ - will default to be `1.0`.

Additionally the `project.src.dir` and `project.res.dir` directories will be created for you in the default and `test` [scopes](Scopes.md).

So issuing the following sequence of commands:

     $ mkdir electric-chilly
     $ cd electric-chilly/
     $ ply init

Will create directories `.ply/config` under `electric-chilly` and the following `project.properties` file within `.ply/config`

     namespace=electric-chilly
     name=electric-chilly
     version=1.0

Also, directories `src/main/java`, `src/main/resources`, `src/test/java` and `src/test/resources` will be created under `electric-chilly`. 

The created property values can be verified via _ply_ itself by doing:

     $ ply get

Here's an image of the steps:

![project setup](https://github.com/blangel/ply/raw/master/docs/imgs/project-setup.png "project setup")

Overriding Default Values
-------------------------

In case the default values are not desired, you can use the [ad hoc](Properties.md) properties of ply to override any value.  For instance, to specify a different __namespace__, say __net.ocheyedan__, you would invoke `ply init` with an ad hoc property:

    $ ply init -Pproject.namespace=net.ocheyedan

Everything would be created as in the original example except the __namespace__ would be __net.ocheyedan__ instead of the default (i.e., the name of the project directory).

Continue on to [Building](BuildingProject.md)