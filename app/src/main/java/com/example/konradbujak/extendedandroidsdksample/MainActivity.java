package com.example.konradbujak.extendedandroidsdksample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.kontakt.sdk.android.ble.configuration.ActivityCheckConfiguration;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.configuration.scan.ScanMode;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.device.BeaconRegion;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.ScanStatusListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleScanStatusListener;
import com.kontakt.sdk.android.ble.rssi.LimitedMeanRssiCalculator;
import com.kontakt.sdk.android.ble.rssi.RssiCalculators;
import com.kontakt.sdk.android.cloud.CloudConstants;
import com.kontakt.sdk.android.cloud.IKontaktCloud;
import com.kontakt.sdk.android.cloud.KontaktCloud;
import com.kontakt.sdk.android.cloud.api.FirmwaresApi;
import com.kontakt.sdk.android.cloud.exception.KontaktCloudException;
import com.kontakt.sdk.android.cloud.response.CloudCallback;
import com.kontakt.sdk.android.cloud.response.CloudError;
import com.kontakt.sdk.android.cloud.response.CloudHeaders;
import com.kontakt.sdk.android.cloud.response.paginated.Actions;
import com.kontakt.sdk.android.cloud.response.paginated.Devices;
import com.kontakt.sdk.android.cloud.response.paginated.SecureCommands;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.Proximity;
import com.kontakt.sdk.android.common.model.Access;
import com.kontakt.sdk.android.common.model.Action;
import com.kontakt.sdk.android.common.model.Device;
import com.kontakt.sdk.android.common.model.SecureCommandType;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {


    private ProximityManager KontaktManager;
    String TAG = "MyActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onetimeconfiguration();
    }
    private void apiendpoints() {
        IKontaktCloud kontaktCloud = KontaktCloud.newInstance();
        //Fetching all devices from API
/*        kontaktCloud.devices().fetch()
                .maxResult(10) //default is 50
                .startIndex(0)
                .access(Access.OWNER)
                .execute(new CloudCallback<Devices>() {
                    @Override public void onSuccess(Devices response, CloudHeaders headers) {
                        List<Device> content = response.getContent();
                    }
                    @Override public void onError(CloudError error) {
                        showToast("Connection error");
                    }
                });*/
        //Browser content ( old iOS app)
/*        kontaktCloud.actions().createBrowserAction()
                .forDevices("deviceUID")
                .withProximity(Proximity.IMMEDIATE)
                .withUrl("https://kontakt.io")
                .execute(new CloudCallback<Action>() {
                    @Override
                    public void onSuccess(Action response, CloudHeaders headers) {
                            // TODO
                    }

                    @Override
                    public void onError(CloudError error) {
                            // TODO
                    }
                });*/
        // Seting beacon NoN Connectable ( after restarting, it is back to normal mode)
        // DO NOT USE ON TOUGH BEACONS
/*        kontaktCloud.commands().fetch()
                .forDevices("Jr2U")
                .withType(SecureCommandType.NONCONNECTABLE)
                .startIndex(5)
                .maxResult(15)
                .execute(new CloudCallback<SecureCommands>() {
                    @Override
                    public void onSuccess(SecureCommands response, CloudHeaders headers) {
                        // TODO
                    }

                    @Override
                    public void onError(CloudError error) {
                            // TODO
                    }
                });*/
        //Share venue with other Manager
/*        kontaktCloud.venues().share(UUID.fromString("venueID"))
                .toManagers("e-mail")
                .withAccess(Access.VIEWER)
                .execute(new CloudCallback<String>() {
                    @Override
                    public void onSuccess(String response, CloudHeaders headers) {
                        showToast("venue shared");
                        // TODO

                    }

                    @Override
                    public void onError(CloudError error) {
                            // TODO
                    }
                });*/
    }
    @Override
    protected void onStart() {
        checkPermissionAndStart();
        super.onStop();
    }
    @Override
    protected void onStop() {
        KontaktManager.stopScanning();
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        KontaktManager.disconnect();
        KontaktManager = null;
        super.onDestroy();
    }
    private void checkPermissionAndStart() {
        int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED == checkSelfPermissionResult) {
            //already granted
            Log.d(TAG,"Permission already granted");
            startScan();
        } else
        {
            //request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            Log.d(TAG,"Permission request called");
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (100 == requestCode) {
                Log.d(TAG,"Permission granted");
                startScan();
            }
        } else
        {
            Log.d(TAG,"Permission not granted");
            showToast("Kontakt.io SDK require this permission");
        }
    }
    private void onetimeconfiguration(){
        sdkInitialise();
        configureProximityManager();
        setRegions();
        setListeners();
        apiendpoints();
    }
    public void sdkInitialise()
    {
        KontaktSDK.initialize("Your_Secret_API_Key");
        if (KontaktSDK.isInitialized())
            Log.v(TAG, "SDK initialised");
    }
    private void configureProximityManager() {
        KontaktManager = new ProximityManager(this);
        KontaktManager.configuration()
                //.supportNonConnectableMode(true)
                .rssiCalculator(RssiCalculators.newLimitedMeanRssiCalculator(5))
                .resolveShuffledInterval(3)
                .scanMode(ScanMode.BALANCED)
                .scanPeriod(ScanPeriod.create(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(5)))
                .activityCheckConfiguration(ActivityCheckConfiguration.DEFAULT);
    }
    private void setRegions()
    {
        Collection<IBeaconRegion> beaconRegions = new ArrayList<>();
        BeaconRegion region = new BeaconRegion.Builder()
                // not secured UUID below
                .secureProximity(UUID.fromString("f7826da6-4fa2-4e98-8024-bc5b71e0893e"))
                .identifier("region")
                .build();
        beaconRegions.add(region);
        KontaktManager.spaces()
                .iBeaconRegions(beaconRegions);
        Log.d(TAG, "regions created");
    }
    private void setListeners()
    {
        KontaktManager.setIBeaconListener(createIBeaconListener());
        KontaktManager.setScanStatusListener(createScanStatusListener());
        Log.d(TAG,"Listeners Configured");
    }
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
    private ScanStatusListener createScanStatusListener() {
        return new SimpleScanStatusListener() {
            @Override
            public void onScanStart()
            {
                Log.d(TAG,"Scanning started");
                showToast("Scanning started");
            }
            @Override
            public void onScanStop()
            {
                Log.d(TAG,"Scanning stopped");
                showToast("Scanning stopped");
            }
        };
    }
    private void startScan() {
        KontaktManager.connect(new OnServiceReadyListener()
        {
            @Override
            public void onServiceReady() {
                KontaktManager.startScanning();
            }
        });
    }
    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion beaconRegion) {
                if ("region".equals(beaconRegion.getIdentifier())) {
                    Log.d(TAG, beaconRegion.getIdentifier() + "  discovered! ");
                    showToast(beaconRegion.getIdentifier() + " entered");
                }
            }
            @Override public void onIBeaconLost(IBeaconDevice ibeacon, IBeaconRegion beaconRegion) {
                Log.d(TAG, beaconRegion.getIdentifier() + " lost! ");
                showToast(beaconRegion.getIdentifier() + " lost!");
            }
        };
    }
}