/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import ca.hss.heatmaplib.HeatMap;
import ca.hss.heatmaplib.HeatMap.DataPoint;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.network.OkHttpClientFactory;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.base.BaseSecurityTokenActivity;
import org.sufficientlysecure.keychain.ui.token.ManageSecurityTokenFragment;


public class CreateSecurityTokenWaitFragment extends Fragment {

    public static boolean sDisableFragmentAnimations = false;

    CreateKeyActivity mCreateKeyActivity;
    View mBackButton;
    HeatMap heatmap;

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (this.getActivity() instanceof BaseSecurityTokenActivity) {
            ((BaseSecurityTokenActivity) this.getActivity()).checkDeviceConnection();
        }

        setHasOptionsMenu(BuildConfig.DEBUG);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.token_debug, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_token_debug_uri:
                mCreateKeyActivity.loadFragment(ManageSecurityTokenFragment.newInstance(
                        SecurityTokenInfo.newInstanceDebugUri()), FragAction.TO_RIGHT);
                break;
            case R.id.menu_token_debug_keyserver:
                mCreateKeyActivity.loadFragment(ManageSecurityTokenFragment.newInstance(
                        SecurityTokenInfo.newInstanceDebugKeyserver()), FragAction.TO_RIGHT);
                break;
            case R.id.menu_token_debug_locked:
                mCreateKeyActivity.loadFragment(ManageSecurityTokenFragment.newInstance(
                        SecurityTokenInfo.newInstanceDebugLocked()), FragAction.TO_RIGHT);
                break;
            case R.id.menu_token_debug_locked_hard:
                mCreateKeyActivity.loadFragment(ManageSecurityTokenFragment.newInstance(
                        SecurityTokenInfo.newInstanceDebugLockedHard()), FragAction.TO_RIGHT);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_security_token_wait_fragment, container, false);

        mBackButton = view.findViewById(R.id.create_key_back_button);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCreateKeyActivity.loadFragment(null, FragAction.TO_LEFT);
            }
        });

        heatmap = (HeatMap) view.findViewById(R.id.heatmap);

        initHeatmap();

        return view;
    }

    private void initHeatmap() {
        new AsyncTask<Void,Void,List<DataPoint>>() {

            @Override
            protected List<DataPoint> doInBackground(Void... params) {
                Request request = new Builder()
                        .url("http://sweetspot.nfcring.com/api/v1/sweetspot?model=" + Build.MODEL)
                        .addHeader("User-Agent", "OpenKeychain")
                        .build();
                try {
                    ArrayList<DataPoint> dataPoints = new ArrayList<>();

                    Response response = OkHttpClientFactory.getSimpleClient().newCall(request).execute();
                    JSONTokener tokener = new JSONTokener(response.body().string().substring(1));
                    String jsonObject = tokener.nextString('"');
                    JSONArray array = new JSONArray(jsonObject);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject point = array.getJSONObject(i);

                        float xPos = point.getInt("x") / (float) point.getInt("maxX");
                        float yPos = point.getInt("y") / (float) point.getInt("maxY");

                        dataPoints.add(new DataPoint(xPos, yPos, 30.0));
                    }

                    return dataPoints;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(List<DataPoint> dataPointList) {
                Map<Float, Integer> colors = new ArrayMap<>();
                //build a color gradient in HSV from red at the center to green at the outside
                for (int i = 0; i < 21; i++) {
                    float stop = ((float)i) / 20.0f;
                    int color = doGradient(i * 5, 0, 100, 0xff00ff00, 0xffff0000);
                    colors.put(stop, color);
                }
                heatmap.setColorStops(colors);

                heatmap.setMinimum(0.0);
                heatmap.setMaximum(100.0);
                for (DataPoint dataPoint : dataPointList) {
                    heatmap.addData(dataPoint);
                }
                heatmap.forceRefresh();
            }
        }.execute();
    }


    private static int doGradient(double value, double min, double max, int min_color, int max_color) {
        if (value >= max) {
            return max_color;
        }
        if (value <= min) {
            return min_color;
        }
        float[] hsvmin = new float[3];
        float[] hsvmax = new float[3];
        float frac = (float)((value - min) / (max - min));
        Color.RGBToHSV(Color.red(min_color), Color.green(min_color), Color.blue(min_color), hsvmin);
        Color.RGBToHSV(Color.red(max_color), Color.green(max_color), Color.blue(max_color), hsvmax);
        float[] retval = new float[3];
        for (int i = 0; i < 3; i++) {
            retval[i] = interpolate(hsvmin[i], hsvmax[i], frac);
        }
        return Color.HSVToColor(retval);
    }

    private static float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    /**
     * hack from http://stackoverflow.com/a/11253987
     */
    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (sDisableFragmentAnimations) {
            Animation a = new Animation() {};
            a.setDuration(0);
            return a;
        }
        return super.onCreateAnimation(transit, enter, nextAnim);
    }

}
