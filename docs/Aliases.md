Aliases
-------

Aliases in ply are analogous to aliases within bash.  A word substitutes for a series of other aliases and/or scripts.  For instance, by default in ply, `compile` is
an alias which expands to be `file-changed dep ply-compiler-1.0.jar`.  The `file-changed` is itself another alias which expands to be the script `ply-file-changed-1.0.jar` and `dep` is also
another alias which expands to be the script `ply-dependency-manager-1.0.jar`.  The `ply-compiler-1.0.jar` is simply a script.  So, typing 

    $ ply compile

is simply shorthand for typing

    $ ply ply-file-changed-1.0.jar ply-dependency-manager-1.0.jar ply-compiler-1.0.jar

Aliases are defined to be properties from within context `aliases`.  In other words, to define an alias simply add the alias as a property name and value pair within the `aliases.properties` file.  Of course
you can use `ply` itself to do the same thing.  For instance, to add an alias named `example` to be `echo you`:

    $ ply set 'example=`echo you`' in aliases

Note the use of tick marks.  See [Scripts](Scripts.md) for an explanation.

Like any other properties, aliases can be overridden.  For example, one could augment the `compile` alias by appending other aliases/scripts to its definition:

    $ ply append example to compile in aliases

Which would append the script `example` (which is actually an another alias in this case) to the already defined value of `compile`.  This makes the expanded value of `compile` to be:

    $ ply ply-file-changed-1.0.jar ply-dependency-manager-1.0.jar ply-compiler-1.0.jar `echo you`

Alternatively, one could indirectly change `compile` by changing one of the aliases defined within it.  For instance, now that `compile` is defined to include the `example` alias, one could
change the `example` alias and indirectly affect the expanded `compile` alias:

    $ ply prepend '`echo hello`' to example in aliases

Which means that when `compile` is fully resolved it becomes:

    $ ply ply-file-changed-1.0.jar ply-dependency-manager-1.0.jar ply-compiler-1.0.jar `echo hello` `echo you`

Changing an alias need not just be appending or prepending.  One can completely override its value too.  For instance to make `compile` simply an alias for `echo hello` one could:

    $ ply set 'compile=`echo hello`' in aliases

Setting and modifying aliases is in fact a powerful tool in allowing projects to customize their build lifecycles in any way they like.  To see all the default aliases provided by ply:

    $ ply get-all from aliases

or

    $ less $PLY_HOME/config/aliases.properties
