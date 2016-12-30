package nl.vosdevelopment.wearwifiswitcher;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

public class WifiRequestIntentService extends IntentService {

    GoogleApiClient mGoogleApiClient;
    WifiManager wifiManager;

    public WifiRequestIntentService() {
        super(WifiRequestIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String wifiToConnectTo = intent.getStringExtra("WifiToConnectTo");
        boolean connectToWifi = intent.getBooleanExtra("ConnectToWifi", true);
        boolean scanWifiList = intent.getBooleanExtra("ScanWifiList", true);

        if(scanWifiList){

            wifiManager.startScan();

        }

        if (wifiToConnectTo != null && connectToWifi){

            int netId;
            for (WifiConfiguration tmp : wifiManager.getConfiguredNetworks())
                if (tmp.SSID.equals( "\""+ wifiToConnectTo +"\"")) {
                    netId = tmp.networkId;
                    wifiManager.enableNetwork(netId, true);
                }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }
}
