package net.ocheyedan.ply.script.github;

/**
 * User: blangel
 * Date: 1/10/13
 * Time: 10:01 AM
 *
 * From calling {@literal https://api.github.com/authorizations}
 * {
 *   "created_at": "2012-04-26T16:32:16Z",
 *   "app": {
 *     "url": "http://dillinger.io",
 *     "name": "Dillinger"
 *   },
 *   "note": null,
 *   "token": "xxxxxx",
 *   "url": "https://api.github.com/authorizations/323907",
 *   "updated_at": "2012-04-26T16:32:16Z",
 *   "scopes": [
 *     "repo"
 *   ],
 *   "note_url": null,
 *   "id": 323907
 * }
 */
public class Authorization {

    private final String note;

    private final String token;

    private final App app;

    private final String url;

    private final String[] scopes;

    private final String id;

    private Authorization() {
        this(null, null, null, null, null, null);
    }

    public Authorization(String note, String token, App app, String url, String[] scopes, String id) {
        this.note = note;
        this.token = token;
        this.app = app;
        this.url = url;
        this.scopes = scopes;
        this.id = id;
    }

    public String getNote() {
        return note;
    }

    public String getToken() {
        return token;
    }

    public App getApp() {
        return app;
    }

    public String getUrl() {
        return url;
    }

    public String[] getScopes() {
        return scopes;
    }

    public String getId() {
        return id;
    }
}
