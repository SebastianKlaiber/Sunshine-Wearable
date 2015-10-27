package com.example.android.sunshine.app;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;


public class WearService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = WearService.class.getSimpleName();

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };

    private static final String PATH = "/weather";
    private static final String KEY_WEATHER_ID = "key_weather_id";
    private static final String KEY_MAX_TEMP = "key_max_temp";
    private static final String KEY_MIN_TEMP = "key_min_temp";

    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;

    private GoogleApiClient mGoogleApiClient;

    public WearService() {
        super("");
}

    @Override
    protected void onHandleIntent(Intent intent) {
        mGoogleApiClient  = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH);

        String locationQuery = Utility.getPreferredLocation(getApplicationContext());

        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

        // we'll query our contentProvider, as always
        Cursor cursor = getApplicationContext().getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

        if (cursor.moveToFirst()) {
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            double high = cursor.getDouble(INDEX_MAX_TEMP);
            double low = cursor.getDouble(INDEX_MIN_TEMP);

            putDataMapRequest.getDataMap().putInt(KEY_WEATHER_ID, weatherId);
            putDataMapRequest.getDataMap().putDouble(KEY_MAX_TEMP, high);
            putDataMapRequest.getDataMap().putDouble(KEY_MIN_TEMP, low);

            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.d(TAG, "Failed to send item");
                            } else {
                                Log.d(TAG, "Successfully send item");
                            }
                        }
                    });
        }

        cursor.close();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, connectionResult.toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }
}
