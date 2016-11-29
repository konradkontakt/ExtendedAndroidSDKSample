package com.example.konradbujak.extendedandroidsdksample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import com.kontakt.sdk.android.ble.device.EddystoneNamespace;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.ScanStatusListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleScanStatusListener;
import com.kontakt.sdk.android.ble.rssi.RssiCalculators;
import com.kontakt.sdk.android.cloud.IKontaktCloud;
import com.kontakt.sdk.android.cloud.KontaktCloud;
import com.kontakt.sdk.android.cloud.api.ActionsApi;
import com.kontakt.sdk.android.cloud.response.CloudCallback;
import com.kontakt.sdk.android.cloud.response.CloudError;
import com.kontakt.sdk.android.cloud.response.CloudHeaders;
import com.kontakt.sdk.android.cloud.response.paginated.Actions;
import com.kontakt.sdk.android.cloud.response.paginated.Devices;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.Proximity;
import com.kontakt.sdk.android.common.model.Access;
import com.kontakt.sdk.android.common.model.Action;
import com.kontakt.sdk.android.common.model.Device;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {


    private ProximityManager KontaktManager;
    String TAG = "MyActivity";
    public static String urls;
    public static Proximity proximity;
    //Replace (Your Secret API key) with your API key aquierd from the Kontakt.io Web Panel
    public static String API_KEY = "QcZNRdfovwLcPVFAvbHgacOnfGBkcHco";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onetimeconfiguration();
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
    // Toasts on device
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
    // Checking the permissions for the Android OS 6.0 +
    private void checkPermissionAndStart() {
        int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, Arrays.toString(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE}));
        if (PackageManager.PERMISSION_GRANTED == checkSelfPermissionResult) {
            //already granted
            Log.d(TAG,"Permission already granted");
            startScan();
        }
        else {
            //request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
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
        apiendpoints();
        setListeners();
    }
    public void sdkInitialise()
    {
        KontaktSDK.initialize(API_KEY);
        if (KontaktSDK.isInitialized())
            Log.v(TAG, "SDK initialised");
    }
    private void configureProximityManager() {
        KontaktManager = new ProximityManager(this);
        KontaktManager.configuration()
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
        BeaconRegion region2 = new BeaconRegion.Builder()
                .proximity(UUID.fromString("17826da4-4fa3-4e98-8024-bc5b71e0893e"))
                .identifier("region2")
                .build();
        BeaconRegion region3 = new BeaconRegion.Builder()
                .proximity(UUID.fromString("f7826da6-4fa2-4e98-8024-bc5b71e0893e"))
                .identifier("4HXX")
                .major(39823)
                .minor(60760)
                .build();
        beaconRegions.add(region);
        beaconRegions.add(region2);
        beaconRegions.add(region3);
        Collection<IEddystoneNamespace> Namespaces = new ArrayList<>();
        IEddystoneNamespace namespace1 = new EddystoneNamespace.Builder()
                .identifier("namespace1")
                .namespace("236691279b094b70b62b")
                .instanceId("73796c744368")
                .build();
        Namespaces.add(namespace1);
        KontaktManager.spaces()
                .iBeaconRegions(beaconRegions)
                .eddystoneNamespaces(Namespaces);
        Log.d(TAG, "regions created");
    }
    private void apiendpoints() {
        IKontaktCloud kontaktCloud = KontaktCloud.newInstance(API_KEY);
        //Fetching all devices from API
        kontaktCloud.devices().fetch()
                .maxResult(10) //default is 50
                .startIndex(0)
                .access(Access.OWNER)
                .execute(new CloudCallback<Devices>() {
                    @Override public void onSuccess(Devices response, CloudHeaders headers) {
                        List<Device> content = response.getContent();
                        for (Device device : content) {
                            String uid = device.getUniqueId();
                            Log.d(TAG, uid);
                        }
                    }
                    @Override public void onError(CloudError error) {
                        showToast("Connection error");
                    }
                });
        // Setting up Browser Content ( can be done via Web Panel too )
        // for beacon with uniqueId 4HXX( replace this uniqueId with yours)
        // REMEMBER : it will create another action each time you will start application
/*        kontaktCloud.actions().createBrowserAction()
                .forDevices("4HXX")
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
        ActionsApi actionsApi = kontaktCloud.actions();
        // Downloading Actions for the beacon 4HXX
        // To make it simple there is only one Action set here
        actionsApi.fetch().forDevices("4HXX").execute(new CloudCallback<Actions>() {
            @Override
            public void onSuccess(Actions response, CloudHeaders headers) {
                Log.d(TAG, "Actions success");
                Log.d(TAG, "Size " + response.getContent().size());
                List<Action> content = response.getContent();
                for (Action action : content) {
                    //Saving the values as global variables
                    urls = action.getUrl();
                    proximity = action.getProximity();
                    Log.d(TAG, "Action Url : " + urls + " at proximity : " + proximity);
                }
            }
            @Override
            public void onError(CloudError error) {
                Log.d(TAG, "actions fail");
            }
        });
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
    private void setListeners()
    {
        KontaktManager.setIBeaconListener(createIBeaconListener());
        KontaktManager.setScanStatusListener(createScanStatusListener());
        KontaktManager.setEddystoneListener(createEddystoneListener());
        Log.d(TAG,"Listeners Configured");
    }
    public IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion beaconRegions) {
                if ("4HXX".equals(beaconRegions.getIdentifier())) {
                    Log.d(TAG, "4HXX discovered with proximity : " + ibeacon.getProximity());
                    // if beacon is in the same proximity that was set for Browser Content action
                    // it will open the link in the default browser
                    if (proximity.equals(ibeacon.getProximity()))
                    {
                        Log.d(TAG, "URL :" + urls + " at proximity : " + proximity);
                        Uri uri = Uri.parse(urls);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                }
                if ("region".equals(beaconRegions.getIdentifier())) {
                    Log.d(TAG, beaconRegions.getIdentifier() + "  discovered! "+ ibeacon.getUniqueId() + " Proximity : " + ibeacon.getProximity());
                    showToast(beaconRegions.getIdentifier() + " entered");
                }
                if ("region2".equals(beaconRegions.getIdentifier())){
                    Log.d(TAG, beaconRegions.getIdentifier() + "  discovered! "+ ibeacon.getUniqueId() + " Proximity : " + ibeacon.getProximity());
                    showToast(beaconRegions.getIdentifier() + " entered");
                }
            }
                @Override
                public void onIBeaconsUpdated (List < IBeaconDevice > ibeacons, IBeaconRegion beaconRegions){
                    // when discovered beacon was not in the set proximity but it can be in the future
                    // monitor the proximity for it
                    for (IBeaconDevice ibeacon : ibeacons) {
                        if ("4HXX".equals(beaconRegions.getIdentifier())) {
                            Log.d(TAG, "4HXX updated with proximity : " + ibeacon.getProximity());
                            if (proximity.equals(ibeacon.getProximity()))
                            {
                                Log.d(TAG, "URL :" + urls + " at proximity : " + proximity);
                                Uri uri = Uri.parse(urls);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                            }
                        }
                        Log.d(TAG, beaconRegions.getIdentifier() + "  updated! " + ibeacon.getUniqueId() + " RSSI :" + ibeacon.getRssi());

                    }
                }
                @Override public void onIBeaconLost (IBeaconDevice ibeacon, IBeaconRegion beaconRegion){
                    Log.d(TAG, beaconRegion.getIdentifier() + " lost! ");
                    showToast(beaconRegion.getIdentifier() + " lost!  ");
                }
        };
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
    private EddystoneListener createEddystoneListener(){
        return new SimpleEddystoneListener(){
            @Override
            public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                if ("namespace1".equals(namespace.getIdentifier())) {
                    Log.d(TAG, namespace.getIdentifier() + " discovered with proximity : " + eddystone.getProximity());
                }
            }
            @Override
            public void onEddystonesUpdated(List<IEddystoneDevice> eddystones, IEddystoneNamespace namespace) {
                // TODO
            }

            @Override
            public void onEddystoneLost(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                // TODO
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
}