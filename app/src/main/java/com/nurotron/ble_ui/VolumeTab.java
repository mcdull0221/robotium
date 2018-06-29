package com.nurotron.ble_ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.nurotron.ble_ui.MainActivity.CURRENT_VOLUME;
import static com.nurotron.ble_ui.MainActivity.PROGRAM1;
import static com.nurotron.ble_ui.MainActivity.PROGRAM2;
import static com.nurotron.ble_ui.MainActivity.PROGRAM3;
import static com.nurotron.ble_ui.MainActivity.PROGRAM4;

/**
 * Created by Nurotron on 11/3/2016.
 */

public class VolumeTab extends Fragment {

    public SeekBar seekBar;
    public ProgressBar volumeBar;

    public int progress = 1;

    @InjectView(R.id.program1) ToggleButton tb1;
    @InjectView(R.id.program2) ToggleButton tb2;
    @InjectView(R.id.program3) ToggleButton tb3;
    @InjectView(R.id.program4) ToggleButton tb4;

    private ImageView volMin, volMax;
    public ImageButton mute;
    public ImageButton add_vol;
    public TextView volByte;
    private TextView mHomeText;
    private TextView volumeText;

    //    public static String currentProgram = getContext().getString(R.string.p1);
    public  String volumeData;

    private int saved_vol;
    private boolean isMute = false;
    private OnSeekbarSelectedListener mListener;
    private boolean isInitialize = false;


    private boolean tb1_status;
    private boolean tb2_status;
    private boolean tb3_status;
    private boolean tb4_status;

    private Button leftButton, rightButton;

    private TextView pgmByte;
    KeyValueStore sharedPrefs;

    /**
     * @return a new instance of {@link VolumeTab}, adding the parameters into a bundle and
     * setting them as arguments.
     */

    public interface OnSeekbarSelectedListener{
        void onSeekbarSelected(int vol);
        void onButtonSelected(int program);
        void onBluetoothButtonSelected(int option);


    }
    public VolumeTab(){

        this.mListener = null;
    }

    public void setCustomerListener (OnSeekbarSelectedListener listener){
        this.mListener = listener;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.volume_tab, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPrefs = new KeyValueStore.SharedPrefs(getContext());
        ButterKnife.inject(this, view);

        //  volumeData = (TextView) view.findViewById(R.id.volume);
        seekBar = (SeekBar) view.findViewById(R.id.volume_adj);
        volumeBar = (ProgressBar) view.findViewById(R.id.volume_bar);
        mute = (ImageButton) view.findViewById(R.id.mute);
        add_vol = (ImageButton) view.findViewById(R.id.add_vol);
        volByte = (TextView) view.findViewById(R.id.vol_byte);
        volMin = (ImageView) view.findViewById(R.id.volume_min);
        volMax = (ImageView) view.findViewById(R.id.volume_max);
        mHomeText = (TextView) view.findViewById(R.id.home_text);
        volumeText = (TextView) view.findViewById(R.id.vol);

        saved_vol = seekBar.getProgress();


//        initialize_parameters();

        if(DeviceScanActivity.isDemo) {
            seekBar.setProgress(5);
            volumeBar.setProgress(5);
        }


        volumeData = "" + (seekBar.getProgress() +1) + " of 12";

        mute.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //isMute = true;
                /*
                int prev_vol = seekBar.getProgress();
                if(prev_vol == 0) return;
                seekBar.setProgress(prev_vol - 1);
                volumeData.setText("Current: " + prev_vol + " / Max: 12");
                mListener.onSeerbarSelected(prev_vol);
                */
                int prev_vol = volumeBar.getProgress();
                if(prev_vol == 0) {
                   // volMin.setImageResource(R.drawable.volume_under_glow);
                    return;
                }
                volMax.setImageResource(R.drawable.volume_under_glow);
                volumeBar.setProgress(prev_vol - 1);
                volumeData = "" + prev_vol + " of 12";
                mListener.onSeekbarSelected(prev_vol);

            }
        });

        add_vol.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                /*
                int prev_vol = seekBar.getProgress();
                if(prev_vol == 11)
                    return;
                seekBar.setProgress(prev_vol + 1);
                volumeData.setText("Current: " + (prev_vol + 2) + " / Max: 12");
                mListener.onSeerbarSelected(prev_vol + 2);azz
                */
                int prev_vol = volumeBar.getProgress();
                if(prev_vol == 11) {
          //          volMax.setImageResource(R.drawable.volume_over_glow);
                    return;
                }
                volMin.setImageResource(R.drawable.volume_over_glow);
                volumeBar.setProgress(prev_vol + 1);
                if(volumeBar.getProgress() == 11)
                    volMax.setImageResource(R.drawable.volume_over_glow);
                volumeData = "" + (prev_vol + 2) + " of 12";
                mListener.onSeekbarSelected(prev_vol + 2);
            }
        });


        leftButton = (Button) view.findViewById(R.id.left_btn);
        rightButton = (Button) view.findViewById(R.id.right_btn);

        changeProgramName();
        if(MainActivity.numberOfDevice == 2) {
            leftButton.setVisibility(View.VISIBLE);
            rightButton.setVisibility(View.VISIBLE);
        }



        pgmByte = (TextView) view.findViewById(R.id.pgm_byte);
        setStyles();

        tb1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(tb1.isChecked()) {
                    switch (event.getAction()) {

                        case MotionEvent.ACTION_UP:
                            System.out.println(" released ");
                            return true;
                    }
                }

                return false;

            }


        });


        tb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isInitialize = false;
//                if(!tb1_status)
//                    tb1.setChecked(true);
            }
        });

        tb1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked){
                    tb2.setChecked(false);
                    tb3.setChecked(false);
                    tb4.setChecked(false);
                    tb1.setTextOn(sharedPrefs.getString(PROGRAM1) +" "+ getString(R.string.selected));
                    if(isInitialize)
                        isInitialize = false;
                    else{
                        if(mListener != null)
                            mListener.onButtonSelected(0);
                    }
                }
                else{
                    tb1.setTextOff(sharedPrefs.getString(PROGRAM1));
                }

            }
        });

        tb2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(tb2.isChecked()) {
                    switch (event.getAction()) {

                        case MotionEvent.ACTION_UP:
                            System.out.println(" released ");

                            return true;

                    }
                }
                return false;

            }
        });
        tb2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isInitialize = false;
//                if(!tb2.isChecked())
//                    tb2.setChecked(true);
            }
        });

        tb2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    tb1.setChecked(false);
                    tb3.setChecked(false);
                    tb4.setChecked(false);
                    tb2.setTextOn(sharedPrefs.getString(PROGRAM2) +" "+ getString(R.string.selected));
                    if(isInitialize)
                        isInitialize = false;
                    else{
                        if(mListener != null)
                            mListener.onButtonSelected(1);
                    }
                }
                else{
                    tb2.setTextOff(sharedPrefs.getString(PROGRAM2));
                }

            }
        });

        tb3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(tb3.isChecked()) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            System.out.println(" released ");
                            return true;
                    }
                }
                return false;

            }
        });

        tb3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isInitialize = false;
//                if(!tb3.isChecked())
//                    tb3.setChecked(true);
            }
        });

        tb3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    tb2.setChecked(false);
                    tb1.setChecked(false);
                    tb4.setChecked(false);
                    tb3.setTextOn(sharedPrefs.getString(PROGRAM3) +" "+ getString(R.string.selected));
                    if(isInitialize)
                        isInitialize = false;
                    else{
                        if(mListener != null)
                            mListener.onButtonSelected(2);
                    }
                }
                else{
                    tb3.setTextOff(sharedPrefs.getString(PROGRAM3));
                }

            }
        });
        tb4.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(tb4.isChecked()) {
                    switch (event.getAction()) {

                        case MotionEvent.ACTION_UP:
                            System.out.println(" released ");
                            return true;

                    }
                }
                return false;

            }
        });
        tb4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isInitialize = false;
//                if(!tb4.isChecked())
//                    tb4.setChecked(true);
            }
        });

        tb4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    tb2.setChecked(false);
                    tb3.setChecked(false);
                    tb1.setChecked(false);
                    tb4.setTextOn(sharedPrefs.getString(PROGRAM4) +" "+ getString(R.string.selected));
                    if(isInitialize)
                        isInitialize = false;
                    else{
                        if(mListener != null)
                            mListener.onButtonSelected(3);
                    }
                }
                else{
                    tb4.setTextOff(sharedPrefs.getString(PROGRAM4));
                }

            }
        });

        lockButton(true);



        //classic bluetooth
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onBluetoothButtonSelected(1);
            }
        });

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onBluetoothButtonSelected(0);
            }
        });
    }


    public void lockButton(boolean wait){
        if(mute != null && add_vol != null && tb1 != null && tb2 != null && tb3 != null && tb4 != null) {
            if (wait) {
                mute.setClickable(false);
                add_vol.setClickable(false);
                tb1.setClickable(false);
                tb2.setClickable(false);
                tb3.setClickable(false);
                tb4.setClickable(false);
            } else {
                mute.setClickable(true);
                add_vol.setClickable(true);
                tb1.setClickable(true);
                tb2.setClickable(true);
                tb3.setClickable(true);
                tb4.setClickable(true);
            }
        }

    }
//    public void setVolumeByte(byte[] volume){
//        volByte.setText(volume[0] + "\n " + volume[1] + "\n " + volume[2] + "\n " + volume[3] + "\n "+ volume[4] + "\n "+ volume[5] + "\n "+ volume[6] + "\n "+ volume[7] + "\n "+ volume[8] + " ");
//    }

    public void setVolumeBar(int vol){
        volumeBar.setProgress(vol-1);
        volumeText.setText(""+sharedPrefs.getInt(CURRENT_VOLUME, 0));
        if(vol == 12)
            volMax.setImageResource(R.drawable.volume_over_glow);
        else
            volMax.setImageResource(R.drawable.volume_under_glow);
    }

    public void setCurrentProgram(int program){
        isInitialize = true;
        if(tb1 != null && tb2 != null && tb3 != null && tb4 != null) {
            switch (program) {
                case 1:
                    //tb1.setChecked(false);
                    if (tb1.isChecked())
                        isInitialize = false;
                    tb1.setChecked(true);
                    break;
                case 2:
                    // tb2.setChecked(false);
                    if (tb2.isChecked())
                        isInitialize = false;
                    tb2.setChecked(true);
                    break;
                case 3:
                    // tb3.setChecked(false);
                    if (tb3.isChecked())
                        isInitialize = false;
                    tb3.setChecked(true);
                    break;
                case 4:
                    // tb4.setChecked(false);
                    if (tb4.isChecked())
                        isInitialize = false;
                    tb4.setChecked(true);
                    break;
                default:
                    break;
            }
        }

    }

    public void changeProgramName(){
        if(sharedPrefs.getString(PROGRAM1) == null)
            sharedPrefs.putString(PROGRAM1,getString(R.string.p1));
        else {
            tb1.setTextOff(sharedPrefs.getString(PROGRAM1));
            tb1.setTextOn(sharedPrefs.getString(PROGRAM1) +" "+ getString(R.string.selected));
        }
        if(sharedPrefs.getString(PROGRAM2) == null)
            sharedPrefs.putString(PROGRAM2,getString(R.string.p2));
        else {
            tb2.setTextOff(sharedPrefs.getString(PROGRAM2));
            tb2.setTextOn(sharedPrefs.getString(PROGRAM2) +" "+ getString(R.string.selected));
        }
        if(sharedPrefs.getString(PROGRAM3) == null)
            sharedPrefs.putString(PROGRAM3,getString(R.string.p3));
        else {
            tb3.setTextOff(sharedPrefs.getString(PROGRAM3));
            tb3.setTextOn(sharedPrefs.getString(PROGRAM3) +" "+ getString(R.string.selected));
        }
        if(sharedPrefs.getString(PROGRAM4) == null)
            sharedPrefs.putString(PROGRAM4,getString(R.string.p4));
        else {
            tb4.setTextOff(sharedPrefs.getString(PROGRAM4));
            tb4.setTextOn(sharedPrefs.getString(PROGRAM4) +" "+ getString(R.string.selected));
        }

    }

    private void setStyles() {
        float sp = 18.0f;
        if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("small")) sp = sp * 0.8f;
        else if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("large")) sp = sp * 1.2f;
        float size = sp * getResources().getDisplayMetrics().scaledDensity;
        Typeface typeface = TypefaceUtil.get(getContext());
        tb1.setTypeface(typeface); tb1.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        tb2.setTypeface(typeface); tb2.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        tb3.setTypeface(typeface); tb3.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        tb4.setTypeface(typeface); tb4.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        leftButton.setTypeface(typeface); leftButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        rightButton.setTypeface(typeface); rightButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mHomeText.setTypeface(typeface); mHomeText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

}
