package com.planverano.central;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;
    private String pendingTab;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Reminders.createChannel(this);
        Reminders.scheduleAll(this);

        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "Android");
        pendingTab = getIntent().getStringExtra("tab");
        if (saved != null) {
            web.restoreState(saved);
        } else {
            web.loadUrl("file:///android_asset/index.html");
        }

        if (Build.VERSION.SDK_INT >= 33
            && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        web.evaluateJavascript("window.refreshNotif && refreshNotif()", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.evaluateJavascript("window.onAppResume && onAppResume()", null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String tab = intent.getStringExtra("tab");
        if (tab != null && tab.matches("[a-z]+")) {
            web.evaluateJavascript("window.showTab && showTab('" + tab + "')", null);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("(window.handleBack && window.handleBack()) ? 'y' : 'n'",
            new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String v) {
                    if (!"\"y\"".equals(v)) finish();
                }
            });
    }

    final class Bridge {
        @JavascriptInterface
        public String launchTab() {
            String t = pendingTab;
            pendingTab = null;
            return t == null ? "" : t;
        }

        @JavascriptInterface
        public void share(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(i, "Compartir"));
                }
            });
        }

        @JavascriptInterface
        public void setTestDone(String id, boolean done) {
            getSharedPreferences(Reminders.PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("done_" + id, done).apply();
        }

        @JavascriptInterface
        public boolean notificationsOn() {
            NotificationManager nm = getSystemService(NotificationManager.class);
            return nm.areNotificationsEnabled();
        }

        @JavascriptInterface
        public void testNotification() {
            Reminders.post(MainActivity.this, 999, "Recordatorios activados",
                "Así te van a llegar los avisos antes de cada test.", "tests");
        }

        @JavascriptInterface
        public void openNotificationSettings() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(i);
                }
            });
        }

        @JavascriptInterface
        public String reminders() {
            return Reminders.json(MainActivity.this);
        }
    }
}
