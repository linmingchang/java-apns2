package me.ifelseif.apns2.impl;

import me.ifelseif.apns2.ApnsHttp2Client;
import me.ifelseif.apns2.ResponseListener;
import me.ifelseif.apns2.model.Notification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by linmingchang on 16/12/21.
 */
public class ApnsHttp2ClientImpl implements ApnsHttp2Client {
    private static final Logger log = LogManager.getLogger(ApnsHttp2ClientImpl.class);
    private static final String APNS_HOST = "api.push.apple.com";
    private static final int APNS_PORT = 443;
    private final String URI_BASE = "https://" + APNS_PORT + ":" + APNS_PORT + "/3/device/";
    private HTTP2Client http2Client;
    private Session session;
    private long sendCount;
    private long createTime;
    private volatile boolean pingFailed = false;
    private final Object lock = new Object();
    private String password;
    private InputStream key;
    private int connectTimeout;//second
    private int pushTimeout;
    private String topic;
    private static final int BACKOFF_MAX = 300000;
    private static final int BACKOFF_MIN = 3000;

    public ApnsHttp2ClientImpl(String password, InputStream key, int connectTimeout, int pushTimeout,String topic) {
        this.password = password;
        this.key = key;
        this.connectTimeout = connectTimeout;
        this.pushTimeout = pushTimeout;
        this.topic = topic;
    }

    @Override
    public ApnsHttp2ClientImpl start(){
        if(http2Client ==null){
            connectRetry();
        }else{
            final int hours = 10;
            //当连接发送次数大于一定值时将重建连接
            //当连接创建时间大于一定值时也将重建连接
            long sendCount = getSendCount();
            if (isPingFailed()) {
                log.warn("APNS客户端Ping失败，共发送了{}条通知，将重建连接",sendCount);
                connectRetry();
            } else if (getSendCount() > 100000) {
                log.warn("APNS客户端已发送{}条通知，将重建连接",sendCount);
                connectRetry();
            } else if (System.currentTimeMillis() - getCreateTime() > 3600*1000*hours) {
                log.warn("APNS客户端连接已保持{}小时,共发送了{}条通知，将重建连接",hours, sendCount);
                connectRetry();
            }
        }
        return this;
    }


    @Override
    public void stop() {
        try {
            http2Client.stop();
            http2Client = null;
        } catch (Exception ex) {
            log.warn("关闭HTTP2连接失败,host={}", APNS_HOST, ex);
        }
    }

    private void connectRetry(){
        int backoff = BACKOFF_MIN;
        while(true) {
            try {
                connect();
                break;
            } catch (Exception ex) {
                log.warn("连接APNS服务失败", ex);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                backoff = backoff > BACKOFF_MAX ? BACKOFF_MAX : backoff * 2;
            }
        }
    }

    private void connect() throws Exception{
        //init KeyStore
        final char[] pwdChars = password.toCharArray();
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(key, pwdChars);
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, pwdChars);
        //init TrustManager
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        //init SSLContext
        final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

        final SslContextFactory sslContextFactory = new SslContextFactory(true);
        sslContextFactory.setSslContext(sslContext);
        sslContextFactory.start();

        this.http2Client = new HTTP2Client();
        this.http2Client.start();

        FuturePromise<Session> sessionPromise = new FuturePromise<>();
        this.http2Client.connect(sslContextFactory, new InetSocketAddress(APNS_HOST, APNS_PORT), new ServerSessionListener.Adapter(), sessionPromise);
        // Obtain the client Session object.
        session = sessionPromise.get(connectTimeout, TimeUnit.SECONDS);
        createTime = System.currentTimeMillis();
        sendCount = 0L;
        log.info("APNS Client builded");
    }

    @Override
    public void push(String token,Notification notification, ResponseListener listener) {
        try {
            if(http2Client == null){
                connectRetry();
            }
            pushs(token,notification,listener);
        } catch (Exception ex) {
            log.warn("推送失败", ex);
            if (http2Client.isStarted() && ex.getCause() instanceof ClosedChannelException) {
                stop();
            }
        }
    }


    private void pushs(String token,Notification notification, ResponseListener listener) throws InterruptedException, TimeoutException, ExecutionException {
        long start = System.currentTimeMillis();
        // Prepare the HTTP request headers.
        HttpFields requestFields = new HttpFields();
        requestFields.put("apns-id", UUID.randomUUID().toString());
        requestFields.put("apns-expiration", "0");
        requestFields.put("apns-priority", "10");
        requestFields.put("apns-topic", notification.getTopic()==null?this.topic:notification.getTopic());
        // Prepare the HTTP request object.
        MetaData.Request request = new MetaData.Request("POST", new HttpURI(URI_BASE + token), HttpVersion.HTTP_2, requestFields);
        // Create the HTTP/2 HEADERS frame representing the HTTP request.
        HeadersFrame headersFrame = new HeadersFrame(request, null, false);
        // Prepare the listener to receive the HTTP response frames.
        Stream.Listener responseListener = new Stream.Listener.Adapter() {
            HeadersFrame headersFrame = null;
            @Override
            public void onHeaders(Stream stream, HeadersFrame frame) {
                log.warn("StreamListener.onHeader(),{}",frame.getMetaData());
                headersFrame = frame;
                MetaData meta = headersFrame.getMetaData();
                if (meta.isResponse()) {
                    MetaData.Response response = (MetaData.Response)meta;
                    int status = response.getStatus();
                    if (status == 200) {
                        listener.success(token,notification);
                        log.warn("推送成功：{},{},{}",notification.getTopic(),token,notification.getPayload());
                    }
                }
            }

            @Override
            public void onData(Stream stream, DataFrame frame, Callback callback) {
                log.debug("StreamListener.onData()");
                ByteBuffer buf = frame.getData();
                String body = new String(buf.array(), 0, buf.remaining(), Charset.forName("UTF-8"));
                if (headersFrame == null) {
                    return;
                }
                MetaData meta = headersFrame.getMetaData();
                if (meta.isResponse()) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                    MetaData.Response response = (MetaData.Response)meta;
                    int status = response.getStatus();
                    String msg = "status:"+status+", "+body;
                    if (status == 200) {
                        log.warn("推送成功：{},{},{}",notification.getTopic(),token,notification.getPayload());
                        callback.succeeded();
                    } else {
                        log.debug(body);
                        JSONParser parser = new JSONParser();
                        String reason = "";
                        try {
                            JSONObject object = (JSONObject) parser.parse(body);
                            reason = (String) object.get("reason");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        callback.failed(new Exception(msg));
                        listener.failure(token,notification,status,reason);
                        //log.warn("推送失败：{},{},{},{}",msg,notification.getTopic(),token,notification.getTopic());
                    }
                }
            }
        };

        synchronized (lock) {
            while (session.getStreams().size() >= 500) {
                lock.wait(100);
                if (System.currentTimeMillis() - start > pushTimeout) {
                    throw new TimeoutException("timeout in waiting for streams count down to 500");
                }
            }
            long waitTime = System.currentTimeMillis() - start;
            if (waitTime > 3000) {
                log.warn("ApnsHttp2Client wait {} ms for Streams count down to 500", waitTime);
            }
            sendCount++;
            // Send the HEADERS frame to create a stream.
            FuturePromise<Stream> streamPromise = new FuturePromise<>();
            session.newStream(headersFrame, streamPromise, responseListener);
            Stream stream = streamPromise.get(5, TimeUnit.SECONDS);

            // Use the Stream object to send request content, if any, using a DATA frame.
            ByteBuffer content = ByteBuffer.wrap(notification.getPayload().getBytes(Charset.forName("UTF-8")));
            DataFrame requestContent = new DataFrame(stream.getId(), content, true);
            stream.data(requestContent, new Callback() {
                //Stream发送状态的的回调
                public void succeeded() {
                    log.debug("stream.data Callback.succeed");
                }

                public void failed(Throwable ex) {
                    pingFailed = true;
                    log.warn("推送失败:{},{},{}", notification.getTopic(), token, notification.getPayload(), ex);
                }
            });
        }
    }

    public long getSendCount() {
        return sendCount;
    }


    public boolean isPingFailed() {
        return pingFailed;
    }

    public long getCreateTime() {
        return createTime;
    }

    public static class Builder{
        private String password;
        private InputStream key;
        private int connectTimeout;
        private int pushTimeout;
        private String topic;
        public Builder password(String password){
            this.password = password;
            return this;
        }

        public Builder key(String key){
            InputStream is = getClass().getResourceAsStream(key);
            if (is == null) {
                throw new IllegalArgumentException("Keystore file not found. " + key);
            }
            this.key = is;
            return this;
        }

        public Builder connectTimeout(int connectTimeout){
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder pushTimeout(int pushTimeout){
            this.pushTimeout = pushTimeout;
            return this;
        }

        public Builder topic(String topic){
            this.topic = topic;
            return this;
        }

        public ApnsHttp2ClientImpl build(){
            return new ApnsHttp2ClientImpl(password,key,connectTimeout,pushTimeout,topic);
        }
    }
}
