package com.example.iconarrstudie;
import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;

// dient der Konfiguration von ACRA für bugreports
@ReportsCrashes(formKey = "", // will not be used
        mailTo = "jlouisgao@gmail.com",
        mode = ReportingInteractionMode.TOAST,
        customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT },
        logcatArguments = { "-t", "300","-v", "time", "MainActivity:V", "Test1_ddrop_alt:V", "Test2_pverbrauch:V", "Test3_pos:V",
                "Pre_Test1:V", "Pre_Test2:V", "Pre_Test3:V", "*:S" },
        resToastText = R.string.crash_toast_text)

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}