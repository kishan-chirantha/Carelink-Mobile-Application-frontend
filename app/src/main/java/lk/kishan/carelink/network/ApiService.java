package lk.kishan.carelink.network;

import java.util.List;
import java.util.Map;

import lk.kishan.carelink.model.AuthRequest;
import lk.kishan.carelink.model.AuthResponse;
import lk.kishan.carelink.model.Cart;
import lk.kishan.carelink.model.Category;
import lk.kishan.carelink.model.Customer;
import lk.kishan.carelink.model.GoogleAuthRequest;
import lk.kishan.carelink.model.Notification;
import lk.kishan.carelink.model.Order;
import lk.kishan.carelink.model.OrderRequest;
import lk.kishan.carelink.model.Pharmacy;
import lk.kishan.carelink.model.Product;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/customers/register")
    Call<ResponseBody> registerCustomer(@Body Customer customer);

    @POST("api/customers/login")
    Call<AuthResponse> loginUser(@Body AuthRequest request);

    @POST("api/customers/google-login")
    Call<lk.kishan.carelink.model.AuthResponse> googleLoginUser(@Body GoogleAuthRequest request);

    @GET("api/customers/{email}")
    Call<Customer> getCustomerByEmail(@Path("email") String email);

    @GET("api/categories")
    Call<List<Category>> getAllCategories();

    @GET("api/products")
    Call<List<Product>> getAllProducts();

    @GET("api/products/category/{categoryId}")
    Call<List<Product>> getProductsByCategory(@Path("categoryId") Long categoryId);

    @GET("api/products/search")
    Call<List<Product>> searchProducts(@Query("keyword") String keyword);

    @Multipart
    @POST("api/cart/add-prescription")
    Call<ResponseBody> addPrescriptionToCart(
            @Part("customerId") RequestBody customerId,
            @Part MultipartBody.Part file
    );

    @POST("api/cart/add-item")
    Call<ResponseBody> addItemToCart(
            @Header("Authorization") String token,
            @Query("customerId") Long customerId,
            @Query("productId") Long productId,
            @Query("quantity") int quantity
    );

    @GET("api/cart/get/{customerId}")
    Call<Cart> getCartDetails(@Path("customerId") Long customerId);

    @DELETE("api/cart/remove-item/{cartItemId}")
    Call<ResponseBody> removeCartItem(@Path("cartItemId") Long cartItemId);

    @GET("api/pharmacies/nearby")
    Call<List<Pharmacy>> getNearbyPharmacies(
            @Query("lat") Double lat,
            @Query("lng") Double lng,
            @Query("radius") Double radius
    );

    @PUT("api/cart/update-quantities")
    Call<ResponseBody> updateCartQuantities(@Body Map<Long, Integer> quantities);

    @POST("api/orders/create")
    Call<Order> placeOrder(@Body OrderRequest orderRequest);

    @GET("api/orders/customer/{customerId}")
    Call<List<Order>> getCustomerOrders(@Path("customerId") Long customerId);

    @PUT("api/orders/{orderId}/cancel")
    Call<ResponseBody> cancelOrder(@Path("orderId") long orderId);

    @PUT("api/customers/{id}/update-phone")
    Call<ResponseBody> updateCustomerPhone(
            @Path("id") Long customerId,
            @Query("phone") String phone
    );

    @PUT("api/orders/{orderId}/confirm")
    Call<ResponseBody> confirmOrder(
            @Path("orderId") long orderId,
            @Query("finalAmount") double finalAmount,
            @Query("paymentStatus") String paymentStatus
    );

    @GET("api/notifications/customer/{customerId}")
    Call<List<Notification>> getNotifications(
            @Header("Authorization") String token,
            @Path("customerId") long customerId
    );
    @PUT("api/customers/{customerId}/fcm-token")
    Call<ResponseBody> updateFcmToken(
            @Header("Authorization") String token,
            @Path("customerId") long customerId,
            @Query("token") String fcmToken
    );

    @Multipart
    @PUT("api/customers/{customerId}/update-profile")
    Call<ResponseBody> updateCustomerProfile(
            @Header("Authorization") String token,
            @Path("customerId") Long customerId,
            @Part("name") RequestBody name,
            @Part("mobile") RequestBody mobile,
            @Part MultipartBody.Part image
    );

    @GET("api/orders/{orderId}/invoice")
    @Headers("Accept: application/pdf")
    Call<ResponseBody> downloadInvoice(@Path("orderId") long orderId);
}