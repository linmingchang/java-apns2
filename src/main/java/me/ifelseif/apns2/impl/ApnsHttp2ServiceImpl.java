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
public class ApnsHttp2ServiceImpl implements ApnsHttp2Service {
    private static final Logger log = LoggerFactory.getLogger(ApnsHttp2ServiceImpl.class);
    private final ExecutorService service;
    private final ApnsHttp2ClientPool clientPool;

    public ApnsHttp2ServiceImpl(Apns2Config config) {
        service = Executors.newFixedThreadPool(config.getPoolSize());
        clientPool = ApnsHttp2ClientPool.newClientPool(config);
    }

    @Override
    public void push(String token, Notification notification, ResponseListener listener) {
        service.execute(()->{
            ApnsHttp2Client client = null;
            try {
                client = clientPool.borrowClient();
                if(client != null){
                    client.push(token, notification, listener);
                }else{
                    log.error("can't get client");
                }
            } finally {
                if (client != null) {
                    clientPool.returnClient(client);
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
