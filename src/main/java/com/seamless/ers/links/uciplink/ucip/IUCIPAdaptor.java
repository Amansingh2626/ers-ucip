package com.seamless.ers.links.uciplink.ucip;

import com.seamless.common.ExtendedProperties;
import com.seamless.common.StringUtils;
import com.seamless.common.locale.ERSInvalidCurrencyException;
import com.seamless.common.uciplib.client.UCIPClient;
import com.seamless.common.uciplib.common.AccumulatorUpdateInformation;
import com.seamless.common.uciplib.common.requests.UCIPSubscriberRequest;
import com.seamless.common.uciplib.v31.requests.UpdateBalanceAndDateRequest;
import com.seamless.common.uciplib.v50.requests.RefillRequest;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.utils.CurrencyHandler;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;
import com.seamless.ers.links.uciplink.utils.UCIPUtils;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.xmlrpc.XmlRpcException;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class IUCIPAdaptor
{
	Logger logger = LoggerFactory.getLogger(IUCIPAdaptor.class);
	
	public static final String ORIGIN_OPERATOR_ID = "originOperatorID";
	public static final String SUBSCRIBER_NUMBER_NAI = "subscriberNumberNAI";
	public static final String RECEIVER_MSISDN = "RECEIVER_MSISDN";
	public static final String SENDER_MSISDN = "SENDER_MSISDN";
	public static final String UPDATE_BALANCE_KEY = "updateBalance";
	public static final String UPDATE_BALANCE_VALUE = "updateBalance";
	public static final String REFILL_KEY = "refill";
	public static final String REFILL_VALUE = "refill";
	public static final String VALIDATE_SUBSCRIBER_LOCATION = "validateSubscriberLocation";

	public static final String FIELD_EXTERNALDATA1 = "externalData1";
	public static final String FIELD_EXTERNALDATA2 = "externalData2";
	public static final String FIELD_EXTERNALDATA3 = "externalData3";
	public static final String FIELD_EXTERNALDATA4 = "externalData4";
	public static final String FIELD_ORIGINNODETYPE = "originNodeType";
	public static final String FIELD_ORIGINHOSTNAME = "originHostName";
	public static final String FIELD_CHANNELPREFIX = "channelPrefix";
	public static final String TRANSACTION_PROFILE="TRANSACTION_PROFILE";
	public static final String PROFILE_ID="ProfileId";
	public static final String PRODUCT_SKU="productSKU";

	public static final String TRANSACTION_CODE = "transactionCode";
	public static final String REVERSE_TOPUP="REVERSE_TOPUP";
	/**
	 * Common members for different versions of UCIP clients.
	 */

	SimpleDateFormat formatter = new SimpleDateFormat("E dd MMM yyyy HH:mm:ss z");

	private UCIPClient client;

	String originHostName;

	String originNodeType;

	ThreadLocal<String> dynamicOriginNodeType = new ThreadLocal<>();
	ThreadLocal<String> dynamicOriginHostName = new ThreadLocal<>();
	ThreadLocal<String> dynamicOriginOperatorId= new ThreadLocal<>();

	String channelPrefix;
	
	//ExternalData params changed to threadlocal as they are not thread-safe
	//and can be different for each request. eg. when they hold ersReferences
	//This keeps a unique externalData param for each thread
	//Also each processor needs to reset or populate these parameters before usage
	//else risk of sending a parameter for a previous request
	ThreadLocal<String> externalData1 = new ThreadLocal<String>();

	ThreadLocal<String> externalData2 = new ThreadLocal<String>();

	ThreadLocal<String> externalData3 = new ThreadLocal<String>();

	ThreadLocal<String> externalData4 = new ThreadLocal<String>();

	Integer subscriberNumberNAI;

	String originOperatorID;

	boolean useFakedPing;

	boolean	enableFetchSubscriberSegment;

	//String transformCurrency = null;

	ExtendedProperties m_properties;

	UCIPLinkConfig config;


	public IUCIPAdaptor(UCIPLinkConfig config, ExtendedProperties properties)
	{
		this.config = config;
		this.m_properties = properties;
		this.useFakedPing = StringUtils.parseBoolean(properties.getProperty("useFakedPing"));
		this.originHostName = properties.getProperty(FIELD_ORIGINHOSTNAME);
		this.originNodeType = properties.getProperty(FIELD_ORIGINNODETYPE);
		this.channelPrefix = properties.getProperty(FIELD_CHANNELPREFIX);
		this.externalData1.set(properties.getProperty(FIELD_EXTERNALDATA1));
		this.externalData2.set(properties.getProperty(FIELD_EXTERNALDATA2));
		this.subscriberNumberNAI = StringUtils.parseInt(properties.getProperty(SUBSCRIBER_NUMBER_NAI), 0);
		this.originOperatorID = properties.getProperty(ORIGIN_OPERATOR_ID);
		this.enableFetchSubscriberSegment = StringUtils.parseBoolean(properties.getProperty("enableFetchSubscriberSegment"));

		try
		{
			client = new UCIPClient(properties);

			logger.info("Server:" + client.getServerURL());
			logger.info("UCIP version:" + client.getProtocolVersion().toString());

			client.connect();
			if (useFakedPing)
			{
				client.ping();
			}
		}
		catch (XmlRpcException e)
		{
			logger.error("UCIPLink cannot reach UCIP server! \n" + e.getMessage());
		}
		catch (Exception e)
		{
			logger.error("UCIPLink unhandled Exception \n" + e.getMessage());
		}
	}

	public boolean isConnected()
	{
		try
		{
			client.ping();
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	void setDefaultVariables(UCIPSubscriberRequest req, ERSHashtableParameter extraFields)
	{
		logger.info("=====================================================================");
		logger.info("=                        Default Variables                          =");
		logger.info("=====================================================================");
		logger.info("= ");
		req.setOriginHostName(originHostName);
		req.setSubscriberNumberNAI(subscriberNumberNAI);
		req.setOriginNodeType(originNodeType);
		if (extraFields.getParameters().containsKey(REFILL_KEY) && config.isOriginOperatorIDRefillEnable())
		{
			req.setOriginOperatorID(originOperatorID);
			logger.info("=  originOperatorID        =  " + originOperatorID);
		}
		else if (extraFields.getParameters().containsKey(UPDATE_BALANCE_KEY) && config.isOriginOperatorIDUpdateBalanceEnable())
		{
			req.setOriginOperatorID(originOperatorID);
			logger.info("=  originOperatorID        =  " + originOperatorID);
		}
		
		logger.info("=  originHostName          =  " + originHostName);
		logger.info("=  subscriberNumberNAI     =  " + subscriberNumberNAI);
		logger.info("=  originNodeType          =  " + originNodeType);
		logger.info("=  voucherSerialNumber     =  " + extraFields.get("voucherSerial"));
		logger.info("= ");
		logger.info("---------------------------------------------------------------------");
		logger.info("--------------------------- ExternalData1 ---------------------------");
		logger.info("---------------------------------------------------------------------");
		logger.info("= ");
		String profileId = extraFields.get(TRANSACTION_PROFILE) == null ? (extraFields.get(PROFILE_ID) == null ? "" : extraFields.get(PROFILE_ID)) : extraFields.get(TRANSACTION_PROFILE);

		logger.info("=  externalData1 = " + (config.isExternalData1enable() ? "enabled" : "disabled"));
		if (config.isExternalData1enable())
		{
			logger.info("=  Looking for externalData1 in extraFields ...  " );

			if(config.getExternalData1ExtraParamsReset().contains(profileId))
			{
				logger.info("Resetting value for externalData1 retrieved from extraParams as profile: " + profileId + " has requested a reset");
				extraFields.put(FIELD_EXTERNALDATA1, null);
			}

			if (extraFields.getParameters().get(FIELD_EXTERNALDATA1) != null)
			{
				logger.info("=  Found externalData1 in extraFields ...");
				logger.info("=  externalData1 = " + extraFields.get(FIELD_EXTERNALDATA1));
				extraFields.put(FIELD_EXTERNALDATA1, extraFields.get(FIELD_EXTERNALDATA1));
				externalData1.set(extraFields.get(FIELD_EXTERNALDATA1));
			}
			else
			{
				logger.info("=  externalData1 not found in extraFields !!!");
				logger.info("= ");
				logger.info("-----------------------------------------------");
				logger.info("=  Reading properties for externalData1 ...");
				logger.info("---------------------");
				logger.info("=");
				logger.info("=  profileBasedExternalData1 = " + (config.isProfileBasedExternalData1Enable() ? "enabled" : "disabled"));

				if (config.isProfileBasedExternalData1Enable() && extraFields != null && !profileId.isEmpty() && !(config.getBundleNameList().contains(profileId)))
				{
					logger.info("=");
					logger.info("=  Profile = " + profileId);
					setExternalData1ForTransactionProfile(extraFields, profileId);
				}
				else
				{
					logger.info("=");
					logger.info("=====");
					logger.info("=");
					logger.info("=  Falling back to productSkuBasedExternalData1 ... ");
					logger.info("=");
					setExternalData1ForProductSKU(extraFields);
				}

			}

			logger.info("=  Looking for externalData1(reference) in extraFields using flag set_externalData1_as_ers_reference ...");

			if(config.isSetExternalData1AsERSReference()){
				req.setExternalData1(extraFields.get("reference"));
				extraFields.put(FIELD_EXTERNALDATA1, req.getExternalData1());
				externalData1.set(req.getExternalData1());
				logger.debug("= Found externalData1(reference) = "+ externalData1.get());
			}
			else
				logger.info("=  externalData1 not found in extraFields(reference) in extraFields using flag set_externalData1_as_ers_reference !!!");
		}

		if (config.isExternalData2enable())
		{
			logger.info("---------------------------------------------------------------------");
			logger.info("--------------------------- ExternalData2 ---------------------------");
			logger.info("---------------------------------------------------------------------");
			logger.info("=");
			
			logger.info("=  externalData2 = " + (config.isExternalData2enable() ? "enabled" : "disabled"));
			logger.info("=  Looking for externalData2 in extraFields ...");

			if(config.getExternalData2ExtraParamsReset().contains(profileId))
			{
				logger.info("Resetting value for externalData2 retrieved from extraParams as profile: " + profileId + " has requested a reset");
				extraFields.put(FIELD_EXTERNALDATA2, null);
			}

			if (extraFields.getParameters().get(FIELD_EXTERNALDATA2) != null)
			{
				logger.info("=  Found externalData2 in extraFields ...");
				logger.info("=  externalData2 = " + extraFields.get(FIELD_EXTERNALDATA2));
				extraFields.put(FIELD_EXTERNALDATA2, extraFields.get(FIELD_EXTERNALDATA2));
				externalData2.set(extraFields.get(FIELD_EXTERNALDATA2));
			}
			else
			{
				logger.info("=  externalData2 not found in extraFields !!!");
				logger.info("= ");
				logger.info("-----------------------------------------------");
				logger.info("=  Reading properties for externalData2 ...");
				logger.info("---------------------");
				logger.info("=");
				logger.info("=  profileBasedExternalData2 = " + (config.isProfileBasedExternalData2Enable() ? "enabled" : "disabled"));
				if (config.isProfileBasedExternalData2Enable() && extraFields != null && !profileId.isEmpty() && !(config.getBundleNameList().contains(profileId)))
				{
					logger.info("=");
					logger.info("=  Profile = " + profileId);
					setExternalData2ForTransactionProfile(extraFields, profileId);
				}
				else
				{
					logger.info("=");
					logger.info("=====");
					logger.info("=");
					logger.info("=  Falling back to productSkuBasedExternalData2 ... ");
					logger.info("=");
					setExternalData2ForProductSKU(extraFields);
				}
			}
			
			logger.info("=  Looking for externalData2(reference) in extraFields using flag set_externalData2_as_ers_reference ...");
			
			if(config.isSetExternalData2AsERSReference()){
				req.setExternalData2(extraFields.get("reference"));
				extraFields.put(FIELD_EXTERNALDATA2, req.getExternalData2());
				externalData2.set(req.getExternalData2());
				logger.debug("= Found externalData2(reference) = "+ externalData2.get());
			}
			else
				logger.info("=  externalData2 not found in extraFields(reference) in extraFields using flag set_externalData2_as_ers_reference !!!");
			
			if(config.isTransactionTypeRefillEnable()&&extraFields.get("TRANSACTION_PROFILE")!=null)
			{
				String transactionType = extraFields.get("TRANSACTION_PROFILE");
				String extraFieldKeyName = config.getExternalData2Properties()
						.getProperty(transactionType + "." + "extraFieldName");
				if (extraFieldKeyName != null) {
					logger.info("Fetch data from extra fields for - " + extraFieldKeyName);
					String value = extraFields.get(extraFieldKeyName);
					logger.info(extraFieldKeyName + " - " + value);
					req.setExternalData2(value);
					externalData2.set(req.getExternalData2());
					logger.info("externalData2 set as " + extraFieldKeyName + " = " + value + " for transaction type = "
							+ transactionType);
				}
			}
		}

		if (config.isExternalData3enable())
		{
			logger.info("---------------------------------------------------------------------");
			logger.info("--------------------------- ExternalData3 ---------------------------");
			logger.info("---------------------------------------------------------------------");
			logger.info("=");
			
			logger.info("=  externalData3 = " + (config.isExternalData3enable() ? "enabled" : "disabled"));
			logger.info("=  Looking for externalData3 in extraFields ...");

			if(config.getExternalData3ExtraParamsReset().contains(profileId))
			{
				logger.info("Resetting value for externalData2 retrieved from extraParams as profile: " + profileId + " has requested a reset");
				extraFields.put(FIELD_EXTERNALDATA3, null);
			}

			if (extraFields.get(FIELD_EXTERNALDATA3) != null)
			{
				logger.info("=  Found externalData3 in extraFields ...");
				logger.info("=  externalData3 = " + extraFields.get(FIELD_EXTERNALDATA3));
				extraFields.put(FIELD_EXTERNALDATA3, extraFields.get(FIELD_EXTERNALDATA3));
				externalData3.set(extraFields.get(FIELD_EXTERNALDATA3));
			}
			else
			{
				logger.info("=  externalData3 not found in extraFields !!!");
				logger.info("= ");
				logger.info("-----------------------------------------------");
				logger.info("=  Reading properties for externalData3 ...");
				logger.info("---------------------");
				logger.info("=");
				logger.info("=  ChannelBasedExternalData3 = " + (config.isChannelBasedExternalData3Enable() ? "enabled" : "disabled"));
				
				if (config.isChannelBasedExternalData3Enable() && extraFields != null && extraFields.get("channel") != null)
				{
					logger.info("=");
					logger.info("=  Channel = " + extraFields.get("channel"));
					externalData3.set(channelPrefix + extraFields.get("channel"));
					
				}
				
			}
			
			logger.info("=  Looking for externalData3(reference) in extraFields using flag set_externalData3_as_ers_reference ...");
			
		}

		if (config.isExternalData4enable())
		{
			logger.info("---------------------------------------------------------------------");
			logger.info("--------------------------- ExternalData4 ---------------------------");
			logger.info("---------------------------------------------------------------------");
			logger.info("=");

			logger.info("=  externalData4 = " + (config.isExternalData4enable() ? "enabled" : "disabled"));
			logger.info("=  Looking for externalData4 in extraFields ...");

			if(config.getExternalData4ExtraParamsReset().contains(profileId))
			{
				logger.info("Resetting value for externalData2 retrieved from extraParams as profile: " + profileId + " has requested a reset");
				extraFields.put(FIELD_EXTERNALDATA4, null);
			}

			if (extraFields.get(FIELD_EXTERNALDATA4) != null)
			{
				logger.info("=  Found externalData4 in extraFields ...");
				logger.info("=  externalData4 = " + extraFields.get(FIELD_EXTERNALDATA4));
				extraFields.put(FIELD_EXTERNALDATA4, extraFields.get(FIELD_EXTERNALDATA4));
				externalData4.set(extraFields.get(FIELD_EXTERNALDATA4));
			}
			else
			{
				logger.info("=  externalData4 not found in extraFields !!!");
//				logger.info("= ");
//				logger.info("-----------------------------------------------");
//				logger.info("=  Reading properties for externalData2 ...");
//				logger.info("---------------------");
//				logger.info("=");

			}

		}
		
		if (externalData1 != null && externalData1.get() != null && !externalData1.get().isEmpty())
		{
			req.setExternalData1(externalData1.get());
		}
		if (externalData2 != null && externalData2.get() != null && !externalData2.get().isEmpty())
		{
			req.setExternalData2(externalData2.get());
		}
		if (externalData3 != null && externalData3.get() != null && !externalData3.get().isEmpty())
		{
			req.setExternalData3(externalData3.get());
		}
		if (externalData4 != null && externalData4.get() != null && !externalData4.get().isEmpty())
		{
			req.setExternalData4(externalData4.get());
		}
		logger.info("=");
		logger.info("=====================================================================");
	}

	/**
	 * Set externalData1 in request based on Transaction Profile
	 * @param extraFields
	 * @param transactionProfile
	 */
	private void setExternalData1ForTransactionProfile(ERSHashtableParameter extraFields,String transactionProfile)
	{
		String externalData1Value="";

		if (config.getExternalData1Properties() != null && extraFields.getParameters().containsKey(UPDATE_BALANCE_KEY))
		{
			logger.info("=  OperationType = " + UPDATE_BALANCE_KEY);
			logger.info("=  Reading properties for Debit ...");
			externalData1Value = config.getExternalData1Properties().getProperty(transactionProfile + ".Debit");
			logger.info("=  externalData1."+transactionProfile+".Debit = " + externalData1Value);
			externalData1Value = setSenderOrReceiverMSISDN(extraFields, externalData1Value);
			if ((externalData1Value == null || externalData1Value.trim().isEmpty()) && extraFields.get("ReceiverId") != null)
			{
				logger.info("=  Setting externalData1 = ReceiverId");
				externalData1Value = extraFields.get("ReceiverId");
			}
			if ((externalData1Value == null || externalData1Value.trim().isEmpty()) && extraFields.get(SENDER_MSISDN) != null)
			{
				logger.info("=  Setting externalData1 = SENDER_MSISDN");
				externalData1Value = extraFields.get(SENDER_MSISDN);
			}
		}
		else if (config.getExternalData1Properties() != null && extraFields.getParameters().containsKey(REFILL_KEY))
		{
			logger.info("=  OperationType = " + REFILL_KEY);
			logger.info("=  Reading properties for Credit ...");
			externalData1Value = config.getExternalData1Properties().getProperty(transactionProfile + ".Credit");
			logger.info("=  externalData1."+transactionProfile+".Credit = " + externalData1Value);
			externalData1Value = setSenderOrReceiverMSISDN(extraFields, externalData1Value);
			if (externalData1Value != null && externalData1Value.trim().isEmpty() && extraFields.get(SENDER_MSISDN) != null)
			{
				logger.info("=  Setting externalData1 = SENDER_MSISDN");
				externalData1Value = extraFields.get(SENDER_MSISDN);
			}
		}

		if(externalData1Value == null || externalData1Value.isEmpty())
		{
			logger.info("=");
			logger.debug("=  Setting externalData1 to default value ... ");
			externalData1Value = config.getExternalData1Properties().getProperty("default", null);
		}
		logger.debug("=  externalData1 = "+ externalData1Value);
		logger.info("=");
		extraFields.put(FIELD_EXTERNALDATA1, externalData1Value);
		externalData1.set(externalData1Value);
	}

	/**
	 * Set externalData1 in request based on ProductSKU
	 * @param extraFields
	 */
	void setExternalData1ForProductSKU(ERSHashtableParameter extraFields)
	{
		String productSKU = extraFields.get(PRODUCT_SKU);
		logger.info("=  ProductSKU = " + productSKU);
		String externalData1Value="";

		if (config.getExternalData1Properties() != null && extraFields.getParameters().containsKey(UPDATE_BALANCE_KEY))
		{
			logger.info("=  OperationType = " + UPDATE_BALANCE_KEY);
			logger.info("=  Reading properties for Debit ...");
			if (productSKU != null)
			{
				externalData1Value = config.getExternalData1Properties().getProperty(productSKU + ".Debit");
				logger.info("=  externalData1."+productSKU+".Debit = " + externalData1Value);
				externalData1Value = setSenderOrReceiverMSISDN(extraFields, externalData1Value);
			}
			if ((externalData1Value == null || externalData1Value.trim().isEmpty()) && extraFields.get("ReceiverId") != null)
			{
				logger.info("=  Setting externalData1 = ReceiverId");
				externalData1Value = extraFields.get("ReceiverId");
			}
			if ((externalData1Value == null || externalData1Value.trim().isEmpty()) && extraFields.get(SENDER_MSISDN) != null)
			{
				logger.info("=  Setting externalData1 = SENDER_MSISDN");
				externalData1Value = extraFields.get(SENDER_MSISDN);
			}
		}
		else if (config.getExternalData1Properties() != null && extraFields.getParameters().containsKey(REFILL_KEY))
		{
			if(productSKU !=null)
			{
				externalData1Value = config.getExternalData1Properties().getProperty(productSKU + ".Credit");
				logger.info("=  externalData1."+productSKU+".Credit = " + externalData1Value);
				externalData1Value = setSenderOrReceiverMSISDN(extraFields, externalData1Value);
			}
			if((externalData1Value == null || externalData1Value.trim().isEmpty()) && extraFields.get(SENDER_MSISDN)!=null)
			{
				logger.info("=  Setting externalData1 = SENDER_MSISDN");
				externalData1Value = extraFields.get(SENDER_MSISDN);
			}
		}

		if(externalData1Value == null || externalData1Value.isEmpty())
		{
			logger.info("=");
			logger.debug("=  Setting externalData1 to default value ... ");
			externalData1Value = config.getExternalData1Properties().getProperty("default", null);
		}
		logger.debug("=  externalData1 = "+ externalData1Value);
		extraFields.put(FIELD_EXTERNALDATA1,externalData1Value);
		externalData1.set(externalData1Value);
	}

	private String setSenderOrReceiverMSISDN(ERSHashtableParameter extraFields, String externalData1Value)
	{
		if (externalData1Value != null && externalData1Value.equalsIgnoreCase(RECEIVER_MSISDN) && extraFields.get(RECEIVER_MSISDN) != null)
		{
			logger.info("=  Setting externalData1 = RECEIVER_MSISDN");
			externalData1Value = extraFields.get(RECEIVER_MSISDN);
		}
		else if (externalData1Value != null && externalData1Value.equalsIgnoreCase(SENDER_MSISDN) && extraFields.get(SENDER_MSISDN) != null)
		{
			logger.info("=  Setting externalData1 = SENDER_MSISDN");
			externalData1Value = extraFields.get(SENDER_MSISDN);
		}
		return externalData1Value;
	}


	/**
	 * Set externalData2 in request based on Transaction Profile
	 * @param extraFields
	 * @param transactionProfile
	 */
	private void setExternalData2ForTransactionProfile(ERSHashtableParameter extraFields,String transactionProfile)
	{
		String externalData2Value="";

		if (config.getExternalData2Properties() != null && extraFields.getParameters().containsKey(UPDATE_BALANCE_KEY))
		{
			logger.info("=  OperationType = " + UPDATE_BALANCE_KEY);
			logger.info("=  Reading properties for Debit ...");
			externalData2Value = config.getExternalData2Properties().getProperty(transactionProfile + ".Debit");
			logger.info("=  externalData2."+transactionProfile+".Debit = " + externalData2Value);
			if((externalData2Value ==null || externalData2Value.trim().isEmpty()) && extraFields.get(RECEIVER_MSISDN)!=null)
			{
				logger.info("=  Setting externalData2 = RECEIVER_MSISDN");
				externalData2Value = extraFields.get(RECEIVER_MSISDN);
			}
		}
		else if (config.getExternalData2Properties() != null && extraFields.getParameters().containsKey(REFILL_KEY))
		{
			logger.info("=  OperationType = " + REFILL_KEY);
			logger.info("=  Reading properties for Credit ...");
			externalData2Value = config.getExternalData2Properties().getProperty(transactionProfile + ".Credit");
			logger.info("=  externalData2."+transactionProfile+".Credit = " + externalData2Value);
			if((externalData2Value == null || externalData2Value.trim().isEmpty()) && extraFields.get(SENDER_MSISDN)!=null)
			{
				logger.info("=  Setting externalData2 = SENDER_MSISDN");
				externalData2Value = extraFields.get(SENDER_MSISDN);
			}
		}

		if(externalData2Value == null || externalData2Value.isEmpty())
		{
			logger.info("=");
			logger.debug("=  Setting externalData2 to default value ... ");
			externalData2Value = config.getExternalData2Properties().getProperty("default", null);
		}
		logger.debug("=  externalData2 = "+ externalData2Value);
		extraFields.put(FIELD_EXTERNALDATA2,externalData2Value);
		externalData2.set(externalData2Value);
	}

	/**
	 * Set externalData2 in request based on ProductSKU
	 * @param extraFields
	 */
	void setExternalData2ForProductSKU(ERSHashtableParameter extraFields)
	{
		String productSKU = extraFields.get("productSKU");
		logger.info("=  ProductSKU = " + productSKU);
		String externalData2Value="";

		if (config.getExternalData2Properties() != null && extraFields.getParameters().containsKey(UPDATE_BALANCE_KEY))
		{
			logger.info("=  OperationType = " + UPDATE_BALANCE_KEY);
			logger.info("=  Reading properties for Debit ...");

			if (productSKU != null)
			{
				externalData2Value = config.getExternalData2Properties().getProperty(productSKU + ".Debit");
				logger.info("=  externalData2."+productSKU+".Debit = " + externalData2Value);
			}
			if ((externalData2Value == null || externalData2Value.trim().isEmpty()) && extraFields.get(RECEIVER_MSISDN) != null)
			{
				logger.info("=  Setting externalData2 = RECEIVER_MSISDN");
				externalData2Value = extraFields.get(RECEIVER_MSISDN);
			}
		}
		else if (config.getExternalData2Properties() != null && extraFields.getParameters().containsKey(REFILL_KEY))
		{
			if(productSKU !=null)
			{
				externalData2Value = config.getExternalData2Properties().getProperty(productSKU + ".Credit");
				logger.info("=  externalData2."+productSKU+".Credit = " + externalData2Value);
			}
			if((externalData2Value == null || externalData2Value.trim().isEmpty()) && extraFields.get(SENDER_MSISDN)!=null)
			{
				logger.info("=  Setting externalData2 = SENDER_MSISDN");
				externalData2Value = extraFields.get(SENDER_MSISDN);
			}
		}

		if(externalData2Value == null || externalData2Value.isEmpty())
		{
			logger.info("=");
			logger.debug("=  Setting externalData2 to default value ... ");
			externalData2Value = config.getExternalData2Properties().getProperty("default", null);
		}

		logger.debug("=  externalData2 = "+ externalData2Value);
		extraFields.put(FIELD_EXTERNALDATA2,externalData2Value);
		externalData2.set(externalData2Value);
	}


	/**
	 * Setting default Variables in UCIPRequest
	 * @param req
     */
	void setDefaultVariables(UCIPSubscriberRequest req)
	{
		req.setOriginHostName(originHostName);
		req.setOriginNodeType(originNodeType);
		req.setSubscriberNumberNAI(subscriberNumberNAI);

		if (externalData1 != null && externalData1.get() != null && !externalData1.get().isEmpty())
		{
			req.setExternalData1(externalData1.get());
		}
		if (externalData2 != null && externalData2.get() != null && !externalData2.get().isEmpty())
		{
			req.setExternalData2(externalData2.get());
		}
		if (externalData3 != null && externalData3.get() != null && !externalData3.get().isEmpty())
		{
			req.setExternalData3(externalData3.get());
		}
		if (externalData4 != null && externalData4.get() != null && !externalData4.get().isEmpty())
		{
			req.setExternalData4(externalData4.get());
		}
	}

	int ifNull(Integer i, int def)
	{
		return (i == null) ? def : i;
	}

	public abstract UCIPResponse getBalance(String subscriberNumber,
			Integer dedicatedAccountId,Integer dedicatedAccountLastId, String transactionId, ERSHashtableParameter extraFields);

	public abstract UCIPResponse refill(String subscriberNumber, Amount amount,
			String transactionId, String transactionType, String profileId,
			Integer dedicatedAccountId, ERSHashtableParameter extraFields);

	public abstract UCIPResponse updateOffer(String subscriberNumber, Amount amount,
			String transactionId, String transactionType, String profileId,
			Integer dedicatedAccountId, ERSHashtableParameter extraFields,OfferProductsDTO offerProductsDTO, String currency);

	public abstract UCIPResponse updateServiceClass(String subscribernumber,
			int serviceClass, String transactionId, ERSHashtableParameter extraFields);

	public abstract UCIPResponse redeemVoucher(String subscribernumber,
			String voucherCode, String transactionId, ERSHashtableParameter extraFields);

	public abstract UCIPResponse getAccountDetails(String subscriberNumber, String transactionId, ERSHashtableParameter extraFields);

	public abstract UCIPResponse getAccountFAF(String subscriberNumber,
			String transactionId);

	public abstract UCIPResponse addToAccountFAF(String subscriberNumber,
			String fafNumber, String transactionId);

	public abstract UCIPResponse removeAccountFAF(String subscriberNumber,
			String fafNumber, String transactionId);

	public abstract UCIPResponse updateBalance(String subscriberNumber,
			Amount relativeBalance, String transactionId,
			String transactionType, Integer dedicatedAccountId, ERSHashtableParameter extraFields);

	public abstract UCIPResponse updateAccumulators(String subscriberNumber, String transactionId, AccumulatorUpdateInformation[] info,
			ERSHashtableParameter extraFields);

	public abstract UCIPResponse getCapabilities();
	public abstract UCIPResponse deleteOffer(String transactionId,
											 String subscriberNumber,
											 OfferProductsDTO offerProductDTO,
											 ERSHashtableParameter extraFields);

	public com.seamless.common.uciplib.common.UCIPResponse makeRequest(UCIPSubscriberRequest request) throws Exception
	{

		EtmPoint point = EtmManager.getEtmMonitor().createPoint("IUCIPAdaptor::makeRequest");
		try
		{
			com.seamless.common.uciplib.common.UCIPResponse response = null;

			int retryCount = 0;

			if (config.getUcipProperties().getProperty("requestRetry") != null)
			{
				retryCount = Integer.parseInt(config.getUcipProperties().getProperty("requestRetry"));
			}

			boolean disableRetriesAfterTimeout = Boolean.parseBoolean(config.getUcipProperties().getProperty("disableRetriesAfterTimeout", "false"));

			int clientCounter = 0;

			// Changed for round robin configuration as it will make multiple
			// requests to CS3 server
			// till it receives response and helps in load balancing as well
			while (response == null)
			{
				client = config.getServerSelector().selectServer(request.getSubscriberNumber());

				if (client == null)
				{
					// This is weird case and might happen if no URL is provided
					// in config file but seems a distant case.
					logger.error("No CS3 service Urls are provided in the config file");
					throw new ConnectException("No CS3 service Urls are provided in the config file.");
				}

				try
				{
					if(org.apache.commons.lang.StringUtils.isBlank(request.getOriginTransactionID()))
					{
						request.setOriginTransactionID(config.getUcipOriginTransactionId());
					}

					EtmPoint pointCS = EtmManager.getEtmMonitor().createPoint("IUCIPAdaptor::makeRequest::" + client.getServerURL());

					try
					{
						response = client.makeRequest(request);
						logger.info("Ucip Response from AIR " +response.toString());
					} finally
					{
						pointCS.collect();
					}
				}
				catch (Exception ex)
				{
					logger.debug("makeRequest to " + client.getServerURL() + " failed: " + ex.getMessage());

					if(ex instanceof  XmlRpcException)
					{
						String indentation = "    ";
						XmlRpcException xmlRpcException = (XmlRpcException) ex;
						logger.error("Fault detected as XmlRpcException, Fault { \n" +
								indentation + "faultCode: " + xmlRpcException.code + "\n" +
								indentation + "faultString: " + xmlRpcException.getMessage() + "\n" +
								"}");
						logger.error("Fault Exception for " + request.getMethodName() + " - " + xmlRpcException.code + " | " + xmlRpcException.getMessage() );
						logger.error("#MakeRequest XmlRpcException ");
					}
					if (disableRetriesAfterTimeout && !(ex instanceof SocketTimeoutException))
					{
						logger.debug("Disabling retries after timeout");
						logger.error("#MakeRequest SocketTimeoutException 1");
						throw new ConnectException("Could not connect to the CS server. Retries disabled.");
					}
                    if (disableRetriesAfterTimeout && ex instanceof SocketTimeoutException)
                    {
                        logger.debug("Disabling retries after socket timeout");
						logger.error("#MakeRequest SocketTimeoutException 2");
                        throw new SocketTimeoutException("Could not connect to the CS server. Retries disabled after socket timeout.");
                    }
					logger.debug("Exception stack trace is " ,ex);
					if (clientCounter >= retryCount)
					{
						logger.debug("***All the retries to the CS3 servers are completed probably all CS3 servers are down***");
						throw new ConnectException("Could not connect to the CS server. All retries failed.");
					}

					clientCounter++;
					response = null;
					continue;
				}

				if (response != null)
				{
					logger.trace("Response received from CS server: " + client.getServerURL());
				}
				else
				{
					logger.error("Invalid response from UCIPLib");

					throw new Exception("Invalid response from UCIPLib");

				}

				// Updated for Bug no. 1736
				clientCounter++;
			}

			return response;

		} finally
		{
			point.collect();
		}
	}

	public Integer getRequestedOwner()
	{
		return client.getRequestedOwner();

	}

	/**
	 * This method is for appending DedicatedAccountInformation to request for Data Bundle calls whether 3g or 4G.
	 * @param extraFields
	 * @throws ParseException,IndexOutOfBoundsException
     */
	public void loadDAInformationForDataBundle(UpdateBalanceAndDateRequest request, ERSHashtableParameter extraFields) throws ParseException, IndexOutOfBoundsException
	{

		HashMap<String, OfferProductsDTO> offerSKUMap = (HashMap<String, OfferProductsDTO>) config.getOfferListMap();
		String productSKUExtaField = extraFields.get("productSKU");
		OfferProductsDTO offerProductsDTO = offerSKUMap.get(productSKUExtaField);
		UpdateBalanceAndDateRequest.DedicatedAccountUpdateInformation dedicatedAccountUpdateInformation;
		UpdateBalanceAndDateRequest.DedicatedAccountUpdateInformation[] dedicatedAccountUpdateInformationArray;
		List<UpdateBalanceAndDateRequest.DedicatedAccountUpdateInformation> dedicatedAccountUpdateInformationList = new ArrayList<UpdateBalanceAndDateRequest.DedicatedAccountUpdateInformation>();

		Boolean is4GCall = false;

		if(config.getEnabled4GDataBundleCalls())
		{
			is4GCall = true;
			for(String channel : config.getCheck4GChannelList())
			{
				is4GCall = StringUtils.parseBoolean(extraFields.get(channel));
				if(!is4GCall) break;
			}
		}

		int daIndex = 0;
		for (String daID : offerProductsDTO.getDaIDList())
		{
			dedicatedAccountUpdateInformation = new UpdateBalanceAndDateRequest.DedicatedAccountUpdateInformation();
			dedicatedAccountUpdateInformation.setDedicatedAccountID(Integer.valueOf(daID));
			if(!offerProductsDTO.getDaAmountList().isEmpty())
			{
				dedicatedAccountUpdateInformation.setAdjustmentAmountRelative(Long.valueOf(offerProductsDTO.getDaAmountList().get(daIndex)));
			}
			dedicatedAccountUpdateInformation.setExpiryDate(((formatter.parse(formatter.format(offerProductsDTO.getEndDate(offerProductsDTO.getValidityList().get(daIndex)))))));
			dedicatedAccountUpdateInformationList.add(dedicatedAccountUpdateInformation);
			daIndex++;
			if(!(is4GCall || "COMBO_BUNDLE".equals(extraFields.get("TRANSACTION_TYPE_KEY")) || "COMBO_BUNDLE".equalsIgnoreCase(extraFields.get(TRANSACTION_PROFILE)) || "COMBO_BUNDLE".equalsIgnoreCase(extraFields.get(PROFILE_ID))))
				{
				break;
				}
		}

		if(is4GCall)
		{
			Double fourGAmount=Double.parseDouble(offerProductsDTO.getDaAmountList().get(offerProductsDTO.getDaAmountList().size()-1))/100;
			extraFields.put("BONUS_AMOUNT_4G",fourGAmount.toString());
		}
		dedicatedAccountUpdateInformationArray = dedicatedAccountUpdateInformationList.toArray(new UpdateBalanceAndDateRequest.DedicatedAccountUpdateInformation[dedicatedAccountUpdateInformationList.size()]);
		request.setDedicatedAccountUpdateInformation(dedicatedAccountUpdateInformationArray);

		extraFields.put(UPDATE_BALANCE_KEY, UPDATE_BALANCE_VALUE);
		request.setTransactionCode("2");
		request.setTransactionType("databp");
		request.setOriginNodeType(extraFields.get(IUCIPAdaptor.FIELD_ORIGINNODETYPE));
		request.setExternalData2(extraFields.get(IUCIPAdaptor.FIELD_EXTERNALDATA2));
		extraFields.put("IS_4G_CALL",is4GCall.toString());
	}

	/**
	 * This method is for appending DedicatedAccountInformation to request for existing flow.
	 * @param request
	 * @throws ParseException,ERSInvalidCurrencyException
	 */
	public void loadDAInformation(UpdateBalanceAndDateRequest request, Integer dedicatedAccountId, Amount relativeBalance, ERSHashtableParameter extraFields) throws ParseException, ERSInvalidCurrencyException
	{
		UpdateBalanceAndDateRequest.DedicatedAccountUpdateInformation info = new UpdateBalanceAndDateRequest.DedicatedAccountUpdateInformation();
		info.setDedicatedAccountID(dedicatedAccountId);
		if(config.isEnableSourceMultiPlier()) {
			BigDecimal bd = UCIPUtils.amountWithSourceMultiplier(config, relativeBalance, extraFields) == null ? relativeBalance.getValue() : UCIPUtils.amountWithSourceMultiplier(config, relativeBalance, extraFields);
			relativeBalance.setValue(bd);
		}
		else {
			info.setAdjustmentAmountRelative(CurrencyHandler.getAmountValueAsLong(relativeBalance));
		}
		info.setDedicatedAccountUnitType(Integer.parseInt(config.getUcipProperties().getProperty("UpdateBalanceAndDate.dedicatedAccountUnitType", "1")));
		UpdateBalanceAndDateRequest.DedicatedAccountUpdateInformation[] list = {info};
		request.setDedicatedAccountUpdateInformation(list);
		request.setExternalData1(extraFields.get(IUCIPAdaptor.FIELD_EXTERNALDATA1));
	}

	/**
	 * Return originNodeType whether its set by client in extraFields or its set in config
	 * @param extraFields
	 * @return
	 */
	public String getOriginNodeType(ERSHashtableParameter extraFields)
	{
		String originNodeType = extraFields.get(IUCIPAdaptor.FIELD_ORIGINNODETYPE);
		if (originNodeType != null)
		{
			return originNodeType;
		}
		if( originNodeType == null && extraFields.get(PRODUCT_SKU)!=null)
		{
			String key = FIELD_ORIGINNODETYPE + "." + extraFields.get(PRODUCT_SKU);
			originNodeType = config.getUcipProperties().getProperty(key);
		}
		return originNodeType;
	}

	public String getOriginHostName(ERSHashtableParameter extraFields)
	{
		String originHostName = extraFields.get(IUCIPAdaptor.FIELD_ORIGINHOSTNAME);
		if (originHostName != null)
		{
			return originHostName;
		}
		if( originHostName == null && extraFields.get(PRODUCT_SKU)!=null)
		{
			String key = FIELD_ORIGINHOSTNAME + "." + extraFields.get(PRODUCT_SKU);
			originHostName = config.getUcipProperties().getProperty(key);
		}
		return originHostName;
	}

	protected void dynamicOverrideOriginHostName(RefillRequest request, ERSHashtableParameter extraFields)
	{

		if(extraFields != null && extraFields.get("originHostName") != null)
		{
			request.setOriginNodeType(extraFields.get("originHostName") + extraFields.get("originHostName"));
			logger.info("Dynamically overriding originHostName to: " + extraFields.get("originHostName"));
		}
	}

	protected void dynamicOverrideOriginNodeType(RefillRequest request, ERSHashtableParameter extraFields)
	{
		if(extraFields != null && extraFields.get("originNodeType") != null)
		{
			request.setOriginNodeType(extraFields.get("originNodeType") + extraFields.get("originNodeType"));
			logger.info("Dynamically overriding originNodeType to: " + extraFields.get("originNodeType"));
		}
	}

	protected void resetExternalDataParameters()
	{
		this.externalData1.set("");
		this.externalData2.set("");
		this.externalData3.set("");
		this.externalData4.set("");
	}
}
