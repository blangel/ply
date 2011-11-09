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
invoke it, otherwise, try -4-

-4- Invoke directly (must be accessible via the system $PATH).

Each script invocation will be passed, via environmental variables, a set of resolved properties particular to the
invocation.  See the [Properties](Properties.md) section for a description of how properties are resolved and then passed to
scripts.
