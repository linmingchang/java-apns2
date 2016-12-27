# java-apns2
HTTP/2 Apple Push Notification Service (APNs) push provider for JAVA
## Features

- 使用苹果最新的推送协议（基于HTTP/2）
- 基于jetty Http2Client
- 维持到APNs的长连接（断线重连，定时发送心跳帧）
- 异步推送，保证推送效率

## Install

- 在java9正式推出之前，需要ALPN支持，具体可以看 [jetty ALPN](http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html).
- 直接check out本项目到本地（即将支持maven安装）


## Example

```java
ApnsHttp2ClientImpl client = new ApnsHttp2ClientImpl.Builder()
                .key("production-195-0.p12")
                .password("apple")
                .topic("com.weather.NOBWeather")
                .build()
                .start();

        Notification notification = new Notification.Builder()
                .alertBody("hello")
                .alertTitle("titletest")
                .badge(1)
                .build();

        client.push("afae802f3bb27e5606c74495453bb4534fc36c5606f663ad4b92afe392e5d7d2", notification, new ResponseListener() {
            @Override
            public void success(String deviceToken,Notification notification) {
                System.out.println(notification.getPayload());
            }

            @Override
            public void failure(String deviceToken,Notification notification,int status,String reason) {
                System.out.println("status:"+status+" reason:"+reason);
            }
        });

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.stop();
```
