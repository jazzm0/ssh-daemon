package com.sshdaemon;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.sshdaemon.net.NetworkChangeReceiver;

import java.util.Objects;

public class ConfigFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.config_fragment, container, false);

        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.network_interfaces);

        Context context = getContext();

        NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver(linearLayout, context);

        Objects.requireNonNull(context).registerReceiver(networkChangeReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

        );
        return view;
    }
}
