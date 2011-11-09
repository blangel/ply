Project Setup
-------------

To enable a directory/project to use ply, simply run init from within the directory:

    $ ply init

This will create the `.ply/config` directory and also a `project.properties` file with the following properties set

* __namespace__ - will default to be the name of the project directory.  The _namespace_ is analogous to _groupId_ in __maven__.
* __name__ - will default to be the name of the project directory.  The _name_ is analogous to _artifactId_ in __maven__.
* __version__ - will default to be `1.0`.

So issueing the following sequence of commands:

     $ mkdir electric-chilly
     $ cd electric-chilly/
     $ ply init

Will create directories `.ply/config` under `electric-chilly` and the following `project.properties` file within `.ply/config`

     namespace=electric-chilly
     name=electric-chilly
     version=1.0

This can also be verified via _ply_ itself by doing:

     $ ply config get

Here's an image of the steps:

![project setup](https://github.com/blangel/ply/raw/master/docs/imgs/project-setup.png "project setup")