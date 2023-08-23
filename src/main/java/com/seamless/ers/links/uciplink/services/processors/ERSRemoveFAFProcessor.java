package com.seamless.ers.links.uciplink.services.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountVASOperationResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;
/**
 * ERSRemoveFAFProcessor is a processor for removing an existing friend and family MSISDN from a specific subscriber account.
 * @author Jiraporn Yindeemoh
 *
 */
public class ERSRemoveFAFProcessor extends AbstractRequestProcessor<AccountVASOperationResponse>
{
	Logger logger = LoggerFactory.getLogger(ERSRemoveFAFProcessor.class);
	/**
	 * Subscriber account 
	 * The subscriber account type is airtime.
	 */
	private Account	subscriber;
	/**
	 * FAF number (MSISDN) wanted to remove 
	 */
	private String 	fafNumber;
	/**
	 * Extra parameters for VAS operation.
	 * Expected parameter key in the parameters is FAF_NUMBER
	 */
	private ERSHashtableParameter parameters;
	/**
	 * ERS transaction reference 
	 */
	private String reference;
	
	private String nativeReference;
	
	/**
	 * Constructor
	 * @param subscriber
	 * @param parameters
	 * @param reference
	 */
	public ERSRemoveFAFProcessor(Account subscriber , ERSHashtableParameter parameters,String reference, String nativeReference)
	{
		super(new AccountVASOperationResponse());
		this.subscriber = subscriber;
		this.parameters = parameters;
		this.reference = reference;
		this.nativeReference = nativeReference;
	}
	/**
	 * Process method according to AbstractRequestProcessor interface
	 */
	public void process()
	{
		setResult(_process());
		
	}
	/**
	 * Actual process method 
	 * 1. Extract FAF_NUMBER from parameters
	 * 2. Remove FAF number using removeAccountFAF
	 * @return
	 */
	protected AccountVASOperationResponse _process()
	{
		AccountVASOperationResponse response = new AccountVASOperationResponse();
		
		if (parameters == null || parameters.get("fafNumber") == null)
		{
			response.setResultCode(ERSWSLinkResultCodes.INVALID_FAF_NUMBER);
			return response;
		}	
		fafNumber = parameters.get("fafNumber");
		
		UCIPResponse ucipResponse = getConfig().getUcipAdaptor().getInstance().removeAccountFAF(subscriber.getAccountId(), fafNumber, nativeReference);
		if (ucipResponse != null)
		{
			
			if (ucipResponse.isSuccessful())
			{
				response.setRetryable(false);
				response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
				return response;	
			}
			else
			{
				response.setResultCode(ucipResponse.getErsResponseCode());
				if (response.getResultCode() == ERSWSLinkResultCodes.LINK_ERROR)
				{
					response.setRetryable(true);
				}else
				{
					response.setRetryable(false);
				}
			}	
		}	
		else
		{
			logger.error("Null response from ucip adaptor, check Air node availability!");
		}
		return response;
	}
	public String getRequestTypeId()
	{
		return getClass().getName();

	}
}
