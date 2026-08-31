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
                .withReportAsFile(true)
                .withReportFileName("Crash.txt")
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
