Submodules
----------
Ply has a concept of submodules which (like __maven__'s aggregators) is simply a convenient way to allow one sequence of script invocations to be progagated to other subprojects during the same execution.  Submodules are configured by placing the subproject directory name in a property file with context `submodules`.  For instance if you had a project named _mypoject_ which has a sub-directory named _mysubproject_ which was also a ply project one could add
_mysubproject_ as a submodule to _myproject_ by invoking the following from within the _myproject_ directory:

    $ ply config --submodules set mysubproject ""

Then all ply scripts run for _mypoject_ are also run for _mysubproject_; e.g.

    $ ply clean install

Run from _myproject_ will also run `clean install` on _mysubproject_.   

One specifies a project as a submodule by using its directory name (which means that submodules need to be subdirectories of a project).  Also, when adding a submodule to the `submodules.properties` file the property value is ignored and so can be anything; convention dictates that it is "".
