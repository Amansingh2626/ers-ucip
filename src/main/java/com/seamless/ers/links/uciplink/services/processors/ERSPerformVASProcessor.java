package com.seamless.ers.links.uciplink.services.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountVASOperationResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
/**
 * ERSPerformVASProcessor is a generic processor used for processing VAS operations. It is a front-end processor determining which sub-processor will be called to process different type of VAS operations.
 * @author Jiraporn Yindeemoh
 *
 */
public class ERSPerformVASProcessor extends AbstractRequestProcessor<AccountVASOperationResponse>
{

	Logger logger = LoggerFactory.getLogger(ERSPerformVASProcessor.class);
	/**
	 * PrincipalID invoking VAS operations.
	 * Mostly it is subscriber MSISDN
	 */
	private String  principalId;
	/**
	 * Subscriber account 
	 * The subscriber account type is airtime.
	 */
	private Account account; 
	/**
	 * Routing information for accessing ucip server.
	 */
	private String  routingInfo; 
	/**
	 * VAS operation id.
	 */
	private String  operationId;
	/**
	 * ERS transaction reference
	 */
	private String  reference; 
	/**
	 * Extra parameters for VAS operation.
	 */
	private ERSHashtableParameter extraFields;
	
	/*
	 * The native reference used to talk to the CS.
	 */
	private String nativeReference;

	/**
	 * Constructor
	 * @param principalId
	 * @param account
	 * @param routingInfo
	 * @param operationId
	 * @param reference
	 * @param extraFields
	 */
	public ERSPerformVASProcessor(String principalId,
			Account account, String routingInfo, String operationId,
			String reference, ERSHashtableParameter extraFields,
			String nativeReference)
	{
		super (new AccountVASOperationResponse());
		
		
		
		this.principalId = principalId;
		this.account 	 = account;
		this.routingInfo = routingInfo;
		this.operationId = operationId;
		this.reference   = reference;
		this.extraFields = extraFields;		
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
	 * 1. Determine operationId and forward the request to the corresponding processor
	 * @return
	 */
	protected AccountVASOperationResponse _process() 
	{
		
		
		this.getConfig().formatMsisdn(account);
			
		if(operationId.equals("BIC_ACTIVATE"))
		{
			return processActivateBonusIncomingCallOperation();
		}
		
		else if(operationId.equals("FAF_ADD"))
		{
			return processAddFriendAndFamilyOperation();
		}
		
		else if(operationId.equals("SC_CHANGE"))
		{
			return processChangeServiceClassOperation();
		}
		
		else if(operationId.equals("BIC_DEACTIVATE"))
		{
			return processDeactivateBonusIncomingCallOperation();	
		}
		
		else if(operationId.equals("BIC_INFO"))
		{
			return processGetBonusIncomingCallOperation();
		}
		
		else if(operationId.equals("FAF_INFO"))
		{
			return processGetFriendAndFamilyOperation();
		}
		
		else if(operationId.equals("SC_INFO"))
		{
			return processGetServiceClassOperation();
		}
		
		else if(operationId.equals("FAF_REMOVE"))
		{
			return processRemoveFriendAndFamilyOperation();
		}
		else if(operationId.equals("REDEEM_AIRTIME_VOUCHER"))
		{
			return processRedeeomAirtimeVoucherOperation();
		}
		else if (operationId.equals("FAF_RESET"))
		{
			return processResetFAFOperation();
		}
		else
		{
			return new AccountVASOperationResponse(operationId, ERSWSLinkResultCodes.OPERATION_NOT_AVAILABLE);
		}
				
	}
	
	
	
	
	
	
	private AccountVASOperationResponse processActivateBonusIncomingCallOperation()
	{
		return new AccountVASOperationResponse(operationId, ERSWSLinkResultCodes.OPERATION_NOT_AVAILABLE);
	}
	
	private AccountVASOperationResponse processAddFriendAndFamilyOperation()
	{
		ERSAddFAFProcessor processor = new ERSAddFAFProcessor(account, extraFields,reference, nativeReference);
		processor.setConfig(getConfig());
		processor.process();
		return processor.getResult();
	}
	
	private AccountVASOperationResponse processChangeServiceClassOperation()
	{
		
					
		ERSSetAccountClassProcessor processor = new ERSSetAccountClassProcessor(account, extraFields,reference, nativeReference);
		processor.setConfig(getConfig());
		processor.process();
		return processor.getResult();		
	}	
	
	private AccountVASOperationResponse processDeactivateBonusIncomingCallOperation()
	{
		return new AccountVASOperationResponse(operationId, ERSWSLinkResultCodes.OPERATION_NOT_AVAILABLE);
	}
	
	private AccountVASOperationResponse processGetBonusIncomingCallOperation()
	{
		return new AccountVASOperationResponse(operationId, ERSWSLinkResultCodes.OPERATION_NOT_AVAILABLE);
	}
	
	private AccountVASOperationResponse processGetFriendAndFamilyOperation()
	{
		ERSGetAccountFAFListProcessor processor = new ERSGetAccountFAFListProcessor(account, reference, nativeReference);
		processor.setConfig(getConfig());
		processor.process();
		return processor.getResult();
	}											
	
	private AccountVASOperationResponse processGetServiceClassOperation()
	{
		ERSGetAccountClassProcessor processor = new ERSGetAccountClassProcessor(account, reference, nativeReference,extraFields);
		processor.setConfig(getConfig());
		processor.process();
		return processor.getResult();
	}	
										
	private AccountVASOperationResponse processRemoveFriendAndFamilyOperation()
	{
		ERSRemoveFAFProcessor processor = new ERSRemoveFAFProcessor(account, extraFields,reference, nativeReference);
		processor.setConfig(getConfig());
		processor.process();
		return processor.getResult();
	}	
	
	private AccountVASOperationResponse processRedeeomAirtimeVoucherOperation()
	{
		ERSRedeemAirtimeVoucherProcessor processor = new ERSRedeemAirtimeVoucherProcessor(account, extraFields,reference, nativeReference);
		processor.setConfig(getConfig());
		processor.process();
		return processor.getResult();
	}
	
	private AccountVASOperationResponse processResetFAFOperation()
	{
		ERSResetFAFProcessor processor = new ERSResetFAFProcessor(account, reference, nativeReference);
		processor.setConfig(getConfig());
		processor.process();
		return processor.getResult();
	}

	public String getRequestTypeId()
	{
		return getClass().getName();

	}	
												
	
}
