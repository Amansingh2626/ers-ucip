package com.seamless.ers.links.uciplink.rules.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;

import com.seamless.common.ExtendedProperties;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.BusinessTransactionData;
import com.seamless.ers.interfaces.ersifextlink.dto.ERSLinkResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;

/**
 * The class matches if a service class is legal or not using regular expression defined
 * in a property file.
 * 
 * the properties used are:
 * <ul>
 * <li>classname=ServiceClassMatchProcessor
 * <li>receiver_sc_regexp=regexp for legal sc for sender if set
 * <li>sender_sc_regexp=regexp for legal sc for sender if set
 * </ul>
 *
 * Current implementation matches sender or receiver if properties are set. In case
 * none of them are set, then the processor will return a successful result.
 */
public class ServiceClassMatchProcessor extends AbstractRequestProcessor<ERSLinkResponse>
{
	Logger logger = LoggerFactory.getLogger(ServiceClassMatchProcessor.class);
	
	@SuppressWarnings("unused")
	private String ruleId;
	
	private BusinessTransactionData transaction;

	private String regexpSenderSC;

	private String regexpReceiverSenderSC;
	
	/*
	 * The native reference used to talk to the CS.
	 */
	private String nativeReference;

	private ERSHashtableParameter extraFields;


	public ServiceClassMatchProcessor(String ruleId,
			BusinessTransactionData transaction, ExtendedProperties properties, ERSHashtableParameter extraFields)
	{
		super(new ERSLinkResponse());
		
		this.ruleId = ruleId;
		this.transaction = transaction;
		this.regexpSenderSC = properties.getProperty("sender_sc_regexp", null);
		this.regexpReceiverSenderSC = properties.getProperty("receiver_sc_regexp", null);
		this.extraFields = extraFields;
	}

	public void process()
	{
		this.nativeReference = super.getConfig().createNativeReference(transaction.getErsReference());

			if (transaction.getSenderAccount() != null && transaction.getSenderAccount().getAccountId() != null && regexpSenderSC != null)
		{
			String accountId = transaction.getSenderAccount().getAccountId();
			UCIPResponse ucipResponse = getConfig().getUcipAdaptor().getInstance().getAccountDetails(accountId, nativeReference, extraFields);
			if (ucipResponse == null)
			{
				String error = "Failed to connect to the charging system";
				logger.error(error);
				getResult().setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
				getResult().setResultDescription(error);
				getResult().setNativeReference(nativeReference);
				return;
			}
			
			if (!ucipResponse.isSuccessful())
			{
				String error = "Failed to retrieve service class with link error: " + ucipResponse.getUcipResponseCode();
				logger.error(error);
				getResult().setResultCode(ucipResponse.getErsResponseCode());
				getResult().setResultDescription(error);
				getResult().setNativeReference(nativeReference);				
				return;
			}
			
			String serviceClass = String.valueOf(ucipResponse.getCurrentServiceClass());
			if (!serviceClass.matches(regexpSenderSC))
			{
				getResult().setResultCode(ERSWSLinkResultCodes.SUBSCRIBER_HAS_NO_SUCH_SERVICE);
				getResult().setNativeReference(nativeReference);				
				return;
			}
		}

		if (transaction.getReceiverAccount() != null
				&& transaction.getReceiverAccount().getAccountId() != null
				&& regexpReceiverSenderSC != null)
		{
			String accountId = transaction.getReceiverAccount().getAccountId();
			UCIPResponse ucipResponse = getConfig().getUcipAdaptor().getInstance().getAccountDetails(accountId, nativeReference, extraFields);
			if (ucipResponse == null)
			{
				String error = "Failed to connect to the charging system";
				logger.error(error);
				getResult().setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
				getResult().setResultDescription(error);
				getResult().setNativeReference(nativeReference);				
				return;
			}
			
			if (!ucipResponse.isSuccessful())
			{
				String error = "Failed to retrieve service class with link error: " + ucipResponse.getUcipResponseCode();
				logger.error(error);
				getResult().setResultCode(ucipResponse.getErsResponseCode());
				getResult().setResultDescription(error);
				getResult().setNativeReference(nativeReference);				
				return;
			}
			
			String serviceClass = String.valueOf(ucipResponse.getCurrentServiceClass());
			if (!serviceClass.matches(regexpReceiverSenderSC))
			{
				getResult().setResultCode(ERSWSLinkResultCodes.SUBSCRIBER_RECEIVER_HAS_NO_SUCH_SERVICE);
				getResult().setNativeReference(nativeReference);				
				return;
			}
		}

		getResult().setNativeReference(nativeReference);		
		getResult().setResultCode(ERSWSLinkResultCodes.SUCCESS);
	}
	

	public String getRequestTypeId()
	{
		return getClass().getName();
	}	
}
