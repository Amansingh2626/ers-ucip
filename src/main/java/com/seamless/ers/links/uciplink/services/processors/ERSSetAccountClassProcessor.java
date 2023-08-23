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
 * ERSSetAccountClassProcessor is a processor for setting a new service class to a specific subscriber account.
 * @author Jiraporn Yindeemoh
 *
 */
public class ERSSetAccountClassProcessor extends AbstractRequestProcessor<AccountVASOperationResponse>
{

	Logger logger = LoggerFactory.getLogger(ERSSetAccountClassProcessor.class);
	/**
	 * Subscriber account 
	 * The subscriber account type is airtime.
	 */
	private Account account;
	/**
	 * Extra parameters for VAS operation.
	 * Expected parameter key in the extrafields is ACCOUNT_CLASS_ID
	 */
	private ERSHashtableParameter extraFields;
	/**
	 * ERS transaction reference 
	 */
	
	private String nativeReference;
	
	private String reference;
	/**
	 * Constructor
	 * @param account
	 * @param extraFields
	 * @param reference
	 */
	public ERSSetAccountClassProcessor(Account account, ERSHashtableParameter extraFields, String reference, String nativeReference)
	{
		super(new AccountVASOperationResponse());
		
		this.account = account;
		this.extraFields = extraFields;
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
	 * 1. Extract ACCOUNT_CLASS_ID from extraFields
	 * 2. Retrieve current service class using getAccountDetails
	 * 3. Set current service class to accountClassIdBefore
	 * 4. Set new service class using updateServiceClass
	 * 5. Set new service class to accountClassIdAfter
	 * @return
	 */
	protected AccountVASOperationResponse _process() 
	{
		String accountClassId = extraFields.get("accountClassId");
		
		if (accountClassId == null)
		{
			return new AccountVASOperationResponse("CHANGE_SERVICECLASS", ERSWSLinkResultCodes.MISSING_PARAMETER_FOR_VAS_OPERATION);
		}
					
		
		int newServiceClassId = 0;
		AccountVASOperationResponse response = new AccountVASOperationResponse(ERSWSLinkResultCodes.INTERNAL_FAILED, "SC_CHANGE");
		
		try
		{
			newServiceClassId = Integer.parseInt(accountClassId);
		} 
		catch(Exception e)
		{
			logger.error("Invalid format for new service class id!");
			response.setResultCode(ERSWSLinkResultCodes.INVALID_ACCOUNT_CLASS_ID);
			return response;
		}		
		
		UCIPResponse ucipGetInfoResponse = getConfig().getUcipAdaptor().getInstance().getAccountDetails(account.getAccountId(), nativeReference,extraFields);
		logger.debug("Recieved get account class response: "+ ucipGetInfoResponse);
		
		if (ucipGetInfoResponse == null)
		{
			return response;
		}
		
		if (!ucipGetInfoResponse.isSuccessful())
		{
			response.setResultCode(ucipGetInfoResponse.getErsResponseCode());
			return response;			
		}
		
		if (newServiceClassId == ucipGetInfoResponse.getCurrentServiceClass())
		{
			response.setResultCode(ERSWSLinkResultCodes.ACCOUNT_CLASS_ALREADY_ACTIVE);
			return response;
		}
		
		response.setField("accountClassIdBefore", ucipGetInfoResponse.getCurrentServiceClass() + "");
		
		
		UCIPResponse ucipSetInfoResponse = getConfig().getUcipAdaptor().getInstance().updateServiceClass(account.getAccountId(), newServiceClassId,nativeReference,extraFields);
		logger.debug("Recieved set account class response: "+ ucipSetInfoResponse);	
		
		if (ucipSetInfoResponse != null && ucipSetInfoResponse.isSuccessful())
		{	
			response.setRetryable(false);
			response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
			response.setField("accountClassIdAfter", newServiceClassId + "");
			response.setField("accountClassIdRequested", newServiceClassId + "");
		
			return response;			
		}	
		else if (ucipSetInfoResponse != null)
		{
			response.setResultCode(ucipSetInfoResponse.getErsResponseCode());
		}
		
		return response;
	}


	public String getRequestTypeId()
	{
		return getClass().getName();

	}	

}
