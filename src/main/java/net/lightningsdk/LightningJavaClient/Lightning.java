package net.lightningsdk.LightningJavaClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

public class Lightning {

    private static volatile Lightning instance;

    public static Lightning getInstance() {
        Lightning localInstance = instance;
        if (localInstance == null) {
            synchronized (Lightning.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new Lightning();
                }
            }
        }
        return localInstance;
    }

    public static final boolean DEBUG = false;

    private URL baseURL;
    private String sessionKey;
    private Boolean debug;
    private Activity activity;

    private final String PREFERENCES = "net.lightningsdk";
    private final String SESSION_KEY = "sessionKey";

    /**
     * Save the user's session key.
     */
    public void setSessionKey(String newSessionKey) {
        sessionKey = newSessionKey;

        SharedPreferences sharedPref = activity.getSharedPreferences(PREFERENCES, activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SESSION_KEY, sessionKey);
        editor.commit();
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void configure(Activity a, String setBaseUrl) {
        configure(a, setBaseUrl, false);
    }

    public void configure(Activity a, String setBaseUrl, Boolean debug_value) {
        // Save the activity reference.
        activity = a;

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
        SharedPreferences sharedPref = activity.getSharedPreferences(PREFERENCES, activity.MODE_PRIVATE);
        sessionKey = sharedPref.getString(SESSION_KEY, null);
    }

    public void setActivity(Activity a) {
        activity = a;
    }

    protected void handleJSONError(JSONObject response) {
        try {
            if (response.has("errors")) {
                JSONArray errors = response.getJSONArray("errors");
                if (errors != null && errors.length() > 0) {
                    // TODO: combine errors.
                    alertError(errors.getString(0));
                    return;
                }
            }
        } catch (Exception e) {
            alertError("There was an error connecting to the server.");
        }
    }

    public void asyncAlert(final String title, final String message) {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
                alertDialog.setTitle(title);
                alertDialog.setMessage(message);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });
    }

    protected void alertError(String errorMessage) {
        asyncAlert("Error", errorMessage);
    }

    /**
     * Convert a complex JSON object into a query string.
     */
    public String JSONToQueryString(JSONObject parameters) {
        try {
            StringBuilder sb = new StringBuilder();
            SubJSONToQueryString(sb, "", parameters, true);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns key/value pairs of query string parameters.
     */
    protected void SubJSONToQueryString(StringBuilder sb, String prefix, Object value, boolean top) {
        try {
            if (value instanceof JSONObject) {
                Iterator<?> keys = ((JSONObject)value).keys();
                String key;
                while (keys.hasNext()) {
                    key = (String)keys.next();
                    SubJSONToQueryString(
                            sb,
                            top ? key : "[" + key + "]",
                            ((JSONObject) value).get(key),
                            false
                    );
                }
            }
            else if (value instanceof JSONArray) {
                int length = ((JSONArray)value).length();
                for (int i = 0; i < length; i++) {
                    SubJSONToQueryString(sb, prefix + "[]", ((JSONArray) value).get(i), false);
                }
            }
            else {
                if(sb.length() > 0){
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(prefix, "UTF-8") + "=" + URLEncoder.encode(value.toString(), "UTF-8"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected HttpURLConnection setupConnection(String method, String urlString, JSONObject parameters) {
        try {
            String appender = urlString.contains("?") ? "&" : "?";
            String parameterString = JSONToQueryString(parameters);
            URL fullUrl;
            if (method.equals("POST")) {
                fullUrl = new URL(baseURL, urlString);
                AppLog.d(fullUrl.toURI().toString() + " " + parameters.toString());
            } else {
                fullUrl = new URL(baseURL, urlString + appender + parameterString);
                AppLog.d(fullUrl.toURI().toString());
            }
            HttpURLConnection connection = (HttpURLConnection) fullUrl.openConnection();

            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);

            // Set the request method.
            connection.setRequestMethod(method);

            // Add the session cookie.
            connection.setRequestProperty("Cookie", "session=" + sessionKey);
            if (!debug) {
                connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            }

            if (method.equals("POST") && parameterString.length() > 0) {
                connection.setRequestProperty("Content-Length", Integer.toString(parameterString.length()));
                connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                connection.getOutputStream().write(parameterString.getBytes("UTF8"));
            }
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected InputStream getInputStream(HttpURLConnection connection) {
        try {
            InputStream inputStream = connection.getInputStream();
            connection.connect();
            // If the output was gzipped, change the input stream to the ungzipped version.
            if ("gzip".equals(connection.getContentEncoding())) {
                inputStream = new GZIPInputStream(inputStream);
            }
            return inputStream;
        } catch (Exception e) {
            return null;
        }
    }

    protected String getContentString(String method, String urlString, JSONObject parameters) {
        HttpURLConnection connection = setupConnection(method, urlString, parameters);
        if (connection == null) {
            return null;
        }
        try {
            InputStream inputStream = getInputStream(connection);
            if (inputStream == null) {
                return null;
            }

            String output = IOUtils.toString(inputStream, "UTF-8");

            AppLog.d("output = " + output);

            inputStream.close();
            connection.disconnect();

            return output;
        } catch (Exception e) {
            // Nothing loaded.
            e.printStackTrace();
            return null;
        }
    }

    protected byte[] getContentBytes(String method, String urlString, JSONObject parameters, String requiredContentTypePrefix) {
        HttpURLConnection connection = setupConnection(method, urlString, parameters);
        if (connection == null) {
            return null;
        }
        try {
            InputStream inputStream = getInputStream(connection);
            if (inputStream == null) {
                return null;
            }

            // Require the content type header.
            if (requiredContentTypePrefix != null && !connection.getContentType().startsWith(requiredContentTypePrefix)) {
                return null;
            }

            byte[] bytes = IOUtils.toByteArray(inputStream);

            AppLog.d("output bytes length = " + bytes.length);

            inputStream.close();
            connection.disconnect();

            return bytes;
        } catch (Exception e) {
            // Nothing loaded.
            e.printStackTrace();
            return null;
        }
    }

    protected JSONObject sendAndReturnObject(String method, String urlString, JSONObject parameters) {
        String content = getContentString(method, urlString, parameters);
        try {
            JSONObject response = new JSONObject(content);
            handleJSONError(response);
            return response;
        } catch (Exception e) {
            alertError("There was an error connecting to the server.");
        }
        return null;
    }

    protected JSONArray sendAndReturnArray(String method, String urlString, JSONObject parameters) {
        // TODO: If there is an error, it will come back as an object.
        String content = getContentString(method, urlString, parameters);
        try {
            return new JSONArray(content);
        } catch (Exception e) {
            try {
                // It's possible the error came back as an object.
                JSONObject response = new JSONObject(content);
                handleJSONError(response);
            } catch (Exception e2) {
                alertError("There was an error connecting to the server.");
            }
            return null;
        }
    }

    protected byte[] sendAndReturnImage(String method, String urlString, JSONObject parameters) {
        byte[] data = getContentBytes(method, urlString, parameters, "image/");
        // If this is json, it could be an error.
        if (data != null && data[0] == '{') {
            try {
                JSONObject response = new JSONObject(data.toString());
                if (response != null) {
                    handleJSONError(response);
                }
            } catch (Exception e) {
                // This could handle under normal circumstances.
                e.printStackTrace();
            }
        }
        return data;
    }

    public JSONObject GET(String url, JSONObject parameters) {
        return sendAndReturnObject("GET", url, parameters);
    }

    public JSONObject GET(String url) {
        return sendAndReturnObject("GET", url, new JSONObject());
    }

    public JSONObject POST(String url, JSONObject parameters) {
        return sendAndReturnObject("POST", url, parameters);
    }

    public JSONObject POST(String url) {
        return sendAndReturnObject("POST", url, new JSONObject());
    }

    public JSONArray GETArray(String url, JSONObject parameters) {
        return sendAndReturnArray("GET", url, parameters);
    }

    public byte[] GETImage(String url, JSONObject parameters) {
        return sendAndReturnImage("GET", url, parameters);
    }
}
