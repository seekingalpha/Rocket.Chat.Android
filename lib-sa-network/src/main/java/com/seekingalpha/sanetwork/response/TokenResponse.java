package com.seekingalpha.sanetwork.response;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {
    @SerializedName("rc_token")
    private String rcToken;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("error")
    private ErrorLogin error;

    public String getRcToken() {
        return rcToken;
    }

    public String getUserId() {
        return userId;
    }

    public ErrorLogin getError() {
        return error;
    }

    public static class ErrorLogin {

        @SerializedName("code")
        private int code;

        @SerializedName("msg")
        private String msg;

        public int getErrorCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}