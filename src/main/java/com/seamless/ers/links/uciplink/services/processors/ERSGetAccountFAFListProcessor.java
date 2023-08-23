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
 * ERSGetAccountFAFListProcessor is a processor for retrieving list of friend and family MSISDN belonged to a specific subscriber account.
 * @author Jiraporn
 *
 */
public class ERSGetAccountFAFListProcessor extends AbstractRequestProcessor<AccountVASOperationResponse>
{

	Logger logger = LoggerFactory.getLogger(ERSGetAccountFAFListProcessor.class);
	/**
	 * Subscriber account 
	 * The subscriber account type is airtime.
	 */
	private Account account;
	/**
	 * ERS transaction reference 
	 */
	private String  reference;
	
	private String nativeReference;
	
	/**
	 * constructor
	 * @param account
	 * @param reference
	 */
	public ERSGetAccountFAFListProcessor(Account account, String reference, String nativeReference)
	{
		super(new AccountVASOperationResponse());
		
		this.account = account;
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
	 * 1. Get FAF_NUMBER list using getAccountFAF
	 * @return
	 */
	protected AccountVASOperationResponse _process() 
	{
		AccountVASOperationResponse response = new AccountVASOperationResponse(ERSWSLinkResultCodes.INTERNAL_FAILED, "FAF_INFO");
		
		UCIPResponse ucipResponse = getConfig().getUcipAdaptor().getInstance().getAccountFAF(account.getAccountId(), nativeReference);
		StringBuffer fafList = new StringBuffer();
		if (ucipResponse != null)
		{
			
			if (ucipResponse.isSuccessful())
			{	
				response.setField("fafList", "");

				FafInformation[] fafInformation = ucipResponse.getFafList();
				if (fafInformation != null)
				{
					for (int index = 0; index < fafInformation.length; index++)
					{
						fafList.append(",");
						fafList.append(fafInformation[index].getFafNumber());
					}
					if (fafList.toString().length() > 0)
					{
						response.setField("fafList", fafList.toString().substring(1));
					}
				}
				response.setRetryable(false);				
				response.setResultCode(ERSWSLinkResultCodes.SUCCESS);				
			}		
			else
			{
				response.setResultCode(ucipResponse.getErsResponseCode());
				if(response.getResultCode() == ERSWSLinkResultCodes.LINK_ERROR)
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

