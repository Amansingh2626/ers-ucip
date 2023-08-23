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
 * ERSGetAccountClassProcessor is a processor for retrieving current service class of a specific subscriber account.
 * @author Jiraporn Yindeemoh
 *
 */
public class ERSGetAccountClassProcessor extends AbstractRequestProcessor<AccountVASOperationResponse>
{

	Logger logger = LoggerFactory.getLogger(ERSGetAccountClassProcessor.class);
	/**
	 * Subscriber account 
	 * The subscriber account type is airtime.
	 */
	Account account;

	/**
	 * ERS transaction reference 
	 */
	private String reference;
	
	/*
	 * The native reference used to talk to the CS.
	 */
	private String nativeReference;

	private ERSHashtableParameter extraFields;

	
	/**
	 * Constructor
	 * @param account
	 * @param reference
	 */
	public ERSGetAccountClassProcessor(Account account, String reference, String nativeReference, ERSHashtableParameter extraFields)
	{
		super(new AccountVASOperationResponse());
		
		this.account = account;
		this.reference = reference;
		this.nativeReference = nativeReference;
		this.extraFields = extraFields;
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
	 * 1. Get current service class using getAccountDetails
	 * 2. Set data back to AccountClassResponse
	 * @return
	 */
	protected AccountVASOperationResponse _process() 
	{

		AccountVASOperationResponse response = new AccountVASOperationResponse(ERSWSLinkResultCodes.INTERNAL_FAILED, "SC_INFO");
		
		UCIPResponse ucipResponse = getConfig().getUcipAdaptor().getInstance().getAccountDetails(account.getAccountId(), nativeReference, extraFields);
		
		
		if (ucipResponse != null)
		{
			logger.debug("Recieved response: "+ ucipResponse.toString());
			
			if (ucipResponse.isSuccessful())
			{	
				response.setField("accountClassIdBefore", ucipResponse.getCurrentServiceClass() + "");
				response.setField("accountClassIdAfter", ucipResponse.getCurrentServiceClass() + "");
				response.setRetryable(false);
				response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
				response.setField("accountClassId", ucipResponse.getCurrentServiceClass() + "");		
			}	
			else
			{
				// Translate result code and return
				response.setResultCode(ucipResponse.getErsResponseCode());
				if (response.getResultCode() == ERSWSLinkResultCodes.LINK_ERROR)
				{
					response.setRetryable(true);
				}
				else
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
