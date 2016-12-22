package me.ifelseif.apns2;

import me.ifelseif.apns2.model.Notification;

/**
 * Created by linmingchang on 16/12/21.
 */
public interface ResponseListener {
    void success(String deviceToken,Notification notification);
    void failure(String deviceToken,Notification notification,int status,String reason);
}
