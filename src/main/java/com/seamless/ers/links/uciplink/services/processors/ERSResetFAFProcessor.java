package com.seamless.ers.links.uciplink.services.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.common.uciplib.common.FafInformation;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountVASOperationResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;

/**
 * The processor resets/clears the FAF list belonged to a specific subscriber account.
 *
 * The processor retrieves the current FAF list for the subscriber from the CS and tries 
 * to remove one by one. 
 *
 * The processor sets the subscribers original FAF list in the extra fields to be used 
 * for TDR logging in case for failure as the processor do not support atomic rollbacks.
 */
public class ERSResetFAFProcessor extends AbstractRequestProcessor<AccountVASOperationResponse>
{

	Logger logger = LoggerFactory.getLogger(ERSResetFAFProcessor.class);
	private Account account;
	private String  reference;
	private String  nativeReference;

	public ERSResetFAFProcessor(Account account, String reference, String nativeReference)
	{
		super(new AccountVASOperationResponse());
		
		this.account = account;
		this.reference = reference;
		this.nativeReference = nativeReference;
	}

	public void process()
	{
		setResult(_process());
	}

	protected AccountVASOperationResponse _process() 
	{
		AccountVASOperationResponse response = new AccountVASOperationResponse(ERSWSLinkResultCodes.INTERNAL_FAILED, "FAF_RESET");

		
		UCIPResponse ucipResponse = getConfig().getUcipAdaptor().getInstance().getAccountFAF(account.getAccountId(), nativeReference);
		if (ucipResponse == null)
		{
			logger.error("Failed to retreive the FAF list with null response from ucip adaptor, check Air node availability!");
			response.setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
			return response;
		}
		
		if (!ucipResponse.isSuccessful())
		{
			logger.debug("Failed to retrieve the FAF list for subscriber: " + account.getAccountId());
			response.setResultCode(ucipResponse.getErsResponseCode());
			if(response.getResultCode() == ERSWSLinkResultCodes.LINK_ERROR)
			{
				response.setRetryable(true);
			}
			else
			{
				response.setRetryable(false);
			}			
			return response;
		}
		
		// Check if no FAF information or empty FAF list then we are done.
		
		FafInformation[] fafInformation = ucipResponse.getFafList();
		if (fafInformation == null || fafInformation.length == 0)
		{
			response.setField("fafList", "");
			response.setRetryable(false);
			response.setResultCode(ERSWSLinkResultCodes.SUCCESS);				
			return response;
		}
		
		
		// Set the complete FAF list in the response so that it can be used for TDR logging in case of failure.
		
		StringBuffer fafNumberList = new StringBuffer();
		for (int index = 0; index < fafInformation.length; index++)
		{
			fafNumberList.append(",");
			fafNumberList.append(fafInformation[index].getFafNumber());
		}
		String fafNumbers = fafNumberList.toString().substring(1);
		response.setField("fafList", fafNumbers);
		
		// Remove each fafNumber from the FAF list for the subscriber.
		for (int i = 0; i < fafInformation.length; i++)
		{
			FafInformation fafEntry = fafInformation[i];
			ucipResponse = getConfig().getUcipAdaptor().getInstance().removeAccountFAF(account.getAccountId(), fafEntry.getFafNumber(), nativeReference);
			if (ucipResponse == null)
			{
				logger.error("Failed to remove fafNumber from the subscriber with null response from ucip adaptor for the subscriber with list: " + fafNumbers);
				response.setResultCode(ERSWSLinkResultCodes.LINK_ERROR);
				return response;
			}
			if (!ucipResponse.isSuccessful())
			{
				logger.error("Failed to remove fafNumber from the subscriber with list: " + fafNumbers);
				response.setResultCode(ucipResponse.getErsResponseCode());
				if(response.getResultCode() == ERSWSLinkResultCodes.LINK_ERROR)
				{
					response.setRetryable(true);
				}
				else
				{
					response.setRetryable(false);
				}							
				return response;
			}						
		}
				
		
		response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
		response.setRetryable(false);
		
		return response;
	}

	public String getRequestTypeId()
	{
		return getClass().getName();

	}

}

