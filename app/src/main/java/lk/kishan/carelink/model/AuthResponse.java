package lk.kishan.carelink.model;

public class AuthResponse {
    private String token;
    private Long customerId;
    private String email;

    public String getToken() { return token; }
    public Long getCustomerId() { return customerId; }
    public String getEmail() { return email; }
}