package nl.vosdevelopment.wearwifiswitcher;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;

public class WearableListenerService extends com.google.android.gms.wearable.WearableListenerService {

    private static final String WIFI_LIST_MESSAGE_PATH = "/wifi_list",
            WIFI_SWITCH_MESSAGE_PATH = "/wifi_switch";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        String wifiToConnectTo = new String(messageEvent.getData());

        if(messageEvent.getPath().equals(WIFI_LIST_MESSAGE_PATH)){

            Intent intent = new Intent(WearableListenerService.this, WifiRequestIntentService.class);
            intent.putExtra("ScanWifiList", true);
            startService(intent);

        } else if (messageEvent.getPath().equals(WIFI_SWITCH_MESSAGE_PATH)){

            Intent intent = new Intent(WearableListenerService.this, WifiRequestIntentService.class);
            intent.putExtra("ConnectToWifi", true);
            intent.putExtra("WifiToConnectTo", wifiToConnectTo);
            startService(intent);
        }
    }
}
