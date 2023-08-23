package com.seamless.ers.links.uciplink.schedulers;

public class GetCapabilitiesResponse {
    private int resultCode;

    public GetCapabilitiesResponse(int resultCode) {
        this.resultCode = resultCode;
    }

    public GetCapabilitiesResponse() {
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }
}
