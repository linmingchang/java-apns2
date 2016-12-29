package me.ifelseif.apns2;

import me.ifelseif.apns2.model.Notification;

/**
 * Created by linmingchang on 16/12/29.
 */
public interface ApnsHttp2Service {
    void shutdown();
    void push(String token, Notification notification, ResponseListener listener);
}
