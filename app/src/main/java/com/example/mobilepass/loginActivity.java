package com.example.mobilepass;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class loginActivity extends AppCompatActivity {

    Button button;
    EditText usernameField;
    EditText passwordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        button = findViewById(R.id.cirLoginButton);
        usernameField = findViewById(R.id.username);
        passwordField = findViewById(R.id.password);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                HashMap<String,String> loginCredentials = new HashMap<>();
                loginCredentials.put("username",usernameField.getText().toString());
                loginCredentials.put("password",passwordField.getText().toString());
                String BASE_URL = "http://192.168.1.96:8080/"; // Replace with your base URL
                Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
                ApiService apiService = retrofit.create(ApiService.class);

                Call<ResponseBody> call = apiService.employeelogin(loginCredentials);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            ResponseBody responseBody = response.body();
                            String responseString = null;
                            try {
                                responseString = responseBody.string();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            Gson gson = new Gson();
                            HashMap<String, Object> employeeDetails = jsonToHashMap(responseString);
                            Toast.makeText(loginActivity.this, "User Logged In Successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(loginActivity.this, MainActivity.class);
                            intent.putExtra("employeeDetails", employeeDetails);
                            startActivity(intent);
                        } else {
                            // Handle error response
                            Toast.makeText(loginActivity.this, "Error:"+ response.code()+" Invalid Login Credentials", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        // Handle network failure
                        Toast.makeText(loginActivity.this, "Error While Authenticating Please Contact Admin" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public HashMap<String, Object> jsonToHashMap(String jsonString) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> map = gson.fromJson(jsonString, type);
        return new HashMap<>(map);
    }
}