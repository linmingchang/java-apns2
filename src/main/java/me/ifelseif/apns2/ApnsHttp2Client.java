package me.ifelseif.apns2;

import me.ifelseif.apns2.model.Notification;

/**
 * Created by linmingchang on 16/12/21.
 */
public interface ApnsHttp2Client {
    ApnsHttp2Client start();

    void stop();

    void push(String token, Notification notification, ResponseListener listener);
}
