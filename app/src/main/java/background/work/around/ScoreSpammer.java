package background.work.around;

import android.app.slice.SliceManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScoreSpammer {

    private static ScheduledExecutorService myScheduler;
    private static ScheduledExecutorService externalScheduler;
    
    private static final Uri[] mySliceUris = new Uri[]{
            Uri.parse("content://background.work.around.provider/ping_0"),
            Uri.parse("content://background.work.around.provider/ping_1"),
            Uri.parse("content://background.work.around.provider/ping_2")
    };
    
    private static final String SLICE_ACTION = "android.intent.action.SLICE";
    private static final String META_DATA_KEY = "background.work.around.provider.bind.me.slice";
    
    private static int myCurrentStep = 0;
    private static int externalCurrentStep = 0;
    private static boolean isRunning = false;

    private ScoreSpammer() {}

    public static synchronized void startSpamming(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || isRunning) return;

        final SliceManager sliceManager = context.getSystemService(SliceManager.class);
        if (sliceManager == null) return;

        isRunning = true;
        
        // --- 1. Цикл для своего собственного приложения ---
        myScheduler = Executors.newScheduledThreadPool(1);
        try {
            for (Uri uri : mySliceUris) {
                sliceManager.pinSlice(uri, Collections.emptySet());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        myScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri targetUri = mySliceUris[myCurrentStep];
                    sliceManager.unpinSlice(targetUri);
                    sliceManager.pinSlice(targetUri, Collections.emptySet());
                    myCurrentStep = (myCurrentStep + 1) % mySliceUris.length;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 2, 2, TimeUnit.SECONDS);

        // --- 2. Отдельный цикл для найденных сторонних приложений ---
        final List<Uri> externalSliceUris = findExternalSlices(context);
        
        if (!externalSliceUris.isEmpty()) {
            externalScheduler = Executors.newScheduledThreadPool(1);
            
            try {
                for (Uri uri : externalSliceUris) {
                    sliceManager.pinSlice(uri, Collections.emptySet());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            externalScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        // ИСПРАВЛЕНО: используем .get() вместо квадратных скобок []
                        Uri targetUri = externalSliceUris.get(externalCurrentStep);
                        sliceManager.unpinSlice(targetUri);
                        sliceManager.pinSlice(targetUri, Collections.emptySet());
                        
                        externalCurrentStep = (externalCurrentStep + 1) % externalSliceUris.size();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 2, 2, TimeUnit.SECONDS);
        }
    }

    private static List<Uri> findExternalSlices(Context context) {
        List<Uri> foundUris = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(SLICE_ACTION);
        
        try {
            // ИСПРАВЛЕНО: queryIntentContentProviders возвращает List<ResolveInfo>
            List<ResolveInfo> resolveInfos = pm.queryIntentContentProviders(intent, PackageManager.GET_META_DATA);
            
            if (resolveInfos != null) {
                String myPackage = context.getPackageName();
                
                for (ResolveInfo resolveInfo : resolveInfos) {
                    ProviderInfo provider = resolveInfo.providerInfo;
                    if (provider == null) {
                        continue;
                    }

                    // Пропускаем свое собственное приложение
                    if (myPackage.equals(provider.packageName)) {
                        continue;
                    }
                    
                    Bundle metaData = provider.metaData;
                    if (metaData != null && metaData.getBoolean(META_DATA_KEY, false)) {
                        String targetAuthority = provider.packageName + ".background.work.around.provider1";
                        foundUris.add(Uri.parse("content://" + targetAuthority));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return foundUris;
    }

    public static synchronized void stopSpamming(Context context) {
        if (!isRunning) return;
        
        isRunning = false;
        
        if (myScheduler != null) {
            myScheduler.shutdownNow();
            myScheduler = null;
        }
        
        if (externalScheduler != null) {
            externalScheduler.shutdownNow();
            externalScheduler = null;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            SliceManager sliceManager = context.getSystemService(SliceManager.class);
            if (sliceManager != null) {
                for (Uri uri : mySliceUris) {
                    try { sliceManager.unpinSlice(uri); } catch (Exception ignored) {}
                }
                List<Uri> externalSliceUris = findExternalSlices(context);
                for (Uri uri : externalSliceUris) {
                    try { sliceManager.unpinSlice(uri); } catch (Exception ignored) {}
                }
            }
        }
        
        myCurrentStep = 0;
        externalCurrentStep = 0;
    }
}
