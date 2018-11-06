package com.hoofbeats.app.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.flir.flironesdk.RenderedImage;
import com.hoofbeats.app.MyApplication;
import com.hoofbeats.app.R;
import com.hoofbeats.app.ui.view.CrossHairView;
import com.hoofbeats.app.util.SavedDisplay;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CompareActivity extends Activity {
    private static final String TAG = CompareActivity.class.getSimpleName();
    private static final String ITEM_A_INDEX_EXTRA = CompareActivity.class.getName() + ".ITEM_A_INDEX_EXTRA";
    private static final String ITEM_B_INDEX_EXTRA = CompareActivity.class.getName() + ".ITEM_B_INDEX_EXTRA";
    private ImageView itemAPreviewImage;
    private CrossHairView itemACrosshairs;
    private ImageView itemBPreviewImage;
    private CrossHairView itemBCrosshairs;
    private Integer itemATempInC;
    private Integer itemBTempInC;
    private TextView tempText;
    private TextView diagnosisText;
    private SavedDisplay itemADisplay;
    private SavedDisplay itemBDisplay;
    private Bitmap itemAPreviewBitmap;
    private Bitmap itemBPreviewBitmap;

    private static interface OnTempReadListener {
        void onTemp(final Integer tempInC);
    }

    public static void startFor(final Context context, final int displayIndexA, final int displayIndexB) {
        final Intent intent = new Intent(context, CompareActivity.class);
        intent.putExtra(ITEM_A_INDEX_EXTRA, displayIndexA);
        intent.putExtra(ITEM_B_INDEX_EXTRA, displayIndexB);
        context.startActivity(intent);
    }

    private OnTempReadListener itemATempListener = new OnTempReadListener() {
        @Override
        public void onTemp(final Integer tempInC) {
            itemATempInC = tempInC;
            updateText();
        }
    };

    private OnTempReadListener itemBTempListener = new OnTempReadListener() {
        @Override
        public void onTemp(final Integer tempInC) {
            itemBTempInC = tempInC;
            updateText();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);
        
        itemAPreviewImage = findViewById(R.id.itemAPreviewImage);
        itemACrosshairs = findViewById(R.id.itemACrosshairs);
        itemBPreviewImage = findViewById(R.id.itemBPreviewImage);
        itemBCrosshairs = findViewById(R.id.itemBCrosshairs);
        tempText = findViewById(R.id.temperatureReadout);
        diagnosisText = findViewById(R.id.diagnosis);

        if (MyApplication.INSTANCE.displays.isEmpty()) {
            Toast.makeText(this, "Save a thermal image first!", Toast.LENGTH_LONG).show();
            //finish();
            return;
        }

        int itemAIndex = 0;
        if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey(ITEM_A_INDEX_EXTRA)) {
            itemAIndex = getIntent().getExtras().getInt(ITEM_A_INDEX_EXTRA);
        }

        int itemBIndex = MyApplication.INSTANCE.displays.size() > 1 ? 1 : 0;
        if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey(ITEM_B_INDEX_EXTRA)) {
            itemBIndex = getIntent().getExtras().getInt(ITEM_B_INDEX_EXTRA);
        }

        itemADisplay = MyApplication.INSTANCE.displays.get(itemAIndex);
        itemBDisplay = MyApplication.INSTANCE.displays.get(itemBIndex);

        itemAPreviewBitmap = BitmapFactory.decodeFile(itemADisplay.savedFrame);
        itemBPreviewBitmap = BitmapFactory.decodeFile(itemBDisplay.savedFrame);

        // To help comparisons, when crosshairs A is dragged, move both
        // When B is dragged, move only B, giving user option of linked and non-linked movement.
        final View.OnTouchListener linkedDrag = new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                Log.d(TAG, "linkedDrag onTouch x,y = " + event.getX() + "," + event.getY());

                if (!itemBCrosshairs.hasLocation()) {
                    updateSelectionCoordinates((int) event.getX(), (int) event.getY(),
                            itemBCrosshairs, itemBPreviewBitmap, itemBDisplay, itemBTempListener);
                    return true;
                }

                if (!itemACrosshairs.hasLocation()) {
                    return true;
                }

                final float deltaX = event.getX() - itemACrosshairs.getLocationX();
                final float deltaY = event.getY() - itemACrosshairs.getLocationY();
                Log.d(TAG, "crosshair a moved = " + deltaX + "," + deltaY);

                final float newBX = itemBCrosshairs.getLocationX() + deltaX;
                final float newBY = itemBCrosshairs.getLocationY() + deltaY;

                updateSelectionCoordinates((int) newBX, (int) newBY,
                        itemBCrosshairs, itemBPreviewBitmap, itemBDisplay, itemBTempListener);

                return false;
            }
        };

        setupItem(itemADisplay, itemAPreviewBitmap, itemAPreviewImage,
                itemACrosshairs, itemATempListener, linkedDrag);
        setupItem(itemBDisplay, itemBPreviewBitmap, itemBPreviewImage,
                itemBCrosshairs, itemBTempListener, null);
    }

    private void updateText() {
        if ( null != itemATempInC && null != itemBTempInC ) {
            final int tempDifference = itemBTempInC - itemATempInC;
            String displayString = "Item A: " + itemATempInC + "\u00B0C  ";
            displayString += "Item B: " + itemBTempInC + "\u00B0C  ";
            displayString += "Delta T: " + tempDifference + "\u00B0C  ";
            tempText.setText(displayString);

            if (tempDifference > 2) {
                diagnosisText.setText("possible....1 See...!");
            } else if (tempDifference < -2) {
                diagnosisText.setText("possible....2 See...!");
            } else {
                diagnosisText.setText("situation normal!");
            }

        } else if ( null != itemATempInC ) {
            String displayString = "Item A: " + itemATempInC + "\u00B0C  ";
            displayString += "Tap Item B to Compare!";
            tempText.setText(displayString);

        } else if ( null != itemBTempInC ) {
            String displayString = "Item B: " + itemBTempInC + "\u00B0C  ";
            displayString += "Tap Item A to Compare!";
            tempText.setText(displayString);
        }
    }

    private void setupItem(final SavedDisplay display,
                           final Bitmap previewBitmap,
                           final ImageView imageView,
                           final CrossHairView crosshairs,
                           final OnTempReadListener tempListener,
                           final View.OnTouchListener extraOnTouchListener) {

        Log.d(TAG, "renderedImage width,height = " + display.renderedImage.width() + "," + display.renderedImage.height());

        imageView.setImageBitmap(previewBitmap);

        crosshairs.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "setupItem onTouch x,y = " + event.getX() + "," + event.getY());

                if (null != extraOnTouchListener) {
                    extraOnTouchListener.onTouch(v, event);
                }

                updateSelectionCoordinates((int) event.getX(), (int) event.getY(),
                        crosshairs, previewBitmap, display, tempListener);

                return true;
            }
        });
    }

    private void updateSelectionCoordinates(final int newX, final int newY,
                                            CrossHairView crosshairs,
                                            Bitmap previewBitmap,
                                            SavedDisplay display,
                                            OnTempReadListener tempListener) {
        Log.d(TAG, "updateSelectionCoordinates x,y = " + newX + "," + newY);

        crosshairs.setLocation(newX, newY);
        crosshairs.invalidate();

        final int horizontalPadding = (crosshairs.getWidth() - previewBitmap.getWidth()) / 2;
        final int verticalPadding = (crosshairs.getHeight() - previewBitmap.getHeight()) / 2;

        final int imageX = newX - horizontalPadding;
        final int imageY = newY - verticalPadding;

        final Integer tempInC = getTempAt(CompareActivity.this, display.renderedImage, imageX, imageY);
        Log.d(TAG, "temp is: " + tempInC);
        tempListener.onTemp(tempInC);
    }

    private static Integer getTempAt(final Context context, final RenderedImage renderedImage,
                              final int previewImageX, final int previewImageY) {
        Log.d(TAG, "getTempAt x,y = " + previewImageX + "," + previewImageY);

        // Didn't save temps
        if (renderedImage.imageType() != RenderedImage.ImageType.ThermalRadiometricKelvinImage) {
            Toast.makeText(context, "Save a radiometric image type!", Toast.LENGTH_LONG).show();
            return null;
        }

        // Preview image is 480x640, temp image is 240x320
        final int tempImageX = previewImageX / 2;
        final int tempImageY = previewImageY / 2;

        // Out of bounds
        if ( tempImageX < 0 || tempImageY < 0
                || tempImageX >= renderedImage.width() || tempImageY >= renderedImage.height()) {
            return null;
        }

        final int index = tempImageX + tempImageY * renderedImage.width();

        short[] shortPixels = new short[renderedImage.pixelData().length / 2];

        // Thermal data is little endian.
        ByteBuffer.wrap(renderedImage.pixelData()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortPixels);

        int tempInC = (shortPixels[index] - 27315) / 100;

        return tempInC;
    }
}
