package com.nurotron.ble_ui;

/**
 * Created by tyeh on 5/2/17.
 */

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Utility Class to Load in Predefined Fonts
 */
public class TypefaceUtil {

    /**
     * Typeface Univers and Styles:
     */
    public enum Univers {
        LIGHT("universltstd-lightcnt.ttf");
        private String path;
        Univers(String path) {
            this.path = path;
        }
    }


    private static final Map<Univers, Typeface> cache = Collections.synchronizedMap(new EnumMap<Univers, Typeface>(Univers.class));


    /**
     * Fetch the Typeface instance.
     * @param ctx context for fetching resources
     * @return Typeface instance
     */
    public static Typeface get(final Context ctx) {
        final Univers name = Univers.LIGHT;
        if (!cache.containsKey(name)) {
            AssetManager mgr = ctx.getAssets();
            cache.put(name, Typeface.createFromAsset(mgr, name.path));
        }
        return cache.get(name);
    }
}
