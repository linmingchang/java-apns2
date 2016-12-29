package me.ifelseif.apns2.impl;

import me.ifelseif.apns2.ApnsHttp2Client;
import me.ifelseif.apns2.ApnsHttp2Service;
import me.ifelseif.apns2.ResponseListener;
import me.ifelseif.apns2.model.Apns2Config;
import me.ifelseif.apns2.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by linmingchang on 16/12/29.
 */
public class ApnsHttp2ServiceImpl implements ApnsHttp2Service{
    private static final Logger log = LoggerFactory.getLogger(ApnsHttp2ServiceImpl.class);
    private ExecutorService service = null;
    private ApnsHttp2ClientPool clientPool = null;

    public ApnsHttp2ServiceImpl(Apns2Config config){
        service = Executors.newFixedThreadPool(5);
        clientPool = ApnsHttp2ClientPool.newClientPool(config);
    }

    @Override
    public void push(String token, Notification notification, ResponseListener listener) {
        service.execute(new Runnable() {
            @Override
            public void run() {
                ApnsHttp2Client client = null;
                try{
                    client = clientPool.borrowClient();
                    client.push(token,notification,listener);
                }finally {
                    if(client!=null){
                        clientPool.returnClient(client);
                    }
                }
            }
        });
    }

    @Override
    public void shutdown() {
        service.shutdown();
        try {
            service.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Shutdown ApnsHttp2Service interrupted", e);
        }
        clientPool.close();
    }

}
