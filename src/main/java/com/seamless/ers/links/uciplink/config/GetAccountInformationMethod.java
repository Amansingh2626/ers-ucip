package com.seamless.ers.links.uciplink.config;

public enum GetAccountInformationMethod {
    GET_ACCOUNT_DETAILS(0),
    GET_BALANCE_AND_DATE(1);

    final int value;

    GetAccountInformationMethod(int value) {
        this.value = value;
    }

    public static GetAccountInformationMethod lookup(int lookupValue){
        for(GetAccountInformationMethod val : GetAccountInformationMethod.values())
        {
            if(val.value == lookupValue){
                return val;
            }
        }
        return null;
    }

    public static GetAccountInformationMethod lookupByName(String lookupValue){
        for(GetAccountInformationMethod val : GetAccountInformationMethod.values())
        {
            if(val.name().equals(lookupValue)){
                return val;
            }
        }
        return null;
    }
}
