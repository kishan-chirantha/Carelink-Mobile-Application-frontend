package lk.kishan.carelink.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import lk.kishan.carelink.fragment.SettingFragment;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import java.io.File;

import lk.kishan.carelink.utils.FileUtil;
import lk.kishan.carelink.network.RetrofitClient;
import lk.kishan.carelink.network.ApiService;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import lk.kishan.carelink.R;
import lk.kishan.carelink.databinding.ActivityMainBinding;
import lk.kishan.carelink.fragment.CartFragment;
import lk.kishan.carelink.fragment.HomeFragment;
import lk.kishan.carelink.fragment.OrderFragment;
import lk.kishan.carelink.model.Prescription;
import lk.kishan.carelink.utils.CartManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private BottomNavigationView bottomNavigationView;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;
    private Uri selectedPrescriptionUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigationView.setSelectedItemId(R.id.bottom_nav_home);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setIcon(R.drawable.home_filled);
        }

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.bottom_nav_home) {
                    loadFragment(new HomeFragment());

                    item.setIcon(R.drawable.home_filled);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_cart).setIcon(R.drawable.cart_outline);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_order).setIcon(R.drawable.article_outline);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_settings).setIcon(R.drawable.settings_outline);
                    return true;

                } else if (itemId == R.id.bottom_nav_cart) {
                    loadFragment(new CartFragment());
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_cart).setIcon(R.drawable.cart_filled);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setIcon(R.drawable.home_outline);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_order).setIcon(R.drawable.article_outline);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_settings).setIcon(R.drawable.settings_outline);
                    return true;

                } else if (itemId == R.id.bottom_nav_order) {
                    loadFragment(new OrderFragment());
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_order).setIcon(R.drawable.article_filled);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_cart).setIcon(R.drawable.cart_outline);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setIcon(R.drawable.home_outline);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_settings).setIcon(R.drawable.settings_outline);
                    return true;

                } else if (itemId == R.id.bottom_nav_settings) {
                    loadFragment(new SettingFragment());
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_settings).setIcon(R.drawable.settings_filled);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setIcon(R.drawable.home_outline);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_cart).setIcon(R.drawable.cart_outline);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_order).setIcon(R.drawable.article_filled);
                    return true;
                }
                return false;
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedPrescriptionUri = uri;
                addPrescriptionToCart();
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) {
                addPrescriptionBitmapToCart(bitmap);
            }
        });

        binding.addPrescriptionButton.setOnClickListener(v -> {
            showImageSelectionDialog();

        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

                if (bottomNav.getSelectedItemId() != R.id.bottom_nav_home) {
                    bottomNav.setSelectedItemId(R.id.bottom_nav_home);

                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_container, fragment).commit();
    }

    private void showImageSelectionDialog() {
        String[] options = {"Take a Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Prescription");

        builder.setItems(options, (dialog, option) -> {
            if (option == 0) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 100);
                } else {
                    openCamera();
                }
            } else if (option == 1) {
                openGallery();
            } else {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void openCamera() {
        cameraLauncher.launch(null);
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void addPrescriptionToCart() {
        CartManager.getInstance().addPrescription(new Prescription(selectedPrescriptionUri));
        Toast.makeText(this, "Prescription successfully added to Cart!", Toast.LENGTH_SHORT).show();
        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_cart);

        try {
            File file = FileUtil.getFileFromUri(this, selectedPrescriptionUri);
            uploadToServer(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addPrescriptionBitmapToCart(Bitmap bitmap) {
        CartManager.getInstance().addPrescription(new Prescription(bitmap));
        Toast.makeText(this, "Camera Prescription added to Cart!", android.widget.Toast.LENGTH_SHORT).show();
        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_cart);

        try {
            File file = FileUtil.getFileFromBitmap(this, bitmap);
            uploadToServer(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadToServer(File file) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        SharedPreferences prefs = getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        long customerId = prefs.getLong("CUSTOMER_ID", -1);

        if (customerId == -1) {
            android.widget.Toast.makeText(this, "Please login first!", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        String customerIdStr = String.valueOf(customerId);

        RequestBody customerIdPart = RequestBody.create(okhttp3.MediaType.parse("text/plain"), customerIdStr);
        RequestBody fileBody = RequestBody.create(okhttp3.MediaType.parse("image/*"), file);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), fileBody);

        apiService.addPrescriptionToCart(customerIdPart, filePart).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    android.widget.Toast.makeText(MainActivity.this, "Uploaded!", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(MainActivity.this, "Upload Failed!", android.widget.Toast.LENGTH_SHORT).show();

                    try {
                        android.util.Log.e("UPLOAD_ERROR", "Server Error: " + response.errorBody().string());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@androidx.annotation.NonNull retrofit2.Call<okhttp3.ResponseBody> call, @androidx.annotation.NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

