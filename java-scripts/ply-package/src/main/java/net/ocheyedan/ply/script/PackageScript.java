package net.ocheyedan.ply.script;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.props.Props;

import java.io.IOException;

/**
 * User: blangel
 * Date: 10/30/11
 * Time: 10:42 AM
 *
 * This script packages the project.  It is packaged according to the value of {@literal project[.scope].packaging}.
 * The supported values are:
 * 'zip' => {@link ZipPackageScript}
 * 'jar' => {@link JarPackageScript}
 * 'war' => {@link WarPackageScript}
 */
public class PackageScript {

    public static void main(String[] args) {
        String packaging = Props.getValue("project", "packaging");
        PackagingScript packagingScript;
        if ("zip".equals(packaging)) {
            packagingScript = new ZipPackageScript();
        } else if ("jar".equals(packaging)) {
            packagingScript = new JarPackageScript();
        } else if ("war".equals(packaging)) {
            packagingScript = new WarPackageScript();
        } else {
            Output.print("Packaging type ^b^%s^r^ not supported.");
            System.exit(1); return;
        }
        try {
            packagingScript.invoke();
        } catch (IOException ioe) {
            Output.print(ioe);
        } catch (InterruptedException ie) {
            Output.print(ie);
        }
    }

}
