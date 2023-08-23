package com.seamless.ers.links.uciplink.config;

public enum ValidateTopupMethod {
    GET_ACCOUNT_DETAILS(0),
    GET_BALANCE_AND_DATE(1);

    final int value;

    ValidateTopupMethod(int value) {
        this.value = value;
    }

    public static ValidateTopupMethod lookup(int lookupValue){
        for(ValidateTopupMethod val : ValidateTopupMethod.values())
        {
            if(val.value == lookupValue){
                return val;
            }
        }
        return null;
    }

    public static ValidateTopupMethod lookupByName(String lookupValue){
        for(ValidateTopupMethod val : ValidateTopupMethod.values())
        {
            if(val.name().equals(lookupValue)){
                return val;
            }
        }
        return null;
    }
}
