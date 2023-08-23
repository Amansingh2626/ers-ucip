package com.seamless.ers.links.uciplink.commandProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.ers.interfaces.ersifextlink.dto.ERSLinkResponse;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;

public class RequestProcessorHandler
{
	UCIPLinkConfig config;
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	

	public RequestProcessorHandler(UCIPLinkConfig config)
	{
		super();
		this.config = config;
	}

	/**
	 * Processes the specified request processor using flow control
	 * @param processor
	 * @throws Exception 
	 */
	
	public <T extends ERSLinkResponse> T handleRequest(AbstractRequestProcessor<T> processor) throws Exception {
		processor.initialize(getConfig());
		getConfig().getFlowControlHandler().handleRequest(processor);
		return processor.getResult();
	}

	public UCIPLinkConfig getConfig()
	{
		return config;
	}

}
