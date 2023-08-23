package com.seamless.ers.links.uciplink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UCIPLinkBaseImpl
{
	private UCIPLinkConfig config;
	protected Logger log = LoggerFactory.getLogger(this.getClass());
	public void setConfig(UCIPLinkConfig config)
	{
		this.config = config;
	}
	public UCIPLinkConfig getConfig()
	{
		return config;
	}
	
}
