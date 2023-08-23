package com.seamless.ers.links.uciplink.ucip;

import com.seamless.common.ExtendedProperties;
import com.seamless.common.StringUtils;
import com.seamless.common.locale.ERSInvalidCurrencyException;
import com.seamless.common.uciplib.common.*;
import com.seamless.common.uciplib.v50.requests.*;
import com.seamless.common.uciplib.v50.responses.*;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.utils.CurrencyHandler;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.UCIPLinkConfig.TreeDefinedFieldValue;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;
import com.seamless.ers.links.uciplink.utils.ExpireFormat;
import com.seamless.ers.links.uciplink.utils.UCIPUtils;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.*;

public class UCIPAdaptor50 extends IUCIPAdaptor
{
	public UCIPAdaptor50(UCIPLinkConfig config, ExtendedProperties properties)
	{
		super(config, properties);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UCIPResponse addToAccountFAF(String subscriberNumber, String fafNumber, String transactionId)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::addToAccountFAF");

		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			FafInformation[] fafInformation = new FafInformation[1];

			fafInformation[0] = new FafInformation();
			fafInformation[0].setFafNumber(fafNumber);
			fafInformation[0].setFafIndicator(config.getFafIndicator());
			fafInformation[0].setOwner("Subscriber");
			UpdateFaFListRequest request = new UpdateFaFListRequest(subscriberNumber, "ADD", fafInformation);
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);
			UpdateFaFListResponse response = (UpdateFaFListResponse) makeRequest(request);
			result.setUcipResponseCode(response.getResponseCode());

		} catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		} catch (Exception e)
		{
			logger.error("UCIP Request failed for addAccountFAF: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		} finally
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
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::removeAccountFAF");

		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			FafInformation[] fafInformation = new FafInformation[1];

			fafInformation[0] = new FafInformation();
			fafInformation[0].setFafNumber(fafNumber);
			fafInformation[0].setFafIndicator(config.getFafIndicator());
			fafInformation[0].setOwner("Subscriber");
			UpdateFaFListRequest request = new UpdateFaFListRequest(subscriberNumber, "DELETE", fafInformation);
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);
			UpdateFaFListResponse response = (UpdateFaFListResponse) makeRequest(request);
			result.setUcipResponseCode(response.getResponseCode());

		} catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		} catch (Exception e)
		{
			logger.error("UCIP Request failed for removeAccountFAF: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		} finally
		{
			point.collect();
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UCIPResponse getAccountDetails(String subscriberNumber, String transactionId,  ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::getAccountDetails");

		GetAccountDetailsRequest request = null;
		GetAccountDetailsResponse response = null;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
				request = new GetAccountDetailsRequest(subscriberNumber);
				resetExternalDataParameters();
				setDefaultVariables(request);
				request.setOriginTransactionID(transactionId);
				response = (GetAccountDetailsResponse) makeRequest(request);
	
				result.setUcipResponseCode(response.getResponseCode());
				result.setDefaultServiceClass(ifNull(response.getServiceClassOriginal(), 0));
				result.setCurrentServiceClass(ifNull(response.getServiceClassCurrent(), 0));
				result.setSupervisionExpiryDate(response.getSupervisionExpiryDate());
				result.setServiceFeeExpiryDate(response.getServiceFeeExpiryDate());
	
				result.setLanguageInISO6391(config.getLanguageInISO6391(response.getLanguageIDCurrent()));
				result.setServiceOfferings(response.getServiceOfferings());
				if (response.getAccountFlags() != null)
				{
					result.setAccountFlags(new AccountFlags(response.getAccountFlags()));
				}
				if (response.getPamInformationList() != null)
				{
					result.setPamInformationList(response.getPamInformationList());
				}
				if (response.getCurrency1() != null)
				{
					// Get main account balance
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

		} catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		} catch (Exception e)
		{
			logger.error("UCIP Request failed for getAccountDetails: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		} finally
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
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::getAccountFAF");

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
		} catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		} catch (Exception e)
		{
			logger.error("UCIP Request failed for getAccountFAF: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		} finally
		{
			point.collect();
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UCIPResponse getBalance(String subscriberNumber, Integer dedicatedAccountId, Integer dedicatedAccountLastId, String transactionId, ERSHashtableParameter extraFields)
	{
		logger.info("Using getBalanceAndDateAPI::UCIPAdaptor50");
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::getBalance");

		GetBalanceAndDateRequest request = null;
		GetBalanceAndDateResponse response = null;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			request = new GetBalanceAndDateRequest(subscriberNumber);
			resetExternalDataParameters();
			setDefaultVariables(request);
			if(config.getBalanceAndDateSetChannelAsOriginHostName())
			{
				if(extraFields != null && StringUtils.isNotBlank(extraFields.get("channel")))
				{
					request.setOriginHostName(extraFields.get("channel"));
				}
			}
			request.setOriginTransactionID(transactionId);
			if(config.isNegotiatedCapabilitiesGetBalanceAndDateEnable())
			{
				request.setNegotiatedCapabilities(UCIPUtils.extractNegotiatedCapabilitiesFromConfig(config));
			}
			if(config.isSubordinateDedicatedAccountDetailsFlagEnable())
			{
				request.setRequestSubordinateDedicatedAccountDetailsFlag(config.getSubordinateDedicatedAccountDetailsFlagValue());
			}

			if (dedicatedAccountId != null)
			{
				final DedicatedAccountSelection dedicatedAccountSelection = new DedicatedAccountSelection(dedicatedAccountId);
				if(dedicatedAccountLastId != null)
				{
					dedicatedAccountSelection.setDedicatedAccountIDLast(dedicatedAccountLastId);
				}
				DedicatedAccountSelection[] das = {dedicatedAccountSelection};
				request.setDedicatedAccountSelection(das);
			}

			if(config.isEnableMessageCapabilityFlag()){
				request.setMessageCapabilitFlag(config.getMessageCapabilityFlag());
			}

			response = (GetBalanceAndDateResponse) makeRequest(request);

			result.setUcipResponseCode(response.getResponseCode());
			result.setLanguageInISO6391(config.getLanguageInISO6391(response.getLanguageIDCurrent()));

			if (response.isOkResponse())
			{
				if (dedicatedAccountId == null)
				{
					if (response.getCurrency1() != null)
					{
						// Get main account balance
						Amount balance = CurrencyHandler.createAmount(response.getAccountValue1(), response.getCurrency1());
						result.setBalance(balance);
					}
				}
				else
				{
					Amount balance = getDedicatedAccountBalance(response.getDedicatedAccountInformation(), dedicatedAccountId, response.getCurrency1());
					result.setBalance(balance);
				}
				if(null == result.getBalance() &&
						null != response.getAccountValue1() && response.getCurrency1() != null){
					Amount balance = CurrencyHandler.createAmount(response.getAccountValue1(), response.getCurrency1());
					result.setBalance(balance);
				}

				if(null != response.getOfferInformation()) {
					result.setOfferInformation(response.getOfferInformation());
				}

				if(null != response.getServiceClassCurrent()) {
					result.setCurrentServiceClass(response.getServiceClassCurrent());
				}
				if(response.getSupervisionExpiryDate() != null) {
					result.setSupervisionExpiryDate(response.getSupervisionExpiryDate());
				}
				result.setCurrentServiceClass(response.getServiceClassCurrent());
			}
			//set native result message as per the error code in extraFields
			if(config.isEnableNativeCodeMapping())
			{
				String errorDescription = config.getNativeCodeMapping().get(response.getResponseCode().toString()) == null ? "Unknown error!" :config.getNativeCodeMapping().get(response.getResponseCode().toString());

				extraFields.put("NativeResultCode", response.getResponseCode().toString());
				extraFields.put("NativeResultDescription", errorDescription);
				extraFields.put("LinkType", "UCIPLink");
				extraFields.put("LinkNodeId", "UCIPLink");
			}

		} catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		} catch (Exception e)
		{
			logger.error("UCIP Request failed for balance enquiry: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		} finally
		{
			point.collect();
		}

		if(response != null && response.getUcipServerIp()!=null){
			logger.info("ucipServerIp is "+ response.getUcipServerIp());
			result.setInfo5(response.getUcipServerIp());
		}else{
			logger.info("ucipServerIp is null");
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UCIPResponse refill(String subscriberNumber, Amount amount, String transactionId, String transactionType, String profileId, Integer dedicatedAccountId, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::refill");

		RefillRequest request = null;
		RefillResponse response = null;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

        try
        {
        	if(extraFields.getParameters().containsKey("SCRATCH_CARD_NUM")) {
				request = new RefillRequest(subscriberNumber, extraFields.getParameters().get("SCRATCH_CARD_NUM"),false);
			}else
			{
				if(config.isEnableSourceMultiPlier()) {
					BigDecimal bd = UCIPUtils.amountWithSourceMultiplier(config, amount, extraFields) == null ? amount.getValue() : UCIPUtils.amountWithSourceMultiplier(config, amount, extraFields);
					amount.setValue(bd);
				}
				request = new RefillRequest(subscriberNumber, CurrencyHandler.getAmountValueAsLong(amount), amount.getCurrency(), profileId);
			}
        	resetExternalDataParameters();
            setDefaultVariables(request, extraFields);
			request.setOriginNodeType(getOriginNodeType(extraFields));
			logger.info("originNodeType value = " + request.getOriginNodeType());
			request.setOriginHostName(getOriginHostName(extraFields));
			logger.info("originHostName value = " + request.getOriginHostName());
            request.setOriginTransactionID(transactionId);
			if(config.isUseCustomTransactionId())
			{
				request.setOriginTransactionID(extraFields.get(config.getCustomTransactionIdExtraFieldName()));
			}
			logger.info("useCustomOriginOperatorId = "+ config.useCustomOriginOperatorId());

            if(config.useCustomOriginOperatorId())
			{
				String customOriginOperationIdField = config.getCustomOriginOperatorIdExtraFieldName();
				final String customOriginOperationId = extraFields.get(customOriginOperationIdField);
				if(StringUtils.isNotBlank(customOriginOperationId))
				{
					logger.info("customOriginOperationId value = " + customOriginOperationId);
					request.setOriginOperatorID(customOriginOperationId);
				}
			}



            if (config.isTransactionTypeRefillEnable() && StringUtils.isNotBlank(config.getUcipProperties().getProperty("transactionType")))
            {
            	logger.info("TransactionTypeRefillEnabled = " + config.isTransactionTypeRefillEnable());
            	logger.info("Setting transactionType from config");
             	request.setTransactionType(config.getUcipProperties().getProperty("transactionType"));
             	logger.info(" Transaction Type added in request with value =  " + request.getTransactionType());
            }
			else if(config.isTransactionTypeRefillEnable() && extraFields.get("TRANSACTION_PROFILE") != null)
			{
				logger.info("TransactionTypeRefillEnabled = " + config.isTransactionTypeRefillEnable());
				logger.info("Setting transactionType from extraFields");
				request.setTransactionType(extraFields.get("TRANSACTION_PROFILE"));
				logger.info(" Transaction Type added in request with value =  " + request.getTransactionType());
			}
            else
            {
            	//default value gets set
				logger.info("Setting transactionType from default config");
             	request.setTransactionType(transactionType);
             	logger.info(" Transaction Type added in request with value =  " + request.getTransactionType());

            }

            if(config.isRequestRefillAccountBeforeFlagEnable()) {
            	request.setRequestRefillAccountBeforeFlag(config.getRequestRefillAccountBeforeFlagValue());
			}

            request.setRequestRefillAccountAfterFlag(config.enableRefillAccountAfterFlag());
            request.setRequestRefillDetailsFlag(config.enableRefillDetailsFlag());
            if (extraFields.get(VALIDATE_SUBSCRIBER_LOCATION) != null)
            {
                request.setValidateSubscriberLocation(StringUtils.parseBoolean(extraFields.get(VALIDATE_SUBSCRIBER_LOCATION)));
            }

			if(config.isNegotiatedCapabilitiesRefillEnable())
			{
				Integer[] negotiatedCapabilities = null;
				request.setEnableNegotiatedCapabilities(config.isNegotiatedCapabilitiesRefillEnable());
				negotiatedCapabilities = UCIPUtils.extractNegotiatedCapabilities(extraFields);
				if(negotiatedCapabilities == null)
				{
					negotiatedCapabilities = UCIPUtils.extractNegotiatedCapabilitiesFromConfig(config);
					request.setNegotiatedCapabilities(negotiatedCapabilities);
					logger.info("NegotiatedCapabilities set from config");
				}
				else
				{
					request.setNegotiatedCapabilities(negotiatedCapabilities);
					logger.info("NegotiatedCapabilities set from extraFields");
				}
			}

            if(config.isTreeDefinedFieldRefillEnable())
            {
             	logger.info("TreeDefinedFieldRefillEnable = " + config.isTreeDefinedFieldRefillEnable());
             	request.setEnableTreeDefinedField(config.isTreeDefinedFieldRefillEnable());
             	HashMap<Integer, TreeDefinedFieldValue> treeMap = config.getTreeDefinedFieldValueMap();

             	logger.debug("Total configuration is = " +  treeMap.size());
             	ArrayList<TreeDefinedField> treeList = new ArrayList<TreeDefinedField>();
             	Boolean valid = false;
             	for (Map.Entry<Integer, TreeDefinedFieldValue> entry : treeMap.entrySet()) 
             	{
             		
             		if (!entry.getValue().getValueType().isEmpty() && !entry.getValue().getFieldName().isEmpty() &&
             				!entry.getValue().getFieldType().isEmpty() && !entry.getValue().getFieldValue().isEmpty()) 
             		{
	             		TreeDefinedField tempTree = new TreeDefinedField();
	             		
	             		
	             		if (entry.getValue().getValueType().equals("static"))
	             		{
	             			logger.info("================ Tree Defined Field Value Type static =================");
	             			tempTree.setTreeDefinedFieldName(entry.getValue().getFieldName());
	                 		tempTree.setTreeDefinedFieldType(entry.getValue().getFieldType());
	                 		valid = validateStaticFields(valid, entry);
	                 		if(valid)
	                 		{
	                 			tempTree.setTreeDefinedFieldValue(entry.getValue().getFieldValue());
		                 		treeList.add(tempTree);
		                 		logger.info("Tree field added to the request ===> " + valid);
	                 		}
	                 		
	                 		logger.info("Tree Defined Field Value = " + entry.getValue().getFieldValue());
	                 		
	             		}else if (entry.getValue().getValueType().equals("dynamic") && extraFields.get(entry.getValue().getFieldValue()) != null)
	             		{
	             			logger.info("================ Tree Defined Field Value Type dynamic =================" );
	             			tempTree.setTreeDefinedFieldName(entry.getValue().getFieldName());
	                 		tempTree.setTreeDefinedFieldType(entry.getValue().getFieldType());
	                 		valid = validateDynamicFields(extraFields, valid, entry);
	                 		if(valid)
	                 		{
		                 		tempTree.setTreeDefinedFieldValue(extraFields.get(entry.getValue().getFieldValue()));
		                 		treeList.add(tempTree);
		                 		logger.info("Tree field added to the request ===> " + valid);
	                 		}
	                 		logger.info("Tree Defined Field Value = " + extraFields.get(entry.getValue().getFieldValue()));
	             		}
             		
             		}
             		

                } 
             	TreeDefinedField[] treeDefinedFieldArray = new TreeDefinedField[treeList.size()];
             	treeDefinedFieldArray = treeList.toArray(treeDefinedFieldArray);
             	request.setTreeDefinedField(treeDefinedFieldArray);
             	
            }

            response = (RefillResponse) makeRequest(request);

			// Set TreeDefinedFields
			if(response.getTreeDefinedFields() != null && !response.getTreeDefinedFields().isEmpty())
			{
				Vector<Map<String, Object>> treeDefinedFields = response.getTreeDefinedFields();
				for(Map<String, Object> map : treeDefinedFields)
				{
					TreeDefinedFieldInformation treeDefinedFieldInformation = new TreeDefinedFieldInformation();
					treeDefinedFieldInformation.setTreeDefinedFieldName((String)map.get("treeDefinedFieldName"));
					treeDefinedFieldInformation.setTreeDefinedFieldType((String)map.get("treeDefinedFieldType"));
					treeDefinedFieldInformation.setTreeDefinedFieldValue((String)map.get("treeDefinedFieldValue"));
					result.setTreeDefinedFields(treeDefinedFieldInformation.getTreeDefinedFieldName(), treeDefinedFieldInformation);
				}
			}

            result.setUcipResponseCode(response.getResponseCode());

			if(response.getTransactionAmount() != null) {
				result.setTransactionAmount(new Amount(response.getTransactionAmount().intValue(), response.getTransactionCurrency()));
			}

			if(StringUtils.isNotBlank(result.getSecondaryNativeResultCode()))
			{
				extraFields.put("SecondaryNativeResultCode",result.getSecondaryNativeResultCode());
			}

			if (response.getAccountBeforeRefill() != null)
			{
				Amount balance;
				if (dedicatedAccountId == null)
				{
					balance = CurrencyHandler.createAmount(response.getAccountBeforeRefill().getAccountValue1(), response.getCurrency1());
				}
				else
				{
					balance = getDedicatedAccountBalance(response.getAccountBeforeRefill().getDedicatedAccountInformation(), dedicatedAccountId, response.getCurrency1());
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
								result.setDedicatedAccountValueBefore(dedicatedAccount.getDedicatedAccountID(), dedicatedAccount.getDedicatedAccountValue1());
							}

						}

					}
				}
			}

			if (response.getAccountAfterRefill() != null)
			{
				Amount balanceAfter = CurrencyHandler.createAmount(response.getAccountAfterRefill().getAccountValue1(), response.getCurrency1());
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
								result.setDedicatedAccountValueAfter(dedicatedAccount.getDedicatedAccountID(), dedicatedAccount.getDedicatedAccountValue1());
							}

						}

					}
				}

			}
			// Set dedicated accounts from refill value total

			if (response.getRefillInformation() != null && response.getRefillInformation().getRefillValueTotal() != null && response.getRefillInformation().getRefillValueTotal().getDedicatedAccountRefillInformation() != null)
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

			if (response.getRefillInformation() != null && response.getRefillInformation().getRefillValuePromotion() != null && response.getRefillInformation().getRefillValuePromotion().getDedicatedAccountRefillInformation() != null)
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
        catch (Exception e)
		{
			if(e != null)
				logger.error("Refill request failed, exception root casue is "+e.getCause()+" and is type of "+e.getClass());

			if(e instanceof java.net.SocketTimeoutException)
			{
				logger.error("Socket timeout exception occurred for refill.");
				result.setErsResponseCode(ERSWSLinkResultCodes.LINK_TIMEOUT);
				setNativeResultCode(extraFields, String.valueOf(ERSWSLinkResultCodes.LINK_TIMEOUT), "Link Timeout");
				if(config.isEnableSuccessResponseOnTimeout() &&
						result.getErsResponseCode() == ERSWSLinkResultCodes.LINK_TIMEOUT)
				{
					logger.info("Forcefully System is sending SUCCESS to TXE when it is a LINK_TIMEOUT.");
					//This is required for ZAIN KSA, based on NativeResultCode, populate tdr result code as TIMEOUT
					extraFields.put("NativeResultCode", String.valueOf(ERSWSLinkResultCodes.LINK_TIMEOUT));
					result.setErsResponseCode(ERSWSLinkResultCodes.SUCCESS);
				}
			}
			else if(e instanceof java.net.ConnectException || e instanceof org.apache.commons.httpclient.ConnectTimeoutException)
			{
				logger.error("The connection to the CS is down: ", e);
				result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
				//set native result message as per the error code in extraFields
				setNativeResultCode(extraFields, String.valueOf(ERSWSLinkResultCodes.LINK_DOWN), "Link Down");
			}
			else
			{
				logger.error("UCIP Request failed for refill: ", e);
				result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
				//set native result message as per the error code in extraFields
				setNativeResultCode(extraFields, String.valueOf(ERSWSLinkResultCodes.INTERNAL_FAILED), "Internal Failed");
			}
		}
		finally
		{
			point.collect();
		}

		if(response != null)
			result.setInfo7(response.getUcipServerIp());
		return result;
	}

	/**
	 * Set native result message as per the error code in extraFields
	 * @param extraFields
	 * @param nativeResultCode
	 * @param nativeResultDescription
	 */
	private void setNativeResultCode(ERSHashtableParameter extraFields, String nativeResultCode, String nativeResultDescription)
	{
		if (config.isEnableNativeCodeMapping())
		{
			extraFields.put("NativeResultCode", nativeResultCode);
			extraFields.put("NativeResultDescription", nativeResultDescription);
			extraFields.put("LinkType", "UCIPLink");
			extraFields.put("LinkNodeId", "UCIPLink");

		}
	}




	/**
	 * Added for Data Bundle request but currently not support in v50
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
	@Override
	public UCIPResponse updateOffer(String subscriberNumber, Amount amount, String transactionId, String transactionType, String profileId, Integer dedicatedAccountId, ERSHashtableParameter extraFields, OfferProductsDTO offerProductDTO, String currency)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::updateOffer");

		com.seamless.common.uciplib.v50.requests.UpdateOfferRequest request;
		com.seamless.common.uciplib.v50.responses.UpdateOfferResponse response;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			request = new com.seamless.common.uciplib.v50.requests.UpdateOfferRequest(subscriberNumber);
			request.setOriginTransactionID(transactionId);
			request.setOfferID(offerProductDTO.getOfferID());
			request.setOfferType(offerProductDTO.getOfferType());
			if(currency != null)
			{
				request.setTransactionCurrency(currency);
			}
			if(offerProductDTO.getValidityList() != null && !offerProductDTO.getValidityList().isEmpty()) {
				final Date originTimeStamp = formatter.parse(formatter.format(offerProductDTO.getStartDate(offerProductDTO.getValidityList().get(0))));
				request.setOriginTimeStamp(originTimeStamp);
				final Date expiryDate = formatter.parse(formatter.format(offerProductDTO.getEndDate(offerProductDTO.getValidityList().get(0))));
				if(config.getUpdateOfferExpiryFormat() == ExpireFormat.DATE)
				{
					request.setExpiryDate(expiryDate);
					request.setExpiryDateTime(null);
					request.setExpiryDateRelative(null);
				}
				else if(config.getUpdateOfferExpiryFormat() ==ExpireFormat.DATETIME)
				{
					request.setExpiryDate(null);
					request.setExpiryDateTime(expiryDate);
					request.setExpiryDateRelative(null);
				}
				else if(config.getUpdateOfferExpiryFormat() == ExpireFormat.DATE_RELATIVE)
				{
					request.setExpiryDate(null);
					request.setExpiryDateTime(null);
					request.setExpiryDateRelative(expiryDate);
				}
				if(request.getExpiryDate() == null && request.getExpiryDateTime() == null && request.getExpiryDateRelative() == null)
				{
					logger.warn("No expiry date set for request");
				}
			}
			final AttributeUpdateInformationList[] attributeUpdateInformationParameters = UCIPUtils.extractAttributeUpdateInformationParameters(extraFields);
			request.setAttributeUpdateInformationList(attributeUpdateInformationParameters);
			if(config.isNegotiatedCapabilitiesUpdateOfferEnable())
			{
				Integer[] negotiatedCapabilities = UCIPUtils.extractNegotiatedCapabilities(extraFields);
				logger.debug("Setting negotiated capabilities");
				if(negotiatedCapabilities == null)
				{
					negotiatedCapabilities = UCIPUtils.extractNegotiatedCapabilitiesFromConfig(config);
					request.setNegotiatedCapabilities(negotiatedCapabilities);
					logger.info("NegotiatedCapabilities set from config");
				}
				else
				{
					request.setNegotiatedCapabilities(negotiatedCapabilities);
					logger.info("NegotiatedCapabilities set from extraFields");
				}
			}

			final DedicatedAccountUpdateInformation[] dedicatedAccountUpdateInformationList = UCIPUtils.extractDedicatedAccountUpdateInformationParameters(extraFields);

			if(dedicatedAccountUpdateInformationList != null)
			{
				request.setDedicatedAccountUpdateInformationList(dedicatedAccountUpdateInformationList);
			}

			final String originOperationId = extraFields.get("originOperationId");
			if(StringUtils.isNotBlank(originOperationId))
			{
				logger.info("originOperationId value = " + originOperationId);
				request.setOriginOperatorID(originOperationId);
			}

			resetExternalDataParameters();
			setDefaultVariables(request, extraFields);
			request.setSubscriberNumberNAI(subscriberNumberNAI);

			if (extraFields.get(VALIDATE_SUBSCRIBER_LOCATION) != null)
			{
				request.setValidateSubscriberLocation(StringUtils.parseBoolean(extraFields.get(VALIDATE_SUBSCRIBER_LOCATION)));
			}

			response = (com.seamless.common.uciplib.v50.responses.UpdateOfferResponse) makeRequest(request);

			result.setUcipResponseCode(response.getResponseCode());
			result.setUpdateOfferResponse(response);

		} catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		} catch(SocketTimeoutException e)
		{
			String err = "socket timeout: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_TIMEOUT);
		} catch (Exception e)
		{
			logger.error("UCIP Request failed for refill: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		} finally
		{
			point.collect();
		}

		return result;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override
	public UCIPResponse updateServiceClass(String subscribernumber, int serviceClass, String reference, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::updateServiceClass");

		String action = "SetOriginal";
		UpdateServiceClassRequest request;
		UpdateServiceClassResponse response;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			request = new UpdateServiceClassRequest(subscribernumber, serviceClass, action, null);
			resetExternalDataParameters();
			setDefaultVariables(request, extraFields);
			request.setOriginTransactionID(reference);
			response = (UpdateServiceClassResponse) makeRequest(request);

			result.setUcipResponseCode(response.getResponseCode());
		} catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		} catch (Exception e)
		{
			logger.error("UCIP Request failed to update service class: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		} finally
		{
			point.collect();
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UCIPResponse updateBalance(String subscriberNumber, Amount relativeBalance, String transactionId, String transactionType, Integer dedicatedAccountId, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::updateBalance");

		UpdateBalanceAndDateRequest request;
		UpdateBalanceAndDateResponse response;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			if (dedicatedAccountId == null)
			{
				if(config.isEnableSourceMultiPlier()) {
					BigDecimal bd = UCIPUtils.amountWithSourceMultiplier(config, relativeBalance, extraFields) == null ? relativeBalance.getValue() : UCIPUtils.amountWithSourceMultiplier(config, relativeBalance, extraFields);
					relativeBalance.setValue(bd);
				}
				request = new UpdateBalanceAndDateRequest(subscriberNumber, CurrencyHandler.getAmountValueAsLong(relativeBalance), relativeBalance.getCurrency());
			}
			else
			{
				request = new UpdateBalanceAndDateRequest(subscriberNumber, 0L, relativeBalance.getCurrency());
				loadDAInformation(request,dedicatedAccountId,relativeBalance,extraFields);
			}
			resetExternalDataParameters();
			setDefaultVariables(request, extraFields);
			request.setOriginTransactionID(transactionId);
			response = (UpdateBalanceAndDateResponse) makeRequest(request);

			result.setUcipResponseCode(response.getResponseCode());

			//set native result message as per the error code in extraFields
			if(config.isEnableNativeCodeMapping())
			{
				String errorDescription = config.getNativeCodeMapping().get(response.getResponseCode().toString()) == null ? "Unknown error!" :config.getNativeCodeMapping().get(response.getResponseCode().toString());
				logger.debug("#UpdateBalance Native error:- " + errorDescription);
				extraFields.put("NativeResultCode", response.getResponseCode().toString());
				extraFields.put("NativeResultDescription", errorDescription);
				extraFields.put("LinkType", "UCIPLink");
				extraFields.put("LinkNodeId", "UCIPLink");
			}

		} catch (ConnectException e)
		{
			String err = "The connection to the CS is down for UpdateBalance: " + e.getMessage();
			logger.error("#UpdateBalance Connection Exception ",e);
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
			if(config.isEnableNativeCodeMapping())
			{
				extraFields.put("NativeResultCode", String.valueOf(ERSWSLinkResultCodes.LINK_DOWN));
				extraFields.put("NativeResultDescription", "Link Down");
				extraFields.put("LinkType", "UCIPLink");
				extraFields.put("LinkNodeId", "UCIPLink");

			}
		}
		catch (SocketTimeoutException e)
		{
			logger.error("Socket timeout exception occurred for UpdateBalance.");
			logger.error("#UpdateBalance SocketTimeout Exception ",e);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_TIMEOUT);
			if(config.isEnableNativeCodeMapping())
			{
				extraFields.put("NativeResultCode", String.valueOf(ERSWSLinkResultCodes.LINK_TIMEOUT));
				extraFields.put("NativeResultDescription", "Link Timeout");
				extraFields.put("LinkType", "UCIPLink");
				extraFields.put("LinkNodeId", "UCIPLink");

			}

		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed for UpdateBalance: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
			logger.error("#UpdateBalance General Exception",e);
			if(config.isEnableNativeCodeMapping())
			{
				extraFields.put("NativeResultCode", String.valueOf(ERSWSLinkResultCodes.INTERNAL_FAILED));
				extraFields.put("NativeResultDescription", "Internal Failed");
				extraFields.put("LinkType", "UCIPLink");
				extraFields.put("LinkNodeId", "UCIPLink");

			}
		} finally
		{
			point.collect();
		}

		if(config.isEnableSuccessResponseOnTimeout() &&
				result.getErsResponseCode() == ERSWSLinkResultCodes.LINK_TIMEOUT)
		{
			logger.info("Forcefully System is sending SUCCESS to TXE when it is a LINK_TIMEOUT.");
			result.setErsResponseCode(ERSWSLinkResultCodes.SUCCESS);
		}
		return result;
	}

	@Override
	public UCIPResponse updateAccumulators(String subscriberNumber, String transactionId, AccumulatorUpdateInformation[] info, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::updateAccumulators");

		UpdateAccumulators request;
		UpdateAccumulatorsResponse response;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{

			request = new UpdateAccumulators(subscriberNumber, info);
			resetExternalDataParameters();
			setDefaultVariables(request, extraFields);
			if(config.isNegotiatedCapabilitiesUpdateAccumulatorsEnable())
			{
				Integer[] negotiatedCapabilities = UCIPUtils.extractNegotiatedCapabilities(extraFields);
				if(negotiatedCapabilities == null)
				{
					negotiatedCapabilities = UCIPUtils.extractNegotiatedCapabilitiesFromConfig(config);
					request.setNegotiatedCapabilities(negotiatedCapabilities);
					logger.info("NegotiatedCapabilities set from config");
				}
				else
				{
					request.setNegotiatedCapabilities(negotiatedCapabilities);
					logger.info("NegotiatedCapabilities set from extraFields");
				}
			}
			request.setOriginTransactionID(transactionId);
			response = (UpdateAccumulatorsResponse) makeRequest(request);

			result.setUcipResponseCode(response.getResponseCode());

			//set native result message as per the error code in extraFields
			if(config.isEnableNativeCodeMapping())
			{
				String errorDescription = config.getNativeCodeMapping().get(response.getResponseCode().toString()) == null ? "Unknown error!" :config.getNativeCodeMapping().get(response.getResponseCode().toString());
				extraFields.put("NativeResultCode", response.getResponseCode().toString());
				extraFields.put("NativeResultDescription", errorDescription);
				extraFields.put("LinkType", "UCIPLink");
				extraFields.put("LinkNodeId", "UCIPLink");
			}

		} catch (ConnectException e)
		{
			String err = "The connection to the CS is down for UpdateBalance: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
			if(config.isEnableNativeCodeMapping())
			{
				extraFields.put("NativeResultCode", String.valueOf(ERSWSLinkResultCodes.LINK_DOWN));
				extraFields.put("NativeResultDescription", "Link Down");
				extraFields.put("LinkType", "UCIPLink");
				extraFields.put("LinkNodeId", "UCIPLink");

			}
		}
		catch (SocketTimeoutException e)
		{
			logger.error("Socket timeout exception occurred for UpdateBalance.");
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_TIMEOUT);
			if(config.isEnableNativeCodeMapping())
			{
				extraFields.put("NativeResultCode", String.valueOf(ERSWSLinkResultCodes.LINK_TIMEOUT));
				extraFields.put("NativeResultDescription", "Link Timeout");
				extraFields.put("LinkType", "UCIPLink");
				extraFields.put("LinkNodeId", "UCIPLink");

			}

		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed for UpdateBalance: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
			if(config.isEnableNativeCodeMapping())
			{
				extraFields.put("NativeResultCode", String.valueOf(ERSWSLinkResultCodes.INTERNAL_FAILED));
				extraFields.put("NativeResultDescription", "Internal Failed");
				extraFields.put("LinkType", "UCIPLink");
				extraFields.put("LinkNodeId", "UCIPLink");

			}
		} finally
		{
			point.collect();
		}

		if(config.isEnableSuccessResponseOnTimeout() &&
				result.getErsResponseCode() == ERSWSLinkResultCodes.LINK_TIMEOUT)
		{
			logger.info("Forcefully System is sending SUCCESS to TXE when it is a LINK_TIMEOUT.");
			result.setErsResponseCode(ERSWSLinkResultCodes.SUCCESS);
		}
		return result;
	}

	@Override
	public UCIPResponse getCapabilities() {

		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::getCapabilities");

		GetCapabilitiesRequest request;
		GetCapabilitiesResponse response;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		request = new GetCapabilitiesRequest();
		try {
			response = (GetCapabilitiesResponse) makeRequest(request);
			result.setUcipResponseCode(response.getResponseCode());
		} catch (Exception e) {
			result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		} finally {
			point.collect();
		}
		return result;
	}

	public UCIPResponse deleteOffer(String transactionId, String subscriberNumber, OfferProductsDTO offerProductDTO, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::deleteOffer");
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		try
		{
			DeleteOfferRequest request = new DeleteOfferRequest(subscriberNumber);
			request.setOriginHostName(originHostName);
			request.setOriginNodeType(originNodeType);
			request.setSubscriberNumberNAI(subscriberNumberNAI);
			request.setOriginTransactionID(transactionId);
			request.setOfferID(offerProductDTO.getOfferID());
			request.setOfferType(offerProductDTO.getOfferType());
			if(config.isNegotiatedCapabilitiesDeleteOfferEnable())
			{
				Integer[] negotiatedCapabilities = UCIPUtils.extractNegotiatedCapabilities(extraFields);
				if(negotiatedCapabilities == null)
				{
					negotiatedCapabilities = UCIPUtils.extractNegotiatedCapabilitiesFromConfig(config);
					logger.info("NegotiatedCapabilities fetched from config");
				}
				request.setNegotiatedCapabilities(negotiatedCapabilities);
			}

			DeleteOfferResponse response = (DeleteOfferResponse) makeRequest(request);
			result.setUcipResponseCode(response.getResponseCode());
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch(SocketTimeoutException e)
		{
			String err = "socket timeout: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_TIMEOUT);
		}
		catch (Exception e)
		{
			logger.error("UCIP Request failed for refill: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		finally
		{
			if(point != null)
			{
				point.collect();
			}
		}
		return result;
	}

	private Amount getDedicatedAccountBalance(DedicatedAccountInformation[] daInfoList, int dedicatedAccountId, String currency) throws ERSInvalidCurrencyException
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
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor50::redeemVoucher");

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
				// FIXME account value should be devided by currency decimal
				// digits
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

		} catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		} catch (Exception e)
		{
			logger.error("UCIP Request failed to redeemVoucher: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		} finally
		{
			point.collect();
		}

		return result;
	}
	
	public static boolean isLong(String str) {
	    if (str == null) {
	        return false;
	    }
	    try {
	        int d = Integer.parseInt(str);
	    } catch (NumberFormatException e) {
	        return false;
	    }
	    return true;
	}
	
	private Boolean validateDynamicFields(ERSHashtableParameter extraFields, Boolean valid,
			Map.Entry<Integer, TreeDefinedFieldValue> entry) {
		
		if (entry.getValue().getFieldType().equalsIgnoreCase("Long"))
		{
			valid = isLong(extraFields.get(entry.getValue().getFieldValue()));
			logger.debug("Long ===> " + valid);
			
		}else if(entry.getValue().getFieldType().equalsIgnoreCase("String"))
		{
			valid = true; 
			logger.debug("String ===> " + valid);
			
		}else if (entry.getValue().getFieldType().equalsIgnoreCase("Boolean"))
		{ 
			if(entry.getValue().getFieldValue().equals("true") || entry.getValue().getFieldValue().equals("false"))
				valid = true;
			else
				valid = false;
			logger.debug("boolean ===> " + valid);
		}else {
			valid = false;
			logger.debug("Other ===> " + valid);
		}
		return valid;
	}

	
    private Boolean validateStaticFields(Boolean valid, Map.Entry<Integer, TreeDefinedFieldValue> entry) {
		
		if (entry.getValue().getFieldType().equalsIgnoreCase("Long"))
		{
			valid = isLong(entry.getValue().getFieldValue());
			logger.debug("Long ===> " + valid);
			
		}else if(entry.getValue().getFieldType().equalsIgnoreCase("String"))
		{
			valid = true; 
			logger.debug("String ===> " + valid);
			
		}else if (entry.getValue().getFieldType().equalsIgnoreCase("Boolean"))
		{ 
			if(entry.getValue().getFieldValue().equals("true") || entry.getValue().getFieldValue().equals("false"))
				valid = true;
			else
				valid = false;
			logger.debug("Boolean ===> " + valid);
		}else {
			valid = true;
			logger.debug("Other ===> " + valid);
		}
		return valid;
	}

}
