package com.seamless.ers.links.uciplink.services.processors;

import com.seamless.ers.links.uciplink.utils.UCIPUtils;

import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountTransactionResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ERSUpdateOfferProcessor extends AbstractRequestProcessor<AccountTransactionResponse>
{
	private Account receiverAccount;
	private Amount amount;
	private String refillProfileId;
	private String transactionType;
	private String nativeReference;
	private ERSHashtableParameter extraFields;
	private static final Logger log = LoggerFactory.getLogger(ERSUpdateOfferProcessor.class);

	public ERSUpdateOfferProcessor(
			Account account,
			Amount amount,
			String nativeReference,
			ERSHashtableParameter extraFields)
	{
		super(new AccountTransactionResponse());

		this.receiverAccount = account;
		this.amount = amount;
		this.nativeReference = nativeReference;
		this.refillProfileId = extraFields.get("refillProfileId");
		this.extraFields = extraFields;
		this.transactionType = extraFields.get("transactionType");
	}

	@Override
	public void process()
	{
		setResult(processUpdateOffer());
	}

	protected AccountTransactionResponse processUpdateOffer()
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(" ERSUpdateOfferProcessor :: processUpdateOffer");
		log.debug("Request recieved to topup - info: [{} : {}]", receiverAccount, amount);
		AccountTransactionResponse response = new AccountTransactionResponse();
		try {
			this.getConfig().formatMsisdn(receiverAccount);

			if (refillProfileId == null)
			{
				refillProfileId = getConfig().getRefillProfileID();
			}

			if (this.getConfig().isValidateSubscriberLocation() != null)
			{
				extraFields.put("validateSubscriberLocation", this.getConfig().isValidateSubscriberLocation().toString());
			}

			response.setFields(new ERSHashtableParameter());
			String originalCurrency = null;
			if(null != amount) {
				originalCurrency = amount.getCurrency();
			}

			OfferProductsDTO offerProductsDTO = UCIPUtils.setupOfferInfo(extraFields);
			// Get receiver account information, it will confirm that receiver
			// account exists in the system.
			UCIPResponse ucipResponse = getConfig().getUcipAdaptor().getInstance().updateOffer(
					receiverAccount.getAccountId(),
					amount,
					nativeReference,
					transactionType,
					refillProfileId,
					null,
					extraFields,offerProductsDTO, null);

			prepareResponse(response, ucipResponse, originalCurrency);
		}
		catch (Exception e)
		{
			log.error("Problem occured while retrieving account information: ", e);
			response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		} finally {
			point.collect();
			return response;
		}
	}

	@Override
	public String getRequestTypeId()
	{
		return getClass().getName();

	}


	private void prepareResponse(AccountTransactionResponse response, UCIPResponse ucipResponse, String originalCurrency) {
		if (ucipResponse != null)
		{
			if(getConfig().isTransformCurrencyEnabled()) {
				transformAmountCurrency(amount, originalCurrency);
				transformUcipResponseCurrency(ucipResponse, originalCurrency);
			}

			if (ucipResponse.getErsResponseCode() == ERSWSLinkResultCodes.SUCCESS)
			{
				response.setBalanceBefore(ucipResponse.getBalance());
				response.setBalanceAfter(ucipResponse.getBalanceAfter());
				response.setResultCode(ERSWSLinkResultCodes.SUCCESS);

				if (ucipResponse.getSupervisionExpiryDateBefore() != null)
				{
					response.getFields().put(
							"supervisionExpiryDateBefore",
							getConfig().formatResponseDate(ucipResponse.getSupervisionExpiryDateBefore()));
				}

				if (ucipResponse.getSupervisionExpiryDateAfter() != null)
				{
					response.getFields().put(
							"supervisionExpiryDateAfter",
							getConfig().formatResponseDate(ucipResponse.getSupervisionExpiryDateAfter()));
				}
			}
			else
			{
				response.setResultCode(ucipResponse.getErsResponseCode());
			}
		}
		else
		{
			log.info("No response from AIR node! check connectivity for receiver account.");
			response.setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
	}

}
