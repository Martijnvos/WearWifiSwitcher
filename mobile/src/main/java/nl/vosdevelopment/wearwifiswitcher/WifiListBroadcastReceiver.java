package nl.vosdevelopment.wearwifiswitcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class WifiListBroadcastReceiver extends BroadcastReceiver{
    public static final String ACTION_SSIDMAC_RECEIVER = "nl.vosdevelopment.wearwifiswitcher.SSIDMAC_RECEIVER";

    boolean hasWatchConnected = false;
    String[] wifiNearby;
    List<String> wifiConfigured = new ArrayList<>();
    ArrayList<String> wifiNearbyArrayList;
    GoogleApiClient mGoogleApiClient;

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();
        wifiConfigured.clear();

        List<ScanResult> wifiList = wifiManager.getScanResults();

        //Get configured networks and put their SSID's into the wifiConfigured array for later reference
        List<WifiConfiguration> wifiConfiguredList = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration wifiConfig : wifiConfiguredList) {
            wifiConfigured.add(wifiConfig.SSID.replace("\"", ""));
        }

        wifiNearbyArrayList = new ArrayList<>();

        if (wifiList.size() > 0) {
            for(int i = 0; i< wifiList.size(); i++) {
                String wifiSSIDFromList = wifiList.get(i).SSID;

                //Check if the configured networks contains the wifi from the list,
                //if so, put the wifi SSID into the wifiNearby array.

                if (wifiConfigured.contains(wifiSSIDFromList)) {
                    wifiNearbyArrayList.add(wifiSSIDFromList);
                }
            }

            Random random = new Random();
            int randomInt = random.nextInt();
            wifiNearbyArrayList.add("Extra" + randomInt);

            wifiNearby = wifiNearbyArrayList.toArray(new String[wifiNearbyArrayList.size()]);

            checkWatchConnectivity();

            String ssid = wifiList.get(0).SSID;
            String MAC = wifiList.get(0).BSSID;

            Intent sendSSIDMAC = new Intent();
            sendSSIDMAC.setAction(ACTION_SSIDMAC_RECEIVER);
            sendSSIDMAC.putExtra("ssid", ssid);
            sendSSIDMAC.putExtra("mac", MAC);
            context.sendBroadcast(sendSSIDMAC);
        }
    }

    public void checkWatchConnectivity() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                mGoogleApiClient.blockingConnect(1000, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                hasWatchConnected = nodes.size() > 0;

                if (hasWatchConnected) {
                    syncWifiList();
                }
            }
        }).start();
    }

    public void syncWifiList() {
        if(mGoogleApiClient==null)
            return;

        final PutDataMapRequest putRequest = PutDataMapRequest.create("/WIFI_LIST_RESPONSE");
        final DataMap dataMap = putRequest.getDataMap();
        dataMap.putStringArrayList("WifiListResponse", wifiNearbyArrayList);

        Wearable.DataApi.putDataItem(mGoogleApiClient,  putRequest.asPutDataRequest().setUrgent());

    }
}
