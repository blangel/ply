package net.ocheyedan.ply.script;

import net.ocheyedan.ply.Output;

import java.io.IOException;

/**
 * User: blangel
 * Date: 10/30/11
 * Time: 10:42 AM
 *
 * This script packages the project.  It is packaged according to the value of {@literal project[.scope].packaging}.
 * The supported values are:
 * 'jar' => {@link JarPackageScript}
 * 'war' => {@link WarPackageScript}
 * 'zip' => {@link ZipPackageScript}
 */
public class PackageScript {

    public static void main(String[] args) {
        JarPackageScript jarPackageScript = new JarPackageScript();
        try {
            jarPackageScript.invoke();
        } catch (IOException ioe) {
            Output.print(ioe);
        } catch (InterruptedException ie) {
            Output.print(ie);
        }
    }

}
