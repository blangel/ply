Properties
----------
Ply is configured via property files.  There are two directories where ply looks for property files.  The first is located
at `$PLY_HOME/config` and represents the system defaults.  The second is located at directory `.ply/config` relative to the project directory.  
This directory is created for you when initializing a project; i.e., `ply init`.

The files within these directories are simply unix style property files.  The name of the property file
is considered to be the context (and if there is a '.' in the file name that after the period is the scope, see [Scopes](Scopes.md) for
a general description of scopes).

Any property defined within the system configuration directory can be overridden within the project's local config
directory.  To override a property locally, create a property file within the `.ply/config` directory with the same name
and assign your overridden value to the same property name.  For instance, the source directory used during compilation
is a property defined within the `$PLY_HOME/config/project.properties` file named `src.dir`.  To override this for
a project, you would add a property named `src.dir` within the `.ply/config/project.properties` file.

Because interacting with these property files is a common task, ply provides tools to assist

    $ ply --usage

Some examples:

To get all locally defined properties (across all contexts):

    $ ply get

To get all properties (including system properties):

    $ ply get-all

To get all local properties with context `project` (i.e., those defined within the `project.properties` file)

    $ ply get from project

To get all properties starting with _name_

    $ ply get-all name*

To get all properties within the `compiler` context starting with _warn_

    $ ply get-all warn* from compiler

To set _version_ within the `project` context to be _2.0_

    $ ply set version=2.0 in project

To set a property named _url_ in a context called `environ` to _http://mydomain.com_ (note, the property file will be created if it doesn't exist).

    $ ply set "url=http://mydomain.com" in environ

To delete the property _url_ from the context `environ` (note, if after removal of _url_ there are no properties left the whole `environ.properties` file will be deleted)

    $ ply remove url from environ

Any script can directly read these property files during execution, however, ply does the heavy lifting by resolving the
properties and passing them as environmental variables to each invoked script.  The resolution of properties files
involves collecting all properties for all contexts (for the given scope; again see [Scopes](Scopes.md) for a general description
of what scopes are) and overriding properties appropriately.

Ply passes the properties in the following format: `ply$context.propName=propValue`.  That is the environment variable name
is `ply$context.propName` and the value is `propValue`.  The first four characters `ply$` are just to disambiguate ply
properties from other environment variables.  The next part is the context.  The context is the property file name (without
the scope), so in the last example above the context is `environ`.  The `propName` and `propValue` are self-explanatory.
If a property within a context is defined in the system defaults but overridden by the local project, only the local
project's overridden property is passed.
For example; the `$PLY_HOME/config/compiler.properties` file contains a property named _warnings_ whose value is _true_.
If the local project overrides this to false

    $ ply set warnings=false in compiler

then only the local override is passed to scripts (i.e., scripts will see one environment variable named `ply$compiler.warnings` with
value `false`).
