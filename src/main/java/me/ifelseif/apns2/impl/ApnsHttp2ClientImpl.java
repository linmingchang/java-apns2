package me.ifelseif.apns2.impl;

import me.ifelseif.apns2.ApnsHttp2Client;
import me.ifelseif.apns2.ResponseListener;
import me.ifelseif.apns2.model.Notification;
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
import org.eclipse.jetty.http2.frames.PingFrame;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by linmingchang on 16/12/21.
 */
public class ApnsHttp2ClientImpl implements ApnsHttp2Client {
    private static final Logger log = LoggerFactory.getLogger(ApnsHttp2ClientImpl.class);
    private static final String APNS_HOST = "api.push.apple.com";
    private static final int APNS_PORT = 443;
    private final String URI_BASE = "https://" + APNS_PORT + ":" + APNS_PORT + "/3/device/";
    private HTTP2Client http2Client;
    private Session session;
    private long sendCount;
    private long createTime;
    private final Object lock = new Object();
    private final int connectTimeout;
    private final int pushTimeout;
    private final String topic;
    private static final int BACKOFF_MAX = 300000;
    private static final int BACKOFF_MIN = 3000;
    private final int pushRetryTimes;
    private final int apnsExpiration;
    private final int apnsPriority;
    private final SslContextFactory sslContextFactory;
    private volatile boolean pingFailed = false;
    private Timer timer;

    public ApnsHttp2ClientImpl(SslContextFactory sslContextFactory, int connectTimeout, int pushTimeout, String topic, int pushRetryTimes, int apnsExpiration, int apnsPriority) {
        this.connectTimeout = connectTimeout;
        this.pushTimeout = pushTimeout;
        this.topic = topic;
        this.pushRetryTimes = pushRetryTimes;
        this.apnsExpiration = apnsExpiration;
        this.apnsPriority = apnsPriority;
        this.sslContextFactory = sslContextFactory;
    }

    @Override
    public ApnsHttp2ClientImpl start() {
        if (http2Client == null) {
            connectRetry();
        } else {
            final int hours = 10;
            long sendCount = getSendCount();
            if (pingFailed) {
                log.warn("ping failed ,already send {} messages，will reconnect", sendCount);
                connectRetry();
            } else if (getSendCount() > 100000) {
                log.warn("already send {} messages，will reconnect", sendCount);
                connectRetry();
            } else if (System.currentTimeMillis() - getCreateTime() > 3600 * 1000 * hours) {
                log.warn("already keep alive {} hours and send {} messages，will reconnect", hours, sendCount);
                connectRetry();
            }
        }
        return this;
    }


    @Override
    public void stop() {
        try {
            if (http2Client != null) {
                http2Client.stop();
                http2Client = null;
            }
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        } catch (Exception ex) {
            log.warn("stop error,host={}", APNS_HOST, ex);
        }
    }

    private void connectRetry() {
        int backoff = BACKOFF_MIN;
        while (true) {
            try {
                connect();
                break;
            } catch (Exception ex) {
                log.warn("connect apns error", ex);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                backoff = backoff > BACKOFF_MAX ? BACKOFF_MAX : backoff * 2;
            }
        }
        //start ping
        if (timer == null) {
            timer = new Timer(true);
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                ping();
            }
        }, 1000, 10000);
    }

    public void ping() {
        session.ping(new PingFrame(System.currentTimeMillis(), false), new Callback() {
            @Override
            public void failed(Throwable x) {
                log.warn("ping failed", x);
                pingFailed = true;
                stop();
            }

            @Override
            public void succeeded() {
                log.info("ping succcess");
            }
        });
    }

    private void connect() throws Exception {
        this.http2Client = new HTTP2Client();
        this.http2Client.start();

        FuturePromise<Session> sessionPromise = new FuturePromise<>();
        this.http2Client.connect(sslContextFactory, new InetSocketAddress(APNS_HOST, APNS_PORT), new ServerSessionListener.Adapter(), sessionPromise);
        // Obtain the client Session object.
        session = sessionPromise.get(connectTimeout, TimeUnit.SECONDS);
        createTime = System.currentTimeMillis();
        sendCount = 0L;
        log.info("APNS Client build");
    }

    private void retryPush(String token, Notification notification, ResponseListener listener) {
        int retryTimes = 0;
        while (true) {
            try {
                start();
                request(token, notification, listener);
                break;
            } catch (Exception e) {
                log.error("push error", e);
                if (this.http2Client != null) {
                    stop();
                }
                if (retryTimes < pushRetryTimes) {
                    retryTimes++;
                    log.warn("retry:{},token:{},payload:{}", retryTimes, token, notification.getPayload());
                } else {
                    log.error("push error after retry {},token:{},payload:{}", retryTimes, token, notification.getPayload());
                    break;
                }
            }
        }
    }

    @Override
    public void push(String token, Notification notification, ResponseListener listener) {
        retryPush(token, notification, listener);
    }


    private void request(String token, Notification notification, ResponseListener listener) throws InterruptedException, TimeoutException, ExecutionException {
        // Prepare the HTTP request headers.
        HttpFields requestFields = new HttpFields();
        requestFields.put("apns-id", UUID.randomUUID().toString());
        requestFields.put("apns-expiration", String.valueOf(apnsExpiration));
        requestFields.put("apns-priority", String.valueOf(apnsPriority));
        requestFields.put("apns-topic", notification.getTopic() == null ? this.topic : notification.getTopic());
        // Prepare the HTTP request object.
        MetaData.Request request = new MetaData.Request("POST", new HttpURI(URI_BASE + token), HttpVersion.HTTP_2, requestFields);
        // Create the HTTP/2 HEADERS frame representing the HTTP request.
        HeadersFrame headersFrame = new HeadersFrame(request, null, false);
        // Prepare the listener to receive the HTTP response frames.
        Stream.Listener responseListener = new Stream.Listener.Adapter() {
            HeadersFrame headersFrame = null;

            @Override
            public void onHeaders(Stream stream, HeadersFrame frame) {
                headersFrame = frame;
                MetaData meta = headersFrame.getMetaData();
                if (meta.isResponse()) {
                    MetaData.Response response = (MetaData.Response) meta;
                    int status = response.getStatus();
                    if (status == 200) {
                        listener.success(token, notification);
                    }
                }
            }

            @Override
            public void onData(Stream stream, DataFrame frame, Callback callback) {
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
                    MetaData.Response response = (MetaData.Response) meta;
                    int status = response.getStatus();
                    if (status == 200) {
                        callback.succeeded();
                    } else {
                        JSONParser parser = new JSONParser();
                        String reason = "";
                        try {
                            JSONObject object = (JSONObject) parser.parse(body);
                            reason = (String) object.get("reason");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        callback.failed(new Exception("status:" + status + ", " + body));
                        listener.failure(token, notification, status, reason);
                    }
                }
            }
        };

        // Send the HEADERS frame to create a stream.
        FuturePromise<Stream> streamPromise = new FuturePromise<>();
        session.newStream(headersFrame, streamPromise, responseListener);
        Stream stream = streamPromise.get(pushTimeout, TimeUnit.SECONDS);

        // Use the Stream object to send request content, if any, using a DATA frame.
        ByteBuffer content = ByteBuffer.wrap(notification.getPayload().getBytes(Charset.forName("UTF-8")));
        DataFrame requestContent = new DataFrame(stream.getId(), content, true);
        stream.data(requestContent, new Callback() {
            @Override
            public void succeeded() {
                log.debug("success");
            }

            @Override
            public void failed(Throwable x) {
                log.debug("error",x);
            }
        });
        sendCount++;
    }

    public long getSendCount() {
        return sendCount;
    }


    public long getCreateTime() {
        return createTime;
    }
}
