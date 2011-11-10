Changing Log Levels
-------------------

The logging output in _ply_ is segregated into four levels; `error`, `warn`, `info`, `debug`.  By default _ply_ prints only `error` and `warn` levels.  You can change this for your project by:

     $ ply config --ply set log.levels X

where `X` is the log levels desired.  For instance, to change your log levels to all:

     $ ply config --ply set log.levels warn,info,debug

Note, you cannot supress `error` logs, which is why the above example didn't explicitly need to set `error`.  

To simply append to the existing log levels:

     $ ply config --ply append log.levels info

Which, given the default log levels of `error` and `warn`, will add `info`.

Log levels can also be changed just for a single execution using the ad-hoc properties of _ply_.  Ad-hoc properties are those specified on the command line with the `-P` directive.  For instance to have `error`, `warn` and `info` information printed while running `test`:

     $ ply test -Pply.log.levels=warn,info

Now the next execution will revert to the existing defaults of just `error` and `warn`.