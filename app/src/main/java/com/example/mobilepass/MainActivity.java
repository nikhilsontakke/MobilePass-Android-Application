package com.example.mobilepass;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import java.util.concurrent.Executor;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    TextView empNameView, empPositionView, empDepartmentView, empIdView;
    HashMap<String, String> empDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        empNameView = findViewById(R.id.empName);
        empPositionView = findViewById(R.id.empPosition);
        empDepartmentView = findViewById(R.id.empDepartment);
        empIdView = findViewById(R.id.empId);

        Intent intent = getIntent();
        empDetails = (HashMap<String, String>) intent.getSerializableExtra("employeeDetails");
        empNameView.setText(empDetails.get("name"));
        empPositionView.setText(empDetails.get("position"));
        empDepartmentView.setText(empDetails.get("department"));
//        empIdView.setText("Employee ID - "+ empDetails.get("id"));

        //Initialise NfcAdapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "Your Phone has no NFC Capabilities", Toast.LENGTH_SHORT).show();
            finish();
        }
        //Create a PendingIntent object so the Android system can populate it with the details of the tag when it is scanned.
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        assert nfcAdapter != null;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    protected void onPause() {
        super.onPause();
        //Onpause stop listening
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String NfcCardUid = ReadNFCTagIntent(intent);
        Executor executor = ContextCompat.getMainExecutor(this);
        final BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Error While Using Biometric Auth Please Contact Admin", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
//                Toast.makeText(getApplicationContext(), "Access Granted - Card UID = "+NfcCardUid, Toast.LENGTH_SHORT).show();
                String BASE_URL = "http://192.168.1.96:8080/";
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                ApiService apiService = retrofit.create(ApiService.class);
                Call<ResponseBody> call = apiService.validateAccess(NfcCardUid,empDetails.get("username"));
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            ResponseBody responseBody = response.body();
                            Toast.makeText(MainActivity.this, "Access Granted", Toast.LENGTH_SHORT).show();
                        } else {
                            // Handle error response
                            Toast.makeText(MainActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        // Handle network failure
                        Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Access Denied - Your Biometric Details Does not Match", Toast.LENGTH_SHORT).show();
            }
        });

        //Biometric Dialog Box Configuration
        final BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Mobile Pass Identity Authentication")
                .setDescription("Use Biometrics for Authentication").setNegativeButtonText("Cancel").build();

        biometricPrompt.authenticate(promptInfo);
    }

    private String ReadNFCTagIntent(Intent intent) {
        String nfcCardUid = "";
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            assert tag != null;
            nfcCardUid = detectTagData(tag);
        }
        return nfcCardUid;
    }
    private String detectTagData(Tag tag) {
        byte[] id = tag.getId();
        String nfcCardUid = toReversedHex(id);
        Log.v("NFC UID : ",nfcCardUid);
        return nfcCardUid;
    }
    private String toReversedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0) {
                sb.append("");
            }
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }


}
