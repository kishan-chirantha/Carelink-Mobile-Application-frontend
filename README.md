# 📱 CareLink - Android Client (Frontend)

> The native Android application for CareLink, a modern M-commerce pharmacy delivery platform connecting patients with trusted local pharmacies.

## ✨ App Features

* **Hybrid Purchasing:** Order prescription medications and healthcare products through a single platform.
* **Live Order Tracking:** Real-time delivery tracking powered by the **Google Maps API**.
* **Real-time Chat:** Built-in patient-pharmacy communication using **Firebase Realtime Database**.
* **Secure Payments:** Integrated **PayHere SDK** for seamless digital transactions.
* **Authentication:** Multi-method login including Email/Password and Google Sign-in via **Firebase Authentication**.
* **Responsive Mobile UI:** Native Android user experience built with Java and XML.

## 🛠️ Built With

* **Language/UI:** Java, XML (Native Android)
* **Networking:** Retrofit2
* **Cloud Services:** Firebase Authentication, Firebase Realtime Database
* **Maps & Location:** Google Maps API
* **Payments:** PayHere SDK
* **IDE:** Android Studio

## ⚙️ Getting Started (Android Studio)

### 1. Clone this repository

```bash
git clone https://github.com/your-username/carelink-android.git
cd carelink-android
```

### 2. Open the project

Open the project using **Android Studio**.

### 3. Configure Firebase

Place your Firebase configuration file inside:

```text
app/google-services.json
```

### 4. Configure API Keys

Add the required API keys to your `local.properties` file:

```properties
MAPS_API_KEY=your_google_maps_key
PAYHERE_MERCHANT_ID=your_merchant_id
```

### 5. Build and Run

* Sync Gradle dependencies.
* Connect an Android device or start an emulator.
* Run the application from Android Studio.

## 📌 Requirements

* Android Studio
* Firebase Project Configuration
* Google Maps API Key
* PayHere Merchant Account
* Active CareLink Backend API Instance

## 🔗 Related Repository

This Android application communicates with the CareLink Backend REST API. Ensure the backend server is running before using the application.

## 📄 License

This project was developed for educational and portfolio purposes.
