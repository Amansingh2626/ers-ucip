package com.seamless.ers.links.uciplink.services.processors;

import com.seamless.common.ExtendedProperties;
import com.seamless.common.StringUtils;
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
import java.util.Objects;

public class ERSTopupProcessor extends AbstractRequestProcessor<AccountTransactionResponse>
{
	Logger logger = LoggerFactory.getLogger(ERSTopupProcessor.class);

	private Account receiverAccount;
	private Amount amount;
	private String reference;
	private String refillProfileId;
	private String transactionType;
	private String nativeReference;
	private ERSHashtableParameter extraFields;

	public ERSTopupProcessor(
			String principalId,
			Account account,
			Amount amount,
			String reference,
			String nativeReference,
			ERSHashtableParameter extraFields)
	{
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

	protected AccountTransactionResponse processTopup()
	{
		logger.debug("Request received to topup - info: [" + receiverAccount + " : " + amount + ", "+ reference +":"+ nativeReference +"]");
		if("TOPUP".equalsIgnoreCase(extraFields.get("productSKU")))
		{
			extraFields.put("SENDER_ACCOUNT_ID", receiverAccount.getAccountId());
		}

		this.getConfig().formatMsisdn(receiverAccount);
	    logger.info("refillProfileId :"+refillProfileId);
	    logger.info("productSelectionKey: "+ getConfig().getProductSelectionKey());
		if(!StringUtils.isEmpty(getConfig().getProductSelectionKey()) && !StringUtils.isEmpty(extraFields.get(getConfig().getProductSelectionKey()))){
			refillProfileId=extraFields.get(getConfig().getProductSelectionKey());
			logger.info("read refillProfileId from request: "+refillProfileId);
		}
		if (refillProfileId == null)
		{
			logger.info("Reading refillProfileId from config");
			if(getConfig().getRefillProfileIdMap().containsKey("productSKU"))
				refillProfileId = getConfig().getRefillProfileIdMap().get("productSKU");
			else
				refillProfileId = getConfig().getRefillProfileID();
		}

		if (this.getConfig().isValidateSubscriberLocation() != null)
		{
			extraFields.put("validateSubscriberLocation", this.getConfig().isValidateSubscriberLocation().toString());
		}
		
		if(reference != null)
			extraFields.put("reference", reference);
		
		AccountTransactionResponse response = new AccountTransactionResponse();
		response.setFields(new ERSHashtableParameter());

		String originalCurrency = amount.getCurrency();
		
		try
		{
			logger.debug("+++++++ extraFields : { " + extraFields.toString() + " } +++++++");

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

			if (ucipResponse != null)
			{
				response.getFields().put("ucipResponseCode", String.valueOf(ucipResponse.getUcipResponseCode()));
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
					setRefillValueDedicatedAccountsBefore(ucipResponse, response.getFields());

					setRefillValueDedicatedAccountsAfter(ucipResponse, response.getFields());
					
					setRefillValueDedicatedAccountsPromotion(ucipResponse, response.getFields());

					if(getConfig().isPopulateResponseWithTreeDefinedFields())
					{
						populateFieldsWithTreeDefinedFields(ucipResponse, response.getFields());
					}
				}
				else
				{
					response.setResultCode(ucipResponse.getErsResponseCode());
				}

				ExtendedProperties moduleProperties = this.getConfig().getModuleProperties("ucip.");
				if (Objects.nonNull(moduleProperties) && moduleProperties.containsKey("viewInServerIp") && Boolean.valueOf(moduleProperties.getProperty("viewInServerIp"))){
					ERSHashtableParameter fields = response.getFields();
					if (Objects.nonNull(fields) && !fields.getParameters().containsKey("info7")){
						fields.put("info7",ucipResponse.getInfo7());
					}
					response.setFields(fields);
				}
			}
			else
			{
				logger.info("No response from AIR node! check connectivity for receiver account.");
				response.setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
			}

		}
		catch (Exception e)
		{
			logger.error("Problem occured while retrieving account information: ", e);
			response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}

		return response;
	}

	private void populateFieldsWithTreeDefinedFields(UCIPResponse ucipResponse, ERSHashtableParameter extraFields)
	{
		Map<String, TreeDefinedFieldInformation> treeDefinedFieldInformationMap = ucipResponse.getTreeDefinedFieldInformationMap();

		if(treeDefinedFieldInformationMap != null)
		{
			String prefix = getConfig().getTreeDefinedFieldsResponsePrefix();
			for(Map.Entry<String, TreeDefinedFieldInformation> entry: treeDefinedFieldInformationMap.entrySet())
			{
				TreeDefinedFieldInformation treeDefinedFieldInformation = entry.getValue();
				if(treeDefinedFieldInformation != null)
				{
					extraFields.put(prefix + entry.getKey(), treeDefinedFieldInformation.getTreeDefinedFieldValue());
				}
			}
		}
	}

	public String getRequestTypeId()
	{
		return getClass().getName();

	}

	public void setRefillValueDedicatedAccountsAfter(UCIPResponse ucipResponse, ERSHashtableParameter extraFields)
	{
		for (Map.Entry<Integer, Long> entry : ucipResponse.getDedicatedAccountValuesAfter().entrySet())
		{
			int dedicatedAccountId = entry.getKey();
			Long refillAmount1 = entry.getValue();
			if (refillAmount1 != null)
			{
				String extraFieldName = getConfig().getRefillValueTotalDAName(dedicatedAccountId);
				if (extraFieldName != null)
				{
					extraFields.put(extraFieldName, refillAmount1.toString());
				}
			}
		}
	}

	public void setRefillValueDedicatedAccountsBefore(UCIPResponse ucipResponse, ERSHashtableParameter extraFields)
	{
		for (Map.Entry<Integer, Long> entry : ucipResponse.getDedicatedAccountValuesBefore().entrySet())
		{
			int dedicatedAccountId = entry.getKey();
			Long refillAmount1 = entry.getValue();
			if (refillAmount1 != null)
			{
				String extraFieldName = getConfig().getRefillValueTotalDAName(dedicatedAccountId);
				if (extraFieldName != null)
				{
					extraFields.put(extraFieldName + "Before", refillAmount1.toString());
				}
			}
		}
	}
	
	public void setRefillValueDedicatedAccountsPromotion(UCIPResponse ucipResponse, ERSHashtableParameter extraFields)
	{
		for (Map.Entry<Integer, DedicatedAccountRefillInformation> entry : ucipResponse.getDedicatedAccountRefillValuePromotionMap().entrySet())
		{
			int dedicatedAccountId = entry.getKey();
			DedicatedAccountRefillInformation value = entry.getValue();
			String extraFieldName = getConfig().getRefillValuePromotionDAName(dedicatedAccountId);
			extraFieldName = extraFieldName + "_";
			if (extraFieldName != null)
			{
				if (value.getDedicatedAccountID() != null)
				{
					extraFields.put(extraFieldName + "dedicatedAccountID", value.getDedicatedAccountID().toString());
				}
				if (value.getRefillAmount1() != null)
				{
					extraFields.put(extraFieldName + "refillAmount1", value.getRefillAmount1().toString());
				}
				if (value.getExpiryDateExtended() != null)
				{
					extraFields.put(extraFieldName + "expiryDateExtended", value.getExpiryDateExtended().toString());
				}
			}
		}
	}
}
