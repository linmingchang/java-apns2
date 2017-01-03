package me.ifelseif.apns2.model;


import java.io.InputStream;

/**
 * Created by linmingchang on 16/12/29.
 */
public class Apns2Config {
    private String password;
    private InputStream key;
    private int connectTimeout;
    private int pushTimeout;
    private String topic;
    private int pushRetryTimes;
    private int apnsExpiration;
    private int apnsPriority;
    private int poolSize;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public InputStream getKey() {
        return key;
    }

    public void setKey(InputStream key) {
        this.key = key;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getPushTimeout() {
        return pushTimeout;
    }

    public void setPushTimeout(int pushTimeout) {
        this.pushTimeout = pushTimeout;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPushRetryTimes() {
        return pushRetryTimes;
    }

    public void setPushRetryTimes(int pushRetryTimes) {
        this.pushRetryTimes = pushRetryTimes;
    }

    public int getApnsExpiration() {
        return apnsExpiration;
    }

    public void setApnsExpiration(int apnsExpiration) {
        this.apnsExpiration = apnsExpiration;
    }

    public int getApnsPriority() {
        return apnsPriority;
    }

    public void setApnsPriority(int apnsPriority) {
        this.apnsPriority = apnsPriority;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public Apns2Config(String password, InputStream key, int connectTimeout, int pushTimeout, String topic, int pushRetryTimes, int apnsExpiration, int apnsPriority, int poolSize) {
        this.password = password;
        this.key = key;
        this.connectTimeout = connectTimeout;
        this.pushTimeout = pushTimeout;
        this.topic = topic;
        this.pushRetryTimes = pushRetryTimes;
        this.apnsExpiration = apnsExpiration;
        this.apnsPriority = apnsPriority;
        this.poolSize = poolSize;
    }

    public static class Builder {
        private String password;
        private InputStream key;
        private int connectTimeout = 60;
        private int pushTimeout = 5;
        private int apnsExpiration = 0;
        private int apnsPriority = 10;
        private int pushRetryTimes = 3;
        private String topic;
        private int poolSize;

        public Apns2Config.Builder password(String password) {
            this.password = password;
            return this;
        }

        public Apns2Config.Builder key(String key) {
            InputStream is = Apns2Config.Builder.class.getClassLoader().getResourceAsStream(key);
            if (is == null) {
                throw new IllegalArgumentException("Keystore file not found. " + key);
            }
            this.key = is;
            return this;
        }

        public Apns2Config.Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Apns2Config.Builder pushTimeout(int pushTimeout) {
            this.pushTimeout = pushTimeout;
            return this;
        }

        public Apns2Config.Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public Apns2Config.Builder pushRetryTimes(int pushRetryTimes) {
            this.pushRetryTimes = pushRetryTimes;
            return this;
        }

        public Apns2Config.Builder apnsExpiration(int apnsExpiration) {
            this.apnsExpiration = apnsExpiration;
            return this;
        }

        public Apns2Config.Builder apnsPriority(int apnsPriority) {
            this.apnsPriority = apnsPriority;
            return this;
        }

        public Apns2Config.Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public Apns2Config build() {
            return new Apns2Config(password, key, connectTimeout, pushTimeout, topic, pushRetryTimes, apnsExpiration, apnsPriority, poolSize);
        }
    }
}
