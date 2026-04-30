package lk.kishan.carelink.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import lk.kishan.carelink.model.GoogleAuthRequest;
import lk.kishan.carelink.model.AuthResponse;
import lk.kishan.carelink.databinding.ActivitySignInBinding;
import lk.kishan.carelink.model.AuthRequest;
import lk.kishan.carelink.model.Customer;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import lk.kishan.carelink.R;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.customerSigninToSignup.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        binding.customerBtnSignIn.setOnClickListener(v -> {
            String email = binding.customerSigninEmail.getText().toString().trim();
            String password = binding.customerSigninPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            loginBackend(email, password);
        });

        binding.customerBtnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

                firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, task1 -> {
                    if (task1.isSuccessful()) {
                        String loggedEmail = account.getEmail();
                        String loggedName = account.getDisplayName();
                        googleLoginBackend(loggedEmail, loggedName);
                    } else {
                        Toast.makeText(this, "Google Login Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (ApiException e) {
                Toast.makeText(this, "Google Error Code: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loginBackend(String email, String password) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.loginUser(new AuthRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveTokenAndFetchDetails(response.body().getToken(), email);
                } else {
                    Toast.makeText(SignInActivity.this, "Invalid credentials!", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e("SERVER_ERROR", t.getMessage() );
                Toast.makeText(SignInActivity.this, "Server Error1" +t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void googleLoginBackend(String email, String name) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.googleLoginUser(new GoogleAuthRequest(email, name)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveTokenAndFetchDetails(response.body().getToken(), email);
                } else {
                    Toast.makeText(SignInActivity.this, "Backend Auth Failed", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e("GOOGLE_SERVER_ERROR", t.getMessage() );
                Toast.makeText(SignInActivity.this, "Server Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTokenAndFetchDetails(String token, String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("JWT_TOKEN", token).apply();
        RetrofitClient.AUTH_TOKEN = token;

        getAccountDetails(email);
    }

    private void getAccountDetails(String email) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getCustomerByEmail(email).enqueue(new Callback<Customer>() {
            @Override
            public void onResponse(Call<Customer> call, Response<Customer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveUserToSession(response.body());
                    Toast.makeText(SignInActivity.this, "Welcome " + response.body().getName() + "!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignInActivity.this, MainActivity.class));
                    finish();
                }
            }
            @Override
            public void onFailure(Call<Customer> call, Throwable t) {
                Toast.makeText(SignInActivity.this, "Connection Failed!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserToSession(Customer customer) {
        SharedPreferences.Editor editor = getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE).edit();
        editor.putLong("CUSTOMER_ID", customer.getId());
        editor.putString("CUSTOMER_NAME", customer.getName());
        editor.putString("CUSTOMER_EMAIL", customer.getEmail());
        editor.putString("CUSTOMER_MOBILE", customer.getMobile());
        editor.putBoolean("IS_LOGGED_IN", true);
        editor.apply();
    }
}