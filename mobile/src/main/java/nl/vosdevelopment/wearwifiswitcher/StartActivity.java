package nl.vosdevelopment.wearwifiswitcher;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StartActivity extends AppCompatActivity {
    WifiManager wifiManager;
    AssetManager assetManager;
    GoogleApiClient mGoogleApiClient;

    Bundle thisSavedInstanceState;

    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        thisSavedInstanceState = savedInstanceState;

        setupBottomBar();

        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assetManager = this.getAssets();

        MobileAds.initialize(getApplicationContext(), "ADS KEY");

        if (findViewById(R.id.fragment_container) != null) {

            FragmentConnectionInfo connectionInfoFragment = new FragmentConnectionInfo();
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, connectionInfoFragment).commit();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        wifiManager.startScan();

    }

    public void setupBottomBar() {
        BottomBar mBottomBar = (BottomBar) findViewById(R.id.bottomBar);
        if (mBottomBar != null) {
            mBottomBar.setOnTabSelectListener(new OnTabSelectListener() {
                @Override
                public void onTabSelected(@IdRes int menuItemId) {
                    switch (menuItemId){
                        case R.id.bb_menu_connection_info:

                            FragmentConnectionInfo connectionInfoFragment = new FragmentConnectionInfo();
                            getFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, connectionInfoFragment)
                                    .addToBackStack(null)
                                    .commit();
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle(R.string.actionbar_home_connection_info_title);
                            }
                            break;
                        case R.id.bb_menu_settings:

                            FragmentSettings settingsFragment = new FragmentSettings();
                            getFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, settingsFragment)
                                    .addToBackStack(null)
                                    .commit();
                            if (getSupportActionBar() != null){
                                getSupportActionBar().setTitle(R.string.bottombar_settings_title);
                            }
                            break;
                        case R.id.bb_menu_about:
                            FragmentAbout aboutFragment = new FragmentAbout();
                            getFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, aboutFragment)
                                    .addToBackStack(null)
                                    .commit();
                            if (getSupportActionBar() != null){
                                getSupportActionBar().setTitle(R.string.bottombar_about_title);
                            }
                            break;
                    }
                }
            });
        }
    }

    public void requestCorrectLocationSetting(){
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    StartActivity.this, 1);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(relativeLayout, R.string.location_permission_granted_text, Snackbar.LENGTH_LONG).show();
            if (Build.VERSION.SDK_INT > 22){
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogStyle);
                alertDialog.setTitle("Restart WifiSwitcher");
                alertDialog.setMessage("Please restart the app to get the MAC and IP address workaround to work...");
                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                alertDialog.create().show();
            }
        } else if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED){
            if (Build.VERSION.SDK_INT > 22){
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogStyle);
                alertDialog.setTitle("Permission needed");
                alertDialog.setMessage("Location permission is needed since Android 6.0 because of a limitation by Google.\n" +
                        "See the Play Store description for more information");
                alertDialog.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                        }
                    }
                });
                alertDialog.setNegativeButton("Close app", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                alertDialog.create().show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        break;
                }
                break;
        }
    }

    public String getLicenseAssets(AssetManager assetManager, String fileName){
        String contents = "";
        InputStream is = null;
        BufferedReader reader = null;
        try {
            is = assetManager.open(fileName);
            reader = new BufferedReader(new InputStreamReader(is));
            contents = reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                contents += '\n' + line;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return contents;
    }

    public void openAlertDialogLicense(String title, String message, final String githubLink){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogStyle);
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}});
        alertDialog.setNegativeButton("Github", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse(githubLink));
                startActivity(intent);
            }
        });
        alertDialog.create().show();
    }

    public void openBottomBarLicense(View view) {
        String contents = getLicenseAssets(assetManager, "bottombar_library.txt");
        openAlertDialogLicense("BottomBar License", contents, "https://github.com/roughike/BottomBar");
    }

    @Override
    public void onBackPressed() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean exitDialog = prefs.getBoolean("exit_dialog_checkbox_preference", true);

        if (exitDialog){
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogStyle);
            alertDialog.setTitle(getString(R.string.alertdialog_home_title_close));
            alertDialog.setMessage(getString(R.string.alertdialog_home_message_close));
            alertDialog.setCancelable(false);
            alertDialog.setPositiveButton(getString(R.string.alertdialog_home_button_wifi_on), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    wifiManager.setWifiEnabled(true);
                    finish();
                }
            });
            alertDialog.setNegativeButton(getString(R.string.alertdialog_home_button_wifi_off), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    wifiManager.setWifiEnabled(false);
                    finish();
                }
            });
            alertDialog.setNeutralButton(getString(R.string.alertdialog_home_button_back), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            alertDialog.show();
        } else{
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null){
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 23){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();

            requestCorrectLocationSetting();
        }
    }
}
