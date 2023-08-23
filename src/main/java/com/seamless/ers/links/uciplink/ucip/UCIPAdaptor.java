package com.seamless.ers.links.uciplink.ucip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.common.ExtendedProperties;
import com.seamless.common.uciplib.common.UCIPException;
import com.seamless.common.uciplib.common.UCIPProtocolVersion;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;

public class UCIPAdaptor
{
	Logger logger = LoggerFactory.getLogger(UCIPAdaptor.class);

	IUCIPAdaptor instance = null;

	public UCIPAdaptor(UCIPLinkConfig config, ExtendedProperties properties)
	{
		UCIPProtocolVersion ucipVersion;
		try
		{
			ucipVersion = UCIPProtocolVersion.parseVersion(
					properties.getProperty("ucipVersion"),
					UCIPProtocolVersion.UCIP_V31);
		}
		catch (UCIPException e)
		{
			logger.error("Unknown UCIP version specified in configuration!");
			logger.error("Using default version! (3.1)");
			ucipVersion = UCIPProtocolVersion.UCIP_V31;
		}

		switch (ucipVersion)
		{
		case UCIP_V20:
		case UCIP_V22:
			instance = new UCIPAdaptor20(config, properties);
			break;
		case UCIP_V31:
			instance = new UCIPAdaptor31(config, properties);
			break;
		case UCIP_V34:
			instance = new UCIPAdaptor34(config, properties);
			break;
		case UCIP_V40:
			instance = new UCIPAdaptor40(config, properties);
			break;
		case UCIP_V42:
			instance = new UCIPAdaptor42(config, properties);
			break;
		case UCIP_V50:
			instance = new UCIPAdaptor50(config, properties);
			break;
		}
	}

	public IUCIPAdaptor getInstance()
	{
		if (instance == null)
		{
			logger.error("FATAL ERROR: UCIPAdaptor not initialized properly!");
		}

		return instance;
	}

}
