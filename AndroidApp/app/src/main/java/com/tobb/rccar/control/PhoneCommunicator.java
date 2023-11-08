package com.tobb.rccar.control;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class PhoneCommunicator {

    public static final int TAG_INFO = 1;
    public static final int TAG_POSITION = 2;
    public static final int TAG_DRIVE = 3;
    public static final int TAG_TEST = 4;

    private String url;
    private Context context;
    private ReceiveResponse receiveResponse;
    private boolean connected = false;

    public PhoneCommunicator(Context context, ReceiveResponse receiveResponse){
        this.context = context;
        this.receiveResponse = receiveResponse;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void getInfo(){
        send(Request.Method.GET, "info", TAG_INFO);
    }

    public void getPosition(){
        send(Request.Method.GET, "position", TAG_POSITION);
    }

    public void drive(float motor, float steering){
        send(Request.Method.GET, "drive?motor=" + motor + "&steering=" + steering, TAG_DRIVE);
    }

    public void testConnection() {
        send(Request.Method.GET, "info", TAG_TEST);
    }

    public void setCamera(){

    }

    public void send(int method, String endpoint, int tag){
        RequestQueue queue = Volley.newRequestQueue(context);
        String uri = url + "/" + endpoint;
        StringRequest stringRequest = new StringRequest(method, uri, response -> {
            connected = true;
            receiveResponse.receive(tag, response, 200, false);
        }, error ->
        {
            if(error.networkResponse != null){
                receiveResponse.receive(tag, null, error.networkResponse.statusCode, true);
            }else{
                receiveResponse.receive(tag, error.getMessage(), 0, true);
            }
        });
        queue.add(stringRequest);
    }

    public boolean isConnected() {
        return connected;
    }
}
