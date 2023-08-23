package com.seamless.ers.links.uciplink.services.processors;

import com.seamless.common.ExtendedProperties;
import com.seamless.common.uciplib.common.OfferInformation;
import com.seamless.ers.links.uciplink.config.ValidateTopupMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.common.uciplib.common.ServiceOffering;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSAccountTypes;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.AccountData;
import com.seamless.ers.interfaces.ersifcommon.utils.CurrencyHandler;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountTransactionResponse;
import com.seamless.ers.links.uciplink.UCIPLinkConfig.TopupProductSettings;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ERSValidateTopupProcessor extends
		AbstractRequestProcessor<AccountTransactionResponse>
{

	Logger logger = LoggerFactory.getLogger(ERSValidateTopupProcessor.class);

	TopupProductSettings productSettings;

	String principalId;

	Account senderAccount;

	Account receiverAccount;

	Amount amount;

	String reference;

	String nativeReference;

	ERSHashtableParameter extraFields;

	public ERSValidateTopupProcessor(TopupProductSettings productSettings,
			String principalId, Account senderAccount, Account receiverAccount,
			Amount amount, String reference, String nativeReference, ERSHashtableParameter extraFields)
	{
		super(new AccountTransactionResponse());

		this.productSettings = productSettings;
		this.principalId = principalId;
		this.senderAccount = senderAccount;
		this.receiverAccount = receiverAccount;
		this.amount = amount;
		this.reference = reference;
		this.nativeReference = nativeReference;
		this.extraFields = extraFields;
	}

	public void process()
	{
		setResult(_process());
	}

	protected AccountTransactionResponse _process()
	{
		logger.info("Request received to validate topup - info: [ senderAccount { " + senderAccount + " } -> receiverAccount { " + receiverAccount +" } amount { "+ amount + " } ]");

		this.getConfig().formatMsisdn(senderAccount);
		this.getConfig().formatMsisdn(receiverAccount);

		AccountTransactionResponse response = new AccountTransactionResponse();
		UCIPResponse ucipResponse;
		try
		{
			logger.info("return_mock_response is ..." + this.getConfig().isReturnMockResponse());
			if(this.getConfig().isReturnMockResponse()) {
				logger.info("return_mock_response is true...");

				AccountData accountData = new AccountData(null);
				if(senderAccount == null)
					accountData = new AccountData(receiverAccount);
				if(receiverAccount == null)
					accountData = new AccountData(senderAccount);

				accountData.setFields(new ERSHashtableParameter());

				for(int index = 1; index <= 5; index++) {
					Map<String, Object> offerMap = new HashMap<>();
					offerMap.put("expiryDateTime", new Date());
					offerMap.put("offerID", index + 1000 + index);
					offerMap.put("offerState", index);
					offerMap.put("offerType",index);
					offerMap.put("startDateTime", new Date());
					accountData.getFields().put("offerInformation_" + index,
							getPipeDelimitedOfferInformationValues(new OfferInformation(offerMap)));
				}
				accountData.setAccountClassId("60");
				response.setAccountData(accountData);
				logger.info("Response to return is: " + response);

				if(getConfig().isEnableNativeCodeMapping())
				{
					accountData.getFields().put("NativeResultCode", String.valueOf(ERSWSLinkResultCodes.SUCCESS));
					accountData.getFields().put("NativeResultDescription", ERSWSLinkResultCodes.getName(ERSWSLinkResultCodes.SUCCESS));
					accountData.getFields().put("LinkType", "UCIPLink");
					accountData.getFields().put("LinkNodeId", "UCIPLink");
				}
				return response;
			}

			String origCurrency = amount.getCurrency();
			
			if(getConfig().isTransformCurrencyEnabled()) {
				transformAmountCurrency(amount, getConfig().getTransformCurrencyCode());
			}

			if (senderAccount != null
					&& senderAccount.getAccountTypeId().equalsIgnoreCase(ERSAccountTypes.MOBILE_AIRTIME))
			{
				logger.info("current sender account == {} with sender accountTypeId == {}", senderAccount, senderAccount.getAccountTypeId());

				if (!getConfig().isAllowSelfTopup())
				{
					if (!extraFields.getParameters().containsKey("subscriber_offers_transaction")
							&& !"true".equals(extraFields.get("subscriber_offers_transaction"))
							&& senderAccount.getAccountId().trim()
							.equals(receiverAccount.getAccountId().trim()))
					{
						response.setResultCode(ERSWSLinkResultCodes.OPERATION_NOT_AVAILABLE);
						return response;
					}
				}

				if(getConfig().getValidateTopupMethod() == ValidateTopupMethod.GET_BALANCE_AND_DATE) {

					// FIXME we need to fix for sender subscriber as well, call
					// GetAccountDetails
					// Get sender account information to see if it has enough money
					// to transfer, and it is not blocked
					ucipResponse = getConfig()
							.getUcipAdaptor()
							.getInstance()
							.getBalance(senderAccount.getAccountId(),
									getConfig().getGetBalanceAndDateDASFirstId(),
									getConfig().getGetBalanceAndDateDASLastId(),
									nativeReference, extraFields);

					if(getConfig().isTransformCurrencyEnabled()) {
						transformAmountCurrency(amount, origCurrency);
						transformUcipResponseCurrency(ucipResponse, origCurrency);
					}

					if (ucipResponse != null)
					{
						logger.info("UCIPResponse result code :: "+ ucipResponse.getUcipResponseCode());
						if (ucipResponse.getErsResponseCode() == ERSWSLinkResultCodes.SUCCESS)
						{
							logger.info("Checking current balance sufficient validation");
							if (CurrencyHandler.getAmountValueAsLong(ucipResponse
									.getBalance()) < CurrencyHandler
									.getAmountValueAsLong(amount))
							// if(ucipResponse.getBalance().lessThan(amount))
							{
								response.setResultCode(ERSWSLinkResultCodes.INSUFFICIENT_CREDIT);
								return response;
							}
							else
							{
								logger.info("sufficient validation else part executed...");
								response.setResultCode(ucipResponse.getErsResponseCode());
								if(getConfig().isReturnOfferInformation())
								{
									logger.info("Getting sender offer Information Id from UCIPLINK");
									OfferInformation[] offerInformationList = ucipResponse.getOfferInformation();
									AccountData accountData = new AccountData(senderAccount);
									accountData.setFields(new ERSHashtableParameter());
									accountData.setAccountClassId(String.valueOf(ucipResponse.getCurrentServiceClass()));
									if(offerInformationList != null && offerInformationList.length > 0)
									{
										int offerIdIndex = 1;
										for(OfferInformation offerInformation : offerInformationList)
										{
											accountData.getFields().put("offerInformation_" + offerIdIndex,
													getPipeDelimitedOfferInformationValues(offerInformation));
											offerIdIndex++;
										}
									}
									response.setAccountData(accountData);
								}
								return response;
							}
						}else {
							logger.info("UCIP call has been failed with resultCode "+ ucipResponse.getUcipResponseCode());
							lookupUCIPErrorCode(ucipResponse, response);
							return response;
						}
					}
					else
					{
						logger.info("No response from AIR node! check connectivity for sender account.");
						response.setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
						return response;
					}
				}

			}

			if (getConfig().getValidateTopupMethod() == ValidateTopupMethod.GET_BALANCE_AND_DATE)
			{
				logger.info("Using GET_BALANCE_AND_DATE to validate topup");
				ucipResponse = getConfig()
						.getUcipAdaptor()
						.getInstance()
						.getBalance(receiverAccount.getAccountId(),
								getConfig().getGetBalanceAndDateDASFirstId(),
								getConfig().getGetBalanceAndDateDASLastId(),
								nativeReference, extraFields);
			}
			else
			{
				logger.info("Using GET_ACCOUNT_DETAILS to validate topup");
				// Get receiver account information to see if it exists in the
				// system, and it is not blocked
				ucipResponse = getConfig()
						.getUcipAdaptor()
						.getInstance()
						.getAccountDetails(receiverAccount.getAccountId(),
								nativeReference,extraFields);
			}

			if (ucipResponse.isSuccessful())
			{
				AccountData accountData = new AccountData(receiverAccount);
				accountData.setFields(new ERSHashtableParameter());

				// TODO: accountClassId from extrafields are not deprecated and
				// should be removed!!!!!!!!!!
				accountData.getFields().put("accountClassId",
						ucipResponse.getCurrentServiceClass() + "");

				accountData.setAccountClassId(ucipResponse
						.getCurrentServiceClass() + "");
				accountData
						.setLanguageCode(ucipResponse.getLanguageInISO6391());
				accountData.setAccountLinkTypeId(getConfig().getLinkTypeId(
						ucipResponse.getCurrentServiceClass()));
				accountData.setAccountExpiry(ucipResponse
						.getServiceFeeExpiryDate());
				accountData.setAccountClassExpiry(ucipResponse
						.getSupervisionExpiryDate());
				accountData.setStatus(getConfig().getAccountStatus(
						ucipResponse.getServiceFeeExpiryDate(),
						ucipResponse.getSupervisionExpiryDate()));

				ServiceOffering[] serviceOfferings = ucipResponse
						.getServiceOfferings();

				OfferInformation[] offerInformationList = ucipResponse.getOfferInformation();

				if (serviceOfferings != null)
				{
					int activeOfferingsCount = 0;
					for (int index = 0; index < serviceOfferings.length; index++)
					{
						if (serviceOfferings[index]
								.isServiceOfferingActiveFlag())
						{
							activeOfferingsCount++;
						}
					}

					for (int indexOfferings = 0; indexOfferings < serviceOfferings.length; indexOfferings++)
					{
						if (serviceOfferings[indexOfferings]
								.isServiceOfferingActiveFlag())
						{
							accountData
									.getFields()
									.put("serviceOffering_"
											+ serviceOfferings[indexOfferings]
													.getServiceOfferingID(),
											"true");
						}
					}

				}

				if(offerInformationList != null && offerInformationList.length > 0)
				{
					int index = 1;
					for(OfferInformation offerInformation : offerInformationList) {
						accountData.getFields().put("offerInformation_" + index, getPipeDelimitedOfferInformationValues(offerInformation));
						index++;
					}
				}

				if (ucipResponse.getAccountFlags() != null
						&& ucipResponse.getAccountFlags()
								.getActivationStatusFlag() != null)
				{
					accountData.getFields().put(
							"activationStatusFlag",
							ucipResponse.getAccountFlags()
									.getActivationStatusFlag().toString());
					accountData.getFields().put(
							"supervisionPeriodExpiryFlag",
							ucipResponse.getAccountFlags()
									.getSupervisionPeriodExpiryFlag()
									.toString());
					accountData.getFields()
							.put("serviceFeePeriodExpiryFlag",
									ucipResponse.getAccountFlags()
											.getServiceFeePeriodExpiryFlag()
											.toString());
				}

				if (ucipResponse.getPamInformationList() != null)
				{
					accountData.getFields().put("pamInformationList",
							ucipResponse.getPamInformationList().toString());
				}
				ExtendedProperties moduleProperties = this.getConfig().getModuleProperties("ucip.");
				if (Objects.nonNull(moduleProperties) && moduleProperties.containsKey("viewInServerIp") && Boolean.valueOf(moduleProperties.getProperty("viewInServerIp"))){
					ERSHashtableParameter fields = response.getFields();
					if (Objects.nonNull(fields) && !fields.getParameters().containsKey("info5")){
						fields.put("info5",ucipResponse.getInfo5());
					}
					response.setFields(fields);
				}
				if(getConfig().isFetchBalanceBeforeInGetBalanceAndDate() && null!= ucipResponse.getBalance()){
					response.setBalanceBefore(ucipResponse.getBalance());
				}
				response.setAccountData(accountData);
				response.setRetryable(false);
				response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
			}
			else
			{
				lookupUCIPErrorCode(ucipResponse, response);
			}

		}
		catch (Exception e)
		{
			logger.error(
					"Problem occured while retrieving account information: ", e);
			response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}

		return response;
	}

	private void lookupUCIPErrorCode(UCIPResponse ucipResponse, AccountTransactionResponse response) {
		switch (ucipResponse.getUcipResponseCode())
		{
			case -1:
				// -1 means no response was received from ucip,
				// interpret that as connection error
				response.setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
				break;
			case 1:
			case 2:
				response.setResultCode(ERSWSLinkResultCodes.INVALID_ACCOUNT_STATUS);
				break;
			case 100:
				response.setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
				break;
			case 102:
				response.setResultCode(ERSWSLinkResultCodes.ACCOUNT_NOT_FOUND);
				break;
			case 126:
				response.setResultCode(ERSWSLinkResultCodes.INVALID_ACCOUNT_STATUS);
				break;
			case 103:
			case 104:
				response.setResultCode(ERSWSLinkResultCodes.ACCOUNT_BLOCKED);
				break;
			case 106:
			case 124:
				response.setResultCode(ERSWSLinkResultCodes.INSUFFICIENT_CREDIT);
				break;
			case 123:
			case 153:
				response.setResultCode(ERSWSLinkResultCodes.CREDIT_TOO_HIGH);
				break;
			case 134:
			case 135:
			case 136:
			case 137:
			case 139:
			case 140:
			case 154:
			case 160:
			case 161:
			case 999:
				response.setResultCode(ERSWSLinkResultCodes.LINK_ERROR);
				break;
			default:
				response.setResultCode(ERSWSLinkResultCodes.ACCOUNT_UNSUPPORTED_OPERATION);
				break;
		}
	}

	private String getPipeDelimitedOfferInformationValues(OfferInformation offerInformation) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(offerInformation.getOfferID());
		stringBuilder.append("|");
		stringBuilder.append(offerInformation.getOfferState());
		stringBuilder.append("|");
		stringBuilder.append(offerInformation.getOfferType());
		stringBuilder.append("|");
		stringBuilder.append(offerInformation.getExpiryDatetime());
		stringBuilder.append("|");
		stringBuilder.append(offerInformation.getStartDate());

		return stringBuilder.toString();
	}

	public String getRequestTypeId()
	{
		return getClass().getName();

	}

}
