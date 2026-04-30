package lk.kishan.carelink.adapter;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.kishan.carelink.R;
import lk.kishan.carelink.fragment.ChatFragment;
import lk.kishan.carelink.fragment.OrderDetailsFragment;
import lk.kishan.carelink.model.Order;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private List<Order> ordersList;

    public OrderAdapter(List<Order> ordersList) {
        this.ordersList = ordersList;
    }

    public void setOrdersList(List<Order> ordersList) {
        this.ordersList = ordersList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.order_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Context context = holder.itemView.getContext();
        Order order = ordersList.get(position);

        holder.orderId.setText(order.getTrackingId());
        holder.customerinfo.setText("For "+order.getCustomer().getName() +" - " +order.getCustomer().getMobile());

        if (order.getPharmacy() != null) {
            String pharmacyDetails = order.getPharmacy().getPharmacyName()
                    + " - " + order.getPharmacy().getFullAddress();
            holder.pharmacyNameAndAddress.setText(pharmacyDetails);
        }

        holder.customerDeliveryAddress.setText(order.getDeliveryAddress());
        holder.orderAmount.setText(String.format("LKR %.2f", order.getTotalAmount()));

        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            holder.paymentIcon.setImageResource(R.drawable.delivery_bike);
        } else {
            holder.paymentIcon.setImageResource(R.drawable.credit_card);
        }

        String status = order.getStatus();
        holder.orderStatus.setText(status);

        if (status != null) {
            status = status.toUpperCase();

            if (status.equals("PENDING") || status.equals("PROCESSING")) {
                holder.statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.yellow)
                );
                holder.orderStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.black)
                );
                holder.statusDot.setBackgroundTintList(
                        ColorStateList.valueOf(
                                ContextCompat.getColor(context, android.R.color.black)
                        )
                );

            } else if (status.equals("REJECTED") || status.equals("CANCELED") || status.equals("CANCELLED")) {
                holder.statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.md_theme_error)
                );
                holder.orderStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.white)
                );
                holder.statusDot.setBackgroundTintList(
                        ColorStateList.valueOf(
                                ContextCompat.getColor(context, android.R.color.white)
                        )
                );

            } else {
                holder.statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.md_theme_primaryContainer)
                );
                holder.orderStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer)
                );
                holder.statusDot.setBackgroundTintList(
                        ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer)
                        )
                );
            }
        }

        try {
            SimpleDateFormat originalFormat =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
            SimpleDateFormat timeFormat =
                    new SimpleDateFormat("hh:mm a", Locale.ENGLISH);

            if (order.getOrderDate() != null) {
                Date date = originalFormat.parse(order.getOrderDate());
                if (date != null) {
                    holder.orderDate.setText(dateFormat.format(date));
                    holder.orderTime.setText(timeFormat.format(date));
                }
            } else {
                holder.orderDate.setText("Unknown Date");
                holder.orderTime.setText("");
            }
        } catch (ParseException e) {
            holder.orderDate.setText("Unknown Date");
            holder.orderTime.setText("");
        }

        holder.btnDetails.setOnClickListener(v -> {
            String orderJson = new Gson().toJson(order);

            Bundle bundle = new Bundle();
            bundle.putString("ORDER_DATA", orderJson);

            OrderDetailsFragment detailsFragment = new OrderDetailsFragment();
            detailsFragment.setArguments(bundle);

            Context context1 = v.getContext();
            while (context1 instanceof android.content.ContextWrapper) {
                if (context1 instanceof androidx.appcompat.app.AppCompatActivity) {
                    break;
                }
                context1 = ((ContextWrapper) context1).getBaseContext();
            }

            if (context1 instanceof androidx.appcompat.app.AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) context1;
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_fragment_container, detailsFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(v.getContext(), "Error opening details!", Toast.LENGTH_SHORT).show();
            }
        });

        holder.callBtn.setOnClickListener(v -> {
            if (order.getPharmacy() != null) {
                String phoneNumber = order.getPharmacy().getContactNumber();

                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
                    intent.setData(android.net.Uri.parse("tel:" + phoneNumber));
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Pharmacy contact number is not available!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Pharmacy details not found!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return ordersList != null ? ordersList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView orderId, orderDate, orderTime, orderStatus;
        TextView pharmacyNameAndAddress, customerDeliveryAddress, orderAmount, customerinfo;
        ImageView paymentIcon;
        View statusDot;
        MaterialCardView statusCard, callBtn;
        MaterialButton btnDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            orderId = itemView.findViewById(R.id.order_id);
            orderDate = itemView.findViewById(R.id.order_date);
            orderTime = itemView.findViewById(R.id.order_time);
            orderStatus = itemView.findViewById(R.id.order_status);
            pharmacyNameAndAddress = itemView.findViewById(R.id.order_pharmacy_name_and_address);
            customerDeliveryAddress = itemView.findViewById(R.id.order_customer_delivery_address);
            customerinfo = itemView.findViewById(R.id.order_item_customer_info);
            orderAmount = itemView.findViewById(R.id.order_amount);
            paymentIcon = itemView.findViewById(R.id.iv_payment_icon);
            statusCard = itemView.findViewById(R.id.status_card);
            statusDot = itemView.findViewById(R.id.status_card_dot);
            btnDetails = itemView.findViewById(R.id.order_btn_details);
            callBtn = itemView.findViewById(R.id.order_item_call_btn);

        }
    }
}