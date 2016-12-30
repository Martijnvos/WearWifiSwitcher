package nl.vosdevelopment.wearwifiswitcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class WifiConnectedBroadcastReceiver extends BroadcastReceiver {

    GoogleApiClient mGoogleApiClient;
    private final static String SWITCHED_WIFI_PATH = "/switched_wifi";

    @Override
    public void onReceive(Context context, Intent intent) {

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {

            final WifiInfo wifiConnectionInfo = wifiManager.getConnectionInfo();
            final String currentWifiNetwork = wifiConnectionInfo.getSSID();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    for (Node node : nodes.getNodes()) {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), SWITCHED_WIFI_PATH, currentWifiNetwork.getBytes()).await();
                        if (!result.getStatus().isSuccess()) {
                            Log.v("Wear list message", "Message not sent to: " + node.getDisplayName());
                        } else {
                            Log.v("Wear list message", "Message succesfully sent to: " + node.getDisplayName());
                        }
                    }
                }
            }).start();
        }
    }
}
