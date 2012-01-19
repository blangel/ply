Scripts
-------
Scripts are anything executable; a bash script, perl script, ruby script, groovy script, etc. A script can even be a java
jar file with a main method specified in its manifest.
Ply resolves scripts through the following sequence:

-1- First check to see if it is an alias, if so expand it (see [Aliases](Aliases.md) for more information).  For each expanded
value go back to -1-.  If there is nothing to expand go to -2-.

-2- Then check for an executable script of the same name within the project's scripts directory (defined to be the value of a property named `scripts.dir` 
within context `project`; see [Properties](Properties.md) for a general description of properties)  This particular property defaults to _scripts_ relative to the project directory (so if
your project is located at `~/projects/myproject` its scripts directory would default to be `~/projects/myproject/scripts`).  If such a script is found, invoke it, otherwise try -3-.

-3- Check for an executable script of the same name within the ply scripts' directory `$PLY_HOME/scripts`. If found,
invoke it, otherwise, use it as an argument to the previous script.

For example; here is how ply resolves the following execution (assuming the default properties; i.e., you haven't re-aliased or added new scripts):

    $ ply dep tree

Ply first looks up `dep` to see if it is an alias.  It is, it is aliased as `ply-dependency-manager-1.0.jar`.  It then checks to see if `ply-dependency-manager-1.0.jar` is itself an alias.  It is not.  So it then checks to see if `ply-dependency-manager-1.0.jar` is a script from with the local project's scripts directory.  It is not so it checks to see if it is a script within the `$PLY_HOME/scripts` directory.  It is.  Ply then needs to resolve the next argument `tree` in case it is an argument to the script.  It goes through the same process as it did for `dep` however `tree` is neither an alias nor is it a script in either the local scripts directory or the system scripts directory and so it is treated as an argument to the `ply-dependency-manager-1.0.jar` script.

You can also run shell scripts directly from ply.  Shell scripts are those things which one could invoke natively from the shell/terminal; e.g., `ls`, `grep` or even `ply` itself.  To invoke shell scripts from ply use tick-marks to surround the shell invocation (i.e., ``ls -al``).  These shell scripts can be aliased like any other script; e.g., `print-directory=`ls -al``.  When invoking directly from the shell itself, most shells will interpret the tick marks and execute the command eagerly, short-circuiting their execution by ply.  To circumvent this escape the tick marks appropriately for your shell.  For instance in bash one can do this:

    $ ply '`echo hello there`'

Each script invocation will be passed, via environmental variables, a set of resolved properties particular to the
invocation.  See the [Properties](Properties.md) section for a description of how properties are resolved and then passed to
scripts.
