package com.example.mobilepass;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    String BASE_URL = "http://192.168.1.96:8080/";
    @GET("/accesscontrol") // Replace with your API endpoint
    Call<ResponseBody> validateAccess(@Query("doorId") String doorId, @Query("username") String username);

    @POST("/login/employee")
    Call<ResponseBody> employeelogin(@Body Object requestBody);
}