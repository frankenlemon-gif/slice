package background.work.around;

import android.app.Activity;
import android.view.Gravity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MainActivity extends Activity {

    private ActivityManager am;
    private LinearLayout container;
    private final Handler h = new Handler();

    @Override
    protected void onResume() {
        super.onResume();
        background.work.around.Start.RunService(this);        
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int topPadding = (int) (screenHeight * 0.07f);
        root.setPadding(30, topPadding, 30, 30);
        root.setBackgroundColor(Color.BLACK);

        TextView tv = new TextView(this);
        tv.setTextSize(18);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.LEFT);

        String lang =  java.util.Locale.getDefault().getLanguage();
        if (lang.equals("ru")) {
            tv.setText("Это приложение BackgroundWorkAround. Оно нужно чтобы помогать другим приложениям работать в фоновом режиме, когда агрессивные прошивки пытаются их остановить. На этой странице вы можете посмотреть приоритет процессов (если это debug версия) или отправить броадкаст чтобы запустить приложение если оно ещё не запущено.");
        } else {
            tv.setText("Hi! This is the BackgroundWorkAround app. It designed to help other applications run in the background when aggressive firmwares attempts to stop them. In this page you can see process priority (if this is debug version) or send broadcast to launch the application if it is not already running.");
        }

        root.addView(tv);

        
        Button runBtn = new Button(this);        
        if (lang.equals("ru")) {
        runBtn.setText("Отправить броадкаст");
        } else {
        runBtn.setText("Send broadcast");
        }
        runBtn.setBackgroundColor(Color.WHITE);
        runBtn.setTextColor(Color.BLACK);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 150);
        btnParams.setMargins(0, 50, 0, 0);
        runBtn.setLayoutParams(btnParams);

        runBtn.setOnClickListener(v -> {
            background.work.around.Start.RunService(this);
        });

        root.addView(runBtn);

        ScrollView scroll = new ScrollView(this);
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(container);
        root.addView(scroll);

        setContentView(root);
        loop();
    }

    private void loop() {
        h.post(new Runnable() {
            @Override
            public void run() {
                refresh();
                h.postDelayed(this, 2500);
            }
        });
    }

    private void refresh() {
        container.removeAllViews();
        
        log("--- /proc/self ---", Color.CYAN);
        
        log("Self OOM Adj: " + readProc("/proc/self/oom_score_adj"), Color.WHITE);

        
        Set<String> manifestProcesses = new HashSet<>();
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 
                    PackageManager.GET_SERVICES | PackageManager.GET_ACTIVITIES | PackageManager.GET_RECEIVERS);
            
            
            if (pi.services != null) {
                for (ServiceInfo si : pi.services) {
                    if (si.processName != null) manifestProcesses.add(si.processName);
                }
            }
            
        } catch (Throwable e) {
            log("Manifest read error: " + e.getMessage(), Color.RED);
        }

        List<ActivityManager.RunningAppProcessInfo> running = am.getRunningAppProcesses();
        if (running != null) {
            for (ActivityManager.RunningAppProcessInfo p : running) {
                
                if (p.processName.contains(getPackageName()) || manifestProcesses.contains(p.processName)) {
                    String oom = readProc("/proc/" + p.pid + "/oom_score_adj");
                    log(" [APP PROCESS] " + p.processName + " (PID:" + p.pid + ") -> OOM ADJ SCORE: " + oom, Color.GREEN);
                }
            }
        }

        log("\n--- API: ActivityManager.getRunningAppProcesses ---", Color.GREEN);
        if (running != null) {
            for (ActivityManager.RunningAppProcessInfo p : running) {
                log(p.processName + " | ​ActivityManager importance: " + p.importance, Color.WHITE);
            }
        }


        log("\n--- IActivityManager/ActivityManagerNative ---", Color.MAGENTA);
        try {
            Class<?> amNative = Class.forName("android.app.ActivityManagerNative");
            Method getDefault = amNative.getMethod("getDefault");
            Object iam = getDefault.invoke(null); 
            Method getProcesses = iam.getClass().getMethod("getRunningAppProcesses");
            List<ActivityManager.RunningAppProcessInfo> hiddenProcs = (List<ActivityManager.RunningAppProcessInfo>) getProcesses.invoke(iam);
            if (hiddenProcs != null) {
                for (ActivityManager.RunningAppProcessInfo p : hiddenProcs) {
                    log(" [PROCESS] " + p.processName + " | PID: " + p.pid, Color.LTGRAY);
                }
            }
        } catch (Throwable e) {
            log(" Error: " + e.getMessage(), Color.RED);
        }

        log("\n--- API: ActivityManager.getRunningServices ---", Color.YELLOW);
        List<ActivityManager.RunningServiceInfo> svcs = am.getRunningServices(100);
        if (svcs != null) {
            for (ActivityManager.RunningServiceInfo s : svcs) {
                log(" [SERVICE] " + s.service.getPackageName(), Color.WHITE);
            }
        }

        
    }

    private void log(String m, int c) {
        TextView t = new TextView(this);
        t.setText(m);
        t.setTextColor(c);
        t.setTextSize(14);
        container.addView(t);
    }

    private String readProc(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = br.readLine();
            br.close();
            return (line != null) ? line.trim() : "Empty";
        } catch (Throwable e) { return "N/A"; }
    }
}
