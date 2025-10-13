package com.coconetgo.global;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class NetworkHelper {

    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    public NetworkHelper(Context context) {
        connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void registerNetworkCallback(TextView offlineBanner) {

        boolean isConnected = isNetworkConnected();
        if (offlineBanner != null) {
            offlineBanner.setVisibility(isConnected ? TextView.GONE : TextView.VISIBLE);
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                if (offlineBanner != null)
                    offlineBanner.post(() -> offlineBanner.setVisibility(TextView.GONE));
            }

            @Override
            public void onLost(@NonNull Network network) {
                if (offlineBanner != null)
                    offlineBanner.post(() -> offlineBanner.setVisibility(TextView.VISIBLE));
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    private boolean isNetworkConnected() {
        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(network);

        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }
}
