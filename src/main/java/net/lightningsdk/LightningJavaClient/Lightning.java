package net.lightningsdk.LightningJavaClient;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * !!
 *
 * @author Daniel Behrman macdabby@gmail.com, Sergey Mitrofanov goretz.m@gmail.com
 */
public class Lightning {

    private static volatile Lightning instance;

    /*private Lightning(){

    };

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
    }*/

    public static final boolean DEBUG = false;

    private URL baseURL;
    private String sessionKey;
    private Boolean debug;
    private Context mContext;

    private final String PREFERENCES = "net.lightningsdk";
    private final String SESSION_KEY = "sessionKey";

    /**
     * Save the user's session key.
     */
    public void setSessionKey(String newSessionKey) {
        sessionKey = newSessionKey;

        SharedPreferences sharedPref = mContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SESSION_KEY, sessionKey);
        editor.commit();
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void configure(Context context, String setBaseUrl) {
        configure(context, setBaseUrl, false);
    }

    public void configure(Context context, String setBaseUrl, Boolean debug_value) {
        // Save the activity reference.
        mContext = context.getApplicationContext();

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
        SharedPreferences sharedPref = mContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        sessionKey = sharedPref.getString(SESSION_KEY, null);
    }

    protected void handleJSONError(JSONObject response, OnQueryResultListener onQueryResultListener) {
        try {
            if (response.has("errors")) {
                JSONArray errors = response.getJSONArray("errors");
                if (errors != null && errors.length() > 0) {
                    // TODO: combine errors.
                    if (onQueryResultListener != null) onQueryResultListener.onError("Error", errors.getString(0));
                    else AppLog.e(errors.getString(0));
                }
            }
        } catch (Exception e) {
            if (onQueryResultListener != null) onQueryResultListener.onError("Error", "There was an error connecting to the server");
            else AppLog.e("There was an error connecting to the server");
        }
    }

    /**
     * Convert a complex JSON object into a query string.
     */
    public String JSONToQueryString(JSONObject parameters) {
        String res;
        try {
            StringBuilder sb = new StringBuilder();
            SubJSONToQueryString(sb, "", parameters, true);
            res = sb.toString();
        } catch (Exception e) {
            res = null;
        }
        return res;
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
                sb.append(URLEncoder.encode(prefix, "UTF-8")).append("=").append(URLEncoder.encode(value.toString(), "UTF-8"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected HttpURLConnection setupConnection(String method, String urlString, JSONObject parameters) {
        HttpURLConnection connection;
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
            connection = (HttpURLConnection) fullUrl.openConnection();

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
        } catch (Exception e) {
            connection = null;
            e.printStackTrace();
        }
        return connection;
    }

    protected InputStream getInputStream(HttpURLConnection connection) {
        InputStream result;
        try {
            InputStream inputStream = connection.getInputStream();
            connection.connect();
            // If the output was gzipped, change the input stream to the ungzipped version.
            if ("gzip".equals(connection.getContentEncoding())) {
                inputStream = new GZIPInputStream(inputStream);
            }
            result = inputStream;
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        return result;
    }

    protected String getContentString(String method, String urlString, JSONObject parameters) {
        HttpURLConnection connection = setupConnection(method, urlString, parameters);
        String output;
        if (connection == null) {
            return null;
        }
        try {
            InputStream inputStream = getInputStream(connection);
            if (inputStream == null) {
                return null;
            }

            output = IOUtils.toString(inputStream, "UTF-8");

            AppLog.d("output = " + output);

            inputStream.close();
            connection.disconnect();

        } catch (Exception e) {
            // Nothing loaded.
            e.printStackTrace();
            output = null;
        }
        return output;
    }

    protected byte[] getContentBytes(String method, String urlString, JSONObject parameters, String requiredContentTypePrefix) {
        HttpURLConnection connection = setupConnection(method, urlString, parameters);
        byte[] bytes;
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

            bytes = IOUtils.toByteArray(inputStream);

            AppLog.d("output bytes length = " + bytes.length);

            inputStream.close();
            connection.disconnect();
        } catch (Exception e) {
            // Nothing loaded.
            e.printStackTrace();
            bytes = null;
        }
        return bytes;
    }

    protected JSONObject sendAndReturnObject(String method, String urlString, JSONObject parameters, OnQueryResultListener onQueryResultListener) {
        String content = getContentString(method, urlString, parameters);
        JSONObject response;
        try {
            response = new JSONObject(content);
            handleJSONError(response, onQueryResultListener);
        } catch (Exception e) {
            if (onQueryResultListener != null) onQueryResultListener.onError("Error", "There was an error connecting to the server");
            else AppLog.e("There was an error connecting to the server");
            response = null;
        }
        return response;
    }

    protected JSONArray sendAndReturnArray(String method, String urlString, JSONObject parameters, OnQueryResultListener onQueryResultListener) {
        // TODO: If there is an error, it will come back as an object.
        String content = getContentString(method, urlString, parameters);
        JSONArray result;
        try {
            result = new JSONArray(content);
        } catch (Exception e) {
            try {
                // It's possible the error came back as an object.
                JSONObject response = new JSONObject(content);
                handleJSONError(response, onQueryResultListener);
            } catch (Exception e2) {
                e2.printStackTrace();
                if (onQueryResultListener != null) onQueryResultListener.onError("Error","There was an error connecting to the server");
                else AppLog.e("There was an error connecting to the server");
            }
            result = null;
        }
        return result;
    }

    protected byte[] sendAndReturnImage(String method, String urlString, JSONObject parameters, OnQueryResultListener onQueryResultListener) {
        byte[] data = getContentBytes(method, urlString, parameters, "image/");
        // If this is json, it could be an error.
        if (data != null && data[0] == '{') {
            try {
                JSONObject response = new JSONObject(data.toString());
                handleJSONError(response, onQueryResultListener);
            } catch (Exception e) {
                // This could handle under normal circumstances.
                e.printStackTrace();
                if (onQueryResultListener != null) onQueryResultListener.onError("Error", "Error");
                else AppLog.e("Error");
            }
        }
        return data;
    }

    public JSONObject GET(String url, JSONObject parameters) {
        return sendAndReturnObject("GET", url, parameters, null);
    }

    public JSONObject GET(String url, JSONObject parameters, OnQueryResultListener onQueryResultListener) {
        return sendAndReturnObject("GET", url, parameters, onQueryResultListener);
    }

    public JSONObject GET(String url) {
        return sendAndReturnObject("GET", url, new JSONObject(), null);
    }

    public JSONObject GET(String url, OnQueryResultListener onQueryResultListener) {
        return sendAndReturnObject("GET", url, new JSONObject(), onQueryResultListener);
    }

    public JSONObject POST(String url, JSONObject parameters) {
        return sendAndReturnObject("POST", url, parameters, null);
    }

    public JSONObject POST(String url, JSONObject parameters, OnQueryResultListener onQueryResultListener) {
        return sendAndReturnObject("POST", url, parameters, onQueryResultListener);
    }

    public JSONObject POST(String url) {
        return sendAndReturnObject("POST", url, new JSONObject(), null);
    }

    public JSONObject POST(String url, OnQueryResultListener onQueryResultListener) {
        return sendAndReturnObject("POST", url, new JSONObject(), onQueryResultListener);
    }

    public JSONArray GETArray(String url, JSONObject parameters) {
        return sendAndReturnArray("GET", url, parameters, null);
    }

    public JSONArray GETArray(String url, JSONObject parameters, OnQueryResultListener onQueryResultListener) {
        return sendAndReturnArray("GET", url, parameters, onQueryResultListener);
    }

    public byte[] GETImage(String url, JSONObject parameters) {
        return sendAndReturnImage("GET", url, parameters, null);
    }

    public byte[] GETImage(String url, JSONObject parameters, OnQueryResultListener onQueryResultListener) {
        return sendAndReturnImage("GET", url, parameters, onQueryResultListener);
    }

    public interface OnQueryResultListener{
        void onError(final String title, final String message);
    }

    private LightningMethods mMethods;
    private Lightning(String baseUrl){//Context context){
        //mContext = context.getApplicationContext();
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(baseUrl)//mContext.getString(R.string.base_url))
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setRequestInterceptor(requestInterceptor)
                .build();
        mMethods = restAdapter.create(LightningMethods.class);
    }

    public static Lightning getInstance(String baseUrl) {
        Lightning localInstance = instance;
        if (localInstance == null) {
            synchronized (Lightning.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new Lightning(baseUrl);
                }
            }
        }
        return localInstance;
    }

    public static Lightning getInstance() {
        return instance;
    }

    RequestInterceptor requestInterceptor = new RequestInterceptor() {
        @Override
        public void intercept(RequestFacade request) {
            request.addHeader("Cookie", "session=" + sessionKey);
        }
    };

    public LightningMethods getMethods() {
        return mMethods;
    }

    public Builder prepareRequest(String apiMethod){
        return new Builder()
                .putPath(apiMethod);
    }

    public class Builder implements Serializable {
        Map<String, String> params;
        String path;

        public Builder() {
            params = new HashMap<>();
        }

        public Builder putPath(String path){
            this.path = path;
            return this;
        }

        public Builder putParam(String paramName, String paramValue){
            params.put(paramName, paramValue);
            return this;
        }
        public Builder putParam(String paramName, int paramValue){
            params.put(paramName, String.valueOf(paramValue));
            return this;
        }
        public Builder putAll(Map<String, String> params){
            this.params.putAll(params);
            return this;
        }
        public void getString(final LightningListener<String> listener)
        {
            instance.getMethods().get(path, params, new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    listener.loadComplete(StreamToString.getString(response));
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    listener.loadError(retrofitError.getMessage());
                }
            });
        }

        public <T> void get(final LightningListener<T> listener){
            Type type = listener.getClass().getGenericInterfaces()[0];
            final Class cls = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
            instance.getMethods().get(path, params, new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    Gson gson = new Gson();
                    String json = StreamToString.getString(response);
                    try {
                        Object t = gson.fromJson(json, cls);
                        listener.loadComplete((T) t);
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                        listener.loadError(ex.getMessage());
                    }
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    listener.networkError(retrofitError.getMessage());
                }
            });
        }

        public  void get(final JsonResult listener){
            instance.getMethods().get(path, params, new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    String json = StreamToString.getString(response);
                    listener.loadComplete(json);
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    listener.loadError(retrofitError.getMessage());
                }
            });
        }

        public  void post(final JsonResult listener){
            instance.getMethods().post(path, params, new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    String json = StreamToString.getString(response);
                    listener.loadComplete(json);
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    listener.loadError(retrofitError.getMessage());
                }
            });
        }

        /*public void get(final ErrorListener errorListener){
            getInstance().getMethods().get(path, params, new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    errorListener.loadComplete();
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    errorListener.loadError(retrofitError.getMessage());
                }
            });
        }

        public void get(){
            getInstance().getMethods().get(path, params, new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                }
            });
        }*/
    }

    public interface LightningListener<T> extends Serializable{
        void loadComplete(T t);
        void loadError(String message);
        void networkError(String message);
    }

    /*public interface ErrorListener{
        void loadComplete();
        void loadError(String message);
    }*/

    public interface JsonResult{
        void loadComplete(String json);
        void loadError(String message);
    }
}
