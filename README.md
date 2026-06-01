# 📱 CareLink - Android Client (Frontend)

> The native Android application for CareLink, a modern M-commerce pharmacy delivery platform bridging patients and trusted local pharmacies.

## ✨ App Features
* **Hybrid Purchasing:** Order digital prescriptions and healthcare products seamlessly.
* **Live Order Tracking:** Real-time geospatial logistics utilizing the **Google Maps API**.
* **Real-time Chat:** Built-in patient-pharmacy messaging using **Firebase Realtime Database**.
* **Secure Payments:** Integrated **PayHere SDK** for frictionless digital checkout.
* **Authentication:** Multi-method login (Email/Password & Google Sign-in) via **Firebase Auth**.

## 🛠️ Built With
* **Language/UI:** Java, XML (Native Android)
* **Networking:** Retrofit2
* **Cloud & Maps:** Firebase (Auth, RTDB), Google Maps API
* **Payments:** PayHere SDK

## ⚙️ Getting Started (Android Studio)
1. Clone this repository.
2. Open the project in **Android Studio**.
3. Place your `google-services.json` file in the `app/` directory.
4. Add your API keys to the `local.properties` file:
```properties
   MAPS_API_KEY=your_google_maps_key
   PAYHERE_MERCHANT_ID=your_merchant_id
