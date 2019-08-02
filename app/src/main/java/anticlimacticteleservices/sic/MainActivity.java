package anticlimacticteleservices.sic;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.text.HtmlCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.text.Spanned;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    public Context dirtyHack = this;

    FragmentManager manager;
    Fragment fragment;
    FragmentTransaction transaction;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener;
    public static UserData masterData;

    private BottomNavigationView navView;
    //hack needed to work with androids hack of a joke of a permission system
    private static final int PERMISSION_REQUEST_CODE = 1;
    int feedLinkCount=0;

    public List<Video> videoFeed = new ArrayList<>();
    final SimpleDateFormat bdf = new SimpleDateFormat("MMM dd, yyyy");
    final SimpleDateFormat ydf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    public static SharedPreferences preferences;
 //   Fragment vfragment = new VideoFragment();
 //   Fragment cfragment = new ChannelFragment();
 //   Fragment sfragment = new SearchFragment();

    {
        mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
  //              FragmentManager fragrentManager;
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        //setTitle("Video Feed");
                        getSupportActionBar().hide();
                        new ChannelUpdate().execute();
                        fragment = new VideoFragment();
                        ((VideoFragment) fragment).setVideos(masterData.getVideos());
                        manager = getSupportFragmentManager();
                        transaction = manager.beginTransaction();
                        transaction.replace(R.id.fragment, fragment);
                        transaction.commit();

                        return true;
                    case R.id.navigation_history:
                        getSupportActionBar().show();
                        setTitle("Not implemented yet");
                        MainActivity.masterData.setForceRefresh(true);
                        new ChannelUpdate().execute();


                        return false;
                    case R.id.navigation_channels:
                        getSupportActionBar().hide();
                        masterData.getChannels();
                        System.out.println(masterData.getChannels().size()+"  "+ masterData.getChannels().size());
                        fragment = new ChannelFragment();
                        ((ChannelFragment) fragment).setChannels(masterData.getChannels());
                        manager = getSupportFragmentManager();
                        masterData.setFragmentManager(manager);
                        transaction = manager.beginTransaction();
                        transaction.replace(R.id.fragment, fragment);
                        transaction.commit();

                        ((ChannelFragment) fragment).setChannels(masterData.getChannels());
                        return true;


                    case R.id.navigation_discover:
                        getSupportActionBar().hide();
                        setTitle("under construction");

                        fragment = new SearchFragment();
//                        ((SearchFragment) fragment).setChannels(channels);
                        MainActivity.masterData.fragment=fragment;
                        manager = getSupportFragmentManager();
                        transaction = manager.beginTransaction();
                        transaction.replace(R.id.fragment, fragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                        return true;

                    case R.id.navigation_settings:
                        getSupportActionBar().hide();
                        //setTitle("settings");
                        fragment = new SettingsFragment();
                        manager = getSupportFragmentManager();
                        transaction = manager.beginTransaction();
                        transaction.replace(R.id.fragment, fragment);
                        transaction.commit();
                        System.out.println("done transacting");
                        return true;
                }
                return false;
            }
        };
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("started oncreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        System.out.println("set the content view");
        navView = findViewById(R.id.nav_view);

        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        getSupportActionBar().show();
        setTitle("Loading video feed");
        preferences = getSharedPreferences( getPackageName() + "_preferences", MODE_PRIVATE);
        masterData = new UserData(getApplicationContext());
        if (masterData.getVideos().isEmpty()){
            System.out.println("no videos");

            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.videoprop);
            dialog.setTitle("new user");

            TextView message = dialog.findViewById(R.id.channelDetails);

            message.setText("Looks like this is your first time\n You can use the search feature to find channels,\n or import channels from the settings page");
          //  message.loadData(,"html","utf-8");
            ImageView image = dialog.findViewById(R.id.thumbNailView);
            image.setImageResource(R.mipmap.sic_round);
            Button dialogButton = dialog.findViewById(R.id.closeButton);
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.show();
            getSupportActionBar().show();
            setTitle("settings");
            fragment = new SettingsFragment();
            manager = getSupportFragmentManager();
            transaction = manager.beginTransaction();
            transaction.replace(R.id.fragment, fragment);
            transaction.commit();
        }
        else {
            new ChannelUpdate().execute();
            getSupportActionBar().hide();
            fragment = new VideoFragment();
            ((VideoFragment) fragment).setVideos(masterData.getVideos());
            manager = getSupportFragmentManager();
            transaction = manager.beginTransaction();
            transaction.replace(R.id.fragment, fragment);
            transaction.commit();
        }
        getSupportActionBar().hide();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_REQUEST_CODE)        {
            int grantResultsLength = grantResults.length;
            if(grantResultsLength > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                System.out.println("You did it finally");
            }else {
                System.out.println("You denied write external storage permission.");
            }
        }
    }
    @Override
    public void onPause() {
        //need to add a dirty data switch to skip saving if unneeded
        super.onPause();
        masterData.saveUserData();
    }
    public void setMainTitle(String t){
        getSupportActionBar().show();
        setTitle(t);
    }
    public void hideMainTitle(){
        getSupportActionBar().hide();
    }
}
