package com.seamless.ers.links.uciplink.services.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.common.locale.ERSInvalidCurrencyException;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifcommon.utils.AmountUtils;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountTransactionResponse;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountTransactionType;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;

public class ERSReverseTopupProcessor extends	AbstractRequestProcessor<AccountTransactionResponse>
{
	private Logger logger = LoggerFactory.getLogger(ERSReverseTopupProcessor.class);
	private Account account;
	private Amount amount;
	private String nativeReference;
	private ERSHashtableParameter extraFields;
	
	public ERSReverseTopupProcessor(String principalId, 
									Account senderAccount,
									Account receiverAccount,
									String routingInfo,
									Amount amount, 
									String topupNativeReference, 
									String reference,
									ERSHashtableParameter extraFields,
									String nativeReference)
	{
		super(new AccountTransactionResponse());
		this.account = receiverAccount;
		
		// Negate the amount as this is a debit
		this.extraFields = extraFields;
		this.amount = amount;
		this.nativeReference = nativeReference;
	}


	public void process()
	{
		String origCurrency = amount.getCurrency();
		
		this.getConfig().formatMsisdn(account);

		AccountTransactionResponse response = new AccountTransactionResponse();
		UCIPResponse ucipResponse = null;
		

		ucipResponse = getConfig().getUcipAdaptor().getInstance().updateBalance(
				account.getAccountId(), 
				amount.clone().negate(), 
				nativeReference, 
				AccountTransactionType.Debit.name(), 
				null,extraFields);
		
		if (!ucipResponse.isSuccessful())
		{
			response.setResultCode(ucipResponse.getErsResponseCode());
			setResult(response);
			return;
		}

		if(getConfig().isTransformCurrencyEnabled()) {
			transformAmountCurrency(amount, origCurrency);
			transformUcipResponseCurrency(ucipResponse, origCurrency);
		}

		if (getConfig().isReverseTopupWantBalance())
		{
			// Calculate the balance before and after with an additional call.
			
			ucipResponse = getConfig().getUcipAdaptor().getInstance().getBalance(
					account.getAccountId(), 
					null,
					null,
					nativeReference, extraFields);
			
			if (ucipResponse.isSuccessful()) 
			{
				if (ucipResponse.getBalance() != null)
				{
					Amount balanceAfter = ucipResponse.getBalance();
					Amount balanceBefore = null;
					
					try
					{
						balanceBefore = AmountUtils.addAmount(balanceAfter, amount);
					}
					catch (ERSInvalidCurrencyException e)
					{
						logger.info(
								"Calculating reverse topup balance after failed for account id: " + 
								account.getAccountId() + " with error: " + e.getMessage() + 
								" and ers reference: " + nativeReference);
					}
					
					response.setBalanceBefore(balanceBefore);
					response.setBalanceAfter(balanceAfter);
				}
				else
				{
					logger.info("The balance amount was not set in the ucip response of get balance!");
				}
			}
			else
			{
				logger.info(
						"Failed to extract balance before and after for reverse of topup for account id: " + 
						account.getAccountId() + " with link result code: " + 
						ucipResponse.getErsResponseCode() + " and ers reference: " + nativeReference);
			}
		}
			
		response.setRetryable(false);
		response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
		setResult(response);
	}

	public String getRequestTypeId()
	{
		return getClass().getName();
	}
	
}
