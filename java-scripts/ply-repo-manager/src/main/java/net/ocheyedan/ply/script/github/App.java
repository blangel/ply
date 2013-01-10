package net.ocheyedan.ply.script.github;

/**
 * User: blangel
 * Date: 1/10/13
 * Time: 10:01 AM
 *
 * The {@literal app} field from calling {@literal https://api.github.com/authorizations}
 * {
 *   "url": "http://dillinger.io",
 *   "name": "Dillinger"
 * }
 */
public class App {

    private final String url;

    private final String name;

    private App() {
        this(null, null);
    }

    public App(String url, String name) {
        this.url = url;
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }
}
