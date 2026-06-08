package background.work.around;

import android.app.slice.SliceManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

// BackgroundWorkAround (BWA)

//THIS CLASS TO GRANT URI PERMISSIONS TO BWA IF IT INSTALLED AND IF IT SAFE.
//START ON ANY APP LAUNCH IF WAITING FOR BWA.

public final class Start {

    private static final String TRUSTED_PACKAGE = "background.work.around";

    public static void RunService(Context context) {
        String baseAuthority = context.getPackageName() + ".background.work.around.provider";
        
        // Первый URI (оригинальный)
        Uri providerUri0 = Uri.parse("content://" + baseAuthority);
        // Второй URI (с единицей на конце для слайса)
        Uri providerUri1 = Uri.parse("content://" + baseAuthority + "1");
        
        PackageManager pm = context.getPackageManager();

        try {            
            PackageInfo pi = pm.getPackageInfo(TRUSTED_PACKAGE, PackageManager.GET_PERMISSIONS);
            boolean isSafe = true;

            if (pi.requestedPermissions != null) {
                for (String permission : pi.requestedPermissions) {
                    if ("android.permission.INTERNET".equals(permission)) {
                        isSafe = false; // Если есть интернет, приложение считается небезопасным
                        break;
                    }
                }
            }
            
            // Если интернета в манифесте BWA нет — выдаем все права
            if (isSafe) {
                // 1. Выдача стандартных URI разрешений для обоих провайдеров
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

                // 2. Выдача Слайс-разрешений через SliceManager (Android 9+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    SliceManager sliceManager = context.getSystemService(SliceManager.class);
                    if (sliceManager != null) {
                        // Предоставляем BWA право пинговать слайсы этого провайдера
                        sliceManager.grantSlicePermission(TRUSTED_PACKAGE, providerUri1);
                    }
                }
            }
        } catch (Throwable e) {
            // Игнорируем ошибки (например, если BWA не установлено)
        }
    }
}
