package nl.vosdevelopment.wearwifiswitcher;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class StartActivity extends WearableActivity implements WearableListView.ClickListener, DataApi.DataListener,
                                                                MessageApi.MessageListener,
                                                                GoogleApiClient.ConnectionCallbacks,
                                                                GoogleApiClient.OnConnectionFailedListener{

    private GoogleApiClient mGoogleApiClient;
    String[] wifiSSIDs;
    String displayShape, selectedWifiNetwork;
    private static final String WIFI_SWITCH_MESSAGE_PATH = "/wifi_switch",
                                WIFI_LIST_MESSAGE_PATH = "/wifi_list",
                                SWITCHED_WIFI_PATH = "/switched_wifi";

    TextView wifiListLoadingPlaceholderRound, wifiListLoadingPlaceholderRect, wifiConnectingPlaceholderRound, wifiConnectingPlaceholderRect;
    WearableListView wearableListViewRound, wearableListViewRect;
    ProgressBar loadingAnimationRound, loadingAnimationRect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        final WatchViewStub startpageStub = (WatchViewStub) findViewById(R.id.startpage_watch_view_stub);
        startpageStub.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                startpageStub.onApplyWindowInsets(insets);

                if (insets.isRound()) {
                    displayShape = "Round";
                }else {
                    displayShape = "Rectangular";
                }
                return insets;
            }
        });
        startpageStub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub watchViewStub) {

                wifiListLoadingPlaceholderRound = (TextView) findViewById(R.id.startpage_wifilist_loading_round);
                wifiConnectingPlaceholderRound = (TextView) findViewById(R.id.startpage_wificonnection_ongoing_round);
                wearableListViewRound = (WearableListView) findViewById(R.id.wifi_list_round);
                loadingAnimationRound = (ProgressBar) findViewById(R.id.startpage_loading_animation_round);

                wifiListLoadingPlaceholderRect = (TextView) findViewById(R.id.startpage_wifilist_loading_rect);
                wifiConnectingPlaceholderRect = (TextView) findViewById(R.id.startpage_wificonnection_ongoing_rect);
                wearableListViewRect = (WearableListView) findViewById(R.id.wifi_list_rect);
                loadingAnimationRect = (ProgressBar) findViewById(R.id.startpage_loading_animation_rect);
            }
        });

    }

    public void fireMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), WIFI_LIST_MESSAGE_PATH, new byte[0]).await();
                    if(!result.getStatus().isSuccess()){
                       Log.v("Wear list message", "Message not sent to: "+ node.getDisplayName());
                    } else {
                        Log.v("Wear list message", "Message succesfully sent to: "+ node.getDisplayName());
                    }
                }
            }
        }).start();
    }

    public void setupListView(){
        if (displayShape.equals("Round")) {
            wifiListLoadingPlaceholderRound.setVisibility(View.GONE);
            wearableListViewRound.setVisibility(View.VISIBLE);
            loadingAnimationRound.setVisibility(View.GONE);
            wifiConnectingPlaceholderRound.setVisibility(View.GONE);

            wearableListViewRound.setAdapter(new ListViewAdapter(StartActivity.this, wifiSSIDs));
            wearableListViewRound.setClickListener(StartActivity.this);

        } else if (displayShape.equals("Rectangular")){
            wifiListLoadingPlaceholderRect.setVisibility(View.GONE);
            wearableListViewRect.setVisibility(View.VISIBLE);
            loadingAnimationRect.setVisibility(View.GONE);
            wifiConnectingPlaceholderRect.setVisibility(View.GONE);

            wearableListViewRect.setAdapter(new ListViewAdapter(StartActivity.this, wifiSSIDs));
            wearableListViewRect.setClickListener(StartActivity.this);
        }
    }

    public void sendDataBack() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), WIFI_SWITCH_MESSAGE_PATH, selectedWifiNetwork.getBytes()).await();
                    if (!result.getStatus().isSuccess()) {
                        Log.v("INFO", "ERROR sending to node");
                    } else {
                        Log.v("INFO", "Success sent to: " + node.getDisplayName());
                    }
                }
            }
        });
        thread.start();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/WIFILIST") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    if (!dataMap.isEmpty()){
                        wifiSSIDs = dataMap.getStringArray("WifiList");
                        setupListView();
                    }else {
                        Toast.makeText(StartActivity.this, "Could not receive Access Points", Toast.LENGTH_SHORT).show();
                    }
                }

                if (item.getUri().getPath().compareTo("/WIFI_LIST_RESPONSE") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    if (!dataMap.isEmpty()){
                        wifiSSIDs = dataMap.getStringArray("WifiListResponse");
                        setupListView();
                    }
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                //TODO delete with official release
                Toast.makeText(StartActivity.this, "DataItem deleted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(SWITCHED_WIFI_PATH)){
            String connectedWifiNetwork = new String(messageEvent.getData()).replaceAll("^\"|\"$", "");

            if (connectedWifiNetwork.equals(selectedWifiNetwork)){
                Intent intent = new Intent(this, ConfirmationActivity.class);
                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                        ConfirmationActivity.SUCCESS_ANIMATION);
                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                        getString(R.string.wifi_connected_message));
                startActivity(intent);
                setupListView();
            } else {
                wifiNotConnectedMessage();
                setupListView();
            }
        }
    }

    public void wifiNotConnectedMessage(){
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.FAILURE_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                getString(R.string.wifi_not_connected_message));
        startActivity(intent);
    }

    @Override
    public void onClick(WearableListView.ViewHolder v) {
        Integer clickedEntry = (Integer) v.itemView.getTag();
        // use this data to complete some action ...
        selectedWifiNetwork = wifiSSIDs[clickedEntry];

        if (displayShape.equals("Round")){
            wifiConnectingPlaceholderRound.setVisibility(View.VISIBLE);
            loadingAnimationRound.setVisibility(View.VISIBLE);
            wearableListViewRound.setVisibility(View.GONE);
            wifiListLoadingPlaceholderRound.setVisibility(View.GONE);
        }else if (displayShape.equals("Rectangular")){
            wifiConnectingPlaceholderRect.setVisibility(View.VISIBLE);
            loadingAnimationRect.setVisibility(View.VISIBLE);
            wearableListViewRect.setVisibility(View.GONE);
            wifiListLoadingPlaceholderRect.setVisibility(View.GONE);
        }

        sendDataBack();
    }

    @Override
    public void onTopEmptyRegionClick() {
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        fireMessage();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    public void onConnectionSuspended(int i) {}
}
