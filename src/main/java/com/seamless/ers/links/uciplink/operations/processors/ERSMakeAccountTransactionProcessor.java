package com.seamless.ers.links.uciplink.operations.processors;

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
import com.seamless.ers.links.uciplink.ucip.IUCIPAdaptor;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;

public class ERSMakeAccountTransactionProcessor extends AbstractRequestProcessor<AccountTransactionResponse>
{

	private Logger logger = LoggerFactory.getLogger(ERSMakeAccountTransactionProcessor.class);
	private Account account;
	private Amount amount;
	private String reference;
	private AccountTransactionType transactionType;
	private String nativeReference;
	private ERSHashtableParameter extraFields;
	private static final String FIELD_PROFILE_ID = "ProfileId";
	private static final String FIELD_DEDICATED_ACCOUNT_ID = "dedicatedAccountId";
	private static final String FIELD_PRODUCTSKU = "productSKU";

	public ERSMakeAccountTransactionProcessor(
			String principalId,
			Account account,
			AccountTransactionType transactionType,
			Amount amount,
			String reference,
			String nativeReference,
			ERSHashtableParameter extraFields)
	{
		super(new AccountTransactionResponse());
		this.account = account;
		this.transactionType = transactionType;
		this.amount = amount;
		this.reference = reference;
		this.nativeReference = nativeReference;
		this.extraFields = extraFields;
	}

	public void process()
	{
		String origCurrency = amount.getCurrency();
		
		logger.debug("ERSMakeAccountTransaction[" + account.getAccountId() + ", " + amount + ", " + reference + ", " + nativeReference
				 + "]");
		this.getConfig().formatMsisdn(account);

		AccountTransactionResponse response = new AccountTransactionResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		UCIPResponse ucipResponse = null;

		String originNodeType = getUpdateBalanceAndDateOriginNodeType();
		extraFields.put(IUCIPAdaptor.FIELD_ORIGINNODETYPE, originNodeType);	

		String field_dedicated_account_value = extraFields.getParameters().get(FIELD_DEDICATED_ACCOUNT_ID);
		if (field_dedicated_account_value == null)
		{
			logger.debug("Can't get the extrafield with key " + FIELD_DEDICATED_ACCOUNT_ID + " maybe it is not used by this customer.");
		}
		if(reference != null)
			extraFields.put("reference", reference);

		try
		{
			ucipResponse = getConfig().getUcipAdaptor().getInstance().updateBalance(
					account.getAccountId(),
					amount,
					nativeReference,
					transactionType.name(),
					(field_dedicated_account_value != null) ? new Integer(field_dedicated_account_value) : null,
					extraFields);

		}
		catch (NumberFormatException ex)
		{
			response.setResultCode(ERSWSLinkResultCodes.INVALID_ACCOUNT_ID);
			response.setResultDescription("Invalid dedicated account id " + field_dedicated_account_value);
		}

		if (ucipResponse != null)
		{
			if(getConfig().isTransformCurrencyEnabled()) {
				transformAmountCurrency(amount, origCurrency);
				transformUcipResponseCurrency(ucipResponse, origCurrency);
			}

			response.setResultCode(ucipResponse.getErsResponseCode());
			response.setBalanceBefore(ucipResponse.getBalanceBefore());
			response.setExpiryBefore(ucipResponse.getServiceFeeExpiryDateBefore());

			if (this.getConfig().isFetchBalanceAfter())
			{
				UCIPResponse balanceAfterResponse = getConfig().getUcipAdaptor().getInstance().getBalance(
						account.getAccountId(),
						(field_dedicated_account_value != null) ? new Integer(field_dedicated_account_value) : null,
						null,
						nativeReference, extraFields);
				if (balanceAfterResponse.getBalance() != null)
				{
					Amount balanceAfter = balanceAfterResponse.getBalance();
					Amount balanceBefore = null;

					try
					{
						if (response.getResultCode() == ERSWSLinkResultCodes.SUCCESS)
						{
							balanceBefore = AmountUtils.deductAmount(balanceAfter, amount);
						}
						else
						{
							logger.debug("Set account balance after and account balance before same for account id: "
									+ account.getAccountId() + " with result code: " + response.getResultCode() + " and ers reference: "
									+ nativeReference);
							balanceBefore = balanceAfter;
						}

					}
					catch (ERSInvalidCurrencyException e)
					{
						logger.info("Calculating make account balance after failed for account id: " + account.getAccountId()
								+ " with error: " + e.getMessage() + " and ers reference: " + nativeReference);
					}

					response.setBalanceBefore(balanceBefore);
					response.setBalanceAfter(balanceAfter);
				}

			}
			else
			{
				response.setBalanceAfter(ucipResponse.getBalanceAfter());
				response.setExpiryAfter(ucipResponse.getServiceFeeExpiryDateAfter());
			}
			response.setFields(extraFields);
		}

		setResult(response);
	}

	private String getUpdateBalanceAndDateOriginNodeType()
	{
		String value = extraFields.get(IUCIPAdaptor.FIELD_ORIGINNODETYPE);
		// If there is OriginNodeType set from client, then use that value.
		if (value != null)
		{
			return value;
		}
		// Otherwise, use the value from configuration
		if (extraFields.get(FIELD_PRODUCTSKU) != null)
		{
		String key = IUCIPAdaptor.FIELD_ORIGINNODETYPE + "." + extraFields.get(FIELD_PRODUCTSKU);
		return getConfig().getUcipProperties().getProperty(key);
		}
		else
		{
		String key = IUCIPAdaptor.FIELD_ORIGINNODETYPE + "." + extraFields.get(FIELD_PROFILE_ID);
		return getConfig().getUcipProperties().getProperty(key);	
		}
	}

	public String getRequestTypeId()
	{
		return getClass().getName();
	}

}
