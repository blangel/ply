package net.ocheyedan.ply.jna;

/**
 * User: blangel
 * Date: 11/8/11
 * Time: 7:11 PM
 */
public final class JnaUtil {

    public static enum Os {
        Linux, OSX, BSD, Solaris, OpenVMS, OS2, Windows;

        public boolean isUnix() {
            return ((this == Linux) || (this == OSX) || (this == BSD) || (this == Solaris));
        }
        
    }

    private static final String JNA_CLASS_NAME = "com.sun.jna.Library";

    private static final boolean jnaPresent;

    private static final Os os;

    static {
        ClassLoader loader = JnaUtil.class.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        boolean jnaPresentLocal = false;
        if (loader != null) {
            try {
                loader.loadClass(JNA_CLASS_NAME);
                jnaPresentLocal = true;
            } catch (ClassNotFoundException cnfe) {
                jnaPresentLocal = false;
            }
        }
        jnaPresent = jnaPresentLocal;

        String osName = System.getProperty("os.name");
        osName = (osName == null ? "linux" /* need to default to something */ : osName.toLowerCase());

        if (osName.contains("linux") || osName.contains("hp-ux")) {
            os = Os.Linux;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            os = Os.OSX;
        } else if (osName.contains("bsd")) {
            os = Os.BSD;
        } else if (osName.contains("solaris") || osName.contains("sunos")) {
            os = Os.Solaris;
        } else if (osName.contains("openvms")) {
            os = Os.OpenVMS;
        } else if (osName.contains("os/2")) {
            os = Os.OS2;
        } else if (osName.contains("windows")) {
            os = Os.Windows;
        } else {
            os = Os.Linux; // need to default to something
        }
    }

    public static boolean isJnaPresent() {
        return jnaPresent;
    }
    
    public static Os getOperatingSystem() {
        return os;
    }

    private JnaUtil() { }

}
