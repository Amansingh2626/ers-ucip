package com.seamless.ers.links.uciplink.rules;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.common.ERSConfigurationException;
import com.seamless.common.ExtendedProperties;
import com.seamless.common.config.ConfigurationFileHandler;
import com.seamless.common.config.PropertiesUpdateHandler;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.BusinessTransactionData;
import com.seamless.ers.interfaces.ersifextlink.dto.ERSLinkResponse;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;

/**
 * The class executes processors based on rule ids and maintain the mapping with help of a property file, 
 * and the rule ids have to be matched by the rule ids defined in the txe businesslogic property file.
 * <p>
 * The mapping between ruleIds and processors is defined with the following syntax in the property file:
 * <ul>
 * <li><rule identifier>.<method name>.<key>=<value>
 * </ul>
 * where method name is either validate, failed, and/or completed.
 *
 */
public class RuleRequestHandler implements PropertiesUpdateHandler
{
	private final String RULE_FILE = "rules.properties";
	
	Logger logger = LoggerFactory.getLogger(RuleRequestHandler.class);	
	Hashtable<String, ExtendedProperties> validateMap;
	Hashtable<String, ExtendedProperties> completedMap;
	Hashtable<String, ExtendedProperties> failedMap;
	UCIPLinkConfig config;

	public RuleRequestHandler(UCIPLinkConfig config) throws ERSConfigurationException
	{
		this.config = config;
		config.addFile(RULE_FILE, 
				new ConfigurationFileHandler()
		{
			public void loadConfiguration(File file)
					throws ERSConfigurationException
			{
				try
				{
					logger.info("Loading configuration from: " + RULE_FILE);
					ExtendedProperties properties = new ExtendedProperties();
					properties.loadFromFile(file);
					propertiesLoaded(properties);
				}
				catch (IOException e)
				{
					throw new ERSConfigurationException("Failed to read: " + RULE_FILE, RULE_FILE, e);
				}
			}
		});
	}
		
		

	public void propertiesLoaded(ExtendedProperties properties)
	throws ERSConfigurationException
	{
		validateMap = new Hashtable<String, ExtendedProperties>();
		completedMap = new Hashtable<String, ExtendedProperties>();
		failedMap = new Hashtable<String, ExtendedProperties>();
		Set<String> ruleIds = properties.truncatedKeys(".");
		for (String ruleId : ruleIds)
		{
			// validate task
			
			ExtendedProperties ruleProperties = new ExtendedProperties(ruleId + ".validate.", properties);
			
			if (ruleProperties.propertyNames().hasMoreElements())
			{
				validateMap.put(ruleId, ruleProperties);
			}
			
			// performed task

			ruleProperties = new ExtendedProperties(ruleId + ".completed.");
			if (ruleProperties.propertyNames().hasMoreElements())
			{
				completedMap.put(ruleId, ruleProperties);
			}
			
			// completed task
			
			ruleProperties = new ExtendedProperties(ruleId + ".failed.");
			if (ruleProperties.propertyNames().hasMoreElements())
			{
				failedMap.put(ruleId, ruleProperties);
			}
		}
		
	}
		
	/**
	 * Executes a validate method call.
	 * 
	 * @param ruleId the rule id.
	 * @param transaction the transaction data.
	 * @return a link response message with success code, otherwise a link response message with reason code.
	 */
	public ERSLinkResponse validate(String ruleId, BusinessTransactionData transaction)
	{
		return execute(validateMap, ruleId, transaction);
	}
	
	/**
	 * Executes a failed method call.
	 * 
	 * @param ruleId the rule id.
	 * @param transaction the transaction data.
	 * @return a link response message with success code, otherwise a link response message with reason code.
	 */
	public ERSLinkResponse failed(String ruleId, BusinessTransactionData transaction)
	{
		return execute(failedMap, ruleId, transaction);
	}
	
	/**
	 * Executes a completed method call.
	 * 
	 * @param ruleId the rule id.
	 * @param transaction the transaction data.
	 * @return a link response message with success code, otherwise a link response message with reason code.
	 */
	public ERSLinkResponse completed(String ruleId, BusinessTransactionData transaction)
	{
		return execute(completedMap, ruleId, transaction);
	}
	
	/**
	 * Maps and executes a processor for a given rule id with data.
	 * 
	 * @param ruleMap the rule mapping to be used
	 * @param ruleId the rule id
	 * @param transaction the transaction data
	 * @return a link response message with success code, otherwise a link response message with reason code
	 */
	private ERSLinkResponse execute(Hashtable<String, ExtendedProperties> ruleMap, String ruleId, BusinessTransactionData transaction)
	{
		ExtendedProperties methodProperties = ruleMap.get(ruleId);
		if (methodProperties == null || methodProperties.getProperty("classname") == null)
		{
			return new ERSLinkResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
		}
		
		String className = methodProperties.getProperty("classname");

		if (className.indexOf(".") < 0)
		{
			className = "com.seamless.ers.links.uciplink.rules.processors." + className;
		}
		
		AbstractRequestProcessor<ERSLinkResponse> processor;

		try
		{
			Class<?> c = Class.forName(className);
			Constructor<?> constructor = c.getConstructor(String.class, BusinessTransactionData.class, ExtendedProperties.class);
			if (constructor == null)
			{
				String error = "Unable to create processor for rule: " + ruleId;
				logger.error(error);
				return new ERSLinkResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
			}
			
			processor = (AbstractRequestProcessor<ERSLinkResponse>) constructor.newInstance(ruleId, transaction, methodProperties);
			processor.setConfig(config);
			processor.process();
		}
		catch (Throwable t)
		{
			String error = "Failed to create and execute processor for rule: " + ruleId;
			logger.error(error, t);
			return new ERSLinkResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		
		return processor.getResult();
	}

	

}
