package com.hoofbeats.app.util;


import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.flir.flironesdk.Frame;
import com.flir.flironesdk.RenderedImage;
import com.hoofbeats.app.ui.activity.MainActivity;

import java.io.File;


public class FrameSaver {

    private static final String TAG = FrameSaver.class.getSimpleName();

    public static String saveFrame(final Context context, final Frame frame) {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

        String fileName = "FLIROne-" + lPadZero(SystemClock.elapsedRealtimeNanos(), 19) + ".jpg";

        String lastSavedPath = path + "/" + fileName;

        try {
            frame.save(new File(lastSavedPath), RenderedImage.Palette.Iron, RenderedImage.ImageType.BlendedMSXRGBA8888Image);

            Log.d(TAG, "***Lance*** thermal data saved to path: " + lastSavedPath);

            ((MainActivity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Saved to: " + lastSavedPath, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error saving frame data", e);
        }

        return lastSavedPath;
    }


    /**
     * @param in   The long value
     * @param fill The number of digits to fill
     * @return The given value left padded with the given number of digits
     */
    public static String lPadZero(long in, int fill) {

        boolean negative = false;
        long value, len = 0;

        if (in >= 0) {
            value = in;
        } else {
            negative = true;
            value = -in;
            in = -in;
            len++;
        }

        if (value == 0) {
            len = 1;
        } else {
            for (; value != 0; len++) {
                value /= 10;
            }
        }

        StringBuilder sb = new StringBuilder();

        if (negative) {
            sb.append('-');
        }

        for (int i = fill; i > len; i--) {
            sb.append('0');
        }

        sb.append(in);

        return sb.toString();
    }
}
