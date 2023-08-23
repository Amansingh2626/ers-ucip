package com.seamless.ers.links.uciplink.services.processors;

import com.seamless.common.uciplib.common.DedicatedAccountRefillInformation;
import com.seamless.common.uciplib.common.TreeDefinedFieldInformation;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountTransactionResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RefillVoucherRechargeProcessor  extends AbstractRequestProcessor<AccountTransactionResponse> {

	Logger logger = LoggerFactory.getLogger(ERSTopupProcessor.class);

	private Account receiverAccount;
	private Amount amount;
	private String reference;
	private String refillProfileId;
	private String transactionType;
	private String nativeReference;
	private ERSHashtableParameter extraFields;

	public RefillVoucherRechargeProcessor(
			String principalId,
			Account account,
			Amount amount,
			String reference,
			String nativeReference,
			ERSHashtableParameter extraFields) {
		super(new AccountTransactionResponse());

		this.receiverAccount = account;
		this.amount = amount;
		this.reference = reference;
		this.nativeReference = nativeReference;
		this.refillProfileId = extraFields.get("refillProfileId");
		this.extraFields = extraFields;
	}

	@Override
	public void process()
	{
		setResult(processTopup());
	}

	protected AccountTransactionResponse processTopup() {
		logger.debug("Request received to voucher recharge - info: [" + receiverAccount + " : " + amount + ", "+ reference +":"+ nativeReference +"]");

		AccountTransactionResponse response = new AccountTransactionResponse();

		if(null != extraFields && !extraFields.getParameters().containsKey("SCRATCH_CARD_NUM")) {
			logger.info("SCRATCH_CARD_NUM not available in fields... ");
			response.setResultCode(ERSWSLinkResultCodes.INVALID_REQUEST);
			return response;
		}

		setDataInFields();

		this.getConfig().formatMsisdn(receiverAccount);

		response.setFields(new ERSHashtableParameter());

		String originalCurrency = amount.getCurrency();

		try {
			logger.debug("+++++++ extraFields : { " + extraFields.toString() + " } +++++++");

			if(getConfig().isReturnMockResponse()) {
				response.setField("transactionAmount","100");
				response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
				return response;
			}

			// Get receiver account information, it will confirm that receiver
			// account exists in the system.
			UCIPResponse ucipResponse = getConfig().getUcipAdaptor().getInstance().refill(
					receiverAccount.getAccountId(),
					amount,
					nativeReference,
					transactionType,
					refillProfileId,
					null,
					extraFields);

			if (ucipResponse != null) {
				response.getFields().put("ucipResponseCode", String.valueOf(ucipResponse.getUcipResponseCode()));
				if(getConfig().isTransformCurrencyEnabled()) {
					transformAmountCurrency(amount, originalCurrency);
					transformUcipResponseCurrency(ucipResponse, originalCurrency);
				}

				if (ucipResponse.getErsResponseCode() == ERSWSLinkResultCodes.SUCCESS) {
					response.setBalanceBefore(ucipResponse.getBalance());
					response.setBalanceAfter(ucipResponse.getBalanceAfter());
					response.setResultCode(ERSWSLinkResultCodes.SUCCESS);

					response.getFields().put("transactionAmount",String.valueOf(ucipResponse.getTransactionAmount().getValue()));

					if(getConfig().isPopulateResponseWithTreeDefinedFields()) {
						populateFieldsWithTreeDefinedFields(ucipResponse, response.getFields());
					}
				}
				else {
					response.setResultCode(ucipResponse.getErsResponseCode());
				}
			}
			else {
				logger.info("No response from AIR node! check connectivity for receiver account.");
				response.setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
			}

		}
		catch (Exception e) {
			logger.error("Problem occured while retrieving account information: ", e);
			response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}

		return response;
	}

	private void setDataInFields() {
		if (refillProfileId == null) {
			if(getConfig().getRefillProfileIdMap().containsKey(extraFields.get("productSKU")))
				refillProfileId = getConfig().getRefillProfileIdMap().get(extraFields.get("productSKU"));
			else
				refillProfileId = getConfig().getRefillProfileID();
		}

		if(reference != null)
			extraFields.put("reference", reference);
	}

	private void populateFieldsWithTreeDefinedFields(UCIPResponse ucipResponse, ERSHashtableParameter extraFields) {
		Map<String, TreeDefinedFieldInformation> treeDefinedFieldInformationMap = ucipResponse.getTreeDefinedFieldInformationMap();

		if(treeDefinedFieldInformationMap != null) {
			String prefix = getConfig().getTreeDefinedFieldsResponsePrefix();
			for(Map.Entry<String, TreeDefinedFieldInformation> entry: treeDefinedFieldInformationMap.entrySet()) {
				TreeDefinedFieldInformation treeDefinedFieldInformation = entry.getValue();
				if(treeDefinedFieldInformation != null) {
					extraFields.put(prefix + entry.getKey(), treeDefinedFieldInformation.getTreeDefinedFieldValue());
				}
			}
		}
	}

	public String getRequestTypeId() {
		return getClass().getName();
	}

}
