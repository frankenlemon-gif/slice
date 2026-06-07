package background.work.around;

import android.app.*;
import android.os.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.widget.*;

public class MyReceiver extends BroadcastReceiver {					

	private static final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

	private static int done=0;

    @Override
    public void onReceive(Context context, Intent intent) {

        intent=null;
		if (done==1) return;
		done = 1;
        Context appContext = context.getApplicationContext();
        context=null;
     
        final PendingResult pendingResult = goAsync();

        new Thread(() -> {
            try {                
                Intent serviceIntent = new Intent(appContext, NotificationService.class);                
                appContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT | Context.BIND_ABOVE_CLIENT);
                try {
                Class<?> serviceClass = Class.forName("background.work.around.RiderService");
                Intent serviceIntent2 = new Intent(appContext, serviceClass);
                appContext.startForegroundService(serviceIntent);
                appContext.startForegroundService(serviceIntent2);
                } catch (Throwable t) {}
                android.os.SystemClock.sleep(30_000);
				Start.RunService(appContext);
            } catch (Throwable t) {
               
            } finally {
                pendingResult.finish();
            }
        }).start();
    }
    	
}
