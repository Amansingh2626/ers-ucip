package com.seamless.ers.links.uciplink.schedulers;

import com.seamless.ers.interfaces.ersifextlink.dto.ERSLinkResponse;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetCapabilitiesProcessor extends
		AbstractRequestProcessor<ERSLinkResponse>
{
	private static final Logger logger = LoggerFactory.getLogger(GetCapabilitiesProcessor.class);

	private UCIPLinkConfig config;


	public GetCapabilitiesProcessor(UCIPLinkConfig config)
	{
		super(new ERSLinkResponse());

		this.config = config;
	}

	public void process()
	{
		setResult(_process());
	}

	protected ERSLinkResponse _process()
	{
		ERSLinkResponse response = new ERSLinkResponse();

		UCIPResponse ucipResponse = getConfig().getUcipAdaptor().getInstance().getCapabilities();

		response.setResultCode(ucipResponse.getUcipResponseCode());

		return response;

	}

	public String getRequestTypeId()
	{
		return getClass().getName();
	}

}
