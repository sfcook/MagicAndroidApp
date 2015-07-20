package com.app.sfcook.magicandroidapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.seismic.ShakeDetector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MagicPhoneApp extends Activity implements ShakeDetector.Listener{

    private final double _SQRT2 = 1.41421356;
    private final String _FILENAME = "answers.xml";

    private ImageView imgAnswerViewport;
    private TextView txtAnswerViewport;
    private Spinner ddlAnswers;
    private List<List<String>> lstAnswers;
    private Random rand;

    private int iCurrentAnswers;
    private boolean bAnswerVisible = true;

    /**
     * The number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int HIDE_DELAY_MILLIS = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_magic_phone_app);

        txtAnswerViewport = (TextView) findViewById(R.id.txtAnswer);
        imgAnswerViewport = (ImageView) findViewById(R.id.imgAnswerViewport);
        ddlAnswers = (Spinner) findViewById(R.id.ddlAnswers);

        resizeAnswerViewport();

        loadAnswers();
        parseAnswers();
        rand = new Random();

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        imgAnswerViewport.setOnTouchListener(mTouchListener);
        ddlAnswers.setOnItemSelectedListener(mItemSelectedListener);

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        ShakeDetector sd = new ShakeDetector(this);
        sd.start(sensorManager);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        loadAnswers();
        parseAnswers();
    }

    // set answer viewport for the size of the device
    private void resizeAnswerViewport()
    {
        DisplayMetrics display;
        int iSize;

        display = this.getResources().getDisplayMetrics();

        if(display.widthPixels>display.heightPixels)
        {
            iSize = display.heightPixels;
        }
        else
        {
            iSize = display.widthPixels;
        }

        iSize = iSize - 20;
        imgAnswerViewport.setMaxHeight(iSize);
        imgAnswerViewport.setMinimumHeight(iSize);
        imgAnswerViewport.setMaxWidth(iSize);
        imgAnswerViewport.setMinimumWidth(iSize);

        //txt to fit inside blue viewport
        iSize = (int)(iSize / _SQRT2);
        txtAnswerViewport.setMaxHeight(iSize);
        txtAnswerViewport.setMinimumHeight(iSize);
        txtAnswerViewport.setMaxWidth(iSize);
        txtAnswerViewport.setMinimumWidth(iSize);
    }

    // load answers xml file
    private void loadAnswers()
    {
        InputStream in_s = null;
        OutputStream out_s = null;
        File file;
        byte[] buffer;

        try {
            in_s = getApplicationContext().getAssets().open(_FILENAME);
            file = new File(getExternalFilesDir(null), _FILENAME);
            if(!file.exists())
            {
                out_s = new FileOutputStream(file);
                buffer = new byte[1024];
                int read;
                while((read = in_s.read(buffer)) != -1){
                    out_s.write(buffer, 0, read);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    // set the possible answers
    private void parseAnswers()
    {
        lstAnswers = new ArrayList<>();

        List<String> Answers = new ArrayList<String>();
        List<String> temp = null;
        XmlPullParserFactory pullParserFactory;
        XmlPullParser parser;
        InputStream in_s;
        File file;
        String name;
        String selections[];
        String sWeight;
        int eventType;
        int iWeight = 0;

        try{
            pullParserFactory = XmlPullParserFactory.newInstance();
            parser = pullParserFactory.newPullParser();

            file = new File(getExternalFilesDir(null), _FILENAME);
            in_s = new FileInputStream(file);

            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in_s, null);

            eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT)
            {
                switch (eventType)
                {
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("AnswerSet"))
                        {
                            temp = new ArrayList<String>();
                            Answers.add(parser.getAttributeValue(null, "title"));
                        }
                        else if (temp != null && name.equalsIgnoreCase("Answer"))
                        {
                            sWeight = parser.getAttributeValue(null, "weight");
                            iWeight = 0;
                            name = parser.nextText();
                            if(sWeight != null)
                            {
                                iWeight = Integer.parseInt(sWeight);
                            }
                            if (iWeight < 1)
                            {
                                iWeight = 1;
                            }
                            for(int i=0; i<iWeight; i++)
                            {
                                temp.add(name);
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("AnswerSet") && temp != null)
                        {
                            lstAnswers.add(temp);
                        }
                }
                eventType = parser.next();
            }
        }
        catch (XmlPullParserException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        selections = Answers.toArray(new String[0]);

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this,   android.R.layout.simple_spinner_item, selections);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
        ddlAnswers.setAdapter(spinnerArrayAdapter);

        iCurrentAnswers = 0;
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            changeMagic();
            return false;
        }
    };

    // roll and call anamations
    private void changeMagic()
    {
        if(bAnswerVisible) {
            bAnswerVisible = false;

            setMagicVisibility(false);

            delayRollMagic(HIDE_DELAY_MILLIS);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void setMagicVisibility(boolean visible)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            // If the ViewPropertyAnimator API is available
            // (Honeycomb MR2 and later), use it to animate the
            // in-layout UI controls at the bottom of the
            // screen.
            txtAnswerViewport.animate()
                    .alpha(visible ? 1 : 0)
                    .setDuration(HIDE_DELAY_MILLIS);
        } else {
            // If the ViewPropertyAnimator APIs aren't
            // available, simply show or hide the in-layout UI
            // controls.
            txtAnswerViewport.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    // roll on a delay
    private void delayRollMagic(int delayMillis)
    {
        mRollHandler.removeCallbacks(mRollRunnable);
        mRollHandler.postDelayed(mRollRunnable, delayMillis);
    }

    Handler mRollHandler = new Handler();
    Runnable mRollRunnable = new Runnable() {
        @Override
        public void run() {
            rollMagic();
            delayedShow(HIDE_DELAY_MILLIS);
        }
    };

    // roll
    private void rollMagic()
    {
        txtAnswerViewport.setText(lstAnswers.get(iCurrentAnswers).get(rand.nextInt(lstAnswers.get(iCurrentAnswers).size())));
    }

    Handler mShowHandler = new Handler();
    Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            setMagicVisibility(true);
            bAnswerVisible = true;
        }
    };

    // show answer on a delay
    private void delayedShow(int delayMillis) {
        mShowHandler.removeCallbacks(mShowRunnable);
        mShowHandler.postDelayed(mShowRunnable, delayMillis);
    }

    Spinner.OnItemSelectedListener mItemSelectedListener = new Spinner.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1,
        int position, long arg3)
        {
            iCurrentAnswers = position;
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub

        }
    };

    @Override
    public void hearShake() {
        changeMagic();
    }
}
