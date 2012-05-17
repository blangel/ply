Scopes
------

Sometimes property values are only relevant (or should be changed to a different value) depending upon the particular invocation.  Scopes provide a way of facilitating this in ply.  Those familiar with the build tool __maven__ can think of scopes as a more powerful form of profiles.  More powerful as, in maven the profile is applied to all phases in a lifecycle but in ply, using maven parlance, scopes allow for each phase to be tied to a different profile during a single execution.  For example, `mvn clean install -PX` means run phases _clean_ and _install_ both within profile X but in ply one could say `ply X:clean install` which means run _clean_ in scope X but _install_ in the default scope.  To illustrate the usefulness of this, first consider, in a simplified form, what happens during compilation. 

    $ ply compile

1) Ply resolves `compile` to be an alias mapped to the `ply-compiler-1.0.jar` script (for simplicity let's ignore the _file-changed_ and _dep_ aliases).  

2) This compiler script then determines where to look for source files by looking for the value of a property named `src.dir` from the `project` context.  

3) It then compiles the code and places the resulting files into the value of a property named `build.path` from the `compiler` context.

By default the value of `project.src.dir` is `src/main/java` and the value of `compiler.build.path` is `${project.build.dir}/classes` (where `project.build.dir` by default is `target`).

Now what happens during test compilation?  The script should essentially do the exact same thing it's just that the `project.src.dir` and `compiler.build.path` property values should change.  Ply solves this by its notion of scopes.  A scope is simply a property value with a '.' in the name.  Everything before the period is the context name (like usual) and everything after the period is the scope name.  For example the file `compiler.properties` has context `compiler` and no scope (i.e., the default scope).  File `compiler.test.properties` has context `compiler` and scope `test`.  Ply then leverages this when resolving properties and passing them to scripts.  If the script is being run in the default scope then any scoped property files (those with a period in the name) are ignored.  If, however, a script is being run in scope (that is the script is prefixed with the scope name and then a ':') then all property files matching the provided scope are also resolved.  So to run compilation using the _test_ scope one would:

    $ ply test:compile 

Which tells ply, run alias/script `compile` but resolve properties using the scope `test`.  Scoped properties inherit from their non-scoped counterparts.  That is, all properties defined within `compiler.properties` are used when running scope `test`, it's just that if there exists a file named `compiler.test.properties` its values are used in addition and will override any same-named property from the non-scoped file. 

This same notion of scopes (which is used for test execution as well as scoping dependencies) can be useful when choosing which property values to use while filtering.  For example, suppose your project placed all URLs in context `urls`; i.e.,

    $ ply set domain=localhost in urls

One could scope the urls and then run `install` with the proper scope depending upon the environment.  For instance;

    $ echo "Setting domain=localhost in default scope."
    $ ply set domain=localhost in urls
    $ echo "Setting domain=dev.com in 'dev' scope."
    $ ply dev:set domain=dev.com in urls
    $ echo "Setting domain=beta.com in 'beta' scope."
    $ ply beta:set domain=beta.com in urls

Now to run a build for the dev environment:

    $ ply dev:install

Because of the `dev` scope, ply will automatically run filtering against the `dev` urls.  

Something useful to note, one can include scopes within aliases and in fact that is how the alias _test_ is defined:

    $ ply set test="install test:install ply-test-junit-1.0.jar" in aliases
