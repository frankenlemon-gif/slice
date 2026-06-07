package background.work.around;

import android.app.slice.Slice;
import android.app.slice.SliceProvider;
import android.content.Intent;
import android.net.Uri;

public class MySliceProvider extends SliceProvider {

    @Override
    public boolean onCreate() {
        
        return true;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        if (sliceUri == null) return null;

        String path = sliceUri.getLastPathSegment();
        
        switch (path) {
            case "ping_0":
                return createPingSlice("Ping 0");
            case "ping_1":
                return createPingSlice("Ping 1");
            case "ping_2":
                return createPingSlice("Ping 2");
            default:
                return null;
        }
    }

    private Slice createPingSlice(String text) {
        return new Slice.Builder(Uri.parse("content://background.work.around.provider/" + text))
                .build();
    }

    @Override
    public Uri onMapIntentToUri(Intent intent) {
       return null;
    }
}
