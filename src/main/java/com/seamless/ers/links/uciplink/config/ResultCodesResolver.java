package com.seamless.ers.links.uciplink.config;

import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.links.uciplink.UCIPLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class ResultCodesResolver
{
    public int getMapping(int ucipCode, HashMap<Integer, ResultRow> codesMap)
    {
        Logger log = LoggerFactory.getLogger(ResultCodesResolver.class);
        int mapping;

        if (codesMap != null && codesMap.get(ucipCode) != null)
        {
            mapping = codesMap.get(ucipCode).getErsCode();
        }
        else
        {
            log.debug("Code " + ucipCode + " is not mapped to any ERS code; mapping to LINK_ERROR.");
            mapping = ERSWSLinkResultCodes.LINK_ERROR;
        }

        return mapping;
    }
}

