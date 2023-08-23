package com.seamless.ers.links.uciplink.commandProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.common.servertools.flowcontrol.ClientRejectReason;
import com.seamless.common.servertools.flowcontrol.RequestProcessor;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.ERSLinkResponse;
import com.seamless.ers.links.uciplink.UCIPLinkBaseImpl;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;

public abstract class AbstractRequestProcessor<T extends ERSLinkResponse> extends UCIPLinkBaseImpl 
implements RequestProcessor
{
	Logger logger = LoggerFactory.getLogger(AbstractRequestProcessor.class);
	protected T result;
	
	public AbstractRequestProcessor(T result)
	{
		this.result = result;
	}
	
	/**
	 * Initializes the processor with specified config
	 * @param config
	 */
	public void initialize(UCIPLinkConfig config) {
		super.setConfig(config);
	}
	
	/**
	 * Returns result object of the processor execution
	 * @return
	 */
	public T getResult() {
		return result;
	}
	
	/**
	 * Sets result object of the processor execution
	 * @return
	 */
	protected void setResult(T result) {
		this.result = result;
	}
	
	public boolean dispatch()
	{
		// ERS Requests does not support asynchronous operations, so return false
		return false;
	}

	public void reject(ClientRejectReason rejectReason)
	{
		if (rejectReason == ClientRejectReason.OVERLOAD)
		{
			String text = "The system is overloaded, rejecting incoming requests";
			logger.warn(text);
			getResult().setResultCode(ERSWSLinkResultCodes.LINK_EXHAUSTED);
			getResult().setResultDescription(text);
		}
		else if (rejectReason == ClientRejectReason.THROTTLE)
		{
			String text = "The system has been overloaded and is now in a cooldown period, rejecting incoming requests";
			logger.warn(text);
			getResult().setResultCode(ERSWSLinkResultCodes.LINK_EXHAUSTED);
			getResult().setResultDescription(text);
		}
		else if (rejectReason == ClientRejectReason.SERVICE_DOWN)
		{
			String text = "The service is down, rejecting incoming requests";
			logger.info(text);
			getResult().setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
			getResult().setResultDescription(text);
		}
		else
		{
			String text = "The service received an internal error code from the flow control dispatcher, rejecting incoming requests";
			logger.error(text);
			getResult().setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
			getResult().setResultDescription(text);
		}

	}
	
	protected void transformAmountCurrency(Amount amount, String currencyCode) {
		if(amount != null && amount.getCurrency() != null && currencyCode != null) {
			amount.setCurrency(currencyCode);
		}
	}
	
	protected void transformUcipResponseCurrency(UCIPResponse ucipResponse, String currencyCode) {
		if(ucipResponse != null)
		{
			transformAmountCurrency(ucipResponse.getBalance(), currencyCode);
			transformAmountCurrency(ucipResponse.getBalanceAfter(), currencyCode);
			transformAmountCurrency(ucipResponse.getBalanceBefore(), currencyCode);
		}
	}

}
