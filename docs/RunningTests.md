Running Tests
-------------

Tests are configured using the `test` scope (see [Scopes](Scopes.md) for a general description).  Following the __maven__ convention, test files by default are located at `src/test` relative to the project directory.  To build test files:

    $ ply test:compile

To build, package and install the project's test files into the local repository:

    $ ply test:install

To build, package, install and run the `junit` tests:

    $ ply test 

Utilizing the `electric-chilly` project made in [Project Setup](ProjectSetup.md), let's create a test file:

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

Make sure you've added the test-scoped `junit` dependency (`ply test:dep add junit:junit:4.10`).  Now let's test!

     $ ply test

If you're lucky, you'll pass the test and see the following:

![test success](https://github.com/blangel/ply/raw/master/docs/imgs/ply-test-success.png "test success")

But you may fall into the failure case:

![test error](https://github.com/blangel/ply/raw/master/docs/imgs/ply-test-failure.png "test error")

You can also find detailed information about the tests (in the exact same format as __maven__'s _surefire_ plugin generates) at `$project.build.dir/$project.reports.dir` which defaults to `target/reports` relative to the project directory.

Let's add another test to see how multiple tests are run.

      $ touch src/test/java/net/ocheyedan/electricchilly/DinnerPartyTest.java

Then copy/paste the following code into the `DinnerPartyTest.java` file.	  
     
     	package net.ocheyedan.electricchilly;

        import org.junit.Test;
        import static junit.framework.Assert.*;

        public class DinnerPartyTest {

            @Test
            public void entertainSomeChums() {
                assertEquals(true, true); // TODO - implement DinnerParty class and test                                                                      
            }

        }

Now, let's rerun the tests

     $ ply test

If `FrigidWinterTest` fails you'll see something similar to this

![multiple tests](https://github.com/blangel/ply/raw/master/docs/imgs/ply-tests-multiple-fail.png "multiple tests")

You can also tell ply which tests you want to have run.  For example to have only the `DinnerPartyTest` tests run:

    $ ply test DinnerPartyTest

![test class select](https://github.com/blangel/ply/raw/master/docs/imgs/ply-tests-class-select.png "test class select")

The argument can be any ant-style wildcard; here are some examples

    $ ply test Dinner*

Would match any class whose name starts with `Dinner`

    $ ply test *Test

Would match any class whose name ended with `Test`

    $ ply test net.**.electricchilly.DinnerPartyTest

Would match any class whose package started with `net` had zero or more packages and then package `electricchilly` and had class name `DinnerPartyTest`

    $ ply test net.**.electricchilly.*

Would match any class whose package started with `net` had zero or more packages and then package `electricchilly` and had any class name.

    $ ply test DinnerPartyTest#enter*

Would match any test method from a class whose name was `DinnerPartyTest` and the method name started with `enter` 

Continue on to [Changing log levels](Logging.md)