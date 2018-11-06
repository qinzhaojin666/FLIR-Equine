package com.hoofbeats.app;


import android.app.Application;

import com.flir.flironesdk.RenderedImage;
import com.hoofbeats.app.util.SavedDisplay;

import java.util.LinkedList;
import java.util.List;


public class MyApplication extends Application {

    public static MyApplication INSTANCE;

    public List<SavedDisplay> displays = new LinkedList<>();

    @Override
    public void onCreate() {

        INSTANCE = this;

        super.onCreate();
    }

    public void saveDisplay(RenderedImage renderedImage, String savedFrame) {

        final SavedDisplay display = new SavedDisplay();
        display.renderedImage = renderedImage;
        display.savedFrame = savedFrame;

        displays.add(display);
    }
}
