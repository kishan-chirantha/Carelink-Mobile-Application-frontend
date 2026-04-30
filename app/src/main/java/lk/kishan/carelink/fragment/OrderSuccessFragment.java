package lk.kishan.carelink.fragment;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import lk.kishan.carelink.R;
import lk.kishan.carelink.databinding.FragmentOrderSuccessBinding;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderSuccessFragment extends Fragment {

    private FragmentOrderSuccessBinding binding;
    private long orderId = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getLong("ORDER_ID", -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentOrderSuccessBinding.inflate(inflater, container, false);

        binding.btnContinue.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment_container, new HomeFragment())
                    .commit();
        });

        binding.btnDownloadInvoice.setOnClickListener(v -> downloadInvoice());

        return binding.getRoot();
    }

    private void downloadInvoice() {
        if (orderId == -1) {
            Toast.makeText(getContext(), "Order ID not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnDownloadInvoice.setEnabled(false);
        binding.downloadInvoiceBtnTxt.setText("Downloading...");

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.downloadInvoice(orderId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded() || binding == null) return;

                binding.btnDownloadInvoice.setEnabled(true);
                binding.downloadInvoiceBtnTxt.setText("Download Invoice");

                if (response.isSuccessful() && response.body() != null) {
                    saveAndOpenPdf(response.body(), "Invoice_Order_" + orderId + ".pdf");
                } else {
                    Toast.makeText(getContext(), "Failed to download invoice!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.btnDownloadInvoice.setEnabled(true);
                binding.downloadInvoiceBtnTxt.setText("Download Invoice");
                Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAndOpenPdf(ResponseBody body, String fileName) {
        try {
            File pdfFile;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = requireContext().getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (FileOutputStream fos = (FileOutputStream) requireContext()
                            .getContentResolver().openOutputStream(uri);
                         InputStream is = body.byteStream()) {

                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    Toast.makeText(getContext(), "Invoice saved to Downloads!", Toast.LENGTH_SHORT).show();
                    openPdfFromUri(uri);
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                pdfFile = new File(downloadsDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(pdfFile);
                     InputStream is = body.byteStream()) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }
                Toast.makeText(getContext(), "Invoice saved to Downloads!", Toast.LENGTH_SHORT).show();
                Uri fileUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider", pdfFile);
                openPdfFromUri(fileUri);
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error saving invoice: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openPdfFromUri(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "No PDF viewer app found!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
            View addPrescriptionBtn = getActivity().findViewById(R.id.addPrescriptionButton);
            if (addPrescriptionBtn != null) addPrescriptionBtn.setVisibility(View.GONE);
            View fragmentContainer = getActivity().findViewById(R.id.main_fragment_container);
            if (fragmentContainer != null) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
                layoutParams.bottomMargin = 0;
                fragmentContainer.setLayoutParams(layoutParams);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
            View addPrescriptionBtn = getActivity().findViewById(R.id.addPrescriptionButton);
            if (addPrescriptionBtn != null) addPrescriptionBtn.setVisibility(View.VISIBLE);
            View fragmentContainer = getActivity().findViewById(R.id.main_fragment_container);
            if (fragmentContainer != null) {
                int marginInPx = (int) (60 * getResources().getDisplayMetrics().density);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
                layoutParams.bottomMargin = marginInPx;
                fragmentContainer.setLayoutParams(layoutParams);
            }
        }
        binding = null;
    }
}