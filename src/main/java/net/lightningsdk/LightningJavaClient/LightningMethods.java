package net.lightningsdk.LightningJavaClient;

import java.util.Map;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.QueryMap;

/**
 * Created by Сергей on 28.08.2015.
 */
public interface LightningMethods {

    @FormUrlEncoded
    @POST("/{path}")
    void post(@Path("path") String path, @FieldMap Map<String, String> parameters, Callback<Response> callback);

    @GET("/{path}")
    void get(@Path("path") String path, @QueryMap Map<String, String> parameters, Callback<Response> callback);

    /* These are alternative implementation */
    /*@GET("/mindpt/image")
    void getImage(@QueryMap Map<String, String> parameters, Callback<Response> callback);

    @GET("/api/mindpt/image")
    void getApiImage(@QueryMap Map<String, String> parameters, Callback<Response> callback);

    @FormUrlEncoded
    @POST("/api/mindpt/image")
    void postApiImage(@FieldMap Map<String, String> parameters, Callback<Response> callback);

    @FormUrlEncoded
    @POST("/api/mindpt/slides")
    void postApiSlides(@FieldMap Map<String, String> parameters, Callback<Response> callback);

    @FormUrlEncoded
    @POST("/api/mindpt/purchase/android")
    void postPurchase(@FieldMap Map<String, String> parameters, Callback<Response> callback);

    @GET("/api/mindpt/permission")
    void getPermissions(@QueryMap Map<String, String> parameters, Callback<Response> callback);

    @FormUrlEncoded
    @POST("/api/mindpt/coupon")
    void postCoupon(@FieldMap Map<String, String> parameters, Callback<Response> callback);

    @FormUrlEncoded
    @POST("/api/user")
    void postUser(@FieldMap Map<String, String> parameters, Callback<Response> callback);

    @GET("/api/mindpt/tags")
    void getTags(@QueryMap Map<String, String> parameters, Callback<Response> callback);

    @FormUrlEncoded
    @POST("/api/mindpt/report")
    void postReport(@FieldMap Map<String, String> parameters, Callback<Response> callback);

    @GET("/api/mindpt/sessions")
    void getSessionsList(@QueryMap Map<String, String> parameters, Callback<Response> callback);

    @GET("/api/mindpt/session")
    void getSession(@QueryMap Map<String, String> parameters, Callback<Response> callback);

    @GET("/api/mindpt/session/image")
    void getSessionImage(@QueryMap Map<String, String> parameters, Callback<Response> callback);

    @FormUrlEncoded
    @POST("/api/mindpt/session")
    void postSession(@FieldMap Map<String, String> parameters, Callback<Response> callback);

    @GET("/mindpt/customize")
    void getCustomized(@QueryMap Map<String, String> parameters, Callback<Response> callback);

    @FormUrlEncoded
    @POST("/mindpt/customize")
    void postCustomized(@FieldMap Map<String, String> parameters, Callback<Response> callback);

    @GET("/api/mindpt/promo_products")
    void getPromo(@QueryMap Map<String, String> parameters, Callback<Response> callback);*/
}
