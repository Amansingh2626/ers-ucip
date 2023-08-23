package com.seamless.ers.links.uciplink.ucip;

import com.seamless.common.ExtendedProperties;
import com.seamless.common.StringUtils;
import com.seamless.common.locale.ERSInvalidCurrencyException;
import com.seamless.common.locale.Locale;
import com.seamless.common.uciplib.common.*;
import com.seamless.common.uciplib.v31.requests.UpdateBalanceAndDateRequest.DedicatedAccountUpdateInformation;
import com.seamless.common.uciplib.v34.requests.*;
import com.seamless.common.uciplib.v34.responses.*;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.utils.CurrencyHandler;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmPoint;

import java.net.ConnectException;
import java.util.HashMap;

public class UCIPAdaptor34 extends IUCIPAdaptor
{

	public static final String TRANSACTION_TYPE_KEY = "transactionType";
	public static final String SUBSCRIBER_NUMBER = "subscriberNumber";
	public static final String CREDIT = "Credit";
	public static final String DEBIT = "Debit";
	private static final String FIELD_PRODUCTSKU = "productSKU";
	private static final String FIELD_PROFILE_ID = "ProfileId";

	public UCIPAdaptor34(
			UCIPLinkConfig config,
			ExtendedProperties properties)
	{
		super(config,
				properties);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UCIPResponse addToAccountFAF(String subscriberNumber, String fafNumber, String transactionId)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor34::addToAccountFAF");

		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			FafInformation[] fafInformation = new FafInformation[1];

			fafInformation[0] = new FafInformation();
			fafInformation[0].setFafNumber(fafNumber);
			fafInformation[0].setFafIndicator(config.getFafIndicator());
			fafInformation[0].setOwner("Subscriber");
			com.seamless.common.uciplib.v34.requests.UpdateFaFListRequest request = new com.seamless.common.uciplib.v34.requests.UpdateFaFListRequest(
					subscriberNumber, "ADD", fafInformation);
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);
			com.seamless.common.uciplib.v34.responses.UpdateFaFListResponse response = (UpdateFaFListResponse) makeRequest(request);
			result.setUcipResponseCode(response.getResponseCode());

		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed for addAccountFAF: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		finally
		{
			point.collect();
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UCIPResponse removeAccountFAF(String subscriberNumber, String fafNumber, String transactionId)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor34::removeAccountFAF");

		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			FafInformation[] fafInformation = new FafInformation[1];

			fafInformation[0] = new FafInformation();
			fafInformation[0].setFafNumber(fafNumber);
			fafInformation[0].setFafIndicator(config.getFafIndicator());
			fafInformation[0].setOwner("Subscriber");
			com.seamless.common.uciplib.v34.requests.UpdateFaFListRequest request = new com.seamless.common.uciplib.v34.requests.UpdateFaFListRequest(
					subscriberNumber, "DELETE", fafInformation);
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);
			com.seamless.common.uciplib.v34.responses.UpdateFaFListResponse response = (UpdateFaFListResponse) makeRequest(request);
			result.setUcipResponseCode(response.getResponseCode());

		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed for removeAccountFAF: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		finally
		{
			point.collect();
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UCIPResponse getAccountDetails(String subscriberNumber, String transactionId, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor34::getAccountDetails");

		GetAccountDetailsRequest request = null;
		GetAccountDetailsResponse response = null;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			request = new GetAccountDetailsRequest(subscriberNumber);
			request.setEnableFetchSubscriberSegment(enableFetchSubscriberSegment);
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);
			response = (GetAccountDetailsResponse) makeRequest(request);

			result.setUcipResponseCode(response.getResponseCode());
			result.setDefaultServiceClass(ifNull(response.getServiceClassOriginal(), 0));
			result.setCurrentServiceClass(ifNull(response.getServiceClassCurrent(), 0));
			//changes for POC
			if(response.getOfferInformation() != null){
				for (OfferInformation oi : response.getOfferInformation()){
					if(oi.getOfferID() == 400 && oi.getAttributeInformations() != null && oi.getAttributeInformations().length > 0){
						for (AttributeInformation ai : oi.getAttributeInformations()){
							logger.debug("AttributeInformation : "+ ai.toString());
							if(ai.getAttributeName().equals("Segment")){
								result.setSegment(ai.getAttributeValueString());
							}
						}
					}
				}
			}
			logger.debug("Segment set in UCIPResponse: "+result.getSegment());
			
			result.setSupervisionExpiryDate(response.getSupervisionExpiryDate());
			result.setServiceFeeExpiryDate(response.getServiceFeeExpiryDate());

			result.setLanguageInISO6391(config.getLanguageInISO6391(response.getLanguageIDCurrent()));
			result.setServiceOfferings(response.getServiceOfferings());
			if (response.getAccountFlags() != null)
			{
				result.setAccountFlags(new AccountFlags(response.getAccountFlags()));
			}
			if (response.getCurrency1() != null)
			{
				// Get main account balance
				if(config.isTransformCurrencyEnabled()) {
					logger.debug(config.getModuleProperties("locale.custom_currency.").toString());
					try {
						Locale.getInstance().getFractionDigits(response.getCurrency1());
					} catch (Exception e) {
						//Currency is not based on ISO standards
						response.setCurrency1(config.getModuleProperties("locale.custom_currency.").keySet().toArray()[0].toString());
					}
				}
				Amount balance = CurrencyHandler.createAmount(response.getAccountValue1(), response.getCurrency1());
				result.setBalance(balance);
			}
			result.setTemporaryBlockedFlag(response.getTemporaryBlockedFlag());

			//set native result message as per the error code in extraFields
			if(config.isEnableNativeCodeMapping())
			{
				String errorDescription = config.getNativeCodeMapping().get(response.getResponseCode().toString()) == null ? "Unknown error!" :config.getNativeCodeMapping().get(response.getResponseCode().toString());

				extraFields.put("NativeResultCode", response.getResponseCode().toString());
				extraFields.put("NativeResultDescription", errorDescription);
				extraFields.put("LinkType", "UCIPLink");
				extraFields.put("LinkNodeId", "UCIPLink");
			}
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed for getAccountDetails: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		finally
		{
			point.collect();
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UCIPResponse getAccountFAF(String subscriberNumber, String transactionId)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor34::getAccountFAF");

		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			GetFaFListRequest request = new GetFaFListRequest(subscriberNumber, getRequestedOwner());
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);
			GetFaFListResponse response = (GetFaFListResponse) makeRequest(request);
			result.setErsResponseCode(response.getResponseCode());
			result.setFafList(response.getFafInformationList());
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed for getAccountFAF: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		finally
		{
			point.collect();
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UCIPResponse getBalance(String subscriberNumber, Integer dedicatedAccountId,Integer dedicatedAccountLastId, String transactionId, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor34::getBalance");

		GetBalanceAndDateRequest request = null;
		GetBalanceAndDateResponse response = null;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			request = new GetBalanceAndDateRequest(subscriberNumber);
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);

			if (dedicatedAccountId != null)
			{
				DedicatedAccountSelection[] das =
				{ new DedicatedAccountSelection(dedicatedAccountId) };
				request.setDedicatedAccountSelection(das);
			}

			response = (GetBalanceAndDateResponse) makeRequest(request);

			result.setUcipResponseCode(response.getResponseCode());

			if (response.isOkResponse())
			{
				if (dedicatedAccountId == null)
				{
					if (response.getCurrency1() != null)
					{
						// Get main account balance
						if(config.isTransformCurrencyEnabled()) {
							logger.debug(config.getModuleProperties("locale.custom_currency.").toString());
							try {
								Locale.getInstance().getFractionDigits(response.getCurrency1());
							} catch (Exception e) {
								//Currency is not based on ISO standards
								response.setCurrency1(config.getModuleProperties("locale.custom_currency.").keySet().toArray()[0].toString());
							}
						}
						Amount balance = CurrencyHandler.createAmount(response.getAccountValue1(), response.getCurrency1());
						result.setBalance(balance);
					}
				}
				else
				{
					if(config.isTransformCurrencyEnabled()) {
						logger.debug(config.getModuleProperties("locale.custom_currency.").toString());
						try {
							Locale.getInstance().getFractionDigits(response.getCurrency1());
						} catch (Exception e) {
							//Currency is not based on ISO standards
							response.setCurrency1(config.getModuleProperties("locale.custom_currency.").keySet().toArray()[0].toString());
						}
					}
					Amount balance = getDedicatedAccountBalance(
							response.getDedicatedAccountInformation(),
							dedicatedAccountId,
							response.getCurrency1());
					result.setBalance(balance);
				}
			}
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed for balance enquiry: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		finally
		{
			point.collect();
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public UCIPResponse refill(
			String subscriberNumber,
			Amount amount,
			String transactionId,
			String transactionType,
			String profileId,
			Integer dedicatedAccountId,
			ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor34::refill");

		RefillRequest request = null;
		RefillResponse response = null;
		String originalCurrency = amount.getCurrency();
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			if(config.isTransformCurrencyEnabled())
				originalCurrency = config.getTransformCurrencyCode();

			request = new RefillRequest(subscriberNumber, CurrencyHandler.getAmountValueAsLong(amount), originalCurrency, profileId);
			// Making a deep copy of extra fields
			ERSHashtableParameter copyExtraFields = new ERSHashtableParameter(new HashMap<>(extraFields.getParameters()));
			extraFields.put(REFILL_KEY, REFILL_VALUE);
			extraFields.put(TRANSACTION_TYPE_KEY, CREDIT);
			resetExternalDataParameters();
			setDefaultVariables(request, extraFields);
			request.setOriginNodeType(getOriginNodeType(extraFields));
			request.setOriginTransactionID(transactionId);
			request.setTransactionType(CREDIT);

			request.setRequestRefillAccountAfterFlag(true);

			if(config.isOverrideRequestParamsInTopUp()) {

				if(config.getOverrideParamKeys().contains(TRANSACTION_TYPE_KEY)) {
					request.setTransactionType(copyExtraFields.get(TRANSACTION_TYPE_KEY));
					extraFields.put(TRANSACTION_TYPE_KEY, copyExtraFields.get(TRANSACTION_TYPE_KEY));
				}

				if(config.getOverrideParamKeys().contains(FIELD_ORIGINHOSTNAME)){
					request.setOriginHostName(copyExtraFields.get(FIELD_ORIGINHOSTNAME));
					extraFields.put(FIELD_ORIGINHOSTNAME, copyExtraFields.get(FIELD_ORIGINHOSTNAME));
				}

				if(config.getOverrideParamKeys().contains(TRANSACTION_CODE)){
					request.setTransactionCode(copyExtraFields.get(TRANSACTION_CODE));
					extraFields.put(TRANSACTION_CODE, copyExtraFields.get(TRANSACTION_CODE));
				}
			}

			if (extraFields.get(VALIDATE_SUBSCRIBER_LOCATION) != null)
			{
				request.setValidateSubscriberLocation(StringUtils.parseBoolean(extraFields.get(VALIDATE_SUBSCRIBER_LOCATION)));
			}

			response = (RefillResponse) makeRequest(request);

			result.setUcipResponseCode(response.getResponseCode());

			if (response.getAccountBeforeRefill() != null)
			{
				Amount balance;
				if (dedicatedAccountId == null)
				{
					balance = CurrencyHandler.createAmount(response.getAccountBeforeRefill().getAccountValue1(), originalCurrency);
				}
				else
				{
					balance = getDedicatedAccountBalance(
							response.getAccountBeforeRefill().getDedicatedAccountInformation(),
							dedicatedAccountId, originalCurrency);
				}
				result.setBalance(balance); /* Set balance before transaction */

				result.setSupervisionExpiryDateBefore(response.getAccountBeforeRefill().getSupervisionExpiryDate());

				// Set dedicated accounts from before
				if (response.getAccountBeforeRefill().getDedicatedAccountInformation() != null && response.getCurrency1() != null)
				{
					for (int i = 0; i < response.getAccountBeforeRefill().getDedicatedAccountInformation().length; i++)
					{
						DedicatedAccountInformation dedicatedAccount = response.getAccountBeforeRefill().getDedicatedAccountInformation()[i];
						if (dedicatedAccount != null)
						{
							if (dedicatedAccount.getDedicatedAccountValue1() != null)
							{
								result.setDedicatedAccountValueBefore(
										dedicatedAccount.getDedicatedAccountID(),
										dedicatedAccount.getDedicatedAccountValue1());
							}

						}

					}
				}

			}

			if (response.getAccountAfterRefill() != null)
			{
				Amount balanceAfter = CurrencyHandler.createAmount(
						response.getAccountAfterRefill().getAccountValue1(), originalCurrency);
				result.setBalanceAfter(balanceAfter); /*
													 * Set balance after
													 * transaction
													 */

				result.setSupervisionExpiryDateAfter(response.getAccountAfterRefill().getSupervisionExpiryDate());

				// Set dedicated accounts from after
				if (response.getAccountAfterRefill().getDedicatedAccountInformation() != null && response.getCurrency1() != null)
				{
					for (int i = 0; i < response.getAccountAfterRefill().getDedicatedAccountInformation().length; i++)
					{
						DedicatedAccountInformation dedicatedAccount = response.getAccountAfterRefill().getDedicatedAccountInformation()[i];
						if (dedicatedAccount != null)
						{
							if (dedicatedAccount.getDedicatedAccountValue1() != null)
							{
								result.setDedicatedAccountValueAfter(
										dedicatedAccount.getDedicatedAccountID(),
										dedicatedAccount.getDedicatedAccountValue1());
							}

						}

					}
				}

			}
			// Set dedicated accounts from refill value total

			if (response.getRefillInformation() != null && response.getRefillInformation().getRefillValueTotal() != null
					&& response.getRefillInformation().getRefillValueTotal().getDedicatedAccountRefillInformation() != null)
			{
				DedicatedAccountRefillInformation[] drf = response.getRefillInformation().getRefillValueTotal().getDedicatedAccountRefillInformation();

				for (int i = 0; i < drf.length; i++)
				{
					if (drf[i] != null && drf[i].getDedicatedAccountID() != null && drf[i].getRefillAmount1() != null)
					{
						result.setDedicatedAccountRefillValueTotal(drf[i].getDedicatedAccountID(), drf[i].getRefillAmount1());
					}
				}

			}
			
			// Set refillValuePromotion
			
			if (response.getRefillInformation() != null && response.getRefillInformation().getRefillValuePromotion() != null
					&& response.getRefillInformation().getRefillValuePromotion().getDedicatedAccountRefillInformation() != null)
			{
				
				DedicatedAccountRefillInformation[] drf = response.getRefillInformation().getRefillValuePromotion().getDedicatedAccountRefillInformation();
				
				for (int i = 0; i < drf.length; i++)
				{
					if (drf[i] != null && drf[i].getDedicatedAccountID() != null)
					{
						result.setDedicatedAccountRefillValuePromotion(drf[i].getDedicatedAccountID(), drf[i]);
					}
				}
			}
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed for refill: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		finally
		{
			point.collect();
		}

		return result;
	}

	/**
	 * Added for Data Bundle request but currently not supported in v34
	 * @param subscriberNumber
	 * @param amount
	 * @param transactionId
	 * @param transactionType
	 * @param profileId
	 * @param dedicatedAccountId
	 * @param extraFields
     * @param offerProductDTO
     * @return
     */
	public UCIPResponse updateOffer(
			String subscriberNumber,
			Amount amount,
			String transactionId,
			String transactionType,
			String profileId,
			Integer dedicatedAccountId,
			ERSHashtableParameter extraFields,
			OfferProductsDTO offerProductDTO, String currency)
	{
		return new UCIPResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
	}



	/**
	 * {@inheritDoc}
	 */
	public UCIPResponse updateServiceClass(String subscribernumber, int serviceClass, String reference, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor34::updateServiceClass");

		String action = "SetOriginal";
		UpdateServiceClassRequest request = null;
		UpdateServiceClassResponse response = null;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			request = new UpdateServiceClassRequest(subscribernumber, serviceClass, action, null);
			resetExternalDataParameters();
			setDefaultVariables(request, extraFields);
			request.setOriginTransactionID(reference);
			response = (UpdateServiceClassResponse) makeRequest(request);

			result.setUcipResponseCode(response.getResponseCode());
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed to update service class: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		finally
		{
			point.collect();
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public UCIPResponse updateBalance(
			String subscriberNumber,
			Amount relativeBalance,
			String transactionId,
			String transactionType,
			Integer dedicatedAccountId,
			ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor34::updateBalance");
		UpdateBalanceAndDateRequest request;
		UpdateBalanceAndDateResponse response;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{

			String originalCurrency = relativeBalance.getCurrency();
			if (config.isTransformCurrencyEnabled())
			{
				originalCurrency = config.getTransformCurrencyCode();
			}

			if (dedicatedAccountId == null)
			{
				request = new UpdateBalanceAndDateRequest(subscriberNumber, CurrencyHandler.getAmountValueAsLong(relativeBalance), originalCurrency);
			}
			else
			{
				request = new UpdateBalanceAndDateRequest(subscriberNumber, 0L, originalCurrency);
				DedicatedAccountUpdateInformation info = new DedicatedAccountUpdateInformation();
				info.setDedicatedAccountID(dedicatedAccountId);
				info.setAdjustmentAmountRelative(CurrencyHandler.getAmountValueAsLong(relativeBalance));
				info.setDedicatedAccountUnitType(Integer.parseInt(config.getUcipProperties().getProperty("UpdateBalanceAndDate.dedicatedAccountUnitType", "1")));
				DedicatedAccountUpdateInformation[] list = {info};
				request.setDedicatedAccountUpdateInformation(list);
			}
			extraFields.put(UPDATE_BALANCE_KEY,UPDATE_BALANCE_VALUE);
			extraFields.put(TRANSACTION_TYPE_KEY, DEBIT);
			extraFields.put(SUBSCRIBER_NUMBER, subscriberNumber);
			resetExternalDataParameters();
			setDefaultVariables(request, extraFields);
			/*String externalData2 = getRefillExternalData2(extraFields, transactionType, "updateBalance");
			request.setExternalData2(externalData2);*/
			request.setOriginTransactionID(transactionId);
			request.setOriginNodeType(getOriginNodeType(extraFields));
			response = (UpdateBalanceAndDateResponse) makeRequest(request);
			result.setUcipResponseCode(response.getResponseCode());
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err, e);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed for UpdateBalance: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		finally
		{
			point.collect();
		}

		return result;
	}

	@Override
	public UCIPResponse updateAccumulators(String subscriberNumber, String transactionId, AccumulatorUpdateInformation[] info, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
				"UCIPAdaptor34::updateAccumulators");
		point.collect();
		return new UCIPResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
	}

	@Override
	public UCIPResponse getCapabilities()
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
				"UCIPAdaptor34::getCapabilities");
		point.collect();
		return new UCIPResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
	}

	@Override
	public UCIPResponse deleteOffer(String transactionId, String subscriberNumber, OfferProductsDTO offerProductDTO, ERSHashtableParameter extraFields) {
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPLinkServicesImpl::deleteOffer");
		try
		{
			return new UCIPResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
		}
		finally
		{
			point.collect();
		}
	}

	private Amount getDedicatedAccountBalance(DedicatedAccountInformation[] daInfoList, int dedicatedAccountId, String currency)
			throws ERSInvalidCurrencyException
	{
		if (daInfoList != null)
		{
			// get Dedicated account balance
			for (DedicatedAccountInformation da : daInfoList)
			{
				if (da.getDedicatedAccountID().equals(dedicatedAccountId) && da.getDedicatedAccountValue1() != null && currency != null)
				{
					return CurrencyHandler.createAmount(da.getDedicatedAccountValue1(), currency);
				}
			}
		}

		return null;
	}

	@Override
	public UCIPResponse redeemVoucher(String subscribernumber, String voucherCode, String transactionId, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor34::redeemVoucher");

		RefillRequest request = null;
		RefillResponse response = null;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			request = new RefillRequest(subscribernumber, voucherCode);
			resetExternalDataParameters();
			setDefaultVariables(request, extraFields);
			request.setOriginTransactionID(transactionId);
			response = (RefillResponse) makeRequest(request);

			result.setUcipResponseCode(response.getResponseCode());
			if (response.getAccountAfterRefill() != null)
			{
				Amount balance = CurrencyHandler.createAmount(response.getAccountAfterRefill().getAccountValue1(), response.getCurrency1());
				result.setBalanceAfter(balance);
				result.setServiceFeeExpiryDateAfter(response.getAccountAfterRefill().getServiceFeeExpiryDate());
			}
			if (response.getAccountBeforeRefill() != null)
			{
				Amount balance = CurrencyHandler.createAmount(response.getAccountBeforeRefill().getAccountValue1(), response.getCurrency1());
				result.setBalanceAfter(balance);
				result.setServiceFeeExpiryDateBefore(response.getAccountBeforeRefill().getServiceFeeExpiryDate());
			}

		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed to redeemVoucher: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		finally
		{
			point.collect();
		}

		return result;
	}


	private String getRefillExternalData1(ERSHashtableParameter extraFields, String transactionType)
	{
		String value = extraFields.get(IUCIPAdaptor.FIELD_EXTERNALDATA1);
		String externalData1;
		// If there is externalData1 set from client, then use that value.
		logger.debug("value : " + value);
		if (value != null && !value.isEmpty())
		{
			logger.debug("No externalData1 set in extraFields");
			return value;
		}
		// Otherwise, use the value from configuration
		logger.debug("PRODUCTSKU : " + extraFields.get(FIELD_PRODUCTSKU));
		logger.debug("extraFields.get(\"SenderId\") : " + extraFields.get("SenderId"));
		logger.debug("extraFields.get(\"SENDER_MSISDN\") : " + extraFields.get("SENDER_MSISDN"));
		if (extraFields.get(FIELD_PRODUCTSKU) != null)
		{
			String key = IUCIPAdaptor.FIELD_EXTERNALDATA1 + "." + extraFields.get(FIELD_PRODUCTSKU) + "." + transactionType;
			logger.debug("key = "+ key);
			String configexternalData1 = config.getUcipProperties().getProperty(key);
			logger.debug("configexternalData1 = "+ configexternalData1);
			if (configexternalData1 != null && configexternalData1.isEmpty() && extraFields.get("SENDER_MSISDN") != null)
			{
				externalData1 = extraFields.get("SENDER_MSISDN");
				return externalData1;
			}
			if (configexternalData1 != null)
			{
				return configexternalData1;
			}
		}
		return "";
	}

	/*private String getRefillExternalData2(ERSHashtableParameter extraFields, String transactionType, String method)
	{
		String value = extraFields.get(IUCIPAdaptor.FIELD_EXTERNADATA2);
		// If there is externalData2 set from client, then use that value.
		if (value != null)
		{
			return value;
		}
		// Otherwise, use the value from configuration
		String key = IUCIPAdaptor.FIELD_EXTERNADATA2 + "." + extraFields.get(FIELD_PRODUCTSKU) + "." + transactionType;
		String externalData2 = config.getUcipProperties().getProperty(key);
		if(externalData2!=null && externalData2.isEmpty() && extraFields.get("RECEIVER_ACCOUNT_ID")!=null)
		{
			externalData2 = extraFields.get("RECEIVER_ACCOUNT_ID");
		}
		Boolean isRefill = config.isSetExternalData2AsERSReference() && "refill".equals(method);
		if(isRefill && (externalData2 == null || externalData2.isEmpty()) && extraFields.get("reference")!=null)
		{
			logger.debug("ERS refill external data2 refence: "+extraFields.get("reference"));
			externalData2 = extraFields.get("reference");
		}
		return externalData2;

	}

	void setDefaultLocalVariables(UCIPSubscriberRequest req, ERSHashtableParameter extraFields)
	{
		req.setOriginHostName(originHostName);
		req.setSubscriberNumberNAI(subscriberNumberNAI);
		req.setOriginNodeType(originNodeType);
		logger.debug("Setting Default Variables ...");
		setExternalData1(extraFields);
		
		Boolean refillOrUpdateBalance = extraFields.getParameters().containsKey("refill") || extraFields.getParameters().containsKey("updateBalance");
		if (config.isExternalData1enable() && extraFields != null && extraFields.getParameters().containsKey(FIELD_EXTERNADATA1) 
				&& refillOrUpdateBalance)
		{
			logger.info("external data 1 : extraFields.get(FIELD_EXTERNADATA1)");
			req.setExternalData1(extraFields.get(FIELD_EXTERNADATA1));
		}

		if (req.getExternalData2() == null)
		{
			req.setExternalData2(externalData2);
		}

		if (externalData3 != null && !externalData3.isEmpty())
		{
			req.setExternalData3(externalData3);
		}

		if (extraFields.getParameters().containsKey("refill") && config.isOriginOperatorIDRefillEnable())
		{
			req.setOriginOperatorID(originOperatorID);
		}
		else if (extraFields.getParameters().containsKey("updateBalance") && config.isOriginOperatorIDUpdateBalanceEnable())
		{

			req.setOriginOperatorID(originOperatorID);
		}
	}

	void setExternalData1(ERSHashtableParameter extraFields)
	{
		String productSKU = extraFields.get("productSKU");
		String externalData1Value = null;

		if(config.isExternalData1enable() && extraFields.getParameters().containsKey(FIELD_EXTERNADATA1))
		{
			externalData1 = extraFields.get(FIELD_EXTERNADATA1);
		}
		else if (config.isExternalData1enable() && config.getExternalData1Properties() != null && extraFields.getParameters().containsKey("updateBalance"))
		{
			externalData1Value = config.getExternalData1Properties().getProperty(productSKU + ".Debit");
			if (externalData1Value != null && externalData1Value.isEmpty() && extraFields.get("SENDER_ACCOUNT_ID") != null)
			{
				logger.debug("externalData1 from request extraFields(SENDER_ACCOUNT_ID)= "+ extraFields.get("SENDER_ACCOUNT_ID"));
				externalData1 = extraFields.get("SENDER_ACCOUNT_ID");
			}
			else
			{
				logger.debug("externalData1Value from properties for debit = "+ externalData1Value);
				externalData1 = externalData1Value;
			}
		}
		else if (config.isExternalData1enable() && config.getExternalData1Properties() != null && extraFields.getParameters().containsKey("refill"))
		{
			externalData1Value = config.getExternalData1Properties().getProperty(productSKU + ".Credit");
			if (externalData1Value != null && externalData1Value.isEmpty() && productSKU.equalsIgnoreCase("p2p") && extraFields.get("SENDER_ACCOUNT_ID") != null)
			{
				logger.debug("externalData1 from P2P request extraFields(SENDER_ACCOUNT_ID)= "+ extraFields.get("SENDER_ACCOUNT_ID"));
				externalData1 = extraFields.get("SENDER_ACCOUNT_ID");
			}
			else if (externalData1Value != null && externalData1Value.isEmpty() && extraFields.get("SENDER_MSISDN") != null)
			{
				logger.debug("externalData1 from request extraFields(SENDER_MSISDN)= "+ extraFields.get("SENDER_MSISDN"));
				externalData1 = extraFields.get("SENDER_MSISDN");
			}
			else if(externalData1Value != null && externalData1Value.isEmpty() && extraFields.get("SENDER_MSISDN") == null)
			{
				logger.debug("externalData1Value properties default : "+ config.getExternalData1Properties().getProperty("default", null));
				externalData1 = config.getExternalData1Properties().getProperty("default", null);
			}
			else
			{
				logger.debug("externalData1Value from properties for credit = "+ externalData1Value);
				externalData1 = externalData1Value;
			}
		}
		logger.debug("setExternalData1ForProductSKU :: externalData1Value : " + externalData1);
		extraFields.put(FIELD_EXTERNADATA1,externalData1);
	}*/
}
