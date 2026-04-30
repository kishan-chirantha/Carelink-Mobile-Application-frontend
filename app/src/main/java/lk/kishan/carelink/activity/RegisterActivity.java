package lk.kishan.carelink.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
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

import lk.kishan.carelink.databinding.ActivityRegisterBinding;
import lk.kishan.carelink.R;
import lk.kishan.carelink.model.AuthResponse;
import lk.kishan.carelink.model.Customer;
import lk.kishan.carelink.model.GoogleAuthRequest;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.customerBtnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        binding.customerSignupToSignin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });

        binding.customerBtnSignUp.setOnClickListener(v -> {
            String name = binding.customerSignupName.getText().toString().trim();
            String email = binding.customerSignupEmail.getText().toString().trim();
            String mobile = binding.customerSignupMobile.getText().toString().trim();
            String password = binding.customerSignupPassword.getText().toString().trim();
            String retypePassword = binding.customerSignupRetypePassword.getText().toString().trim();

            if (name.isEmpty()) {
                binding.customerSignupName.setError("Name is required");
                binding.customerSignupName.requestFocus();
                return;
            }

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.customerSignupEmail.setError("Enter valid email");
                binding.customerSignupEmail.requestFocus();
                return;
            }

            if (mobile.isEmpty() || !Patterns.PHONE.matcher(mobile).matches()) {
                binding.customerSignupMobile.setError("Enter valid Mobile Number");
                binding.customerSignupMobile.requestFocus();
                return;
            }

            if (password.isEmpty() || password.length() < 6) {
                binding.customerSignupPassword.setError("Password must be at least 6 characters");
                binding.customerSignupPassword.requestFocus();
                return;
            }

            if (!retypePassword.equals(password)) {
                binding.customerSignupRetypePassword.setError("Passwords must match");
                binding.customerSignupRetypePassword.requestFocus();
                return;
            }

            Customer newCustomer = new Customer(name, email, mobile, password);
            saveCustomer(newCustomer);
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
                        googleSignUp(loggedEmail, loggedName);
                    } else {
                        Toast.makeText(this, "Firebase Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (ApiException e) {
                Toast.makeText(this, "Google SignUp Failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveCustomer(Customer customer) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.registerCustomer(customer).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Registration Successful! Please Sign In.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RegisterActivity.this, SignInActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Error: User might already exist", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Server Connection Failed!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void googleSignUp(String email, String name) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.googleLoginUser(new GoogleAuthRequest(email, name)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    String token = response.body().getToken();
                    SharedPreferences sharedPreferences = getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
                    sharedPreferences.edit().putString("JWT_TOKEN", token).apply();
                    RetrofitClient.AUTH_TOKEN = token;

                    getAccountDetails(email);

                } else {
                    Toast.makeText(RegisterActivity.this, "Google Sign Up Failed at Backend", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Server Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAccountDetails(String email) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getCustomerByEmail(email).enqueue(new Callback<Customer>() {
            @Override
            public void onResponse(Call<Customer> call, Response<Customer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Customer loggedInCustomer = response.body();

                    SharedPreferences.Editor editor = getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE).edit();
                    editor.putLong("CUSTOMER_ID", loggedInCustomer.getId());
                    editor.putString("CUSTOMER_NAME", loggedInCustomer.getName());
                    editor.putString("CUSTOMER_EMAIL", loggedInCustomer.getEmail());
                    editor.putString("CUSTOMER_MOBILE", loggedInCustomer.getMobile());
                    editor.putBoolean("IS_LOGGED_IN", true);
                    editor.apply();

                    Toast.makeText(RegisterActivity.this, "Welcome " + loggedInCustomer.getName() + "!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Customer> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Connection Failed!", Toast.LENGTH_LONG).show();
            }
        });
    }
}