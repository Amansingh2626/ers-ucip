package com.seamless.ers.links.uciplink;

import com.seamless.common.ERSConfigurationException;
import com.seamless.ers.links.uciplink.commandProcessor.RequestProcessorHandler;
import com.seamless.ers.links.uciplink.schedulers.KeepAliveScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Endpoint;


public class UCIPLink
{
	static Logger log = LoggerFactory.getLogger(UCIPLink.class);
	
	UCIPLinkConfig config;
	
	public UCIPLink(String linkName) throws ERSConfigurationException
	{
		log.info("Initializing UCIPLink link '" + linkName + "'");
		config = new UCIPLinkConfig(linkName, true);
	}

	public static void main(String[] args)
	{
		try
		{								
			String linkName = "uciplink";
			if (args.length > 0)
				linkName = args[0];
			
			log.info("Starting UCIPLink '" + linkName + "': $Id$");
			
			UCIPLink ucipLink = new UCIPLink(linkName);
			ucipLink.publish();
			
			log.info("UCIPLink link '" + linkName + "' has started");

			// Wait until stopped
			synchronized (ucipLink.config)
			{
				while (ucipLink.config.isRunning())
					ucipLink.config.wait();
			}
			
			log.info("Shutting down UCIPLink link '" + linkName + "'");
		}
		catch (InterruptedException e)
		{
			log.info("Interrupted... exiting...");
		}
		catch (Throwable e)
		{
			log.error("Caught Exception, exiting...", e);
		}
		
		System.exit(0);

	}


	public void publish()
	{
		RequestProcessorHandler reqProcessorHandler = new RequestProcessorHandler(config);
		
		Endpoint.publish(config.getOperationsURL(), new UCIPLinkOperationsImpl(reqProcessorHandler));
		Endpoint.publish(config.getServicesURL(), new UCIPLinkServicesImpl(reqProcessorHandler));
		Endpoint.publish(config.getManagementURL(), new UCIPLinkManagementImpl(reqProcessorHandler));
		Endpoint.publish(config.getBusinessLogicRuleURL(), new UCIPLinkBusinessLogicRuleImpl(reqProcessorHandler));
		if(config.enableKeepAlive())
		{
			new KeepAliveScheduler(config, reqProcessorHandler);
		}

		log.info("Published operations on " + config.getOperationsURL());
		log.info("Published services on " + config.getServicesURL());
		log.info("Published management on " + config.getManagementURL());
		log.info("Published businesslogic on " + config.getBusinessLogicRuleURL());
	}
	
}
