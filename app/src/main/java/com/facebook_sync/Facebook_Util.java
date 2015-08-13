package com.facebook_sync;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ViewDebug;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gary on 2015/8/10.
 */
public class Facebook_Util extends Application {
    private AccessToken accessToken;
    private String pic_url = null;
    public static final String pic_url_broadcast = "com.facebook_sync_broadcast_pic_url";

    public void getTagFriendPic(final int uid) {
        accessToken = AccessToken.getCurrentAccessToken();

        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback() {

                    //當RESPONSE回來的時候
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        GraphRequest request_tagFriend = new GraphRequest(
                                accessToken,
                                "/me/taggable_friends",
                                null,
                                HttpMethod.GET,
                                new GraphRequest.Callback() {
                                    public void onCompleted(GraphResponse response) {
                                        /* handle the result */
                                        JSONObject tagFriend_object = response.getJSONObject();
                                        String object_string = tagFriend_object.optString("data");
                                        JSONObject uaer_object = null;
                                        int tag_friend_length = 0;

                                        try {
                                            JSONArray jsonArray = new JSONArray(object_string);
                                            tag_friend_length = jsonArray.length();
                                            Log.d("FB", "how many tag friend = "+Integer.toString(tag_friend_length));
                                            if(tag_friend_length > uid) {
                                                uaer_object = jsonArray.getJSONObject(uid);
                                            }
                                            else {
                                                pic_url = null;
                                                sendMessage(pic_url);
                                                return;
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        try {
                                            object_string = uaer_object.optString("picture");
                                            JSONObject pic_object = new JSONObject(object_string);
                                            JSONObject pic_url_object =  new JSONObject(pic_object.optString("data"));
                                            pic_url = pic_url_object.optString("url");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        /*broadcast url*/
                                        sendMessage(pic_url);
                                    }
                                }
                        );
                        Bundle params = new Bundle();
                        params.putString("limit","1000");
                        params.putString("fields", "id,name,picture.width(500).height(500)");
                        request_tagFriend.setParameters(params);
                        request_tagFriend.executeAsync();
                    }
                }
        );
        //包入你想要得到的資料 送出request
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,link");
        request.setParameters(parameters);
        request.executeAsync();
    }

    // Send an Intent with an action named "custom-event-name". The Intent sent should
    // be received by the ReceiverActivity.
    private void sendMessage(String message) {
        Intent intent = new Intent(pic_url_broadcast);
        // You can also include some extra data.
        intent.putExtra("pic_url", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
