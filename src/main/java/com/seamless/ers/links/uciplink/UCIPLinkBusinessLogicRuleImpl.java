package com.seamless.ers.links.uciplink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.log4j.MDC;

import com.seamless.ers.interfaces.ersifextlink.ERSWSBusinessLogicRuleLink;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.BusinessTransactionData;
import com.seamless.ers.interfaces.ersifextlink.dto.ERSLinkResponse;
import com.seamless.ers.interfaces.ersifextlink.dto.LinkStatusResponse;
import com.seamless.ers.links.uciplink.commandProcessor.RequestProcessorHandler;

import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmPoint;



/**
 * The class exposes a web service interface for executing business logic rules.
 *
 */
public class UCIPLinkBusinessLogicRuleImpl implements ERSWSBusinessLogicRuleLink
{
	private static final String ERS_REFERENCE = "ersReference";
	private static final Logger logger = LoggerFactory.getLogger(UCIPLinkBusinessLogicRuleImpl.class);	
	private RequestProcessorHandler handler;

	public UCIPLinkBusinessLogicRuleImpl(RequestProcessorHandler reqProcessorHandler)
	{
		this.handler = reqProcessorHandler;
	}
	
	public ERSLinkResponse validate(String ruleId,
			BusinessTransactionData transaction)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkBusinessLogicRuleImpl::validate");
		
		try
		{
			MDC.put(ERS_REFERENCE, transaction.getErsReference());
			return handler.getConfig().getBusinessRuleDispatcher().validate(ruleId, transaction);
		}
		catch (Exception e)
		{
			String error = "Failed to process validate call with error: " + e.getMessage();
			logger.error(error, e);
			ERSLinkResponse resp = new ERSLinkResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
			resp.setResultDescription(e.getMessage());
			return resp;
		}		
		finally
		{
			point.collect();
			MDC.remove(ERS_REFERENCE);
		}
	}	
	
	public ERSLinkResponse completed(String ruleId,
			BusinessTransactionData transaction)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkBusinessLogicRuleImpl::completed");
		
		try
		{
			return handler.getConfig().getBusinessRuleDispatcher().completed(ruleId, transaction);
		}
		catch (Exception e)
		{
			String error = "Failed to process completed call with error: " + e.getMessage();
			logger.error(error, e);
			ERSLinkResponse resp = new ERSLinkResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
			resp.setResultDescription(e.getMessage());
			return resp;
		}		
		finally
		{
			point.collect();
		}
	}

	public ERSLinkResponse failed(String ruleId,
			BusinessTransactionData transaction)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkBusinessLogicRuleImpl::failed");
		
		try
		{
			return handler.getConfig().getBusinessRuleDispatcher().failed(ruleId, transaction);
		}
		catch (Exception e)
		{
			String error = "Failed to process failed call with error: " + e.getMessage();
			logger.error(error, e);
			ERSLinkResponse resp = new ERSLinkResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
			resp.setResultDescription(e.getMessage());
			return resp;
		}		
		finally
		{
			point.collect();
		}
	}

	public LinkStatusResponse getLinkStatus()
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkBusinessLogicRuleImpl::getLinkStatus");
		
		try
		{
			return new LinkStatusResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
		}
		finally
		{
			point.collect();
		}
	}
}
