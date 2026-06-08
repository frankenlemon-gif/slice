package background.work.around;

import android.app.*;
import android.os.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.widget.*;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {					

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
		super.onReceive(context, intent);

        intent=null;		
        Context appContext = context.getApplicationContext();
        context=null;
		Start.RunService(appContext);
		if (done==1) return;
		done = 1;

		try {
          Intent serviceIntent = new Intent(appContext, NotificationService.class);   
		  Class<?> serviceClass = Class.forName("background.work.around.RiderService");
          Intent serviceIntent2 = new Intent(appContext, serviceClass);
          appContext.startForegroundService(serviceIntent);
          appContext.startForegroundService(serviceIntent2);
        } catch (Throwable t) {}    
        
    }
    	
    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context,"Device Admin Enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context,"Device Admin Disabled", Toast.LENGTH_SHORT).show();
    }
}
