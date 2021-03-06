package com.example.roman.test;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.roman.test.data.ChatMessage;
import com.example.roman.test.data.Message;
import com.example.roman.test.data.MessagesTable;
import com.example.roman.test.data.Order;
import com.example.roman.test.data.Sector;
import com.example.roman.test.services.SocketService;
import com.example.roman.test.utilities.Constants;
import com.example.roman.test.utilities.Functions;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.example.roman.test.LoginActivity.attemptLogin;
import static com.example.roman.test.utilities.Constants.DEFAULT;
import static com.example.roman.test.utilities.Constants.ERROR;
import static com.example.roman.test.utilities.Constants.ERROR_NONE;
import static com.example.roman.test.utilities.Constants.ERROR_NOT_CONFIRMED;
import static com.example.roman.test.utilities.Constants.ERROR_ORDER_NOT_FOUND;
import static com.example.roman.test.utilities.Constants.ERROR_ORDER_TAKEN;
import static com.example.roman.test.utilities.Constants.METHOD_GET_CURRENT_SECTOR;
import static com.example.roman.test.utilities.Constants.METHOD_GET_NEW_STATUS;
import static com.example.roman.test.utilities.Constants.METHOD_GET_ORDERS;
import static com.example.roman.test.utilities.Constants.METHOD_GET_ORDER_BY_ID;
import static com.example.roman.test.utilities.Constants.METHOD_NEW_ORDER;
import static com.example.roman.test.utilities.Constants.METHOD_SET_ORDER;
import static com.example.roman.test.utilities.Constants.METHOD_SET_ORDER_STATUS;
import static com.example.roman.test.utilities.Constants.ORDER_ID;
import static com.example.roman.test.utilities.Constants.ORDER_STATUS_TAKE;
import static com.example.roman.test.utilities.Constants.RESPONSE;
import static com.example.roman.test.utilities.Constants.SECTOR_ID;
import static com.example.roman.test.utilities.Constants.STATUS_ARRAY;
import static com.example.roman.test.utilities.Constants.STATUS_ID;
import static com.example.roman.test.utilities.Functions.getSectorList;
import static com.example.roman.test.utilities.Functions.getStreetFromLocation;
import static com.example.roman.test.utilities.Functions.isActive;
import static com.example.roman.test.utilities.Functions.isBetterLocation;
import static com.example.roman.test.utilities.Functions.saveToPreferences;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        NetworkChangeReceiver.SnackInterface {

    public static final int REQUEST_LANGUAGE = 2;
    public static final int RESULT_SUCCESS = 1;
    private static final int REQUEST_LOCATION = 1;
    private static final int MAIN = 0;
    private static final int AIR = 1;
    private static final int ORDER = 2;
    private static boolean backPressed = false;
    private static boolean mainFragment = true;

    private Menu mMenu;
    private Location mCurrentBestLocation;
    private NetworkChangeReceiver mNetworkReceiver;
    private SocketServiceReceiver mReceiver;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private ActionBarDrawerToggle mDrawerToggle;

    private int mTabPosition = ListView.INVALID_POSITION;

    @Inject Gson gson;
    @Inject SharedPreferences prefs;

    private AirFragment mAirFragment;
    private MainFragment mMainFragment;
    private OrderFragment mOrderFragment;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.view_pager)
    ViewPager mViewPager;

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawer;

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((TaxiApp) getApplication()).getNetComponent().inject(this);
        Functions.setWholeTheme(this, prefs);
        Functions.setLanguage(this, prefs);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar,
                R.string.nav_drawer_open,
                R.string.nav_drawer_close) {
        };

        mDrawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        // TODO save checked item
        navigationView.setCheckedItem(R.id.nav_main);

        MyPageAdapter mPageAdapter = new MyPageAdapter(getSupportFragmentManager(), getFragments());

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                mTabPosition = position;
            }

            @Override
            public void onPageSelected(int position) {
                mTabPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        if (mTabPosition == ListView.INVALID_POSITION) {
            mTabPosition = AIR;
        }

        mViewPager.setAdapter(mPageAdapter);
        mViewPager.setCurrentItem(mTabPosition);
        mAirFragment = (AirFragment) mPageAdapter.getRegisteredFragment(AIR);
        mMainFragment = (MainFragment) mPageAdapter.getRegisteredFragment(MAIN);
        tabLayout.setupWithViewPager(mViewPager);

        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (isBetterLocation(location, mCurrentBestLocation)) {
                    mCurrentBestLocation = location;
                    String address = getStreetFromLocation(MainActivity.this, location);
                    if (mMenu != null) {
                        mMenu.findItem(R.id.address).setTitle(address);
                    }
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) { }

            @Override
            public void onProviderEnabled(String s) { }

            @Override
            public void onProviderDisabled(String s) { }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }

        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, mLocationListener);

        if (mReceiver == null) {
            mReceiver = new SocketServiceReceiver();
            IntentFilter intentFilter = new IntentFilter(Constants.MAIN_INTENT);
            registerReceiver(mReceiver, intentFilter);
        }

        if (mNetworkReceiver == null) {
            mNetworkReceiver = new NetworkChangeReceiver();
            IntentFilter intentNetworkFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(mNetworkReceiver, intentNetworkFilter);
        }

        if (savedInstanceState == null) {
            new StartUpTask().execute();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LANGUAGE) {
            Functions.recreate(this);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
        getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
        setContentView(R.layout.activity_main);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar,
                R.string.nav_drawer_open,
                R.string.nav_drawer_close) {
        };
    }

    @Override
    protected void onDestroy() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            mLocationManager.removeUpdates(mLocationListener);
        }

        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        if (mNetworkReceiver != null) {
            unregisterReceiver(mNetworkReceiver);
            mNetworkReceiver = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (!mainFragment) {
            onNavigationItemSelected(navigationView.getMenu().getItem(0));
        } else if (mTabPosition != AIR) {
            mViewPager.setCurrentItem(AIR);
        } else {
            if (backPressed) {
                exit();
            }

            backPressed = true;
            Toast.makeText(MainActivity.this,
                    getString(R.string.exit_toast), Toast.LENGTH_SHORT)
                    .show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    backPressed = false;
                }}, 2000);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (!drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.openDrawer(GravityCompat.START);
            }
            return true;
        }

        return super.onKeyDown(keyCode, e);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMenu = menu;
        getMenuInflater().inflate(R.menu.main_action_bar, menu);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return true;
        Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location == null) {
            location = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }

        if (location != null) {
            String street = getStreetFromLocation(this, location);
            mMenu.findItem(R.id.address).setTitle(street);
            mMenu.findItem(R.id.address).setTitleCondensed(street.split(",")[0]);
        }

        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id != R.id.nav_main) {
            tabLayout.setVisibility(GONE);
            mViewPager.setVisibility(GONE);
            findViewById(R.id.flContent).setVisibility(VISIBLE);
            mainFragment = false;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();

        switch(id) {
            case R.id.nav_main:
                tabLayout.setVisibility(VISIBLE);
                mViewPager.setVisibility(VISIBLE);
                findViewById(R.id.flContent).setVisibility(GONE);
                mViewPager.setCurrentItem(mTabPosition);
                mainFragment = true;
                break;
            case R.id.nav_my_orders:
                fragmentManager.beginTransaction()
                        .replace(R.id.flContent, OrderFragment.newInstance())
                        .commit();
                break;
            case R.id.nav_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_LANGUAGE);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                break;
            case R.id.nav_messages:
                fragmentManager.beginTransaction()
                        .replace(R.id.flContent, MessageFragment.newInstance())
                        .commit();
                break;
            case R.id.nav_alarm:
                item.setChecked(false);
                showAlert();
                backToMain();
                return false;
            case R.id.nav_exit:
                try {
                    SocketService.getInstance().logout();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                showExitDialog();
                break;
            case R.id.nav_gps_map:
            case R.id.nav_meter:
            case R.id.nav_add_functions:
                backToMain();
                return false;
        }

        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void reconnect(boolean isConnected) {
        View view = findViewById(android.R.id.content);
//        final MediaPlayer mp = MediaPlayer.create(MainActivity.this, R.raw.reconnect);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 2000, 100};

        if (isConnected) {
            final Snackbar snackBar = Snackbar.make(view,
                    getString(R.string.internet_came_back),
                    Snackbar.LENGTH_INDEFINITE);
            v.cancel();
//            mp.setLooping(false);
//            mp.stop();
            snackBar.setAction(getString(R.string.internet_online), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    attemptLogin(MainActivity.this);
                    snackBar.dismiss();
                    recreate();
                }
            });
            snackBar.show();
        } else {
//            mp.setLooping(true);
//            mp.start();
            v.vibrate(pattern, 0);
            Snackbar.make(view, getString(R.string.internet_gone), Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)     {
        // The action bar home/up action should open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class MyPageAdapter extends FragmentStatePagerAdapter {
        private String tabTitles[] = new String[] { getString(R.string.main_main),
                getString(R.string.main_air),
                getString(R.string.main_order)};
        private final List<Fragment> fragments;

        MyPageAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            this.fragments = fragments;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles[position];
        }

        Fragment getRegisteredFragment(int position) {
            return fragments.get(position);
        }
    }

    private List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<>();

        mAirFragment = AirFragment.newInstance();
        mMainFragment = MainFragment.newInstance();
        mOrderFragment = OrderFragment.newInstance();

        fList.add(mMainFragment);
        fList.add(mAirFragment);
        fList.add(mOrderFragment);

        return fList;
    }

    private void showAlert() {
        AlertDialog.Builder helpBuilder = Functions.getDialog(this,
                getString(R.string.alert_fire),
                getString(R.string.alert_message));

        helpBuilder.setPositiveButton(getString(R.string.alert_fire),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            SocketService.getInstance().alert();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).setNegativeButton(getString(R.string.alert_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).create().show();
    }

    public void showMessage(final Message message) {
        Bundle bundle = new Bundle();
        bundle.putString("MSG", message.getMessage());
        MyDialogFragment dialogFragment = new MyDialogFragment();
        dialogFragment.setArguments(bundle);
        dialogFragment.show(getSupportFragmentManager(), "tag");
    }

    public static class MyDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle bundle = getArguments();
            String text = bundle.getString("MSG");

            return new AlertDialog.Builder(getActivity())
                    .setTitle("New message")
                    .setMessage(text)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).create();
        }
    }

    private void newMessage(Message message) {
        boolean isActive = isActive(this);

        getContentResolver().insert(MessagesTable.CONTENT_URI,
                MessagesTable.getContentValues(new ChatMessage(message, false), false));

        if (isActive) {
            showMessage(message);
        } else {
            Functions.showMessageNotification(this, message);
        }
    }

    @Override
    public void recreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.recreate();
        } else {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    public void showSectorInfo() {
        List<Sector> sectors = getSectorList(this);
        StringBuilder textMessage = new StringBuilder("");

        for (Sector sector : sectors) {
            if (sector.getDrivers() != 0) {
                textMessage
                        .append(sector.getName())
                        .append(": ")
                        .append(sector.getDrivers())
                        .append("\n");
            }
        }

        newMessage(new Message("1", textMessage.toString(), "19/08/2016"));
    }

    private class SocketServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            new HandleRequestTask().execute(intent);
        }
    }

    private static class StartUpTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                SocketService.getInstance().getDriverStatus();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public class HandleRequestTask extends AsyncTask<Intent, Void, Void> {

        @Override
        protected Void doInBackground(Intent... intents) {
            Intent intent = intents[0];
            int error = intent.getIntExtra(ERROR, DEFAULT);

            switch (error) {
                case ERROR_NONE:
                    int method = intent.getIntExtra(Constants.METHOD, DEFAULT);
                    switch (method) {
                        case METHOD_SET_ORDER_STATUS:
                            final String orderId = intent.getStringExtra(ORDER_ID);
                            final String orderStatus = intent.getStringExtra(STATUS_ID);
                            if (orderStatus.equals(ORDER_STATUS_TAKE)) {
                                showToast("You have successfully taken order #" + orderId);
                                try {
                                    SocketService.getInstance().getOrderById();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;

                        case METHOD_GET_ORDERS:
                            String orderList = intent.getStringExtra(RESPONSE);
                            final Order[] orders = gson.fromJson(orderList, Order[].class);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {mAirFragment.addOrders(orders);
                                }
                            });
                            break;

                        case METHOD_GET_ORDER_BY_ID:
                            String jsonOrder = intent.getStringExtra(RESPONSE);
                            final Order newOrder = gson.fromJson(jsonOrder, Order.class);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mOrderFragment.addOrder(newOrder);
                                }
                            });
                            break;

                        case METHOD_SET_ORDER:
                            String setOrder = intent.getStringExtra(RESPONSE);
                            final Order newSetOrder = gson.fromJson(setOrder, Order.class);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
                            final String currentDate = sdf.format(new Date());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    newMessage(new Message(newSetOrder.getOrderId(), newSetOrder.getFrom(), currentDate));
                                }
                            });
                            break;

                        case METHOD_GET_CURRENT_SECTOR:
                            JSONObject currentSector = null;
                            try {
                                currentSector = new JSONObject(intent.getStringExtra(RESPONSE));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            if (currentSector != null) {
                                try {
                                    String sectorId = currentSector.getString(SECTOR_ID);
                                    saveToPreferences(sectorId, "sectorId", prefs);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            break;

                        case METHOD_NEW_ORDER:
                            final Order order = gson.fromJson(intent.getStringExtra(RESPONSE), Order.class);
                            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/dd");
                            final String currentDate1 = sdf1.format(new Date());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mAirFragment.addOrder(order);
                                    newMessage(new Message(order.getOrderId(), order.getFrom(), currentDate1));
                                }
                            });
                            break;

                        case METHOD_GET_NEW_STATUS:
                            String id = intent.getStringExtra(RESPONSE);
                            String text = prefs.getString(STATUS_ARRAY, "[]");
                            com.example.roman.test.data.Status[] statuses =
                                    gson.fromJson(text, com.example.roman.test.data.Status[].class);
                            for (final com.example.roman.test.data.Status status : statuses) {
                                if (status.getId().equals(id)) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Preference preference = mMainFragment.findPreference(getString(R.string.pref_status_key));
                                            if (preference != null) {
                                                preference.setTitle(getString(R.string.format_new_status,
                                                        status.getName()));
                                            }
                                        }
                                    });
                                    break;
                                }
                            }
                            break;

                        case Constants.METHOD_DELETE_ORDER:
                            final String delOrderId = intent.getStringExtra(RESPONSE);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mAirFragment.removeOrder(delOrderId);
                                }
                            });
                            break;

                        case Constants.METHOD_NEW_MESSAGE:
                            Message msg = gson.fromJson(intent.getStringExtra(RESPONSE), Message.class);
                            showMessage(msg);
                            break;
                    }
                    break;

                case ERROR_ORDER_NOT_FOUND:
                    showToast(getString(R.string.order_error_not_found));
                    break;

                case ERROR_ORDER_TAKEN:
                    showToast(getString(R.string.order_error_taken));
                    break;

                case ERROR_NOT_CONFIRMED:
                    showToast(getString(R.string.order_error_not_complete));
                    break;
            }
            return null;
        }

        private void showToast(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT);
                    TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                    if( v != null) v.setGravity(Gravity.CENTER);
                    toast.show();
                }
            });
        }
    }

    private void exit() {
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setMessage(getString(R.string.exit_message))
                .setPositiveButton(getString(R.string.exit_exit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exit();
//                        final MediaPlayer mp = MediaPlayer.create(MainActivity.this, R.raw.changestatusexit);
//                        mp.start();
//                        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                            @Override
//                            public void onCompletion(MediaPlayer mediaPlayer) {
//                                exit();
//                            }
//                        }
//                    );
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        TextView messageView = (TextView)dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setGravity(Gravity.CENTER);
        }
    }

    private void backToMain() {
        tabLayout.setVisibility(VISIBLE);
        mViewPager.setVisibility(VISIBLE);
        findViewById(R.id.flContent).setVisibility(GONE);
        mViewPager.setCurrentItem(mTabPosition);
        mainFragment = true;
        mDrawer.closeDrawer(GravityCompat.START);
    }
}
