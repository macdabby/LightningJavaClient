package net.lightningsdk.LightningJavaClient;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import android.content.Context;
import android.content.SharedPreferences;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public class Lightning {
    static URL baseURL;
    static String sessionKey;
    static Boolean debug;
    static Context context;

    private static final String PREFERENCES = "net.lightningsdk";
    private static final String SESSION_KEY = "sessionKey";

    /**
     * Save the user's session key.
     */
    public static void setSessionKey(String newSessionKey) {
        sessionKey = newSessionKey;

        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SESSION_KEY, sessionKey);
        editor.commit();
    }

    public static void configure(Context c, String setBaseUrl) {
        configure(c, setBaseUrl, false);
    }

    public static void configure(Context c, String setBaseUrl, Boolean debug_value) {
        // Save the context reference.
        context = c;

        // Enable SNI for SSL connections:
        System.setProperty("jsse.enableSNIExtension", "true");

        // Set the base URL.
        try {
            baseURL = new URL(setBaseUrl);
        }
        catch (Exception e) {
            // Failed to create URL.
            e.printStackTrace();
        }
        debug = debug_value;

        // Load the user's session key.
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        sessionKey = sharedPref.getString(SESSION_KEY, null);
    }

    public static String JSONToQueryString(JSONObject parameters) {
        try {
            StringBuilder sb = new StringBuilder();
            Iterator<?> keys = parameters.keys();
            String key;
            while (keys.hasNext()) {
                key = (String)keys.next();
                if(sb.length() > 0){
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(key, "UTF-8"))
                        .append('=')
                        .append(URLEncoder.encode(parameters.getString(key), "UTF-8"));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    protected static String getContent(String method, String urlString, JSONObject parameters) {
        try {
            String appender = urlString.contains("?") ? "&" : "?";
            String parameterString = JSONToQueryString(parameters);
            URL fullUrl;
            if (method.equals("POST")) {
                fullUrl = new URL(baseURL, urlString);
            } else {
                fullUrl = new URL(baseURL, urlString + appender + parameterString);
            }
            HttpURLConnection connection = (HttpURLConnection) fullUrl.openConnection();

            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);

            // Set the request method.
            connection.setRequestMethod(method);

            // Add the session cookie.
            connection.setRequestProperty("Cookie", "session=" + sessionKey);
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");

            if (method.equals("POST") && parameterString.length() > 0) {
                connection.setRequestProperty("Content-Length", Integer.toString(parameterString.length()));
                connection.addRequestProperty("Content-Type", "application/" + "POST");
                connection.getOutputStream().write(parameterString.getBytes("UTF8"));
            }

            connection.connect();
            InputStream inputStream = connection.getInputStream();

            // If the output was gzipped, change the input stream to the ungzipped version.
            if ("gzip".equals(connection.getContentEncoding())) {
                inputStream = new GZIPInputStream(inputStream);
            }

            connection.disconnect();

            String output = IOUtils.toString(inputStream, "UTF-8");
            inputStream.close();
            return output;
        } catch (Exception e) {
            // Nothing loaded.
            return null;
        }
    }

    protected static JSONObject send(String method, String urlString, JSONObject parameters) {
        String content = getContent(method, urlString, parameters);
        try {
            JSONObject jsonObject = new JSONObject(content);
            return jsonObject;
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject GET(String url, JSONObject parameters) {
        return send("GET", url, parameters);
    }

    public static JSONObject GET(String url) {
        return send("GET", url, new JSONObject());
    }

    public static JSONObject POST(String url, JSONObject parameters) {
        return send("POST", url, parameters);
    }

    public static JSONObject POST(String url) {
        return send("POST", url, new JSONObject());
    }
}
