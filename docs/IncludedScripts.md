Included Scripts
----------------

Ply ships with the following scripts (for detailed information about each, check the corresponding wiki page):

__ply-clean-1.0.jar__ - deletes directory `project.build.dir` and all its subdirectories

__ply-file-changed-1.0.jar__ - determines which files within `project.src.dir` have changed since last invocation (using timestamp and SHA1 hash).

__ply-compiler-1.0.jar__ - compiles files within `project.src.dir` which have changed (determines change by using the `ply-file-changed-1.0.jar` output) and places the compiled output in `compiler.build.path`

__ply-dependency-manager-1.0.jar__ - resolves dependency-atoms from the `dependencies` context property file.

__ply-resources-1.0.jar__ - copies files from `project.res.dir` to `project.res.build.dir`

__ply-filter-file-1.0.jar__ - filters files from `project.filter.dir` will all available properties

__ply-package-1.0.jar__ - packages compiled code and resources into an archive (jar/zip/war/etc).

__ply-repo-install-1.0.jar__ - copies the packaged code/resources into the `depmng.localRepo`

__ply-test-junit-1.0.jar__ - runs all junit tests