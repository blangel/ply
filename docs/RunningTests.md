Running Tests
-------------

Tests are configured using the `test` scope (see [Scopes](Scopes.md) for a description).  Following the __maven__ convention, test files by default are located at `src/test` relative to the project directory.  To build test files:

    $ ply test:compile

To build, package and install the project's test files into the local repository:

    $ ply test:install

To build, package install and run the `junit` tests:

    $ ply test 

Utilizing the `electric-chilly` project made in [Project Setup](ProjectSetup.md), let's create a test files:

      $ mkdir -p src/test/java/net/ocheyedan/electricchilly/
      $ touch src/test/java/net/ocheyedan/electricchilly/FrigidWinterTest.java

Then copy/paste the following code into the `FrigidWinterTest.java` file.	  
     
     package net.ocheyedan.electricchilly;
     
     import org.junit.Test;
     import static junit.framework.Assert.*;

     public class FrigidWinterTest {
     
        @Test
        public void pluginFizzleElectricBlanket() {
	     FrigidWinter frigidWinter = new FrigidWinter();
             assertEquals(true, frigidWinter.pluginFizzleElectricBlanket());
    	}

     }

Make sure you've added the test-scoped `junit` dependency (`ply "test:dep add junit:junit:4.10"`).  Now let's test!

     $ ply test

If you're lucky, you'll pass the test and see the following:

![test success](https://github.com/blangel/ply/raw/master/docs/imgs/ply-test-success.png "test success")

But you may fall into the failure case:

![test error](https://github.com/blangel/ply/raw/master/docs/imgs/ply-test-failure.png "test error")

You can also find detailed information about the tests (in the exact same format as __maven__'s _surefire_ plugin generates) at `$project.build.dir/$project.reports.dir` which defaults to `target/reports` relative to the project directory.