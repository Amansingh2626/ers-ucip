package com.seamless.ers.links.uciplink.services.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;

import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountInformationResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;


public class ERSValidateAccountProcessor extends AbstractRequestProcessor<AccountInformationResponse>
{

	Logger logger = LoggerFactory.getLogger(ERSValidateAccountProcessor.class);
	
	private String 	accountType; 
	private String 	subscriberNumber;

	private String reference;

	private String nativeReference;

	private ERSHashtableParameter extraFields;


	public ERSValidateAccountProcessor(String accountType, String accountId, String reference, String nativeReference , ERSHashtableParameter extraFields)
	{
		super(new AccountInformationResponse());		
		this.accountType 	  = accountType;
		this.subscriberNumber = accountId;
		this.reference = reference;
		this.nativeReference = nativeReference;
		this.extraFields = extraFields;
	}
	
	public void process()
	{
		setResult(_process());
	}
	
	protected AccountInformationResponse _process() 
	{
		this.subscriberNumber = getConfig().formatMsisdn(subscriberNumber);

		logger.debug("Request recieved to validate account - info: [" + subscriberNumber + ":" + accountType + "]");

		
		AccountInformationResponse response = new AccountInformationResponse();
		
		try
		{
				
			// Get subscriber info to see if it exists in AIR node
			UCIPResponse ucipResponse = getConfig().getUcipAdaptor().getInstance().getAccountDetails(subscriberNumber, nativeReference,extraFields);
						
			if(ucipResponse != null)
			{
				
				if(ucipResponse.isSuccessful())
				{				
					// subscriber found		
					response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
				}				
				else
				{
					// subscriber not found
					response.setResultCode(ERSWSLinkResultCodes.SUBSCRIBER_DOES_NOT_EXIST);
				}
			}
			else
			{
				logger.info("No response from AIR node! check connectivity.");
				response.setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
			}
			
		}
		catch(Exception e)
		{
			logger.error("Problem occured while retrieving account information: ", e);
			response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		
		return response;
	}

	public String getRequestTypeId()
	{
		return getClass().getName();

	}

}
