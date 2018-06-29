package com.nurotron.ble_ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.nurotron.ble_ui.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.nurotron.ble_ui.MainActivity.CURRENT_PROGRAM;
import static com.nurotron.ble_ui.MainActivity.PROGRAM1;
import static com.nurotron.ble_ui.MainActivity.PROGRAM2;
import static com.nurotron.ble_ui.MainActivity.PROGRAM3;
import static com.nurotron.ble_ui.MainActivity.PROGRAM4;

/**
 * Created by Nurotron on 11/21/2016.
 */

public class PreprocessingTab extends Fragment {

    //public static EditText mProgramName;
    private GRadioGroup mInput;
    private GRadioGroup mMode;
    private GRadioGroup mCTone;
    private GRadioGroup mNoise;
    private GRadioGroup mRatio;
    private OnButtonChangeListener mListener;

    @InjectView(R.id.pre_title) TextView title;
    @InjectView(R.id.ctone_title) TextView ctone_title;
    @InjectView(R.id.input_title) TextView input_title;
    @InjectView(R.id.mode_title) TextView mode_title;
    @InjectView(R.id.noise_title) TextView noise_title;
    @InjectView(R.id.ratio_title) TextView ratio_title;
    @InjectView(R.id.custom) TextView custom;
    @InjectView(R.id.programText) EditText mProgram;

    public static EditText mProgramName;

    KeyValueStore sharedPrefs;
    View inflatedView;

    public interface OnButtonChangeListener{
        public void onButtonChange(String setting, int value);
        public void onProgramNameChanged(int program);
    }
    public PreprocessingTab (){
        this.mListener = null;
    }

    public void setCustomerListener (OnButtonChangeListener listener){
        this.mListener = listener;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        inflatedView = inflater.inflate(R.layout.preprocessing_tab_small, container, false);

        return inflatedView;
    }



    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPrefs = new KeyValueStore.SharedPrefs(getContext());
        ButterKnife.inject(this, inflatedView);
        setStyles();

        mProgramName = (EditText) view.findViewById(R.id.programText);
        mProgramName.addTextChangedListener(new TextWatcher() {
            int program = 0;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                program = sharedPrefs.getInt(CURRENT_PROGRAM, 0);
                if(s.toString().isEmpty()){
                    if (program == 1)
                        mProgramName.setHint(getString(R.string.p1));
                    else if (program == 2)
                        mProgramName.setHint(getString(R.string.p2));
                    else if (program == 3)
                        mProgramName.setHint(getString(R.string.p3));
                    else if (program == 4)
                        mProgramName.setHint(getString(R.string.p4));
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0) {
                    getProgramName(false, program, s.toString());
                    if(mListener != null)
                        mListener.onProgramNameChanged(program);
                }
                else{
                    getProgramName(true, program, s.toString());
                    if(mListener != null)
                        mListener.onProgramNameChanged(program);
                }

            }
        });

       // mInput = new GRadioGroup(view, R.id.input_m, R.id.input_a, R.id.input_t, R.id.input_ma,R.id.input_mt );
        mInput = new GRadioGroup(view, R.id.input_atm, R.id.input_ma, R.id.input_mt, R.id.input_bluetooth);
        mInput.setName("input");

        mMode = new GRadioGroup(view, R.id.mode_rich, R.id.mode_normal);
        mMode.setName("mode");

        mCTone = new GRadioGroup(view, R.id.ctone_disable, R.id.ctone_enable);
        mCTone.setName("ctone");

        mNoise = new GRadioGroup(view, R.id.noise_disable, R.id.noise_enable);
        mNoise.setName("noise");

        mRatio = new GRadioGroup(view, R.id.ratio_2, R.id.ratio_3, R.id.ratio_5, R.id.ratio_9);
        mRatio.setName("ratio");

        ratio_title.setVisibility(View.GONE);
        mRatio.setVisible(false);
        //mProgram.setText(R.string.p1);

    }


    public void lockRadioButton(boolean lock){
        if(lock){
            mInput.setClickable(false);
            mMode.setClickable(false);
            mCTone.setClickable(false);
            mNoise.setClickable(false);
            mRatio.setClickable(false);
        }
        else {
            mInput.setClickable(true);
            mMode.setClickable(true);
            mCTone.setClickable(true);
            mNoise.setClickable(true);
            mRatio.setClickable(true);
        }
    }

    public void getProgramName( boolean def, int program, String s){
        if(def){
            if (program == 1)
                sharedPrefs.putString(PROGRAM1, getString(R.string.p1));
            else if (program == 2)
                sharedPrefs.putString(PROGRAM2, getString(R.string.p2));
            else if (program == 3)
                sharedPrefs.putString(PROGRAM3, getString(R.string.p3));
            else if (program == 4)
                sharedPrefs.putString(PROGRAM4, getString(R.string.p4));
        }
        else{
            if (program == 1)
                sharedPrefs.putString(PROGRAM1, s);
            else if (program == 2)
                sharedPrefs.putString(PROGRAM2, s);
            else if (program == 3)
                sharedPrefs.putString(PROGRAM3, s);
            else if (program == 4)
                sharedPrefs.putString(PROGRAM4, s);
        }

    }

    public void setProgramName(int program){
        if(program == 1)
            mProgramName.setText(sharedPrefs.getString(PROGRAM1));
        else if (program == 2)
            mProgramName.setText(sharedPrefs.getString(PROGRAM2));
        else if (program == 3)
            mProgramName.setText(sharedPrefs.getString(PROGRAM3));
        else if (program == 4)
            mProgramName.setText(sharedPrefs.getString(PROGRAM4));
    }

//    public void setInputSource(byte input) {
//        if(input == (byte)0x61){
//            mInput.getRB(getView(),R.id.input_m).setChecked(true);
//            mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_m));
//            ratio_title.setVisibility(View.GONE);
//            mRatio.setVisible(false);
//        }
//        else if (input == (byte) 0x70){
//            mInput.getRB(getView(),R.id.input_a).setChecked(true);
//            mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_a));
//            ratio_title.setVisibility(View.GONE);
//            mRatio.setVisible(false);
//        }
//        else if (input == (byte) 0x50){
//            mInput.getRB(getView(),R.id.input_t).setChecked(true);
//            mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_t));
//            ratio_title.setVisibility(View.GONE);
//            mRatio.setVisible(false);
//        }
//        else if (input == (byte) 0x71){
//            mInput.getRB(getView(),R.id.input_ma).setChecked(true);
//            mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_ma));
//            ratio_title.setVisibility(View.VISIBLE);
//            mRatio.setVisible(true);
//            getActivity().findViewById(android.R.id.content).invalidate();
//
//        }
//        else if (input == (byte) 0x51){
//            mInput.getRB(getView(),R.id.input_mt).setChecked(true);
//            mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_mt));
//            ratio_title.setVisibility(View.VISIBLE);
//            mRatio.setVisible(true);
//        }
//    }

    public void setInputSource(byte input){
        switch(input){
            case 0x31:
                mInput.getRB(getView(),R.id.input_mt).setChecked(true);
                mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_mt));
                ratio_title.setVisibility(View.VISIBLE);
                mRatio.setVisible(true);
                getActivity().findViewById(android.R.id.content).invalidate();
                mRatio.getRB(getView(),R.id.ratio_2).setChecked(true);
                mRatio.uncheckOtherRBs(mRatio.getRB(getView(),R.id.ratio_2));
                break;
            case 0x33:
                mInput.getRB(getView(),R.id.input_mt).setChecked(true);
                mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_mt));
                ratio_title.setVisibility(View.VISIBLE);
                mRatio.setVisible(true);
                getActivity().findViewById(android.R.id.content).invalidate();
                mRatio.getRB(getView(),R.id.ratio_3).setChecked(true);
                mRatio.uncheckOtherRBs(mRatio.getRB(getView(),R.id.ratio_3));

                break;
            case 0x35:
                mInput.getRB(getView(),R.id.input_mt).setChecked(true);
                mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_mt));
                ratio_title.setVisibility(View.VISIBLE);
                mRatio.setVisible(true);
                getActivity().findViewById(android.R.id.content).invalidate();
                mRatio.getRB(getView(),R.id.ratio_5).setChecked(true);
                mRatio.uncheckOtherRBs(mRatio.getRB(getView(),R.id.ratio_5));

                break;
            case 0x37:
                mInput.getRB(getView(),R.id.input_mt).setChecked(true);
                mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_mt));
                ratio_title.setVisibility(View.VISIBLE);
                mRatio.setVisible(true);
                getActivity().findViewById(android.R.id.content).invalidate();
                mRatio.getRB(getView(),R.id.ratio_9).setChecked(true);
                mRatio.uncheckOtherRBs(mRatio.getRB(getView(),R.id.ratio_9));

                break;
            case 0x71:
                mInput.getRB(getView(),R.id.input_ma).setChecked(true);
                mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_ma));
                ratio_title.setVisibility(View.VISIBLE);
                mRatio.setVisible(true);
                getActivity().findViewById(android.R.id.content).invalidate();
                mRatio.getRB(getView(),R.id.ratio_2).setChecked(true);
                mRatio.uncheckOtherRBs(mRatio.getRB(getView(),R.id.ratio_2));
                break;
            case 0x73:
                mInput.getRB(getView(),R.id.input_ma).setChecked(true);
                mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_ma));
                ratio_title.setVisibility(View.VISIBLE);
                mRatio.setVisible(true);
                getActivity().findViewById(android.R.id.content).invalidate();
                mRatio.getRB(getView(),R.id.ratio_3).setChecked(true);
                mRatio.uncheckOtherRBs(mRatio.getRB(getView(),R.id.ratio_3));

                break;
            case 0x75:
                mInput.getRB(getView(),R.id.input_ma).setChecked(true);
                mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_ma));
                ratio_title.setVisibility(View.VISIBLE);
                mRatio.setVisible(true);
                getActivity().findViewById(android.R.id.content).invalidate();
                mRatio.getRB(getView(),R.id.ratio_5).setChecked(true);
                mRatio.uncheckOtherRBs(mRatio.getRB(getView(),R.id.ratio_5));

                break;
            case 0x77:
                mInput.getRB(getView(),R.id.input_ma).setChecked(true);
                mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_ma));
                ratio_title.setVisibility(View.VISIBLE);
                mRatio.setVisible(true);
                getActivity().findViewById(android.R.id.content).invalidate();
                mRatio.getRB(getView(),R.id.ratio_9).setChecked(true);
                mRatio.uncheckOtherRBs(mRatio.getRB(getView(),R.id.ratio_9));

                break;
            case 0x0E:
                mInput.getRB(getView(),R.id.input_atm).setChecked(true);
                mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_atm));
                ratio_title.setVisibility(View.GONE);
                mRatio.setVisible(false);
                getActivity().findViewById(android.R.id.content).invalidate();
                break;
            case 0x0F:
                mInput.getRB(getView(),R.id.input_bluetooth).setChecked(true);
                mInput.uncheckOtherRBs(mInput.getRB(getView(),R.id.input_bluetooth));
                ratio_title.setVisibility(View.GONE);
                mRatio.setVisible(false);
                getActivity().findViewById(android.R.id.content).invalidate();
                break;
        }
    }
    public void setMode(byte mode) {
        if (mode == (byte) 0x00) {
            mMode.getRB(getView(), R.id.mode_normal).setChecked(true);
            mMode.uncheckOtherRBs(mMode.getRB(getView(), R.id.mode_normal));
        }
        else if (mode == (byte) 0x02) {
            mMode.getRB(getView(), R.id.mode_rich).setChecked(true);
            mMode.uncheckOtherRBs(mMode.getRB(getView(), R.id.mode_rich));
        }
    }

    public void setCtone(byte ctone) {
        if (ctone == (byte) 0x01) {
            mCTone.getRB(getView(), R.id.ctone_enable).setChecked(true);
            mCTone.uncheckOtherRBs(mCTone.getRB(getView(), R.id.ctone_enable));
        } else {
            mCTone.getRB(getView(), R.id.ctone_disable).setChecked(true);
            mCTone.uncheckOtherRBs(mCTone.getRB(getView(), R.id.ctone_disable));
        }
    }

    public void setNR(byte nr) {
        if (nr == (byte) 0x01) {
            mNoise.getRB(getView(), R.id.noise_enable).setChecked(true);
            mNoise.uncheckOtherRBs(mNoise.getRB(getView(), R.id.noise_enable));
        }
        else {
            mNoise.getRB(getView(), R.id.noise_disable).setChecked(true);
            mNoise.uncheckOtherRBs(mNoise.getRB(getView(), R.id.noise_disable));
        }
    }

    public void setRatio(byte ratio){
        if (ratio == (byte) 0x00) {
            mRatio.getRB(getView(), R.id.ratio_2).setChecked(true);
            mRatio.uncheckOtherRBs(mRatio.getRB(getView(), R.id.ratio_2));
        }
        else if(ratio == (byte) 0x01) {
            mRatio.getRB(getView(), R.id.ratio_3).setChecked(true);
            mRatio.uncheckOtherRBs(mRatio.getRB(getView(), R.id.ratio_3));
        }
        else if(ratio == (byte) 0x02) {
            mRatio.getRB(getView(), R.id.ratio_5).setChecked(true);
            mRatio.uncheckOtherRBs(mRatio.getRB(getView(), R.id.ratio_5));
        }
        else {
            mRatio.getRB(getView(), R.id.ratio_9).setChecked(true);
            mRatio.uncheckOtherRBs(mRatio.getRB(getView(), R.id.ratio_9));
        }
    }

    private void setStyles() {
        float sp = 18.0f;
        if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("small")) sp = sp * 0.8f;
        else if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("large")) sp = sp * 1.2f;
        float size = sp * getResources().getDisplayMetrics().scaledDensity;
        Typeface typeface = TypefaceUtil.get(getContext());
        title.setTypeface(typeface); title.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        input_title.setTypeface(typeface); input_title.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        ctone_title.setTypeface(typeface); ctone_title.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mode_title.setTypeface(typeface); mode_title.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        noise_title.setTypeface(typeface); noise_title.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        ratio_title.setTypeface(typeface); ratio_title.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        custom.setTypeface(typeface); custom.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mProgram.setTypeface(typeface); mProgram.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }
    public class GRadioGroup {

        List<RadioButton> radios = new ArrayList<RadioButton>();
        String name;

        /**
         * Constructor, which allows you to pass number of RadioButtons
         * represented by resource IDs, making a group.
         *
         * @param activity
         *            Current View (or Activity) to which those RadioButtons
         *            belong.
         * @param radiosIDs
         *            One RadioButton or more.
         */
        public GRadioGroup(View activity, int... radiosIDs) {
            super();
            for (int radioButtonID : radiosIDs) {
                RadioButton rb = (RadioButton)activity.findViewById(radioButtonID);
                if (rb != null) {
                    this.radios.add(rb);
                    setRbStyle(rb);
                    rb.setOnClickListener(onClick);
                }
            }
        }

        /**
         * This occurs everytime when one of RadioButtons is clicked,
         * and deselects all others in the group.
         */
        OnClickListener onClick = new OnClickListener() {

            @Override
            public void onClick(View v) {

                RadioButton rb = (RadioButton) v;
                rb.setChecked(true);

                mListener.onButtonChange(name, radios.indexOf(rb));
                for (RadioButton rg: radios){
                    if (!rg.equals(rb) && rg.isChecked())
                        rg.setChecked(false);
                }
            }

        };

        public void uncheckOtherRBs(RadioButton checkedRB){
            for(RadioButton rb: radios){
                if(!rb.equals(checkedRB) && rb.isChecked())
                    rb.setChecked(false);
            }
        }

        public RadioButton getRB(View activity, int radioId){
            RadioButton rb = (RadioButton)activity.findViewById(radioId);
            return rb;
        }

        public void setName(String name){
            this.name = name;
        }

        public void setClickable(boolean clickable){
            for(RadioButton rb: radios){
                rb.setClickable(clickable);
            }
        }

        private void setRbStyle(RadioButton rb){
            float sp = 18.0f;
            if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("small")) sp = sp * 0.8f;
            else if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("large")) sp = sp * 1.2f;
            float size = sp * getResources().getDisplayMetrics().scaledDensity;
            Typeface typeface = TypefaceUtil.get(getContext());
            rb.setTypeface(typeface);
            rb.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }


        private void setVisible(boolean visible){
            for(RadioButton rb: radios){
                if(visible)
                    rb.setVisibility(View.VISIBLE);
                else
                    rb.setVisibility(View.GONE);
            }
        }
    }






}
