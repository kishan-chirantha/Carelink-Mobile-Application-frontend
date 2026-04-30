package lk.kishan.carelink.model;

import com.google.gson.annotations.SerializedName;

public class Pharmacy {

    @SerializedName("id")
    private Long id;

    @SerializedName("pharmacyName")
    private String pharmacyName;

    @SerializedName("contactNumber")
    private String contactNumber;

    @SerializedName("addressLine1")
    private String addressLine1;

    @SerializedName("addressLine2")
    private String addressLine2;

    @SerializedName("city")
    private String city;

    @SerializedName("district")
    private String district;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;


    public Long getId() {
        return id;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getCity() {
        return city;
    }

    public String getFullAddress() {
        String fullAddress = addressLine1;
        if (addressLine2 != null && !addressLine2.isEmpty()) {
            fullAddress += ", " + addressLine2;
        }
        if (city != null && !city.isEmpty()) {
            fullAddress += ", " + city;
        }
        return fullAddress;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}