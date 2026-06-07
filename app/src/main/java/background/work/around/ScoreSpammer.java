package background.work.around;

import android.app.slice.SliceManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScoreSpammer {

    private static ScheduledExecutorService scheduler;
    
    private static final Uri[] sliceUris = new Uri[]{
            Uri.parse("content://background.work.around.provider/ping_0"),
            Uri.parse("content://background.work.around.provider/ping_1"),
            Uri.parse("content://background.work.around.provider/ping_2")
    };
    
    private static int currentStep = 0;
    private static boolean isRunning = false;

    private ScoreSpammer() {}

    public static synchronized void startSpamming(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || isRunning) return;

        final SliceManager sliceManager = context.getSystemService(SliceManager.class);
        if (sliceManager == null) return;

        isRunning = true;
        
        scheduler = Executors.newScheduledThreadPool(1);

        try {
            for (Uri uri : sliceUris) {
                sliceManager.pinSlice(uri, Collections.emptySet());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri targetUri = sliceUris[currentStep];

                    sliceManager.unpinSlice(targetUri);
                    sliceManager.pinSlice(targetUri, Collections.emptySet());

                    currentStep = (currentStep + 1) % sliceUris.length;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 2, 2, TimeUnit.SECONDS); 
    }

    public static synchronized void stopSpamming(Context context) {
        if (!isRunning) return;
        
        isRunning = false;
        
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            SliceManager sliceManager = context.getSystemService(SliceManager.class);
            if (sliceManager != null) {
                for (Uri uri : sliceUris) {
                    try { 
                        sliceManager.unpinSlice(uri); 
                    } catch (Exception ignored) {}
                }
            }
        }
        
        currentStep = 0;
    }
}
