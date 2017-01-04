package me.ifelseif.apns2.model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Payload {
    private static final String APS = "aps";
    private Map<String, Object> params;
    private String alert;
    private Integer badge;
    private String sound = "default.caf";
    private Integer contentAvailable;

    private String alertBody;
    private String alertTitle;
    private String alertActionLocKey;
    private String alertLocKey;
    private String[] alertLocArgs;
    private String alertLaunchImage;

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void addParam(String key, Object obj) {
        if (params == null) {
            params = new HashMap<String, Object>();
        }
        if (APS.equalsIgnoreCase(key)) {
            throw new IllegalArgumentException("the key can't be aps");
        }
        params.put(key, obj);
    }

    public String getAlert() {
        return alert;
    }

    public void setAlert(String alert) {
        this.alert = alert;
    }

    public Integer getBadge() {
        return badge;
    }

    public void setBadge(Integer badge) {
        this.badge = badge;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String toString() {
        JSONObject object = new JSONObject();
        JSONObject apsObj = new JSONObject();
        if (getAlert() == null || getAlert().isEmpty()) {
            if (getAlertBody() != null || getAlertLocKey() != null) {
                JSONObject alertObj = new JSONObject();
                putIntoJson("body", getAlertBody(), alertObj);
                putIntoJson("title", getAlertTitle(), alertObj);
                putIntoJson("action-loc-key", getAlertActionLocKey(), alertObj);
                putIntoJson("loc-key", getAlertLocKey(), alertObj);
                putIntoJson("launch-image", getAlertLaunchImage(), alertObj);
                if (getAlertLocArgs() != null) {
                    JSONArray array = new JSONArray();
                    for (String str : getAlertLocArgs()) {
                        array.add(str);
                    }
                    alertObj.put("loc-args", array);
                }
                apsObj.put("alert", alertObj);
            }
        } else {
            apsObj.put("alert", getAlert());
        }

        if (getBadge() != null) {
            apsObj.put("badge", getBadge().intValue());
        }
        putIntoJson("sound", getSound(), apsObj);

        if (getContentAvailable() != null) {
            apsObj.put("content-available", getContentAvailable().intValue());
        }

        object.put(APS, apsObj);
        if (getParams() != null) {
            for (Entry<String, Object> e : getParams().entrySet()) {
                object.put(e.getKey(), e.getValue());
            }
        }
        return object.toString();
    }

    @SuppressWarnings("unchecked")
    private void putIntoJson(String key, String value, JSONObject obj) {
        if (value != null) {
            obj.put(key, value);
        }
    }

    public String getAlertBody() {
        return alertBody;
    }

    public void setAlertBody(String alertBody) {
        this.alertBody = alertBody;
    }

    public String getAlertTitle() {
        return alertTitle;
    }

    public void setAlertTitle(String alertTitle) {
        this.alertTitle = alertTitle;
    }

    public String getAlertActionLocKey() {
        return alertActionLocKey;
    }

    public void setAlertActionLocKey(String alertActionLocKey) {
        this.alertActionLocKey = alertActionLocKey;
    }

    public String getAlertLocKey() {
        return alertLocKey;
    }

    public void setAlertLocKey(String alertLocKey) {
        this.alertLocKey = alertLocKey;
    }

    public String getAlertLaunchImage() {
        return alertLaunchImage;
    }

    public void setAlertLaunchImage(String alertLaunchImage) {
        this.alertLaunchImage = alertLaunchImage;
    }

    public String[] getAlertLocArgs() {
        return alertLocArgs.clone();
    }

    public void setAlertLocArgs(String[] alertLocArgs) {
        if(alertLocArgs == null){
            this.alertLocArgs = new String[0];
        } else {
            this.alertLocArgs = Arrays.copyOf(alertLocArgs,alertLocArgs.length);
        }
    }

    public Integer getContentAvailable() {
        return contentAvailable;
    }

    public void setContentAvailable(Integer contentAvailable) {
        this.contentAvailable = contentAvailable;
    }
}
