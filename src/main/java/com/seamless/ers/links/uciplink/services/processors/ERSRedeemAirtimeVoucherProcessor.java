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
 * ERSRedeemAirtimeVoucherProcessor is a processor for redeeming an airtime voucher to a specific subscriber account.
 * @author Jiraporn Yindeemoh
 *
 */
public class ERSRedeemAirtimeVoucherProcessor extends AbstractRequestProcessor<AccountVASOperationResponse>
{
	Logger logger = LoggerFactory.getLogger(ERSRedeemAirtimeVoucherProcessor.class);
	/**
	 * Subscriber account 
	 * The subscriber account type is airtime.
	 */
	private Account account; 
	/**
	 * Airtime voucher code
	 */
	private String  voucherCode;
	/**
	 * ERS transaction reference
	 */
	private String  reference; 
	/**
	 * Extra parameters for VAS operation.
	 * Expected parameter key in the parameters is VOUCHER_CODE
	 */
	private ERSHashtableParameter parameters;

	private String nativeReference;
	
	/**
	 * Constructor
	 * @param account
	 * @param parameters
	 * @param reference
	 */
	public ERSRedeemAirtimeVoucherProcessor(Account account, ERSHashtableParameter parameters, String reference, String nativeReference)
	{
		super(new AccountVASOperationResponse());
		
		this.account = account;
		this.reference = reference;
		this.parameters = parameters;
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
	 * Acutal process method 
	 * 1. Extract VOUCHER_CODE from parameters
	 * 2. Redeem voucher using redeemVoucher
	 * 3. Set AccountVoucherRedeempitionResponse
	 * @return
	 */
	public AccountVASOperationResponse _process()
	{
		AccountVASOperationResponse response = new AccountVASOperationResponse(ERSWSLinkResultCodes.INTERNAL_FAILED, "REDEEM_AIRTIME_VOUCHER");
		voucherCode = parameters.get("voucherCode");
		
		if (voucherCode == null)
		{
			return new AccountVASOperationResponse(ERSWSLinkResultCodes.INVALID_VOUCHER, "REDEEM_AIRTIME_VOUCHER");
		}
		UCIPResponse ucipRedeem = getConfig().getUcipAdaptor().getInstance().redeemVoucher(account.getAccountId(), voucherCode, nativeReference,parameters);
		logger.debug("Recieved response: "+ ucipRedeem);	
		
		if (ucipRedeem != null && ucipRedeem.isSuccessful())
		{	
			response.setRetryable(false);
			response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
			response.setField("balanceBefore", ucipRedeem.getBalanceBefore() + "");
			response.setField("balanceAfter", ucipRedeem.getBalanceAfter() + "");
			response.setField("expiryDateBefore", ucipRedeem.getServiceFeeExpiryDateBefore() + "");
			response.setField("expiryDateAfter", ucipRedeem.getServiceFeeExpiryDateAfter() + "");
			return response;			
		}	
		else if (ucipRedeem != null)
		{
			response.setResultCode(ucipRedeem.getErsResponseCode());
		}
		
		return response;
	}
	public String getRequestTypeId()
	{
		return getClass().getName();

	}
}
