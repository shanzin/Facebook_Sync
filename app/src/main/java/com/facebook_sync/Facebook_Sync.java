package com.facebook_sync;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;

import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.HttpMethod;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class Facebook_Sync extends Activity {

    CallbackManager callbackManager;
    private AccessToken accessToken;
    private Button button_get_user_list;
    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mReceiver;
    public static final String pic_url_broadcast = "com.facebook_sync_broadcast_pic_url";
    ProgressDialog pDialog;
    Bitmap bitmap;
    ImageView myImageView;
    private Handler timer_handler = new Handler( );
    private Runnable timer_runnable;
    private int uid = 0;

    @Override
    protected void onResume() {
        super.onResume();

        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //初始化FacebookSdk，記得要放第一行，不然setContentView會出錯
        FacebookSdk.sdkInitialize(getApplicationContext());
        Log.d("FB", "sdkInitialize complete");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        //宣告callback Manager
        callbackManager = CallbackManager.Factory.create();

        //找到login button
        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("user_friends");

        //幫loginButton增加callback function
        //這邊為了方便 直接寫成inner class
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

                    @Override
                    public void onSuccess(LoginResult loginResult) {

                        //accessToken之後或許還會用到 先存起來

                        accessToken = loginResult.getAccessToken();

                        Log.d("FB", "access token got.");

                        //send request and call graph api
                        GraphRequest request = new GraphRequest(
                                AccessToken.getCurrentAccessToken(),
                                "/{friend-list-id}",
                                null,
                                HttpMethod.GET,
                                new GraphRequest.Callback() {
                                    //當RESPONSE回來的時候
                                    @Override
                                    public void onCompleted(GraphResponse response) {
                                    /* handle the result */
                                        Log.d("FB", "friend list id");
                                    }
                                }
                        );

                        //包入你想要得到的資料 送出request
                        Bundle parameters = new Bundle();
                        parameters.putString("fields", "id,name,link");
                        request.setParameters(parameters);
                        request.executeAsync();
                    }

                    //登入取消

                    @Override
                    public void onCancel() {
                        // App code
                        Log.d("FB", "CANCEL");
                    }

                    //登入失敗

                    @Override
                    public void onError(FacebookException exception) {
                        // App code

                        Log.d("FB", exception.toString());
                    }
                }/*loginButton.registerCallback*/
        );

        /*Broadcast register*/
        IntentFilter pic_url_filter = new IntentFilter();
        pic_url_filter.addAction(pic_url_broadcast);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(pic_url_broadcast)) {
                    if(intent.getStringExtra("pic_url") == null) {
                        Log.d("FB", "UID out of range");
                        timer_handler.removeCallbacks(timer_runnable); //停止Timer
                    }
                    else {
                        Log.d("FB", intent.getStringExtra("pic_url"));
                    }
                }
                new LoadImage().execute(intent.getStringExtra("pic_url"));
            }
        };
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mReceiver, pic_url_filter);
    }/*protected void onCreate*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_facebook__sync, menu);
        return false;
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

    private void dispFacebookUserPic(String pic_url) {
        try{
            URL url = new URL(pic_url);
            URLConnection urlConnection = url.openConnection();
            InputStream is = (InputStream) urlConnection.getInputStream();
            Drawable draw = Drawable.createFromStream(is, "src");
            myImageView.setImageDrawable(draw);
        }catch (Exception e) {
            //TODO handle error
            Log.d("FB", "disp pic error");
        }
    }

    private class LoadImage extends AsyncTask<String, String, Bitmap> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        protected Bitmap doInBackground(String... args) {
            try {
                bitmap = BitmapFactory.decodeStream((InputStream)new URL(args[0]).getContent());

            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap image) {

            if(image != null){
                myImageView.setImageBitmap(image);

            }else{
                Toast.makeText(Facebook_Sync.this, "Image Does Not exist or Network Error", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void pic_update_timer(){

        timer_runnable = new Runnable( ) {
            public void run ( ) {
                Facebook_Util fb_util = new Facebook_Util();
                fb_util.getTagFriendPic(uid);
                uid++;
                timer_handler.postDelayed(this,2000);
            }
        };
        timer_handler.postDelayed(timer_runnable, 2000); // 开始Timer
    }
}
