package com.seamless.ers.links.uciplink.utils;

import com.seamless.common.StringUtils;
import com.seamless.common.uciplib.common.AccumulatorUpdateInformation;
import com.seamless.common.uciplib.common.AttributeUpdateInformationList;
import com.seamless.common.uciplib.common.DedicatedAccountUpdateInformation;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountTransactionResponse;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UCIPUtils
{
	private static final Logger logger = LoggerFactory.getLogger(UCIPUtils.class);
	private static Pattern accumulatorCurrentDatePatterns = Pattern.compile("NOW|CURRENT_DATE|CURRENT_TIME|IMMEDIATELY");

	public static AttributeUpdateInformationList[] extractAttributeUpdateInformationParameters(ERSHashtableParameter extraFields)
	{
		if (extraFields == null)
		{
			return null;
		}
		final List<AttributeUpdateInformationList> attributeUpdateInformationLists = new ArrayList<>();
		final String attributeUpdateInformationListParameters = extraFields.get("attributeUpdateInformationListParameters");
		if (attributeUpdateInformationListParameters != null)
		{
			final String[] attributes = attributeUpdateInformationListParameters.split(",");
			if (attributes != null)
			{
				for (String attribute : attributes)
				{
					attribute = attribute.trim();
					final String actionParam = extraFields.get(attribute + ".action");
					final AttributeAction action = AttributeAction.lookup(actionParam);
					if (action == null)
					{
						logger.warn("[attributeName = " + attribute + "] Skipping due to Invalid AttributeUpdateAction: [" + action + "]. Valid values are: " + AttributeAction.values());
						continue;
					}

					String value = extraFields.get(attribute + ".value");
					if (action == AttributeAction.SET || action == AttributeAction.ADD || action == AttributeAction.DELETE)
					{
						if (value != null)
						{
							value = value.trim();
						}
					}

					AttributeUpdateInformationList attributeUpdateInformationList = new AttributeUpdateInformationList();
					attributeUpdateInformationList.setAttributeName(attribute);
					attributeUpdateInformationList.setAttributeUpdateAction(action.getValue());
					if (action == AttributeAction.SET || action == AttributeAction.ADD || action == AttributeAction.DELETE)
					{
						attributeUpdateInformationList.setAttributeValueString(value);
					}

					attributeUpdateInformationLists.add(attributeUpdateInformationList);
				}
			}
		}

		if (attributeUpdateInformationLists.isEmpty())
		{
			return null;
		}

		return attributeUpdateInformationLists.toArray(new AttributeUpdateInformationList[attributeUpdateInformationLists.size()]);
	}

	public static AccumulatorUpdateInformation[] extractAccumulatorUpdateInformationParameters(ERSHashtableParameter extraFields)
	{
		if (extraFields == null)
		{
			return null;
		}
		final List<AccumulatorUpdateInformation> accumulatorUpdateInformationList = new ArrayList<>();
		final String accumulatorIndexes = extraFields.get("accumulatorIndexes");
		if (accumulatorIndexes != null)
		{
			final String[] indexes = accumulatorIndexes.split(",");
			if (indexes != null)
			{
				for (String index : indexes)
				{
					try
					{
						int id = 0;
						Date startDate = null;
						AccumulatorValueType valueType = null;
						final String idValue = extraFields.get("accumulator." + index + ".id");
						final String valueAbsolute = extraFields.get("accumulator." + index + ".valueAbsolute");
						final String valueRelative = extraFields.get("accumulator." + index + ".valueRelative");
						final String startDateValue = extraFields.get("accumulator." + index + ".startDate");
						if (StringUtils.isBlank(idValue))
						{
							logger.error("Empty id definition for accumulator index: " + index);
							continue;
						}
						try
						{
							id = Integer.parseInt(idValue);
						}
						catch (NumberFormatException e)
						{
							logger.error("Invalid accumulator id for index: " + index);
							continue;
						}

						if (StringUtils.isNotBlank(valueAbsolute))
						{
							valueType = AccumulatorValueType.VALUE_ABSOLUTE;
						}
						else if (StringUtils.isNotBlank(valueRelative))
						{
							valueType = AccumulatorValueType.VALUE_RELATIVE;
						}

						if (StringUtils.isNotBlank(startDateValue))
						{
							try
							{
								final Matcher matcher = accumulatorCurrentDatePatterns.matcher(startDateValue);
								if (matcher.matches())
								{
									startDate = new Date();
								}
								else
								{
									SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
									startDate = simpleDateFormat.parse(startDateValue);
								}
							}
							catch (ParseException e)
							{
								logger.error("Error while parsing accumulator startDate value: " + startDateValue);
								logger.error(e.getMessage(), e);
							}
						}

						AccumulatorUpdateInformation accumulatorUpdateInformation = new AccumulatorUpdateInformation(id);
						if (valueType == AccumulatorValueType.VALUE_ABSOLUTE)
						{
							try
							{
								accumulatorUpdateInformation.setAccumulatorValueAbsolute(Integer.parseInt(valueAbsolute));
							}
							catch (NumberFormatException e)
							{
								logger.error("Invalid accumulator valueAbsolute for index: " + index);
								continue;
							}
						}

						if (valueType == AccumulatorValueType.VALUE_RELATIVE)
						{
							try
							{
								accumulatorUpdateInformation.setAccumulatorValueRelative(Integer.parseInt(valueRelative));
							}
							catch (NumberFormatException e)
							{
								logger.error("Invalid accumulator valueRelative for index: " + index);
								continue;
							}
						}

						if (startDate != null)
						{
							accumulatorUpdateInformation.setAccumulatorStartDate(startDate);
						}

						accumulatorUpdateInformationList.add(accumulatorUpdateInformation);

					}
					catch (Exception e)
					{
						logger.error("Error whilst extracting updateAccumulator parameters");
						logger.error(e.getMessage(), e);
					}
				}
			}

		}

		if (accumulatorUpdateInformationList.isEmpty())
		{
			return null;
		}

		return accumulatorUpdateInformationList.toArray(new AccumulatorUpdateInformation[0]);
	}

	public static DedicatedAccountUpdateInformation[] extractDedicatedAccountUpdateInformationParameters(ERSHashtableParameter extraFields)
	{
		if(extraFields == null)
		{
			return null;
		}
		final List<DedicatedAccountUpdateInformation> dedicatedAccountUpdateInformationList = new ArrayList<>();
		final Set<String> extraFieldKeys = extraFields.getParameters().keySet();

		Map<String, String> dedicatedAccountUpdateParameters = new HashMap<>();
		if(!extraFieldKeys.isEmpty())
		{
			for(String key : extraFieldKeys)
			{
				if(key.startsWith("dedicatedAccountUpdate.")){
					dedicatedAccountUpdateParameters.put(key, extraFields.get(key));
				}
			}
		}

		if(!dedicatedAccountUpdateParameters.isEmpty())
		{
			for(int i=1 ; i < 999; i++){

				DedicatedAccountUpdateInformation dedicatedAccountUpdateInformation = new DedicatedAccountUpdateInformation();
				final String dedicatedAccountId = dedicatedAccountUpdateParameters.get("dedicatedAccountUpdate." + i + ".dedicatedAccountID");
				if(dedicatedAccountId == null){
					break;
				}
				else {
					try{
						dedicatedAccountUpdateInformation.setDedicatedAccountID(Integer.parseInt(dedicatedAccountId));
					}catch (Exception e){
						logger.error(e.getMessage(), e);
					}
				}
				final String dedicatedAccountUnitType = dedicatedAccountUpdateParameters.get("dedicatedAccountUpdate." + i + ".dedicatedAccountUnitType");
				if(dedicatedAccountUnitType != null){
					try {
						dedicatedAccountUpdateInformation.setDedicatedAccountUnitType(Integer.parseInt(dedicatedAccountUnitType));
					}catch (NumberFormatException e){
						logger.error(e.getMessage(), e);
					}
				}

				final String adjustmentAmountRelative = dedicatedAccountUpdateParameters.get("dedicatedAccountUpdate." + i + ".adjustmentAmountRelative");
				if(adjustmentAmountRelative != null){
					try {
						dedicatedAccountUpdateInformation.setAdjustmentAmountRelative(Long.parseLong(adjustmentAmountRelative));
					}catch (NumberFormatException e){
						logger.error(e.getMessage(), e);
					}
				}

				dedicatedAccountUpdateInformationList.add(dedicatedAccountUpdateInformation);

			}
		}

		if(dedicatedAccountUpdateInformationList.isEmpty())
		{
			return null;
		}

		return dedicatedAccountUpdateInformationList.toArray(new DedicatedAccountUpdateInformation[0]);
	}

	public static Integer[] extractNegotiatedCapabilitiesFromConfig(UCIPLinkConfig config)
	{
		Integer[] negotiatedCapabilitiesValue = null;
		if (config.getUcipProperties().getProperty("negotiatedCapabilities") != null)
		{
			String[] capabilityIntegersAsText = config.getUcipProperties().getProperty("negotiatedCapabilities").split(",");
			negotiatedCapabilitiesValue = new Integer[capabilityIntegersAsText.length];
			int i = 0;
			for (String str : capabilityIntegersAsText)
			{
				negotiatedCapabilitiesValue[i] = Integer.parseInt(str);
				i++;
			}

			logger.info("NegotiatedCapabilities added in request with value =  " + Arrays.toString(capabilityIntegersAsText));

		}
		return negotiatedCapabilitiesValue;
	}

	public static Integer[] extractNegotiatedCapabilities(ERSHashtableParameter extraFields)
	{
		List<Integer> negotiatedCapabilities = new ArrayList<>();
		if (extraFields != null)
		{
			final String capabilities = extraFields.get("negotiatedCapabilities");
			if (capabilities != null && capabilities.length() > 0)
			{
				final String[] capabilitiesAsStrings = capabilities.split(",");
				if (capabilitiesAsStrings.length > 0)
				{
					for (String capability : capabilitiesAsStrings)
					{
						try
						{
							negotiatedCapabilities.add(Integer.parseInt(capability));
						}
						catch (NumberFormatException e)
						{
							logger.warn("Invalid negotiatedCapabilities value: " + capabilities);
							continue;
						}
					}
				}
			}

		}

		if (negotiatedCapabilities.isEmpty())
		{
			return null;
		}

		return negotiatedCapabilities.toArray(new Integer[0]);
	}

	public static void updateOfferResponseSuccess(AccountTransactionResponse response, UCIPResponse updateOfferResponse)
	{
		response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
		if (updateOfferResponse.getUpdateOfferResponse() != null && updateOfferResponse.getUpdateOfferResponse().getExpiryDate() != null)
		{
			response.getFields().put("UpdateOffer ExpiryDate ", updateOfferResponse.getUpdateOfferResponse().getExpiryDate().toString());
		}
		if (updateOfferResponse.getUpdateOfferResponse() != null && updateOfferResponse.getUpdateOfferResponse().getResponseCode() != null)
		{
			response.getFields().put("UpdateOffer Response Code ", updateOfferResponse.getUpdateOfferResponse().getResponseCode().toString());
		}
		if (updateOfferResponse.getUpdateOfferResponse() != null && updateOfferResponse.getUpdateOfferResponse().getOfferID() != null)
		{
			response.getFields().put("UpdateOffer Offer ID ", updateOfferResponse.getUpdateOfferResponse().getOfferID().toString());
		}
		if (updateOfferResponse.getUpdateOfferResponse() != null && updateOfferResponse.getUpdateOfferResponse().getOfferType() != null)
		{
			response.getFields().put("UpdateOffer Offer Type ", updateOfferResponse.getUpdateOfferResponse().getOfferType().toString());
		}
		if (updateOfferResponse.getUpdateOfferResponse() != null && updateOfferResponse.getUpdateOfferResponse().getOriginTransactionID() != null)
		{
			response.getFields().put("UpdateOffer Transaction ID ", updateOfferResponse.getUpdateOfferResponse().getOriginTransactionID().toString());
		}
		if (updateOfferResponse.getUpdateOfferResponse() != null && updateOfferResponse.getUpdateOfferResponse().getStartDate() != null)
		{
			response.getFields().put("UpdateOffer Start Date ", updateOfferResponse.getUpdateOfferResponse().getStartDate().toString());
		}
	}

	public static OfferProductsDTO setupOfferInfo(ERSHashtableParameter extraFields)
	{
		OfferProductsDTO offerProductsDTO = new OfferProductsDTO();
		Integer offerId = null;
		Integer offerType = null;
		String offerValidityType = null;
		List<String> offerValidityList = new ArrayList<>();
		if (extraFields != null)
		{
			try
			{
				offerId = Integer.parseInt(extraFields.get("offerId"));
			}
			catch (NumberFormatException e)
			{
				logger.error("Invalid offerId: " + offerId + ". OfferId must be a valid integer");
				throw e;
			}

			try
			{
				offerType = Integer.parseInt(extraFields.get("offerType"));
			}
			catch (NumberFormatException e)
			{
				logger.error("Invalid offerType: " + offerType + ". offerType must be a valid integer");
				throw e;
			}

			offerValidityType = extraFields.get("offerValidityType");
			if (extraFields.get("offerValidityList") != null)
			{
				final String[] ovls = extraFields.get("offerValidityList").split(",");
				if (ovls.length > 0)
				{
					for (String ovl : ovls)
					{
						offerValidityList.add(ovl.trim());
					}
				}

			}

			logger.info("offerValidityList: " + offerValidityList);

			offerProductsDTO.setOfferID(offerId);
			offerProductsDTO.setOfferType(offerType);
			offerProductsDTO.setValidityType(offerValidityType);
			offerProductsDTO.setValidityList(offerValidityList);
		}

		return offerProductsDTO;
	}

	public static BigDecimal amountWithSourceMultiplier(UCIPLinkConfig config, Amount amount, ERSHashtableParameter extraFields)
	{
		if (extraFields.getParameters() != null && extraFields.getParameters().containsKey(config.getSourceMultiPlierKeyName())
				&& extraFields.getParameters().get(config.getSourceMultiPlierKeyName()) != null)
		{
			BigDecimal sourceMultiPlier = new BigDecimal(extraFields.getParameters().get(config.getSourceMultiPlierKeyName()));
			logger.info("Current source multiplier value is = " + sourceMultiPlier);
			return amount.getValue().multiply(sourceMultiPlier);
		}
		return null;
	}
}