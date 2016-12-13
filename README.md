# ExtendedAndroidSDKSample

Kontakt.io Android SDK in advanced use. This application was developed in Android Studio.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

1. A [Kontakt.io Panel](https://panel.kontakt.io/signin) account
2. Android Studio
3. Android OS 4.4 or later emulator or physical device

### Installing

#### Option 1

Download the whole archive and open it as Android Studio project.

#### Option 2

You can also only copy the code from the java files but than remember to add in *build.gradle* for the app

```
dependencies {
    compile 'com.kontaktio:sdk:3.2.0'
}
```
also in *AndroidManifest.xml* you will need to add permissions listed below if you still did not do it

```
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

and also enable our Proximity service in the application section

```
<service android:name="com.kontakt.sdk.android.ble.service.ProximityService" android:exported="false"/>
```

## Authors

* **Konrad Bujak** - *Initial work* - [PurpleBooth](https://github.com/PurpleBooth)

See also the list of [contributors](https://github.com/konradkontakt/ExtendedAndroidSDKSample/graphs/contributors) who participated in this project.

## Acknowledgments

* Kontakt.io for developing such good SDK and hardware
* Inspiration
