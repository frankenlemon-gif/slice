package background.work.around;

import android.app.slice.SliceManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

// BackgroundWorkAround (BWA)

public final class Start {

    private static final String TRUSTED_PACKAGE = "background.work.around";

    public static void RunService(Context context) {
        String baseAuthority = context.getPackageName() + ".background.work.around.provider";
        
        Uri providerUri0 = Uri.parse("content://" + baseAuthority);
        Uri providerUri1 = Uri.parse("content://" + baseAuthority + "1");
        
        PackageManager pm = context.getPackageManager();

        try {            
            PackageInfo pi = pm.getPackageInfo(TRUSTED_PACKAGE, PackageManager.GET_PERMISSIONS);
            boolean isSafe = true;

            if (pi.requestedPermissions != null) {
                for (String permission : pi.requestedPermissions) {
                    if ("android.permission.INTERNET".equals(permission)) {
                        isSafe = false;
                        break;
                    }
                }
            }
            
            if (isSafe) {
                // Для обычного контент-провайдера оставляем стандартные флаги доступа
                context.grantUriPermission(
                    TRUSTED_PACKAGE,
                    providerUri0,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION 
                );

                context.grantUriPermission(
                    TRUSTED_PACKAGE,
                    providerUri1,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION 
                );

                // Для слайс-провайдера выдаем только специализированные права
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    SliceManager sliceManager = context.getSystemService(SliceManager.class);
                    if (sliceManager != null) {
                        sliceManager.grantSlicePermission(TRUSTED_PACKAGE, providerUri1);
                    }
                }
            }
        } catch (Throwable e) {
            // Игнорируем, если BWA не установлен
        }
    }
}
