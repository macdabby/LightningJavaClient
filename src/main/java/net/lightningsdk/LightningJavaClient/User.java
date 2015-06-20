package net.lightningsdk.LightningJavaClient;

import java.util.HashMap;

public class User {
    protected HashMap<String, Object> data;

    public User() {
        this.load();
    }

    /**
     * Simple login, returns a new user object without prior sinitialization.
     *
     * @param email
     * @param password
     * @return
     */
    public static User logIn (String email, String password) {
        User user = new User();
        if (user.isLoggedIn()) {
            user.logOut();
        }
        if (user._logIn(email, password)) {
            return user;
        } else {
            return null;
        }
    }

    /**
     * Log in the current user object.
     *
     * @param email
     * @param password
     * @return
     */
    protected Boolean _logIn  (String email, String password) {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("action", "login");
        parameters.put("email", email);
        parameters.put("password", password);

        // Log in with the client.
        HashMap response = Lightning.POST("/api/user", parameters);
        this.data = new HashMap<String, Object>();

        // Check the status.
        String status = (String)response.get("status");
        Boolean success = false;
        if (status != null && status.equals("success")) {
            // Save the settings.
            success = true;
            this.data = response;
            this.setCookieWithResponse(response);
        }

        return success;
    }

    public static User registerWithEmail(String email, String password) {
        User user = new User();
        user.registerWithEmail(email, password, new HashMap());
        return user;
    }

    public void registerWithEmail(String email, String password, HashMap<String, Object> parameters) {
        // Log in with the client.
        parameters.put("action", "register");
        parameters.put("email", email);
        parameters.put("password", password);
        HashMap<String, Object> response = Lightning.POST("/api/user", parameters);
        this.data = new HashMap<String, Object>();

        // Check the status.
        String status = (String)response.get("status");
        Boolean success = false;
        if (status != null && status.equals("success")) {
            // Save the settings.
            success = true;
            this.data = response;
            this.setCookieWithResponse(response);
        }
    }

    public void setCookieWithResponse(HashMap<String, Object>response) {
        if (response != null) {
            HashMap cookies = (HashMap)response.get("cookies");
            if (cookies != null) {
                String sessionKey = (String)cookies.get("session");
                Lightning.setSessionKey(sessionKey);
            }
        }
    }

    public void logOut() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("action", "logout");
        Lightning.POST("/api/user", parameters);
    }

    public Boolean isLoggedIn() {
        return true;
    }

    public void load() {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        self.data = [defaults objectForKey:@"Lightning.User"];
        if (!self.data) {
            self.data = [[NSMutableDictionary alloc] init];
        }
    }

    public void save() {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        [defaults setObject:self.data forKey:@"Lightning.User"];
        [defaults synchronize];
    }
}
