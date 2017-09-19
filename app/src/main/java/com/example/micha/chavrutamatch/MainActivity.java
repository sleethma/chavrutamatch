package com.example.micha.chavrutamatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.micha.chavrutamatch.Data.HostSessionData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.micha.chavrutamatch.Data.HostSessionData;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.example.micha.chavrutamatch.AcctLogin.LoginActivity;
import com.example.micha.chavrutamatch.AcctLogin.UserDetails;
import com.example.micha.chavrutamatch.Data.HostSessionData;
import com.example.micha.chavrutamatch.Data.ServerConnect;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.PhoneNumber;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    //TODO add up nav arrow to each activity
    @BindView(R.id.iv_no_match_add_match)
    ImageView noMatchView;
    @BindView(R.id.lv_my_chavruta)
    ListView myChavrutaListView;
    OpenChavrutaAdapter mAdapter;
    static ArrayList<HostSessionData> myChavrutasArrayList;
    static Context mContext;
    private static String jsonString;
    JSONObject jsonObject;
    JSONArray jsonArray;
    String accountId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        //receives intent from ServerConnect to display myChavruta list, else gets myChavruta info from db
        if (getIntent().getExtras() != null) {
            jsonString = getIntent().getExtras().getString("myChavrutaKey");

            myChavrutasArrayList = new ArrayList<>();

            //add and remove views to display myChavrutas
            if (myChavrutasArrayList != null && !jsonString.isEmpty()) {
                //attaches data source to adapter and displays list
                mAdapter = new OpenChavrutaAdapter(this, myChavrutasArrayList);
                myChavrutaListView.setAdapter(mAdapter);
                noMatchView.setVisibility(View.GONE);
                myChavrutaListView.setVisibility(View.VISIBLE);
                //parses and adds data in JSON string from MyChavruta Server call
                parseJSONEntry();

            } else {
                //sets empty array list view
                myChavrutaListView.setVisibility(View.GONE);
                noMatchView.setVisibility(View.VISIBLE);
                //Todo: delete this and reapply to another element more revelant
                noMatchView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        animateTransition(v);
                    }
                });

            }

            //activate user details class for account kit
            final UserDetails userDetails = new UserDetails();

            //check if already logged in
            //get current account and create new anonymous inner class
            AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                @Override
                public void onSuccess(final com.facebook.accountkit.Account account) {
                    // Get Account Kit ID
                    String accountKitId = account.getId();
                    userDetails.setmUserId(accountKitId);
                    //stores user id, email, or phone in SP
                    SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.user_data_file), MODE_PRIVATE).edit();
                    editor.putString(getString(R.string.user_account_id_key), accountKitId);
                    editor.putBoolean("new_user_key", false);

                    PhoneNumber phoneNumber = account.getPhoneNumber();
                    if (account.getPhoneNumber() != null) {
                        // if the phone number is available, display it
                        String formattedPhoneNumber = formatPhoneNumber(phoneNumber.toString());
                        userDetails.setmUserPhoneNumber(formattedPhoneNumber);
                        editor.putString(getString(R.string.user_phone_key), formattedPhoneNumber);

                    } else {
                        // if the email address is available, store it
                        String emailString = account.getEmail();
                        userDetails.setmUserEmail(emailString);
                        editor.putString(getString(R.string.user_email_key), emailString);
                    }
                    editor.apply();
                }

                @Override
                public void onError(final AccountKitError error) {
                    //display error
//                String toastMessage = error.getErrorType().getMessage();
//                Toast.makeText(MainActivity.this, toastMessage, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            mContext = this;

        } else {
            //if db not yet accessed, gets all chavrutas that user has requested
            //@var sp: sets userId to UserDetails for server calls
            SharedPreferences sp = getSharedPreferences(getString(R.string.user_data_file), MODE_PRIVATE);
            accountId = sp.getString(getString(R.string.user_account_id_key),null);
            UserDetails.setmUserId(accountId);
            String getMyChavrutasKey = "my chavrutas";
            ServerConnect getMyChavrutas = new ServerConnect(this);
            getMyChavrutas.execute(getMyChavrutasKey);
        }


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddSelect.class);
                startActivity(intent);
            }
        });
    }

    //parses JSON string data to form myChavrutas ListView
    //@ var chavrutaId = autoInc from db
    public void parseJSONEntry() {
        String chavrutaId;

        String hostFirstName, hostLastName, sessionMessage, sessionDate,
                startTime, endTime, sefer, location, hostId, chavrutaRequest1, chavrutaRequest2,
                chavrutaRequest3, confirmed;
        try {

            jsonObject = new JSONObject(jsonString);
            jsonArray = jsonObject.getJSONArray("server_response");

            //loop through array and extract objects, adding them individually as setter objects,
            //and adding objects to list adapter.
            int count = 0;
            while (count < jsonArray.length()) {
                JSONObject jo = jsonArray.getJSONObject(count);
                chavrutaId = jo.getString("chavruta_id");
                hostFirstName = jo.getString("hostFirstName");
                hostLastName = jo.getString("hostLastName");
                sessionMessage = jo.getString("sessionMessage");
                sessionDate = jo.getString("sessionDate");
                startTime = jo.getString("startTime");
                endTime = jo.getString("endTime");
                sefer = jo.getString("sefer");
                location = jo.getString("location");
                hostId = jo.getString("host_id");
                chavrutaRequest1 = jo.getString("chavruta_request_1");
                chavrutaRequest2 = jo.getString("chavruta_request_2");
                chavrutaRequest3 = jo.getString("chavruta_request_3");
                confirmed = jo.getString("confirmed");


                //make user data object of UserDataSetter class
                HostSessionData myChavrutaData = new HostSessionData(chavrutaId, hostFirstName, hostLastName, sessionMessage, sessionDate,
                        startTime, endTime, sefer, location, hostId, chavrutaRequest1, chavrutaRequest2, chavrutaRequest3, confirmed);
                mAdapter.add(myChavrutaData);
                count++;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //gives context to OpenChavrutaAdapter
    public static Context getMAContextForAdapter() {
        return mContext;
    }

    private void animateTransition(View view) {
        Slide slide = new Slide();
        slide.setSlideEdge(Gravity.END);

        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        TransitionManager.beginDelayedTransition(root, slide);
        view.setVisibility(View.INVISIBLE);
    }

    //Account Activity from FB login
    public void onLogout(View view) {
        // logout of Account Kit
        AccountKit.logOut();
        launchLoginActivity();
    }

    private void launchLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private String formatPhoneNumber(String phoneNumber) {
        // helper method to format the phone number for display
        try {
            PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber pn = pnu.parse(phoneNumber, Locale.getDefault().getCountry());
            phoneNumber = pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            e.printStackTrace();
        }
        return phoneNumber;
    }
}

