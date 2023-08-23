package com.seamless.ers.links.uciplink.schedulers;

import com.seamless.ers.interfaces.ersifextlink.dto.ERSLinkResponse;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.commandProcessor.RequestProcessorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeepAliveRunner implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveRunner.class);
    private RequestProcessorHandler handler;
    private UCIPLinkConfig config;

    public KeepAliveRunner(RequestProcessorHandler handler, UCIPLinkConfig config) {
        this.handler = handler;
        this.config = config;
    }

    @Override
    public void run() {

        try
        {
            ERSLinkResponse ersLinkResponse = handler.handleRequest(new GetCapabilitiesProcessor(config));
            logger.info("Keep-alive ping result: " + ersLinkResponse.getResultCode() + " : " + ersLinkResponse.getResultString());
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }

    }
}
