package com.sshdaemon;

import android.app.Application;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.config.ToastConfigurationBuilder;
import org.acra.data.StringFormat;

public class SshDaemonApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        var toastConfiguration = new ToastConfigurationBuilder()
                .withText(getString(R.string.acra_toast_text))
                .build();

        var mailConfiguration = new MailSenderConfigurationBuilder()
                .withMailTo("tibor.tarnai@gmail.com")
                // Send the report inline in the mail body rather than as a Crash.txt
                // attachment. The attachment path builds an ACTION_SEND_MULTIPLE intent
                // inside a chooser; on Android 16 the framework's
                // migrateExtraStreamToClipData reads EXTRA_TEXT as an ArrayList and
                // crashes with a ClassCastException (ACRA 5.13.1, still present upstream).
                // The inline path uses ACTION_SENDTO (mailto:) with no EXTRA_STREAM/chooser.
                .withReportAsFile(false)
                .withSubject(getString(R.string.mail_subject))
                .withBody(getString(R.string.mail_body))
                .build();

        CoreConfigurationBuilder builder = new CoreConfigurationBuilder()
                .withBuildConfigClass(BuildConfig.class)
                .withReportFormat(StringFormat.JSON)
                .withPluginConfigurations(
                        toastConfiguration,
                        mailConfiguration
                );

        ACRA.init(this, builder);
    }
}
