package com.example.micha.chavrutamatch;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.example.micha.chavrutamatch.AcctLogin.UserDetails;
import com.example.micha.chavrutamatch.Data.AvatarImgs;
import com.example.micha.chavrutamatch.Data.ServerConnect;
import com.example.micha.chavrutamatch.Utils.ChavrutaTextValidation;
import com.example.micha.chavrutamatch.Utils.ChavrutaUtils;
import com.example.micha.chavrutamatch.Utils.GlideApp;
import com.example.micha.chavrutamatch.Utils.ImgUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by micha on 8/26/2017.
 */

public class AddBio extends AppCompatActivity {

    private final static String LOG_TAG = AddBio.class.getSimpleName();
    private final static String CUSTOM_AVATAR_NUMBER_STRING = "999";
    private final static int CUSTOM_AVATAR_NUMBER_INT = 999;

    private final int REQUEST_CODE = 100;
    static String mUserId, mUserEmail, mUserPhoneNumber, mUserName, mUserFirstName, mUserLastName,
            mUserBio, mUserAvatarNumberString, mUserCityState, jsonString;
    @BindView(R.id.et_user_phone_number)
    EditText UserPhoneView;
    @BindView(R.id.et_user_email)
    EditText UserEmailView;
    @BindView(R.id.user_name)
    EditText UserNameView;
    @BindView(R.id.user_first_name)
    EditText UserFirstNameView;
    @BindView(R.id.user_last_name)
    EditText UserLastNameView;
    @BindView(R.id.et_user_bio)
    EditText UserBioView;
    @BindView(R.id.iv_awaiting_host_avatar)
    ImageView UserAvatarView;
    @BindView(R.id.ac_city_state)
    AutoCompleteTextView autoCompleteTextView;
    @BindView(R.id.sv_add_bio)
    ScrollView scrollView;
    SharedPreferences prefs;

    //controls whether or not db update necessary
    Boolean bioDataChanged = false;
    //controls whether activity used to update or to create new account
    Boolean updateBio = false;
    // checks if user img file sent from @AvatarSelectMasterList
    Uri mNewProfImgUri = null;
    //Holds list view of possible avatars
    List<Integer> mAvatarsList = AvatarImgs.getAllAvatars();
    String mCustomUserAvatarUriString;
    String mCustomUserAvatarBase64String;

    Activity thisActivity = this;
    //new user to be stored in db
    static boolean storeNewUserInDb = false;
    //checks if avatar chosen from AvatarSelectFragment
    boolean newCustomAvatarChosen = false;
    Context context;

    @Inject
           public UserDetails userDetailsInstance;

    public AddBio(){
    }

    //TODO: Add input validation using: https://www.androidhive.info/2015/09/android-material-design-floating-labels-for-edittext/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_bio);
        ButterKnife.bind(this);

        context = this;
        (ChavrutaMatch.get(this).getApplicationComponent()).inject(this);

        mUserId = userDetailsInstance.getUserId();
        mUserAvatarNumberString = "0";
        prefs = getSharedPreferences("user_data", MODE_PRIVATE);



        //incoming intents
        if (getIntent().getExtras() != null) {
            Intent incomingIntent = getIntent();
            // check if data coming from avatar select frag & sets user selected avatar in addBio Activitiy
            if (incomingIntent.getBooleanExtra("affirm update bio", false)) {
                Bundle bundle = incomingIntent.getExtras();
                updateBio = bundle.getBoolean("affirm update bio", false);

                int userAvatarSelected = bundle.getInt("avatar position", -1);

                if (userAvatarSelected == CUSTOM_AVATAR_NUMBER_INT) {
                    mCustomUserAvatarUriString = bundle.getString("img_uri_string_key");

                    mNewProfImgUri = Uri.parse(mCustomUserAvatarUriString);
                    newCustomAvatarChosen = true;

                    if (mNewProfImgUri != null) {

                        GlideApp
                                .with(this)
                                .load(mNewProfImgUri)
                                .into(UserAvatarView);
                    }
                } else {
                    UserAvatarView.setImageResource(mAvatarsList.get(userAvatarSelected));
                }
                mUserAvatarNumberString = "" + userAvatarSelected;
                populateUserDataFromSP("pick chooser return");
            }
            //intent sent from user selecting update bio in MA
            else if (incomingIntent.getExtras().getBoolean("update_bio", false)) {
                updateBio = incomingIntent.getExtras().getBoolean("update_bio");
                populateUserDataFromSP("no new custom avatar selected");

                //if userdata not in device, db call is returned with user acct data
            } else if (incomingIntent.getExtras().getString("user_data_json_string") != null) {
                jsonString = incomingIntent.getExtras().getString("user_data_json_string");
                parseUserDetailsFromDB(jsonString);
            } else if (incomingIntent.getBooleanExtra("add new user to db", false)) {
                storeNewUserInDb = incomingIntent.getBooleanExtra("add new user to db", false);
                //@var will be used if template avatar selected
                updateBio = true;
            }
        } else {
            //if userFirstName in SP == null then user has not used current device,
            // then check db for user details, else load from Shared Preferences
            //todo: check which is being used and get info from db if necessary
            if (prefs.getString(getString(R.string.user_first_name_key), null) != null) {
                populateUserDataFromSP("no new custom avatar selected");
            } else {
                getUserBioDatafromDb();
            }
        }
        //auto moves edittext when softkeyboard called
        this.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        UserAvatarView.setOnClickListener(v -> {
            Intent intent = new Intent(getBaseContext(), AvatarSelectMasterList.class);
            intent.putExtra("update_bio", updateBio);
            startActivity(intent);
        });
        //set auto-complete for closest US city
        ChavrutaUtils cu = new ChavrutaUtils();
        String jsonFileString = cu.getJsonFileFromResource(this);
        List<String> cityList = cu.parseCityName(jsonFileString);

// Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cityList);
        autoCompleteTextView.setAdapter(adapter);

        //setUp on TextWatchers for input validation
        setUpTextWatcherValidation();

        //check to see if user consented to privacy policy
        if(!prefs.getBoolean(getString(R.string.user_priv_policy_consent),false)) {
            displayUserConsent();
        }

    }

    //stores user Biodata in db and sp
    public void storeUserBioDataInDb(View view) {
        String newUserEmail = UserEmailView.getText().toString();
        String newUserPhoneNumber = UserPhoneView.getText().toString();
        String newUserName = UserNameView.getText().toString();
        String newUserFirstName = UserFirstNameView.getText().toString();
        String newUserLastName = UserLastNameView.getText().toString();
        String newUserBio = UserBioView.getText().toString();
        String newUserAvatarNumberString = mUserAvatarNumberString;
        String newCustomUserAvatarString = mCustomUserAvatarUriString;
        String newUserCityState = autoCompleteTextView.getText().toString();
        Uri newProfImgUser = mNewProfImgUri;

        //todo: reinstate if need email functionality
//        if (!ChavrutaTextValidation.isValidEmail(newUserEmail)) {
//            Toast.makeText(this, "Please enter valid email.", Toast.LENGTH_SHORT).show();
//            return;

        if (!ChavrutaTextValidation.hasCharLimitOf(17, UserPhoneView.getText().toString())) {
            Toast.makeText(this, "Please enter valid phone #.", Toast.LENGTH_SHORT).show();
            return;
        }

        //check for changes before queing db
        dataChangeCheck(newUserBio, newUserFirstName, newUserLastName,
                newUserName, newUserPhoneNumber, newUserEmail, newUserAvatarNumberString, newProfImgUser, newUserCityState);

        if (bioDataChanged) {
            userDetailsInstance.setAllAddBioUserDataToUserDetails(newUserName, newUserAvatarNumberString,
                    newUserFirstName, newUserLastName, newUserPhoneNumber, newUserEmail, newUserCityState,
                    newCustomUserAvatarString,  newUserBio);
            if (mCustomUserAvatarBase64String != null)
                userDetailsInstance.setByteArrayFromString(mCustomUserAvatarBase64String);

            saveAddBioDataToSP();
            postUserBio();
        } else {
            Toast.makeText(this, "No Changes Made", Toast.LENGTH_SHORT).show();
        }
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    //gets user bio info from server
    public void getUserBioDatafromDb() {
        // check to see if have an account in db
        ServerConnect getUserDetailsFromDb = new ServerConnect(this, userDetailsInstance);
        getUserDetailsFromDb.execute("get UserDetails", mUserId);
    }

    //checks to see if user changed data before saving in SP and DB
    public void dataChangeCheck(String newUserBio, String newUserFirstName, String newUserLastName,
                                String newUserName, String newUserPhoneNumber, String newUserEmail,
                                String newUserAvatarNumberString, Uri newProfImgUri, String newUserCityState) {
        bioDataChanged = false;
        if (mUserBio == null || !mUserBio.equals(newUserBio)) {
            mUserBio = newUserBio;
            bioDataChanged = true;
        }
        if (mUserFirstName == null || !mUserFirstName.equals(newUserFirstName)) {
            mUserFirstName = newUserFirstName;
            bioDataChanged = true;
        }
        if (mUserLastName == null || !mUserLastName.equals(newUserLastName)) {
            mUserLastName = newUserLastName;
            bioDataChanged = true;
        }
        if (mUserName == null || !mUserName.equals(newUserName)) {
            mUserName = newUserName;
            bioDataChanged = true;
        }
        if (mUserPhoneNumber == null || !mUserPhoneNumber.equals(newUserPhoneNumber) &&
                !userDetailsInstance.userLoginType.equals("phone")) {
            mUserPhoneNumber = newUserPhoneNumber;
            bioDataChanged = true;
        }
        if (mUserEmail == null || !mUserEmail.equals(newUserEmail) &&
                !userDetailsInstance.userLoginType.equals("email")) {
            mUserEmail = newUserEmail;
            bioDataChanged = true;
        }
        if (mUserAvatarNumberString == null || !userDetailsInstance.getmUserAvatarNumberString().equals(newUserAvatarNumberString)) {
            mUserAvatarNumberString = newUserAvatarNumberString;
            bioDataChanged = true;
        }

        if (mUserCityState == null || !mUserCityState.equals(newUserCityState)) {
            mUserCityState = newUserCityState;
            bioDataChanged = true;
        }

        //todo: must check rotation differently if API< 24, check with emulators! Currently <24 just returns 0 rotation
        if (mCustomUserAvatarUriString != null &&
                newUserAvatarNumberString.equals(CUSTOM_AVATAR_NUMBER_STRING) && newCustomAvatarChosen) {
            bioDataChanged = true;
            //check to see if image needs rotating b4 saving to db
            int rotation = 0;
            try {
                rotation = ImgUtils.rotateImgNeededCk(this, newProfImgUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), newProfImgUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //if image is not upright
            if (rotation != 0) {
                bitmap = ImgUtils.rotateImg(bitmap, rotation);
                mCustomUserAvatarBase64String = ImgUtils.bitmapToCompressedBase64String(bitmap);
            } else {
                mCustomUserAvatarBase64String = ImgUtils.uriToCompressedBase64String(this, newProfImgUri);
            }
            //Custom avatar not newly selected
        } else if (mCustomUserAvatarUriString != null &&
                newUserAvatarNumberString.equals(CUSTOM_AVATAR_NUMBER_STRING)) {
            mCustomUserAvatarBase64String = userDetailsInstance.getUserAvatarBase64String();
        } else {
            mCustomUserAvatarBase64String = "none";
        }

    }

    //posts saved user bio info to server
    public void postUserBio() {
        final String userPost = "user post";
        String userPostType;
        if (storeNewUserInDb) {
            userPostType = "new user post";
            mUserId = userDetailsInstance.getUserId();
            //reset static variable once user saves bio 1st time
            storeNewUserInDb = false;
        } else {
            userPostType = "update user post";
        }


        ServerConnect postUserToServer = new ServerConnect(this, userDetailsInstance);

        postUserToServer.execute(userPost, mUserId, mUserName, mUserAvatarNumberString, mUserFirstName, mUserLastName,
                mUserPhoneNumber, mUserEmail, mUserBio, mUserCityState, userPostType, mCustomUserAvatarBase64String);
    }

    //populates activity data from SP based on if started from media chooser or not
    public void populateUserDataFromSP(String activityOnCreateType) {
        //get info from newUserLogin if exists on device
        mUserPhoneNumber = prefs.getString(getString(R.string.user_phone_key), null);
        mUserEmail = prefs.getString(getString(R.string.user_email_key), null);
        mUserName = prefs.getString(getString(R.string.user_name_key), null);
        mUserFirstName = prefs.getString(getString(R.string.user_first_name_key), null);
        mUserLastName = prefs.getString(getString(R.string.user_last_name_key), null);
        mUserBio = prefs.getString(getString(R.string.user_bio_key), null);
        mUserId = prefs.getString(getString(R.string.user_account_id_key), null);
        mUserCityState = prefs.getString(getString(R.string.user_city_state_key), null);
        String userAvatarNumberString = userDetailsInstance.getmUserAvatarNumberString();
        // if user selected a new custom avatar, don't populate from SP
        if (activityOnCreateType.equals("no new custom avatar selected")) {
            mCustomUserAvatarUriString = prefs.getString(
                    getString(R.string.custom_user_avatar_string_uri_key), null);
            mUserAvatarNumberString = prefs.getString(getString(R.string.user_avatar_number_key), userAvatarNumberString);
        }
        populateEditTextData(activityOnCreateType);
        //get user id if not in SP
        if (mUserId == null) mUserId = userDetailsInstance.getUserId();
    }

    //sets user details from db call
    public void parseUserDetailsFromDB(String jsonString) {
        JSONObject jsonObject;
        JSONArray jsonArray;

        try {
            jsonObject = new JSONObject(jsonString);
            jsonArray = jsonObject.getJSONArray("server_response");

            //loop through array and extract objects, adding them individually as setter objects,
            //and adding objects to list adapter.
            JSONObject jo = jsonArray.getJSONObject(0);
            mUserId = jo.getString("userId");
            mUserName = jo.getString("userName");
            mUserAvatarNumberString = jo.getString("userAvatarNumber");
            mUserFirstName = jo.getString("userFirstName");
            mUserLastName = jo.getString("userLastName");
            mUserPhoneNumber = jo.getString("userPhoneNumber");
            mUserEmail = jo.getString("userEmail");
            mUserBio = jo.getString("userBio");
            mUserCityState = jo.getString("userCityState");
            populateEditTextData("no new custom avatar selected");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void populateEditTextData(String activityOnCreateType) {
        UserPhoneView.setText(mUserPhoneNumber);
        UserEmailView.setText(mUserEmail);
        UserNameView.setText(mUserName);
        UserFirstNameView.setText(mUserFirstName);
        UserLastNameView.setText(mUserLastName);
        autoCompleteTextView.setText(mUserCityState);
        UserBioView.setText(mUserBio);

        //set avatar image if not just chosen
        if (activityOnCreateType.equals("no new custom avatar selected")) {

            if (userDetailsInstance.getmUserAvatarNumberString() != null &&
                    !userDetailsInstance.getmUserAvatarNumberString().equals("999")) {
                UserAvatarView.setImageResource(AvatarImgs.getAvatarNumberResId(
                        Integer.parseInt(userDetailsInstance.getmUserAvatarNumberString())));
            } else {
                try {
                    if (userDetailsInstance.getUserAvatarUri() != null) {
                        GlideApp
                                .with(this)
                                .load(userDetailsInstance.getUserAvatarUri())
                                .placeholder(R.drawable.ic_unknown_user)
                                .circleCrop()
                                .into(UserAvatarView);
                    } else if (userDetailsInstance.getUserCustomAvatarBase64ByteArray() != null) {
                        GlideApp
                                .with(this)
                                .load(userDetailsInstance.getUserCustomAvatarBase64ByteArray())
                                .placeholder(R.drawable.ic_unknown_user)
                                .into(UserAvatarView);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveAddBioDataToSP() {
        SharedPreferences.Editor editor =
                getSharedPreferences(getString(R.string.user_data_file), MODE_PRIVATE).edit();

        //only save email/phone if user did not use that info to login
        if (!userDetailsInstance.userLoginType.equals("email"))
            editor.putString(getString(R.string.user_email_key), mUserEmail);
        if (!userDetailsInstance.userLoginType.equals("phone"))
            editor.putString(getString(R.string.user_phone_key), mUserPhoneNumber);

        editor.putString(getString(R.string.user_name_key), mUserName);
        editor.putString(getString(R.string.user_first_name_key), mUserFirstName);
        editor.putString(getString(R.string.user_last_name_key), mUserLastName);
        editor.putString(getString(R.string.user_avatar_number_key), mUserAvatarNumberString);
        editor.putString(getString(R.string.user_bio_key), mUserBio);
        editor.putString(getString(R.string.user_city_state_key), mUserCityState);

        //clear sp if current avatar string not custom
        if (mUserAvatarNumberString.equals(CUSTOM_AVATAR_NUMBER_STRING)) {
            editor.putString(getString(R.string.custom_user_avatar_string_uri_key), mCustomUserAvatarUriString);
            editor.putString(getString(R.string.user_avatar_base_64_key), mCustomUserAvatarBase64String);
        } else {
            editor.putString(getString(R.string.custom_user_avatar_string_uri_key), null);
        }
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    //sets up text watcher validation
    private void setUpTextWatcherValidation() {
        final String CANNOT_EDIT = "Changes to this item will not save, you used this to Login!";

        UserNameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textToCheck = UserNameView.getText().toString();
                if (!ChavrutaTextValidation.hasLimit9Char(textToCheck)) {
                    Toast.makeText(
                            AddBio.this,
                            "Please keep  below 10 characters",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        UserFirstNameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textToCheck = UserFirstNameView.getText().toString();
                if (!ChavrutaTextValidation.hasLimit9Char(textToCheck)) {
                    Toast.makeText(
                            AddBio.this,
                            "Please keep below 10 characters",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        UserPhoneView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(userDetailsInstance.userLoginType.equals("phone")){
                    Toast.makeText(AddBio.this, CANNOT_EDIT, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textToCheck = UserPhoneView.getText().toString();
                if (!ChavrutaTextValidation.hasCharLimitOf(16, textToCheck)) {
                    Toast.makeText(
                            AddBio.this,
                            "Valid phone numbers are shorter than 17 characters",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        UserEmailView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(userDetailsInstance.userLoginType.equals("email")){
                    Toast.makeText(AddBio.this, CANNOT_EDIT, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void displayUserConsent(){
        new AlertDialog.Builder(this)
                .setTitle("ChavrutaMatch Privacy Policy")
                .setMessage("We do not share or profit from any personal data entrusted to us!\n" +
                        "Privacy Policy provided at:\n\nhttp://brightlightproductions.online/privacy_policy.html\n\n" +
                        "Please select \"Yes\" to consent to our Private Policy statement.")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                            Snackbar.make(UserAvatarView,
                                    "Please Select \"Yes\", Your Data Is Kept Secure",
                                    Snackbar.LENGTH_LONG ).show();
                        thisActivity.recreate();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        boolean userConfirmsConsentToPP = true;
                        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.user_data_file), MODE_PRIVATE).edit();
                        editor.putBoolean(getString(R.string.user_priv_policy_consent), userConfirmsConsentToPP);
                        editor.putString(getString(R.string.user_account_id_key), mUserId);
                        editor.apply();
                    }
                })
                .create().show();
    }

}
