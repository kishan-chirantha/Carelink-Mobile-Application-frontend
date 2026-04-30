package lk.kishan.carelink.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.adapter.ChatAdapter;
import lk.kishan.carelink.databinding.FragmentChatBinding;
import lk.kishan.carelink.model.Chat;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatAdapter chatAdapter;
    private List<Chat> chatList;
    private DatabaseReference chatDatabaseRef;
    private ValueEventListener chatListener;

    private String orderId;
    private String currentUserId;
    private String pharmacyName = "";

    public ChatFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getString("ORDER_ID", "");
            pharmacyName = getArguments().getString("PHARMACY_NAME", "Pharmacy");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);

        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        long customerId = prefs.getLong("CUSTOMER_ID", -1);
        currentUserId = "Customer_" + customerId;

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList, currentUserId);

        binding.chatPharmacyName.setText(pharmacyName);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        binding.chatMessagesRecyclerView.setLayoutManager(layoutManager);
        binding.chatMessagesRecyclerView.setAdapter(chatAdapter);

        chatDatabaseRef = FirebaseDatabase.getInstance("https://carelink-41b88-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Chats").child("Order_" + orderId);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });

        binding.chatBtnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        binding.chatBtnSend.setOnClickListener(v -> {
            String messageText = binding.chatTypeMessage.getText().toString().trim();
            Toast.makeText(getContext(), "Clicked "+ messageText, Toast.LENGTH_SHORT).show();
            if (!TextUtils.isEmpty(messageText)) {
                sendMessageToFirebase(messageText);
            }
        });

        loadMessagesFromFirebase();

        return binding.getRoot();
    }

    private void sendMessageToFirebase(String messageText) {
        String messageId = chatDatabaseRef.push().getKey();
        long timestamp = System.currentTimeMillis();

        Log.d("CHAT_DEBUG", "Order ID: " + orderId);
        Log.d("CHAT_DEBUG", "Message ID: " + messageId);

        Chat chatMessage = new Chat(messageId, currentUserId, messageText, timestamp);

        if (messageId != null) {
            chatDatabaseRef.child(messageId).setValue(chatMessage)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("CHAT_DEBUG", "Message Saved!");
                        binding.chatTypeMessage.setText("");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CHAT_DEBUG", "Error: " + e.getMessage());
                        Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadMessagesFromFirebase() {
        chatListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Chat message = dataSnapshot.getValue(Chat.class);
                    if (message != null) {
                        chatList.add(message);
                    }
                }

                chatAdapter.updateMessages(chatList);

                if (chatList.size() > 0) {
                    binding.chatMessagesRecyclerView.scrollToPosition(chatList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load chats", Toast.LENGTH_SHORT).show();
            }
        };

        chatDatabaseRef.addValueEventListener(chatListener);
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
        if (chatDatabaseRef != null && chatListener != null) {
            chatDatabaseRef.removeEventListener(chatListener);
        }

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