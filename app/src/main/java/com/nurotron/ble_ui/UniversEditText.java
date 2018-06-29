package com.nurotron.ble_ui;

/**
 * Created by tyeh on 5/2/17.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.EditText;

import com.nurotron.ble_ui.TypefaceUtil;

public class UniversEditText extends EditText {

    public UniversEditText(Context context) {
        super(context);
        init(context);
    }

    public UniversEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public UniversEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) public UniversEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        //this.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        if (!isInEditMode()) {
            setTypeface(TypefaceUtil.get(context));
        }
    }
}
