package com.example.naziur.weatherapp;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Weather>>{

    protected LocationRequest mLocationRequest;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final int REQUEST_CODE = 1;
    protected LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    protected final static String KEY_LOCATION = "LOCATION";
    protected Location mCurrentLocation;
    protected Boolean mRequestingLocationUpdates;

    private static final int WEATHER_LOADER_ID = 1;
    private static final String IMG_URL = "http://www.weatherunlocked.com/Images/icons/1/";
    private WeatherAdapter forcastAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRequestingLocationUpdates = false;

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    System.out.println("no locationResult");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    mCurrentLocation = location;
                    getWeatherData();
                }
            };
        };
        updateValuesFromBundle(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        createLocationRequest();
        buildLocationSettingsRequest();
        loadWeatherData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    protected void checkLocationSettings() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(this).checkLocationSettings(mLocationSettingsRequest);
        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    task.getResult(ApiException.class);
                    startLocationUpdates();
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            ResolvableApiException resolvable = (ResolvableApiException) exception;
                            try {
                                resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                e.printStackTrace();
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            exception.printStackTrace();
                            break;
                    }
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState){
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
                getWeatherData();
            }
        }
    }

    private void stopLocationUpdates() {
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mRequestingLocationUpdates = false;
            }
        });
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        System.out.println("Location resolution completed with CANCELLATION");
                        break;
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE :
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkLocationSettings();
                } else {
                    finish();
                    Toast.makeText(this, "You must enable location permission to access the weather" ,Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
       LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                mCurrentLocation = task.getResult();
            }
        });

        if(mCurrentLocation == null) {
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback,
                    null
            ).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        mRequestingLocationUpdates = true;
                        System.out.println("Location is now available for use ");
                    } else {
                        System.out.println("Location is not available for use ");
                    }
                }
            });
        }

    }

    private void getWeatherData(){
        getLocationInformation();
        initaliseLoader();
    }

    private boolean getLocationInformation(){

        try {
            Geocoder coder = new Geocoder(getApplicationContext(), Locale.getDefault());

            if (mCurrentLocation != null) {
                List<Address> addresses = coder.getFromLocation(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 1);
                if (!addresses.isEmpty()) {
                    setLocationHeaderInformation(addresses);
                    return true;
                }
            }
        } catch (SecurityException s){
            System.out.println("SecurityException Permission denied");
        } catch (IOException io){
            System.out.println("IOException Permission denied");
        }
        return false;
    }

    private void setLocationHeaderInformation (List<Address> addresses) {
        TextView countryCode = (TextView) findViewById(R.id.weather_country);
        TextView city = (TextView) findViewById(R.id.weather_city);
        if (addresses != null) {
            if(addresses.get(0).getLocality().length() >= 25)
                city.setTextSize(14);

            countryCode.setText(addresses.get(0).getCountryCode());
            city.setText(addresses.get(0).getLocality());
        }
    }

    private void initaliseLoader(){

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            LoaderManager loaderManager = getLoaderManager();

            loaderManager.initLoader(WEATHER_LOADER_ID, null, this);
        } else {
            Toast.makeText(this, "Your device needs internet access for this operation", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWeatherData () {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            checkLocationSettings();
        } else {
            AlertDialog.Builder dialog = createDialog(getResources().getString(R.string.need_wifi), false);

            dialog.setPositiveButton(
                    getResources().getString(R.string.enable_wifi),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    });

            dialog.setNegativeButton(
                    getResources().getString(R.string.Cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                            dialog.cancel();
                        }
                    });

            dialog.show();
        }
    }

    private AlertDialog.Builder createDialog (String msg, boolean cancelable) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        builder1.setMessage(msg);
        builder1.setCancelable(cancelable);
        return builder1;
    }

    @Override
    public Loader<List<Weather>> onCreateLoader(int i, Bundle bundle) {
        Uri baseUri = Uri.parse("https://api.weatherunlocked.com/api/trigger/" + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude() + "/forecast%20tomorrow%20temperature%20gt%2016%20include7dayforecast?app_id=your_app_id&app_key=your_api_key");
        return new WeatherLoader(this, baseUri.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<Weather>> loader, List<Weather> data) {
        RelativeLayout weatherBck = (RelativeLayout) findViewById(R.id.current_weather_bck);
        weatherBck.setBackground(ResourcesCompat.getDrawable(getResources(), setWeatherBack(data.get(0).getDescriptionCode()), null));
        // setting current weather from first item in the list
        TextView date = (TextView) findViewById(R.id.weather_date);
        date.setText(data.get(0).getDate());

        TextView day = (TextView) findViewById(R.id.weather_day);
        day.setText(data.get(0).getDay());

        TextView description = (TextView) findViewById(R.id.weather_description);
        description.setText(data.get(0).getDescription() + " " + data.get(0).getMinTemp() + "\u00B0" + " - " + data.get(0).getMaxTemp() + "\u00B0");

        ImageView currentDayIcon = (ImageView) findViewById(R.id.weather_icon);
        Glide.with(this).load("http://www.weatherunlocked.com/Images/icons/1/" + data.get(0).getImgUrl()).into(currentDayIcon);

        TextView temp = (TextView) findViewById(R.id.weather_temp);
        temp.setText(data.get(0).getTemp()+ "\u00B0");
        temp.setVisibility(View.VISIBLE);

        // setting 7-days forcast data
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        forcastAdapter = new WeatherAdapter(this, data);

        RecyclerView forcastView = (RecyclerView) findViewById(R.id.weather_forcast_list);
        forcastView.setLayoutManager(layoutManager);
        forcastView.setAdapter(forcastAdapter);
    }

    @Override
    public void onLoaderReset(Loader<List<Weather>> loader) {

    }

    private int setWeatherBack (int weatherCode) {
        switch (weatherCode) {
            // sunny
            case  0 :
                return R.drawable.weather_bck_sunny_clr;
            // cloudy
            case 1 : case 2 : case 3 : case 21 : case 22 : case 23 : case 24 : case 29 :
                return R.drawable.weather_bck_cloudy;
            // rain
            case 50 : case 51 : case 56 : case 57 : case 60 :  case 61 :  case 62 :  case 63 :  case 64 :  case 65 :  case 66 :  case 67 :
            case 80 :   case 81 :   case 82 :
                return R.drawable.weather_bck_rain;
            // snow/sleet
            case 38 : case 39 : case 68 : case 69 : case 70 :  case 71 :  case 72 :  case 73 :  case 75 :  case 79 : case 83 :
            case 84 :  case 85 :  case 86 :  case 87 :  case 88 :
                return R.drawable.weather_bck_snw_clr;
            // mist/fog
            case 45 : case 49 :
                return R.drawable.weather_bck_mist;
            // storm
            case 91 : case 92 : case 93 : case 94 :
                return R.drawable.weather_bck_storm;
            default: return R.drawable.weather_bck_cloudy;
        }
    }

    private class WeatherAdapter extends  RecyclerView.Adapter<WeatherAdapter.ViewHolder> {
        private List<Weather> allForcast;
        private Context context;
        public WeatherAdapter(Context context, List<Weather> weathers) {
            allForcast = weathers;
            this.context = context;
        }

        @Override
        public WeatherAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            View forcastView = inflater.inflate(R.layout.forecast_list_item, parent, false);

            ViewHolder viewHolder = new ViewHolder(forcastView);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(WeatherAdapter.ViewHolder holder, int position) {
            Weather weather = allForcast.get(position);
            holder.forcastDay.setText(weather.getDay());
            Glide.with(MainActivity.this).load(IMG_URL +weather.getImgUrl()).into(holder.forcastIcon);
            holder.forcastTemps.setText(weather.getMinTemp() + "\u00B0" + " - " + weather.getMaxTemp()+ "\u00B0");
        }

        @Override
        public int getItemCount() {
            return allForcast.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            private TextView forcastDay;
            private ImageView forcastIcon;
            private TextView forcastTemps;

            public ViewHolder(View itemView) {
                super(itemView);
                forcastDay = (TextView) itemView.findViewById(R.id.forcast_day);
                forcastIcon = (ImageView) itemView.findViewById(R.id.forcast_icon);
                forcastTemps = (TextView) itemView.findViewById(R.id.forcast_min_max);
            }
        }

    }
}
