Included Scripts
----------------

Ply ships with the following scripts (for detailed information about each, check the corresponding wiki page):

__ply-clean-1.0.jar__ - deletes directory `project.build.dir` and all its subdirectories

__ply-dependency-manager-1.0.jar__ - resolves dependency-atoms from the `dependencies` context property file.

__ply-file-changed-1.0.jar__ - determines which files within `project.src.dir` have changed since last invocation (using timestamp and SHA1 hash).

__ply-compiler-1.0.jar__ - compiles files within `project.src.dir` which have changed (determines change by using the `ply-file-changed-1.0.jar` output) and places the compiled output in `compiler.build.path`

__ply-resources-1.0.jar__ - copies files from `project.res.dir` to `project.res.build.dir`

__ply-filter-file-1.0.jar__ - filters files from `project.filter.dir` with all available properties

__ply-package-1.0.jar__ - packages compiled code (within `compiler.build.path`) and resources (within `project.res.build.dir`) into an archive (jar/zip/war/etc).

__ply-repo-install-1.0.jar__ - copies the packaged code/resources into the `depmng.localRepo`

__ply-test-junit-1.0.jar__ - runs all junit tests found within the package `project.artifact.name` in directory `project.build.dir`

__ply-exec-1.0.jar__ - executes the current project using the specified main class (via `exec.class` and if that property is not specified looks at the `package.manifest.mainClass`)

__ply-print-classpath-1.0.jar__ - prints the project's classpath (especially useful in conjunction with `-Pply.decorated=false`)

__ply-update-1.0.jar__ - updates ply to the most recent version of itself.

__ply-intellij-1.0.jar__ - creates _Intellij IDEA_ project (.ipr) and module (.iml) files for the project (the files will point to the resolved dependencies and correctly setup the JDK and source and output directories based on ply properties).