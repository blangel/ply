Building
-------------

To build a project, simply run

    $ ply compile

To build and package a project, simply run

    $ ply compile package

To build, package and install the package into the local repository, simply run

    $ ply install

Those familiar with __maven__ will note the similarities.

Utilizing the `electric-chilly` project made in [Project Setup](ProjectSetup.md), let's create a source file and build.

      $ mkdir -p src/main/java/net/ocheyedan/electricchilly/
      $ touch src/main/java/net/ocheyedan/electricchilly/FrigidWinter.java

Then copy/paste the following code into the `FrigidWinter.java` file.	  
     
     package net.ocheyedan.electricchilly;
     
     public class FrigidWinter {
     
        public boolean pluginFizzleElectricBlanket() {
             return (System.currentTimeMillis() % 2) == 0;
    	}

     }

Now let's compile!

     $ ply compile

![project compile](https://github.com/blangel/ply/raw/master/docs/imgs/building-compile.png "project compile")

We can package and install the project as well.

     $ ply install

![project install](https://github.com/blangel/ply/raw/master/docs/imgs/building-install.png "project install")

Note, the output, because the `FrigidWinter.java` file has not changed since last we compiled, nothing needed to be recompiled.  Doing a `ply clean install` would force a recompilation.