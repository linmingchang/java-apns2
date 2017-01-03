package me.ifelseif.apns2.model;

import java.util.Map;

/**
 * Created by linmingchang on 16/12/21.
 */
public class Notification {
    private String payload;
    private String token;
    private String topic;

    public Notification(String payload, String token, String topic) {
        this.payload = payload;
        this.token = token;
        this.topic = topic;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public static class Builder {
        private Payload payload;
        private String token;
        private String topic;

        public Builder() {
            payload = new Payload();
        }

        public Builder alert(String alert) {
            payload.setAlert(alert);
            return this;
        }

        public Builder badge(int badge) {
            payload.setBadge(badge);
            return this;
        }

        public Builder sound(String sound) {
            payload.setSound(sound);
            return this;
        }

        public Builder alertActionLocKey(String actionLocKey) {
            payload.setAlertActionLocKey(actionLocKey);
            return this;
        }

        public Builder alertBody(String body) {
            payload.setAlertBody(body);
            return this;
        }

        public Builder alertTitle(String title) {
            payload.setAlertTitle(title);
            return this;
        }

        public Builder alertLocKey(String locKey) {
            payload.setAlertLocKey(locKey);
            return this;
        }

        public Builder alertLocArgs(String[] locArgs) {
            payload.setAlertLocArgs(locArgs);
            return this;
        }

        public Builder alertLaunchImage(String launchImagee) {
            payload.setAlertLaunchImage(launchImagee);
            return this;
        }

        public Builder addParam(String key, String value) {
            payload.addParam(key, value);
            return this;
        }

        public Builder setParams(Map<String, Object> params) {
            payload.setParams(params);
            return this;
        }

        public Builder setContentAvailable(Integer contentAvailable) {
            payload.setContentAvailable(contentAvailable);
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public Notification build() {
            return new Notification(payload.toString(), token, topic);
        }
    }
}
