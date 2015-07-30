package net.lightningsdk.LightningJavaClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.spec.ECField;
import java.util.HashMap;

public class User {
    protected JSONObject data;

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
        Boolean success = false;
        try {
            JSONObject parameters = new JSONObject();
            parameters.put("action", "login");
            parameters.put("email", email);
            parameters.put("password", password);

            // Log in with the client.
            JSONObject response = Lightning.getInstance().POST("/api/user", parameters);
            this.data = new JSONObject();

            // Check the status.
            String status;

            status = response.getString("status");
            if (status != null && status.equals("success")) {
                // Save the settings.
                success = true;
                this.data = response;
                this.setCookieWithResponse(response);
            }
        } catch (Exception e) {}

        return success;
    }

    public static User registerWithEmail(String email, String password) {
        User user = new User();
        user.registerWithEmail(email, password, new JSONObject());
        return user;
    }

    public boolean registerWithEmail(String email, String password, JSONObject parameters) {
        Boolean success = false;

        // Log in with the client.
        try {
            parameters.put("action", "register");
            parameters.put("email", email);
            parameters.put("password", password);
            JSONObject response = Lightning.getInstance().POST("/api/user", parameters);
            this.data = new JSONObject();

            // Check the status.
            String status = response.getString("status");
            if (status != null && status.equals("success")) {
                // Save the settings.
                this.data = response;
                this.setCookieWithResponse(response);
                success = true;
            }
        } catch (Exception e) {}

        return success;
    }

    public void setCookieWithResponse(JSONObject response) {
        if (response != null) {
            try {
                JSONObject cookies = response.getJSONObject("cookies");
                if (cookies != null) {
                    String sessionKey = cookies.getString("session");
                    Lightning.getInstance().setSessionKey(sessionKey);
                }
            } catch (Exception e) {}
        }
    }

    public void logOut() {
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("action", "logout");
            Lightning.getInstance().POST("/api/user", parameters);
        } catch (Exception e){}
    }

    public Boolean isLoggedIn() {
        return true;
    }

    public void load() {
//        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
//        self.data = [defaults objectForKey:@"Lightning.User"];
//        if (!self.data) {
//            self.data = [[NSMutableDictionary alloc] init];
//        }
    }

    public void save() {
//        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
//        [defaults setObject:self.data forKey:@"Lightning.User"];
//        [defaults synchronize];
    }
}
