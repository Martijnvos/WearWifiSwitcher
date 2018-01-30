package nl.vosdevelopment.wearwifiswitcher;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;

public class FragmentConnectionInfo extends Fragment {
    public static final String ACTION_SSIDMAC_RECEIVER = "nl.vosdevelopment.wearwifiswitcher.SSIDMAC_RECEIVER";

    TextView currentlyConnected, currentMACAddress, currentIPAddress, currentLinkSpeed, currentNetworkID, currentSignalStrength, currentWifiFrequency;
    int wifiLinkSpeed, wifiSignalStrength;
    String ipAddressString, wifiSSID;

    Timer timer;
    AdView mAdView;

    WifiManager wifiManager;
    WifiInfo wifiInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflatedView =  inflater.inflate(R.layout.fragment_connection_info, container, false);

        currentlyConnected = (TextView) inflatedView.findViewById(R.id.currently_connected_textview);
        currentMACAddress = (TextView) inflatedView.findViewById(R.id.current_mac_address_textview);
        currentIPAddress = (TextView) inflatedView.findViewById(R.id.current_ip_address_textview);
        currentLinkSpeed = (TextView) inflatedView.findViewById(R.id.current_link_speed_textview);
        currentSignalStrength = (TextView) inflatedView.findViewById(R.id.current_signal_strength_textview);
        currentWifiFrequency = (TextView) inflatedView.findViewById(R.id.current_wifi_frequency_textview);

        wifiManager = (WifiManager) this.getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiInfo = wifiManager.getConnectionInfo();

        mAdView = (AdView) inflatedView.findViewById(R.id.adView);

        setupWifiAndInfo();

        return inflatedView;
    }

    public void setupWifiAndInfo() {
        AdRequest adRequest = new AdRequest.Builder().build();
        if (mAdView != null) {
            mAdView.loadAd(adRequest);
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    mAdView.setVisibility(View.GONE);
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    mAdView.setVisibility(View.VISIBLE);
                }
            });
        }

        if (wifiManager != null) {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
        }

        wifiLinkSpeed = wifiInfo.getLinkSpeed();
        wifiSignalStrength = wifiInfo.getRssi();
        int IP = wifiInfo.getIpAddress();

            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                IP = Integer.reverseBytes(IP);
            }
            byte[] ipByteArray = BigInteger.valueOf(IP).toByteArray();

            try {
                ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
            } catch (UnknownHostException ex) {
                Log.e("WIFIIP", "Unable to get host address.");
                ipAddressString = null;
            }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            currentWifiFrequency.setVisibility(View.VISIBLE);
            int wifiFrequency = wifiInfo.getFrequency();
            Resources res = getResources();
            currentWifiFrequency.setText(String.format(res.getString(R.string.textview_home_wifi_frequency_loaded), wifiFrequency));
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.LOLLIPOP){
            currentWifiFrequency.setVisibility(View.GONE);
        }

        if (ipAddressString != null) {
            currentIPAddress.setText(ipAddressString);
        }

        if(wifiLinkSpeed != 0) {
            Resources res = getResources();
            currentLinkSpeed.setText(String.format(res.getString(R.string.textview_home_link_speed_loaded), wifiLinkSpeed));
        }

        if (wifiSignalStrength != 0) {
            Resources res = getResources();
            currentSignalStrength.setText(String.format(res.getString(R.string.textview_home_signal_strength_loaded), wifiSignalStrength));
        }
    }

    private BroadcastReceiver receiveSSIDMAC = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_SSIDMAC_RECEIVER)){
                String receivedSSID = intent.getStringExtra("ssid");
                String receivedMAC = intent.getStringExtra("mac");
                wifiSSID = wifiInfo.getSSID();

                setupWifiAndInfo();

                if (receivedSSID.equals(wifiSSID)){
                    currentlyConnected.setText(receivedSSID);
                } else {
                    currentlyConnected.setText(wifiSSID.replaceAll("^\"|\"$", ""));
                }
                currentMACAddress.setText(receivedMAC);
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        try{
            getActivity().registerReceiver(receiveSSIDMAC, new IntentFilter("nl.vosdevelopment.wearwifiswitcher.SSIDMAC_RECEIVER"));
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String scanDelaySetting = prefs.getString("refresh_interval_preference", "10");

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                wifiManager.startScan();
            }
        }, 0, Integer.parseInt(scanDelaySetting)*1000);
    }

    @Override
    public void onPause() {
        try{
            getActivity().unregisterReceiver(receiveSSIDMAC);
        }catch (IllegalArgumentException e){e.printStackTrace();}

        if (timer != null){
            timer.cancel();
        }
        super.onPause();
    }
}
