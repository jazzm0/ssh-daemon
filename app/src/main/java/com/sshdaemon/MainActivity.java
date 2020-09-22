package com.sshdaemon;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.sshdaemon.sshd.SshDaemon;

import java.io.IOException;

import static com.sshdaemon.util.ExternalStorage.createDirIfNotExists;

public class MainActivity extends AppCompatActivity implements NavigationHost {

    public static final String ApplicationName = "SshDaemon";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new ConfigFragment())
                    .commit();
        }

        String appPath = Environment.getExternalStorageDirectory().getPath();
        String configurationPath = appPath + "/" + ApplicationName;
        createDirIfNotExists(configurationPath);

        try {
            new SshDaemon(8022);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void navigateTo(Fragment fragment, boolean addToBackstack) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, fragment);

        if (addToBackstack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    public void startStopClicked(View view) {
    }
}
