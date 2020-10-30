package com.example.chatapp.Fragments;

import com.example.chatapp.Notifications.MyResponse;
import com.example.chatapp.Notifications.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService{
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAh1KbJxw:APA91bG2bw5Ttl1QkfM8oC8DaS2THzbqso83xoGVvWKFNrWXTKEed_guX4x9qRGS4vAVAispq9MvtMEeHCSlLlrzFrE_i-X6Kq8nn4SVUs9Lt-iB52GaGq039cOsrbzyiw53R45QJvt1"
    })
    @POST("fcm/send")
    Call<MyResponse>sendNotification(@Body Sender body);
}
