Changing Log Levels
-------------------

The logging output in _ply_ is segregated into four levels; `error`, `warn`, `info`, `debug`.  By default _ply_ prints only `error` and `warn` levels.  You can change this for your project by:

     $ ply set log.levels=X in ply

where `X` is the log levels desired.  For instance, to change your log levels to all:

     $ ply set log.levels=warn,info,debug in ply

Note, you cannot supress `error` logs, which is why the above example didn't explicitly need to set `error`.  

To simply append to the existing log levels:

     $ ply append info to log.levels in ply

Which, given the default log levels of `error` and `warn`, will add `info`.

Log levels can also be changed just for a single execution using the ad-hoc properties of _ply_.  Ad-hoc properties are those specified on the command line with the `-P` directive.  For instance to have `error`, `warn` and `info` information printed while running `test`:

     $ ply test -Pply.log.levels=warn,info

Since we used the ad-hoc directive, the next execution will revert to the existing log levels of just `error` and `warn`.