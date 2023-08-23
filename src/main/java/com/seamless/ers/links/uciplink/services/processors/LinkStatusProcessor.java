package com.seamless.ers.links.uciplink.services.processors;

import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.LinkStatusResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;

public class LinkStatusProcessor extends AbstractRequestProcessor<LinkStatusResponse>
{
	public LinkStatusProcessor()
	{
		super(new LinkStatusResponse());
	}

	public String getRequestTypeId() 
	{
		return LinkStatusProcessor.class.getName();
	}

	public void process() 
	{
		boolean isConnected = getConfig().getUcipAdaptor().getInstance().isConnected();
		
		if(isConnected == true)
		{
			setResult(new LinkStatusResponse(ERSWSLinkResultCodes.SUCCESS));
		}
		else
		{
			setResult(new LinkStatusResponse(ERSWSLinkResultCodes.LINK_DOWN));
		}
	}
	
	
}
