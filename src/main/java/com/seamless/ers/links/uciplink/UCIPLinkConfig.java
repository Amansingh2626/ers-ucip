package com.seamless.ers.links.uciplink;

import com.seamless.common.*;
import com.seamless.common.config.ConfigurationFileHandler;
import com.seamless.common.config.ERSModuleConfiguration;
import com.seamless.common.config.PropertiesUpdateHandler;
import com.seamless.common.config.SeamlessProperties;
import com.seamless.common.servertools.flowcontrol.FlowControlHandler;
import com.seamless.common.servertools.flowcontrol.StandardFlowControlHandler;
import com.seamless.common.uciplib.client.UCIPClient;
import com.seamless.common.uciplib.common.MessageCapabilityFlag;
import com.seamless.common.uciplib.common.UCIPException;
import com.seamless.common.referencegeneration.ReferenceGenerator;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.AccountStatus;
import com.seamless.ers.links.uciplink.GroupIntervalList.Interval;
import com.seamless.ers.links.uciplink.config.GetAccountInformationMethod;
import com.seamless.ers.links.uciplink.config.ResultRow;
import com.seamless.ers.links.uciplink.config.ValidateTopupMethod;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;
import com.seamless.ers.links.uciplink.offers.OfferRequestHandler;
import com.seamless.ers.links.uciplink.rules.RuleRequestHandler;
import com.seamless.ers.links.uciplink.ucip.UCIPAdaptor;
import com.seamless.ers.links.uciplink.utils.ExpireFormat;
import com.seamless.util.net.ServerFactory;
import com.seamless.util.net.ServerSelector;
import etm.core.configuration.XmlEtmConfigurator;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration class for UCIPLink module.
 */
public class UCIPLinkConfig extends ERSModuleConfiguration implements ServerFactory
{
	private static final Logger logger = LoggerFactory.getLogger(UCIPLinkConfig.class);
	private StandardFlowControlHandler flowControlHandler;
	private boolean running = true;
	private String servicesURL;
	private String nodeId;
	private String operationsURL;
	private String managementURL;
	private String businessRuleURL;
	private UCIPAdaptor ucipAdaptor;
	private Integer refillAccumulator;
	private String refillProfileID;
	private String productSelectionKey;
	private boolean allowSelfTopup;
	private RuleRequestHandler businessRuleDispatcher;
	private OfferRequestHandler offerRequestDispatcher;
	
	private ExtendedProperties externalData1Properties;
	private ExtendedProperties externalData2Properties;
	private ExtendedProperties externalData3Properties;
	private ExtendedProperties externalData4Properties;

	private String ucipOriginTransactionId;

	private ExtendedProperties ucipProperties;
	private UCIPClient ucipClient;

	private ServerSelector<UCIPClient> serverSelector = null;
	private String ucipOperatorSpecificLanguage2;
	private String ucipOperatorSpecificLanguage3;
	private String ucipOperatorSpecificLanguage4;
	private String ucipOperatorSpecificLanguage1;
	private GroupIntervalList linkTypeList;
	private SimpleDateFormat responseDateFormatter;
	private boolean matchAccountStatusExactTime;
	private boolean nativeReferenceLengthCheck;
	private boolean nativeReferenceReverseDigits;
	private boolean nativeReferenceGeneratedByDatabaseSequence;
	private boolean useReferenceAsNativeReference;
	private boolean useInputReferenceNumberEnabled;
	private String useInputReferenceNumberOldprefix;
	private String useInputReferenceNumberNewprefix;

	UCIPNumberFormatter numberFormatter;

	private HashMap<Integer, String> refillValueTotalDANameMap = new LinkedHashMap<Integer, String>();
	private HashMap<Integer, String> refillValuePromotionDANameMap = new LinkedHashMap<Integer, String>();
	private boolean reverseTopupWantBalance;

	private boolean fetchBalanceAfter;

	private HashMap<String, TopupProductSettings> topupProductSettingsMap = new HashMap<String, TopupProductSettings>();
	private HashMap<Integer, TreeDefinedFieldValue> treeDefinedFieldValueMap = new HashMap<Integer, TreeDefinedFieldValue>();
	private HashMap<String, OfferProductsDTO> offerSKUMapping=new HashMap<String, OfferProductsDTO>();
	private HashMap<String,String> nativeErrorCodeMap=new HashMap<String, String>();
	private HashMap<Integer, ResultRow> configurablePrimaryErrorCodesMap = new HashMap<Integer, ResultRow>();
	private HashMap<Integer, ResultRow> configurableSecondaryErrorCodesMap = new HashMap<Integer, ResultRow>();

	private Boolean validateSubscriberLocation;
	
	private boolean isTransformCurrencyEnabled;
	private String transformCurrencyCode = "";
	
	private boolean originOperatorIDRefillEnable;
	private boolean updateOffer2CurrencyEnable;
	private boolean transactionTypeRefillEnable;
	private boolean negotiatedCapabilitiesRefillEnable;
	private boolean negotiatedCapabilitiesUpdateOfferEnable;
	private boolean negotiatedCapabilitiesGetBalanceAndDateEnable;
	private boolean negotiatedCapabilitiesUpdateAccumulatorsEnable;
	private boolean negotiatedCapabilitiesDeleteOfferEnable;
	private boolean treeDefinedFieldRefillEnable;
	private boolean externalData1enable;
	private boolean externalData2enable;
	private boolean externalData3enable;
	private boolean externalData4enable;
	private boolean originOperatorIDUpdateBalanceEnable;
	private boolean setExternalData2AsERSReference;
	private boolean setExternalData1AsERSReference;
	private boolean subordinateDedicatedAccountDetailsFlagEnable;
	private boolean subordinateDedicatedAccountDetailsFlagValue;
	private boolean requestRefillAccountBeforeFlagEnable;
	private boolean requestRefillAccountBeforeFlagValue;
	private String sourceMultiPlierKeyName;
	private boolean enableSourceMultiPlier;

	private String mappingFileName;

	private String mappedProductSKUName;

	private Boolean enableDataBundle;
	private List excludUpdateOfferlist;
	private Boolean enabled4GDataBundleCalls;
	private List<String> check4GChannelList;
	private int fafIndicator;
	private boolean profileBasedExternalData1Enable;
	private boolean profileBasedExternalData2Enable;
	private boolean channelBasedExternalData3Enable;
	private List<String> bundleNameList;
	private List<String> externalData1ExtraParamsReset;
	private List<String> externalData2ExtraParamsReset;
	private List<String> externalData3ExtraParamsReset;
	private List<String> externalData4ExtraParamsReset;
	private List<String> refillProfileIdParameters;
	private boolean populateResponseWithTreeDefinedFields;
	private String treeDefinedFieldsResponsePrefix;
	private boolean enableNativeCodeMapping;
	private boolean enableSuccessResponseOnTimeout;
	private boolean enableConfigurablePrimaryErrorCodes;
	private boolean enableTranslateSecondaryErrorCodes;
	private boolean enableConfigurableSecondaryErrorCodes;
	private ValidateTopupMethod validateTopupMethod;
	private GetAccountInformationMethod getAccountInformationMethod;
	private Integer getBalanceAndDateDASFirstId = 0;
	private Integer getBalanceAndDateDASLastId = 0;
	private ExpireFormat updateOfferExpiryFormat = null;

	private HashMap<String, String> refillProfileIdMap=new HashMap<String, String>();

	private boolean simulateAccountInformation;
	private String defaultCurreny;
	// Added to give the option to generate native reference from database sequence number.
	private ReferenceGenerator referenceGenerator;
	private boolean customOriginOperatorIdEnabled;
	private boolean getBalanceAndDateSetChannelAsOriginHostName;
	private boolean enableRefillAccountAfterFlag;
	private boolean enableRefillAccountBeforeFlag;
	private boolean enableRefillDetailsFlag;
	private boolean enableMessageCapabilityFlag;
	private MessageCapabilityFlag messageCapabilityFlag;
	private boolean fetchBalanceBeforeInGetBalanceAndDate;
	private boolean enableKeepAlive;
	private int keepAliveRequestCount;
	private int keepAlivePeriodInMillis;
	private int keepAliveThreadPoolSize;
	private long keepAliveStartDelayMillis;

	private boolean useCustomTransactionId;

	private String customOriginOperatorIdExtraFieldName;

	private String customTransactionIdExtraFieldName;

	public boolean useCustomOriginOperatorId()
	{
		return customOriginOperatorIdEnabled;
	}

	public boolean getBalanceAndDateSetChannelAsOriginHostName()
	{
		return getBalanceAndDateSetChannelAsOriginHostName;
	}

	public boolean enableRefillAccountAfterFlag()
	{
		return enableRefillAccountAfterFlag;
	}

	public boolean enableRefillAccountBeforeFlag()
	{
		return enableRefillAccountBeforeFlag;
	}

	public boolean enableRefillDetailsFlag()
	{
		return enableRefillDetailsFlag;
	}
	public boolean returnMockResponse;
	public boolean returnOfferInformation;

	private boolean overrideRequestParamsInTopUp;

	private List<String> overrideParamKeys;

    public int getKeepAliveRequestCount() {
    	return keepAliveRequestCount;
    }

    public int getKeepAlivePeriodInMillis(){
    	return keepAlivePeriodInMillis;
	}

	public int getKeepAliveThreadPoolSize(){
    	return keepAliveThreadPoolSize;
	}

	public long getKeepAliveStartDelayMillis() {
		return keepAliveStartDelayMillis;
	}



	public boolean enableKeepAlive() {
    	return this.enableKeepAlive;
	}

	public class TopupProductSettings
	{
		boolean validateTopupAccount;
		boolean requiresActiveAccount;

		public TopupProductSettings(
				ExtendedProperties properties) throws ERSConfigurationException
		{
			validateTopupAccount = properties.getProperty("validate_topup_account", "false", false).equals("true");
			requiresActiveAccount = properties.getProperty("requires_active_account", "false", false).equals("true");
		}

		public boolean isValidateTopupAccount()
		{
			return validateTopupAccount;
		}

		public boolean isRequiresActiveAccount()
		{
			return requiresActiveAccount;
		}

	}
	
	public class TreeDefinedFieldValue
	{
		String fieldName;
		String fieldType;
		String valueType;
		String fieldValue;

		public TreeDefinedFieldValue() {
			
		}
		
		public TreeDefinedFieldValue(ExtendedProperties extendedProperties) {
			
			super();
			this.fieldName = extendedProperties.getProperty("fieldName");
			this.fieldType = extendedProperties.getProperty("fieldType");
			this.valueType = extendedProperties.getProperty("valueType");
			this.fieldValue = extendedProperties.getProperty("fieldValue");
			
		}


		public String getFieldName() {
			return fieldName;
		}


		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}


		public String getFieldType() {
			return fieldType;
		}


		public void setFieldType(String fieldType) {
			this.fieldType = fieldType;
		}
		
		
		public String getValueType() {
			return valueType;
		}
		
		
		public void setValueType(String valueType) {
			this.valueType = valueType;
		}
		
		
		public String getFieldValue() {
			return fieldValue;
		}

		
		public void setFieldValue(String fieldValue) {
			this.fieldValue = fieldValue;
		}

	}

	private class PropertyLoader implements PropertiesUpdateHandler
	{

		public void propertiesLoaded(ExtendedProperties properties) throws ERSConfigurationException
		{
			loadProperties(properties);
			loadUCIPAdapter(new ExtendedProperties("ucip.", properties));
			loadUCIPTreeDefinedProperties(new ExtendedProperties("ucip.treeDefinedField.", properties));
			setExternalData1Properties(new ExtendedProperties("ucip.externalData1.",properties));
			setExternalData2Properties(new ExtendedProperties("ucip.externalData2.",properties));
			setExternalData3Properties(new ExtendedProperties("ucip.externalData3.",properties));
			setExternalData4Properties(new ExtendedProperties("ucip.externalData4.",properties));
			if(enableNativeCodeMapping){
				loadNativeErrorCodeMapping(new ExtendedProperties("ucip.native_code_mapping.", properties));
			}
			if (enableConfigurablePrimaryErrorCodes || enableConfigurableSecondaryErrorCodes)
			{
				addFile("configured_result_codes.properties", this::loadConfigurableErrorCodes);
			}
			loadFlowControlHandler(properties);
			loadReferenceGeneratorProperties(properties);
			returnMockResponse = properties.getBooleanProperty("return_mock_response" , false);
			returnOfferInformation = properties.getBooleanProperty("return_offer_information" , false);
		}

		private void loadConfigurableErrorCodes(File file) throws ERSConfigurationException
		{
			try
			{
				SeamlessProperties seamlessProperties = new SeamlessProperties();
				seamlessProperties.loadFromFile(file);
				SeamlessProperties subProperties = seamlessProperties.getSubProperties("ucip.configurable_result_code_mapping.");
				loadConfigurableErrorCodesMap(subProperties);
			}
			catch (IOException e)
			{
				throw new ERSConfigurationException(e.getMessage());
			}
		}
	}

	void loadUCIPTreeDefinedProperties(ExtendedProperties extendedProperties) {
		

		logger.info("Loading TreeDefinedProperties for Refill Request");
		int index = 1;
		while (true)
		{
			String prefix = index + ".";
			ExtendedProperties treeProperties = new ExtendedProperties(prefix, extendedProperties);
			
			if (treeProperties.getProperty("fieldName") == null)
			{
				logger.debug("Tree Defined Field confg loding done");
				break;
			}
			else
			{
				TreeDefinedFieldValue treeValue = new TreeDefinedFieldValue(treeProperties);
				treeDefinedFieldValueMap.put(index, treeValue);
				logger.debug(" loaded" + treeValue.getFieldName() + treeValue.getFieldType()+treeValue.getFieldValue());
				
			}
			index++;
		}
		
		logger.debug("Tree Defined Field config loaded" + treeDefinedFieldValueMap);
	}

	void loadNativeErrorCodeMapping(ExtendedProperties extendedProperties){

		logger.info("loading native error code mapping");
		int index =1;

		while (true)
		{
			String prefix = index + ".";
			ExtendedProperties code = extendedProperties.getSubProperties(prefix);

			if (code.getProperty("nativeCode") == null)
			{
				logger.debug("native code map confg loding done");
				break;
			}
			else
			{
				this.nativeErrorCodeMap.put(code.getProperty("nativeCode"),code.getProperty("description"));

			}
			index++;
		}

	}

	void loadConfigurableErrorCodesMap(SeamlessProperties seamlessProperties) throws ERSConfigurationException
	{
		logger.info("Loading configurable error codes mapping");
		SeamlessProperties subProperties = seamlessProperties.getSubProperties("");  // read something like primary.1, secondary.3, etc.
		Set<String> resultCodeLabels = subProperties.stringPropertyNames();
		List<String> labels = resultCodeLabels.stream().sorted().collect(Collectors.toList());
		for (String label : labels)
		{
			String mappingDetails = subProperties.getProperty(label);
			if (mappingDetails == null)
			{
				throw new ERSConfigurationException("Error in result code mapping file");
			}
			else
			{
				String[] split = mappingDetails.split(":");
				if (split.length < 2)
				{
					throw new ERSConfigurationException("Error in result code mapping definition");
				}
				Integer ucipCode = new Integer(split[0]);
				Integer ersCode = new Integer(split[1]);
				String description = "";
				if (split.length >= 3)
				{
					description = split[2];
				}
				ResultRow resultRow = new ResultRow(ucipCode, ersCode, description);
				if (label.contains("primary"))
				{
					configurablePrimaryErrorCodesMap.put(ucipCode, resultRow);
				}
				else if (label.contains("secondary"))
				{
					configurableSecondaryErrorCodesMap.put(ucipCode, resultRow);
				}
			}
		}
		logger.info("Loading of configurable error codes done");
	}

	void loadProperties(ExtendedProperties properties) throws ERSConfigurationException
	{
		servicesURL = properties.getProperty("servicesURL", "http://localhost:8010/UCIPLink/services");
		operationsURL = properties.getProperty("operationsURL", "http://localhost:8010/UCIPLink/operations");
		managementURL = properties.getProperty("managementURL", "http://localhost:8010/UCIPLink/management");
		businessRuleURL = properties.getProperty("businesRuleURL", "http://localhost:8010/UCIPLink/BusinessRuleLink");
		refillAccumulator = StringUtils.parseInt(properties.getProperty("refillAccumulator"));
		refillProfileID = properties.getProperty("refillProfileID", "PURCHASE_PRODUCT");
		productSelectionKey = properties.getProperty("productSelectionKey");
		allowSelfTopup = StringUtils.parseBoolean(properties.getProperty("allowSelfTopup", "false"));
		originOperatorIDRefillEnable = StringUtils.parseBoolean(properties.getProperty("originOperatorID_refill_enable", "false"));
		updateOffer2CurrencyEnable = StringUtils.parseBoolean(properties.getProperty("updateOffer2_currency_enable", "true"));
		transactionTypeRefillEnable = StringUtils.parseBoolean(properties.getProperty("transactionType_refill_enable", "false"));
		negotiatedCapabilitiesRefillEnable = StringUtils.parseBoolean(properties.getProperty("negotiatedCapabilities_refill_enable", "false"));
		negotiatedCapabilitiesUpdateOfferEnable = StringUtils.parseBoolean(properties.getProperty("negotiatedCapabilities_update_offer_enable", "false"));
		negotiatedCapabilitiesGetBalanceAndDateEnable = StringUtils.parseBoolean(properties.getProperty("negotiatedCapabilities_get_balance_and_date_enable", "false"));
		negotiatedCapabilitiesUpdateAccumulatorsEnable = StringUtils.parseBoolean(properties.getProperty("negotiatedCapabilities_update_accumulators_enable", "false"));
		treeDefinedFieldRefillEnable = StringUtils.parseBoolean(properties.getProperty("treeDefinedField_refill_enable", "false"));
		externalData1enable = StringUtils.parseBoolean(properties.getProperty("externalData1_enable", "false"));
		originOperatorIDUpdateBalanceEnable = StringUtils.parseBoolean(properties.getProperty("originOperatorID_updateBalance_enable", "false"));
		nativeReferenceLengthCheck = StringUtils.parseBoolean(properties.getProperty("native_reference_length_check", "false"));
		nativeReferenceReverseDigits = StringUtils.parseBoolean(properties.getProperty("native_reference_reverse_digits", "false"));
		numberFormatter = new UCIPNumberFormatter(properties);
		responseDateFormatter = new SimpleDateFormat(properties.getProperty("message.date_format", "yyyy-MM-dd HH:mm:ss"));
		nodeId = properties.getProperty("node_id", "01");
		matchAccountStatusExactTime = StringUtils.parseBoolean(properties.getProperty("account_status.exact_time_matching", "false"));
		
		reverseTopupWantBalance = StringUtils.parseBoolean(properties.getProperty("reverse_topup_want_balance", "true"));
		fetchBalanceAfter = StringUtils.parseBoolean(properties.getProperty("fetch_balance_after", "false"));
		validateSubscriberLocation = properties.getProperty("validate_subscriber_location") != null ? StringUtils.parseBoolean(properties.getProperty("validate_subscriber_location")) : null;
		profileBasedExternalData1Enable = StringUtils.parseBoolean(properties.getProperty("profile_based_externalData1_enable", "false"));

		externalData2enable = StringUtils.parseBoolean(properties.getProperty("externalData2_enable", "false"));
		externalData3enable = StringUtils.parseBoolean(properties.getProperty("externalData3_enable", "false"));
		externalData4enable = StringUtils.parseBoolean(properties.getProperty("externalData4_enable", "false"));
		profileBasedExternalData2Enable = StringUtils.parseBoolean(properties.getProperty("profile_based_externalData2_enable", "false"));
		channelBasedExternalData3Enable = StringUtils.parseBoolean(properties.getProperty("channel_based_externalData3_enable", "false"));
		setExternalData2AsERSReference = StringUtils.parseBoolean(properties.getProperty("set_externalData2_as_ers_reference", "false"));
		setExternalData1AsERSReference = StringUtils.parseBoolean(properties.getProperty("set_externalData1_as_ers_reference", "false"));
		simulateAccountInformation = StringUtils.parseBoolean(properties.getProperty("ucip.simulate_account_information", "false"));
		defaultCurreny = properties.getProperty("locale.default_currency");
		refillProfileIdParameters = properties.getStringListProperty("refill_profile_id_productsku", new ArrayList<String>(), ",");
		populateResponseWithTreeDefinedFields = StringUtils.parseBoolean(properties.getProperty("populateResponseWithTreeDefinedFields", "false"));
		treeDefinedFieldsResponsePrefix = properties.getProperty("treeDefinedFieldsResponsePrefix", "");
		enableNativeCodeMapping = StringUtils.parseBoolean(properties.getProperty("enableNativeCodeMapping", "false"));
		enableSuccessResponseOnTimeout = StringUtils.parseBoolean(properties.getProperty("enableSuccessResponseOnTimeout", "false"));
		enableConfigurablePrimaryErrorCodes = StringUtils.parseBoolean(properties.getProperty("enableConfigurablePrimaryErrorCodes", "false"));
		enableTranslateSecondaryErrorCodes = StringUtils.parseBoolean(properties.getProperty("enableTranslateSecondaryErrorCodes", "false"));
		enableConfigurableSecondaryErrorCodes = StringUtils.parseBoolean(properties.getProperty("enableConfigurableSecondaryErrorCodes", "false"));
		getBalanceAndDateDASFirstId = StringUtils.parseInt(properties.getProperty("getBalanceAndDate.dedicatedAccountSelection.dedicatedAccountIDFirst"), -1);
		getBalanceAndDateDASLastId = StringUtils.parseInt(properties.getProperty("getBalanceAndDate.dedicatedAccountSelection.dedicatedAccountIDLast"), -1);
		getBalanceAndDateDASFirstId = (getBalanceAndDateDASFirstId == -1) ? null : getBalanceAndDateDASFirstId;
		getBalanceAndDateDASLastId = (getBalanceAndDateDASLastId == -1) ? null : getBalanceAndDateDASLastId;
		customOriginOperatorIdEnabled = StringUtils.parseBoolean(properties.getProperty("enableCustomOriginOperatorId", "false"));
		getBalanceAndDateSetChannelAsOriginHostName = StringUtils.parseBoolean(properties.getProperty("getBalanceAndDateSetChannelAsOriginHostName", "false"));
		subordinateDedicatedAccountDetailsFlagEnable = StringUtils.parseBoolean(properties.getProperty("getBalanceAndDate.subordinateDedicatedAccountDetailsFlag.enable", "false"));
		subordinateDedicatedAccountDetailsFlagValue = StringUtils.parseBoolean(properties.getProperty("getBalanceAndDate.subordinateDedicatedAccountDetailsFlag.value", "false"));
		requestRefillAccountBeforeFlagEnable = StringUtils.parseBoolean(properties.getProperty("refill.requestRefillAccountBeforeFlagEnable", "false"));
		requestRefillAccountBeforeFlagValue = StringUtils.parseBoolean(properties.getProperty("refill.requestRefillAccountBeforeFlagValue", "false"));
		sourceMultiPlierKeyName = properties.getProperty("ucip.sourceMultiPlier.keyName", "SOURCE_MULTIPLIER");
		enableSourceMultiPlier = StringUtils.parseBoolean(properties.getProperty("ucip.sourceMultiPlierEnable", "false"));
		enableRefillAccountAfterFlag = StringUtils.parseBoolean(properties.getProperty("enableRefillAccountAfterFlag", "true"));
		enableRefillAccountBeforeFlag = StringUtils.parseBoolean(properties.getProperty("enableRefillAccountBeforeFlag", "true"));
		enableRefillDetailsFlag = StringUtils.parseBoolean(properties.getProperty("enableRefillDetailsFlag", "true"));
		updateOfferExpiryFormat = ExpireFormat.lookup(properties.getProperty("updateOfferExpiryFormat", "date"));
		enableKeepAlive = StringUtils.parseBoolean(properties.getProperty("keepAlive.enable", "false"));
		keepAliveRequestCount = StringUtils.parseInt(properties.getProperty("keepAlive.requestCount", "1"));
		keepAlivePeriodInMillis = StringUtils.parseInt(properties.getProperty("keepAlive.periodInMillis", "30000"));
		keepAliveThreadPoolSize = StringUtils.parseInt(properties.getProperty("keepAlive.threadPoolSize", "2"));
		keepAliveStartDelayMillis = StringUtils.parseInt(properties.getProperty("keepAlive.startDelayInMillis", "30000"));
		overrideRequestParamsInTopUp = properties.getBooleanProperty("ucip.topup.overrideParamsWithRequest" , false);
		overrideParamKeys = Arrays.asList(properties.getProperty("ucip.topup.overrideParamKeys", "").split(","));

		setValidateTopupMethod(properties);
		setGetAccountInformationMethod(properties);

		if(updateOfferExpiryFormat == null)
		{
			String errorMessage = "Invalid setting for updateOfferExpiryFormat: " +
					properties.getProperty("updateOfferExpiryFormat", "date") +
					". Should be one of: " + Arrays.stream(ExpireFormat.values()).collect(Collectors.toList());
			logger.error(errorMessage);
			throw new ERSConfigurationException(errorMessage);
		}
		loadDataBundleProperties(properties);

		Map<String, String> tmpMap = new LinkedHashMap<String, String>();
		tmpMap = properties.getListProperty("refill_response.refill_value_total.extra_fields_da_names", tmpMap, ",", true, true);
		int i = 1;
		for (Iterator<String> iter = tmpMap.values().iterator(); iter.hasNext();)
		{
			String extraFieldName = iter.next();
			refillValueTotalDANameMap.put(i, extraFieldName);
			i++;
		}
		tmpMap = properties.getListProperty("refill_response.refill_value_promotion.extra_fields_da_names", tmpMap, ",", true, true);
		i = 1;
		for (Iterator<String> iter = tmpMap.values().iterator(); iter.hasNext();)
		{
			String extraFieldName = iter.next();
			refillValuePromotionDANameMap.put(i, extraFieldName);
			i++;
		}

		loadDAConfig(properties);

		ExtendedProperties productsProperties = new ExtendedProperties("topup_products.", properties);
		for (String productId : productsProperties.truncatedKeys("."))
		{
			TopupProductSettings settings = new TopupProductSettings(new ExtendedProperties(productId + ".", productsProperties));
			topupProductSettingsMap.put(productId, settings);
		}
		for(String productSku: refillProfileIdParameters) {
			String tempKey = "refill_profile_id."+productSku;
			
			refillProfileIdMap.put(productSku, properties.getProperty(tempKey, refillProfileID));
		}

		try
		{
			linkTypeList = new GroupIntervalList(properties);
		}
		catch (Exception e)
		{
			throw new ERSConfigurationException("failed to read service class group mapping", null, e);
		}

		enableMessageCapabilityFlag = StringUtils.parseBoolean(properties.getProperty("getBalanceAndDate.enableMessageCapabilityFlag", "false"));
		if(enableMessageCapabilityFlag){
			try {
				ExtendedProperties subProperties = properties.getSubProperties("getBalanceAndDate.messageCapabilityFlag.");
				Map<String,Object> map = new HashMap();
				subProperties.entrySet().forEach(objectObjectEntry -> map.put(objectObjectEntry.getKey().toString(),objectObjectEntry.getValue()));
				messageCapabilityFlag = new MessageCapabilityFlag(map);
			} catch (Exception e) {
				logger.error(" Failed to load MessageCapabilityFlag config "+ ExceptionUtils.getFullStackTrace(e));
				throw new ERSConfigurationException(" Failed to load MessageCapabilityFlag config");
			}
		}
		fetchBalanceBeforeInGetBalanceAndDate = StringUtils.parseBoolean(properties.getProperty("getBalanceAndDate.fetch.balanceBefore", "false"));
		useCustomTransactionId = properties.getBooleanProperty("useCustomTransactionId", false, false);
		customOriginOperatorIdExtraFieldName = properties.getProperty("fetch.custom-origin-operator-id.extra-field", "customOriginOperationId", false);
		customTransactionIdExtraFieldName = properties.getProperty("fetch.custom-transaction-id.extra-field", "reference", false);
		negotiatedCapabilitiesDeleteOfferEnable = StringUtils.parseBoolean(properties.getProperty("negotiatedCapabilities_delete_offer_enable", "false"));
	}

	private void setGetAccountInformationMethod(ExtendedProperties properties) throws ERSConfigurationException {
		String value = properties.getProperty("getAccountInformation.method", "0");
		try
		{
			int val = Integer.parseInt(value);
			this.getAccountInformationMethod = GetAccountInformationMethod.lookup(val);
			if(this.getAccountInformationMethod == null){
				throw new ERSConfigurationException("Invalid value for getAccountInformation.method");
			}
		}
		catch (NumberFormatException e){
			this.getAccountInformationMethod = GetAccountInformationMethod.lookupByName(value);
			if(this.getAccountInformationMethod == null){
				throw new ERSConfigurationException("Invalid value for getAccountInformation.method");
			}
		}
	}

	private void setValidateTopupMethod(ExtendedProperties properties) throws ERSConfigurationException {
		String value = properties.getProperty("validateTopup.method", "0");
		try
		{
				int val = Integer.parseInt(value);
				this.validateTopupMethod = ValidateTopupMethod.lookup(val);
				if(this.validateTopupMethod == null){
					throw new ERSConfigurationException("Invalid value for validateTopup.method");
				}
		}
		catch (NumberFormatException e){
				this.validateTopupMethod = ValidateTopupMethod.lookupByName(value);
				if(this.validateTopupMethod == null){
					throw new ERSConfigurationException("Invalid value for validateTopup.method");
				}
		}
	}

	public List<String> getRefillProfileIdParameters()
	{
		return refillProfileIdParameters;
	}

	void setExternalData1Properties(ExtendedProperties properties)throws ERSConfigurationException {
      externalData1Properties=properties;
      externalData1ExtraParamsReset = new ArrayList<String>();
      if(properties != null){
      	String profiles = properties.getProperty("override_extraparams.profiles");
      	if(profiles != null){
			externalData1ExtraParamsReset = Arrays.asList(profiles.split("\\s*,\\s*"));
		}
	  }
	}


	void loadFlowControlHandler(ExtendedProperties properties) throws ERSConfigurationException
	{
		logger.info("Loading flow control handler config");
		try
		{
			flowControlHandler = new StandardFlowControlHandler(properties);
		}
		catch (Exception e)
		{
			throw new ERSConfigurationException("failed to read flow control config", null, e);
		}
	}

	void loadUCIPAdapter(ExtendedProperties properties) throws ERSConfigurationException
	{
		logger.info("Loading ucip adapter configuration");

		ucipProperties = properties;

		ucipOperatorSpecificLanguage1 = properties.getProperty("operatorSpecificLanguage1", "en");
		ucipOperatorSpecificLanguage2 = properties.getProperty("operatorSpecificLanguage2", "pt");
		ucipOperatorSpecificLanguage3 = properties.getProperty("operatorSpecificLanguage3", "en");
		ucipOperatorSpecificLanguage4 = properties.getProperty("operatorSpecificLanguage4", "en");
		
		fafIndicator = properties.getIntegerProperty("UpdateFaFList.faf_indicator", 1, false);
		
		isTransformCurrencyEnabled = StringUtils.parseBoolean(properties.getProperty("transformCurrency.enabled", "false"));
		if(isTransformCurrencyEnabled) {
			transformCurrencyCode = properties.getProperty("transformCurrency.code", "", true).trim();
			if(transformCurrencyCode.length() == 0) {
				throw new ERSConfigurationException("Property '" + properties.getFullPropertyKey("transformCurrency.code") + "' has invalid value");
			}
		}

		ucipAdaptor = new UCIPAdaptor(UCIPLinkConfig.this, properties);

		ucipOriginTransactionId = properties.getProperty("originTransactionID");

		// round robin selector here
		String serverSelectorConfig = properties.getProperty("serverSelectorConfig");

		try
		{
			serverSelector = ServerSelector.createSelector(UCIPLinkConfig.this, serverSelectorConfig, properties.getProperty("serverURL"));
		}
		catch (Exception e)
		{
			throw new ERSConfigurationException("failed to create the ucip server selector", null, e);
		}
	}

	void loadDAConfig(ExtendedProperties properties) throws ERSConfigurationException
	{
		logger.info("Loading Dedicated accounts config");
		int index = 1;
		while (true)
		{
			Integer daId;
			String prefix = "refill_response." + index + ".";
			ExtendedProperties da_account = new ExtendedProperties(prefix, properties);
			String daId_string = da_account.getProperty("da_id");
			if (daId_string == null)
			{
				break;
			}
			else
			{
				daId = new Integer(daId_string);
			}

			if (!refillValueTotalDANameMap.containsKey(daId))
			{
				String daName = da_account.getProperty("da_name");
				refillValueTotalDANameMap.put(daId, daName);
			}

			index++;
		}
		logger.debug("Dedicated Accounts config loaded" + refillValueTotalDANameMap);
	}

	private void loadReferenceGeneratorProperties(ExtendedProperties properties) {

		logger.info("Loading reference generator properties.");
		try
		{
            nativeReferenceGeneratedByDatabaseSequence = StringUtils.parseBoolean(properties.getProperty("enable_native_reference_generated_by_database_sequence","false"));
			useReferenceAsNativeReference = StringUtils.parseBoolean(properties.getProperty("useReferenceAsNativeReference","false"));
            if(nativeReferenceGeneratedByDatabaseSequence)
			{
				referenceGenerator = PluginManager.loadPlugin(properties.getSubProperties("uciplink.reference_generator."));
				referenceGenerator.initializeReference(properties.getProperty("uciplink.reference_generator.node_id", "01"), null);
			}
		} catch (ERSConfigurationException e) {
			logger.error("Error reading reference generator properties.", e);
		}
		try {
			useInputReferenceNumberEnabled = properties.getBooleanProperty("use_input_reference_number_enabled",false);
			useInputReferenceNumberOldprefix = properties.getProperty("use_input_reference_number_oldprefix","BD");
			useInputReferenceNumberNewprefix = properties.getProperty("use_input_reference_number_newprefix","12");
		} catch (ERSConfigurationException e) {
			logger.error("Error reading reference generator properties.", e);
		}
	}

	private class LogSettingsLoader implements ConfigurationFileHandler
	{
		public void loadConfiguration(File file) throws ERSConfigurationException
		{
//			LogManager.resetConfiguration();
//			PropertyConfigurator.configure(file.getAbsolutePath());
		}
	}

	public UCIPLinkConfig(
			String name,
			boolean autostart) throws ERSConfigurationException
	{
		super(name,
				autostart);
		addFile("log4j2.xml", new LogSettingsLoader());
		addModulePropertiesHandler(null, new PropertyLoader());

		if (autostart)
		{
			addFile("jetm-config.xml", new ConfigurationFileHandler()
			{
				public void loadConfiguration(File file) throws ERSConfigurationException
				{
					loadJETM(file);
				}
			});

			businessRuleDispatcher = new RuleRequestHandler(this);
			if(enableDataBundle)
			{
				offerRequestDispatcher = new OfferRequestHandler(this, mappingFileName);
			}
		}
	}

	void loadJETM(File jetmConfigFile) throws ERSConfigurationException
	{
		logger.info("Loading JETM config");
		if (jetmConfigFile != null)
			XmlEtmConfigurator.configure(jetmConfigFile);

	}

	public void stop()
	{
		setRunning(false);
		this.notifyAll();
	}

	public void formatMsisdn(Account account)
	{
		if (account != null)
		{
			account.setAccountId(formatMsisdn(account.getAccountId()));
		}
	}

	public String formatMsisdn(String msisdn)
	{
		return numberFormatter.formatMsisdn(msisdn);
	}

	public String getLanguageInISO6391(Integer languageIdCurrent)
	{
		String result;
		int language = 1;
		if (languageIdCurrent == null)
		{
			logger.info("LanguageIdCurrent is not set in the CS");
		}
		else
		{
			language = languageIdCurrent.intValue();
		}

		switch (language)
		{

			case 2:
				result = ucipOperatorSpecificLanguage2;
				break;

			case 3:
				result = ucipOperatorSpecificLanguage3;
				break;

			case 4:
				result = ucipOperatorSpecificLanguage4;
				break;

			case 1:
			default:
				result = ucipOperatorSpecificLanguage1;
				break;

		}

		return result;
	}

	public String getServicesURL()
	{
		return servicesURL;
	}

	public String getOperationsURL()
	{
		return operationsURL;
	}

	public String getManagementURL()
	{
		return managementURL;
	}

	public UCIPAdaptor getUcipAdaptor()
	{
		return ucipAdaptor;
	}

	public boolean isRunning()
	{
		return running;
	}

	public void setRunning(boolean running)
	{
		this.running = running;
	}

	public String getRefillProfileID()
	{
		return refillProfileID;
	}

	public FlowControlHandler getFlowControlHandler()
	{
		return flowControlHandler;
	}

	public String getBusinessLogicRuleURL()
	{
		return businessRuleURL;
	}

	public RuleRequestHandler getBusinessRuleDispatcher()
	{
		return businessRuleDispatcher;
	}
	public Map getOfferListMap() 
	{
		return offerRequestDispatcher.getOfferListMap();
	}
	public List<String> getMappedSKUList() {
		return offerRequestDispatcher.getMappingSKUList();
	}

	public String getUcipOriginTransactionId()
	{
		return ucipOriginTransactionId;
	}

	public ExtendedProperties getUcipProperties()
	{
		return ucipProperties;
	}

	public boolean isAllowSelfTopup()
	{
		return allowSelfTopup;
	}

	public String getLinkTypeId(int sc)
	{
		logger.debug("fetching link type ID for SC: " + sc);
		Interval interval = linkTypeList.findInterval(sc);
		if (interval == null)
		{
			logger.debug("no link type ID found!!!");
			return null;
		}
		logger.debug("link type ID found: " + interval.getIdentity());
		return interval.getIdentity();
	}

	public Object createServerConnection(String address) throws Exception
	{
		try
		{
			if (address != null)
			{
				ucipProperties.setProperty("serverURL", address);
			}
			ucipClient = new UCIPClient(ucipProperties);

			ucipClient.setServerURL(address);

			return ucipClient;
		}
		catch (UCIPException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public ServerSelector<UCIPClient> getServerSelector()
	{
		return serverSelector;
	}

	/**
	 * Create NativeReference from Reference length of which is not more than 20
	 * 
	 * @param reference
	 *            reference code in <yyyyMMddHHmmssSSS>+<NN>+<CCCCCC> format
	 *            where <NN> is the node id and <CCCCCC> is a 6-digit sequence
	 *            counter
	 * @return String - NativeReference (TIME + node_id + CCCCCC.substring(1,5))
	 */
	public String createNativeReference(String reference)
	{
		logger.debug("Creating Native Reference " + reference);
        String nativeReference = "";

		if(useInputReferenceNumberEnabled){
			nativeReference = reference.replace(useInputReferenceNumberOldprefix,useInputReferenceNumberNewprefix);
		}
		else if(nativeReferenceGeneratedByDatabaseSequence == false)
		{
            Date originTimeStamp = new Date();
            nativeReference = String.valueOf(originTimeStamp.getTime());
            if (nativeReferenceLengthCheck && reference != null && !reference.equals(""))
                nativeReference += nodeId + reference.substring(21, 25);
            if (nativeReferenceReverseDigits) {

                StringBuilder nativeRef = new StringBuilder(nativeReference);
                nativeReference = nativeRef.reverse().toString();
            }
        }
		else if (useReferenceAsNativeReference)
		{
			nativeReference = reference;
		}
        else
        {
            nativeReference = "";
            try {

                nativeReference = referenceGenerator.getNextReference();
                logger.debug("DatabaseSequenceReferenceGenerated nativeReference = " + nativeReference);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return nativeReference;
	}

	/**
	 * Returns the account status based on serviceFeeExpiryDate and
	 * supervisionExpiryDate compared to the current time.
	 * <p>
	 * serviceFeeExpiryDate is before current time implies that the account is
	 * totally de-activated.
	 * <p>
	 * supervisionExpirtyDate is before current time implies that the account is
	 * in the grace period and can receive sms and calls.
	 * 
	 * @param serviceFeeExpiryDate
	 *        the service fee expiry date
	 * @param supervisionExpiryDate
	 *        the supervision expiry date
	 * 
	 * @return the account status as either Active, Disabled, or Suspended.
	 */
	public AccountStatus getAccountStatus(Date serviceFeeExpiryDate, Date supervisionExpiryDate)
	{
		// TODO: Make configurable from property file

		Date currentTime = new Date(System.currentTimeMillis());

		logger.debug("Mapping account status from serviceFeeExpiryDate=" + serviceFeeExpiryDate + " and supervisionExpiryDate="
				+ supervisionExpiryDate + " using current time=" + currentTime);

		logger.debug("Mapping account status on milliseconds granularity: " + (matchAccountStatusExactTime ? "yes" : "no"));

		// Check if the account has been activated (and thus has been assigned
		// an end date for service fee.

		if (serviceFeeExpiryDate == null)
		{
			logger.debug("Account status mapped to pending as serviceFeeExpiryDate is not set");
			return AccountStatus.Pending;
		}

		// Check if the account is totally de-activated.

		if (!isLater(serviceFeeExpiryDate, currentTime))
		{
			logger.debug("Account status mapped to disabled as serviceFeeExpiryDate is not after current time");
			return AccountStatus.Disabled;
		}

		// Check if the account is in grace period where the user can receive
		// SMS and calls or is not set yet.
		if (supervisionExpiryDate == null || !isLater(supervisionExpiryDate, currentTime))
		{
			logger.debug("Account status mapped to suspended as supervisionExpriryDate is not after current time");
			return AccountStatus.Suspended;
		}

		return AccountStatus.Active;
	}

	private boolean isLater(Date date1, Date date2)
	{
		if (matchAccountStatusExactTime)
		{
			return date1.after(date2);
		}
		else
		{
			return isLaterDay(date1, date2);
		}
	}

	/**
	 * Compares two dates on whole days.
	 * 
	 * @param date1
	 *        the first date to compare
	 * @param date2
	 *        the second date to compare
	 * @return true if date1 is greater than date2 on a whole day.
	 */
	public static boolean isLaterDay(Date date1, Date date2)
	{
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date1);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);

		if (cal1.get(Calendar.ERA) != cal2.get(Calendar.ERA))
			return false;

		if (cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR))
			return true;

		if (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR))
			return false;

		return cal1.get(Calendar.DAY_OF_YEAR) > cal2.get(Calendar.DAY_OF_YEAR);

	}

	/**
	 * Formats a given date according to the configuration.
	 * 
	 * @param date
	 *        the given date
	 * @return returns the formatted date string.
	 */
	public String formatResponseDate(Date date)
	{
		return responseDateFormatter.format(date);
	}

	/**
	 * Returns the extra field name for a given dedicated account id for refill
	 * response refill value total dedicated account id.
	 * 
	 * @param daId
	 *        the dedicated account id.
	 * @return a mapped name if successful, otherwise null.
	 */
	public String getRefillValueTotalDAName(Integer daId)
	{
		// refillValueTotalDANameMap

		if (refillValueTotalDANameMap.get(daId) == null || refillValueTotalDANameMap.get(daId).length() == 0)
		{
			logger.debug("Refill value total dedicated account extra field mapping is missing for dedicated account: " + daId);
			return null;
		}

		return refillValueTotalDANameMap.get(daId);
	}

	/**
	 * Returns the extra field name for a given dedicated account id for refill
	 * response refill value promotion dedicated account id.
	 * 
	 * @param daId
	 *        the dedicated account id.
	 * @return a mapped name if successful, otherwise null.
	 */
	public String getRefillValuePromotionDAName(Integer daId)
	{
		if (refillValuePromotionDANameMap.get(daId) == null || refillValuePromotionDANameMap.get(daId).length() == 0)
		{
			logger.debug("Refill value promotion dedicated account extra field mapping is missing for dedicated account: " + daId);
			return null;
		}

		return refillValuePromotionDANameMap.get(daId);
	}
	
	public boolean isReverseTopupWantBalance()
	{
		return reverseTopupWantBalance;
	}

	public TopupProductSettings getTopupProductSettings(String productId)
	{
		TopupProductSettings settings = topupProductSettingsMap.get(productId);
		if (settings == null)
		{
			settings = topupProductSettingsMap.get("default");
		}

		return settings;
	}

	public boolean isFetchBalanceAfter()
	{
		return fetchBalanceAfter;
	}

	public Boolean isValidateSubscriberLocation()
	{
		return validateSubscriberLocation;
	}
	
	public boolean isTransformCurrencyEnabled() {
		return isTransformCurrencyEnabled;
	}

	public String getTransformCurrencyCode()
	{
		return transformCurrencyCode;
	}

	public boolean isOriginOperatorIDRefillEnable() {
		return originOperatorIDRefillEnable;
	}

	public boolean isUpdateOffer2CurrencyEnable() {
		return updateOffer2CurrencyEnable;
	}

	public boolean isTransactionTypeRefillEnable() {
		return transactionTypeRefillEnable;
	}
	
	public boolean isNegotiatedCapabilitiesRefillEnable() {
		return negotiatedCapabilitiesRefillEnable;
	}

	public boolean isNegotiatedCapabilitiesUpdateOfferEnable()
	{
		return negotiatedCapabilitiesUpdateOfferEnable;
	}

	public boolean isNegotiatedCapabilitiesGetBalanceAndDateEnable()
	{
		return negotiatedCapabilitiesGetBalanceAndDateEnable;
	}

	public boolean isNegotiatedCapabilitiesUpdateAccumulatorsEnable()
	{
		return negotiatedCapabilitiesUpdateAccumulatorsEnable;
	}

	public boolean isTreeDefinedFieldRefillEnable() {
		return treeDefinedFieldRefillEnable;
	}
	
	public boolean isOriginOperatorIDUpdateBalanceEnable() {
		return originOperatorIDUpdateBalanceEnable;
	}
	
	public boolean isExternalData1enable() {
		return externalData1enable;
	}

	public boolean isExternalData4enable()
	{
		return externalData4enable;
	}

	public Boolean getEnableDataBundle()
	{
		return enableDataBundle;
	}

	public void loadMappingFileProperties(ExtendedProperties properties, Boolean enableDataBundle)
	{
		if (enableDataBundle)
		{
			mappingFileName = properties.getProperty("mappingFileName", "vas_mapping.properties");
		}
	}

	public ExtendedProperties getExternalData1Properties() {
		return externalData1Properties;
	}

	public List getExcludUpdateOfferlist()
	{
		return excludUpdateOfferlist;
	}

	public List<String> getCheck4GChannelList()
	{
		return check4GChannelList;
	}

	public Boolean getEnabled4GDataBundleCalls()
	{
		return enabled4GDataBundleCalls;
	}

	public void loadDataBundleProperties(ExtendedProperties properties)
	{
		ExtendedProperties dataBundleProperties = properties.getSubProperties("data_bundles.");
		ExtendedProperties dataBundle4GProperties = dataBundleProperties.getSubProperties("4g.");

		enableDataBundle = StringUtils.parseBoolean(dataBundleProperties.getProperty("enabled", "false"));
		excludUpdateOfferlist = dataBundleProperties.getStringListProperty("exculde_update_offer_calls", new ArrayList<String>(), ",");
		enabled4GDataBundleCalls = StringUtils.parseBoolean(dataBundle4GProperties.getProperty("enabled", "false"));
		check4GChannelList = dataBundle4GProperties.getStringListProperty("channels", new ArrayList<String>(), ",");
		bundleNameList = dataBundleProperties.getStringListProperty("name_list", new ArrayList<String>(), ",");
		loadMappingFileProperties(dataBundleProperties, enableDataBundle);
	}

	/**
	 * @return the fafIndicator
	 */
	public int getFafIndicator() {
		return fafIndicator;
	}

	public boolean isProfileBasedExternalData1Enable() {
		return profileBasedExternalData1Enable;
	}

	public void setProfileBasedExternalData1Enable(boolean profileBasedExternalData1Enable) {
		this.profileBasedExternalData1Enable = profileBasedExternalData1Enable;
	}

	public List<String> getBundleNameList()
	{
		return bundleNameList;
	}

	public void setBundleNameList(List<String> bundleNameList)
	{
		this.bundleNameList = bundleNameList;
	}

	public ExtendedProperties getExternalData2Properties()
	{
		return externalData2Properties;
	}

	public void setExternalData2Properties(ExtendedProperties externalData2Properties)
	{
		this.externalData2Properties = externalData2Properties;
		externalData2ExtraParamsReset = new ArrayList<String>();
		if(externalData2Properties != null){
			String profiles = externalData2Properties.getProperty("override_extraparams.profiles");
			if(profiles != null){
				externalData2ExtraParamsReset = Arrays.asList(profiles.split("\\s*,\\s*"));
			}
		}

	}

	public void setExternalData3Properties(ExtendedProperties externalData3Properties)
	{
		this.externalData3Properties = externalData3Properties;
		externalData3ExtraParamsReset = new ArrayList<String>();
		if(externalData3Properties != null){
			String profiles = externalData3Properties.getProperty("override_extraparams.profiles");
			if(profiles != null){
				externalData3ExtraParamsReset = Arrays.asList(profiles.split("\\s*,\\s*"));
			}
		}

	}

	public void setExternalData4Properties(ExtendedProperties externalData4Properties)
	{
		this.externalData4Properties = externalData4Properties;
		externalData4ExtraParamsReset = new ArrayList<String>();
		if(externalData4Properties != null){
			String profiles = externalData4Properties.getProperty("override_extraparams.profiles");
			if(profiles != null){
				externalData4ExtraParamsReset = Arrays.asList(profiles.split("\\s*,\\s*"));
			}
		}

	}

	public boolean isExternalData2enable()
	{
		return externalData2enable;
	}

	public boolean isProfileBasedExternalData2Enable()
	{
		return profileBasedExternalData2Enable;
	}
	
	public boolean isExternalData3enable()
	{
		return externalData3enable;
	}

	public boolean isChannelBasedExternalData3Enable()
	{
		return channelBasedExternalData3Enable;
	}

	/**
	 * @return the setExternalData2AsERSReference
	 */
	public boolean isSetExternalData2AsERSReference() {
		return setExternalData2AsERSReference;
	}

	/**
	 * @return the simulateAccountInformation
	 */
	public boolean isSimulateAccountInformation()
	{
		return simulateAccountInformation;
	}

	/**
	 * @return the simulateAccountInformation
	 */
	public String getDefaultCurreny()
	{
		return defaultCurreny;
	}

	public List<String> getExternalData1ExtraParamsReset()
	{
		return externalData1ExtraParamsReset;
	}

	public List<String> getExternalData2ExtraParamsReset()
	{
		return externalData2ExtraParamsReset;
	}

	public List<String> getExternalData3ExtraParamsReset()
	{
		return externalData3ExtraParamsReset;
	}

	public List<String> getExternalData4ExtraParamsReset()
	{
		return externalData4ExtraParamsReset;
	}

	public HashMap<String, String> getRefillProfileIdMap()
	{
		return refillProfileIdMap;
	}

	public TreeDefinedFieldValue getTreeDefinedFieldValue(Integer index)
	{
		TreeDefinedFieldValue value = treeDefinedFieldValueMap.get(index);
		
		return value;
	}
	
	

	public HashMap<Integer, TreeDefinedFieldValue> getTreeDefinedFieldValueMap() {
		return treeDefinedFieldValueMap;
	}

	public void setTreeDefinedFieldValueMap(HashMap<Integer, TreeDefinedFieldValue> treeDefinedFieldValueMap) {
		this.treeDefinedFieldValueMap = treeDefinedFieldValueMap;
	}

	public boolean isPopulateResponseWithTreeDefinedFields()
	{
		return populateResponseWithTreeDefinedFields;
	}

	public String getTreeDefinedFieldsResponsePrefix()
	{
		return treeDefinedFieldsResponsePrefix;
	}

    public boolean isNativeReferenceGeneratedByDatabaseSequence() {
        return nativeReferenceGeneratedByDatabaseSequence;
    }

    public void setNativeReferenceGeneratedByDatabaseSequence(boolean nativeReferenceGeneratedByDatabaseSequence) {
        this.nativeReferenceGeneratedByDatabaseSequence = nativeReferenceGeneratedByDatabaseSequence;
    }

    public boolean isEnableNativeCodeMapping() { return enableNativeCodeMapping; }

	public boolean isEnableSuccessResponseOnTimeout() {
		return enableSuccessResponseOnTimeout;
	}

	public Map<String,String> getNativeCodeMapping() { return nativeErrorCodeMap; }

	public boolean isEnableConfigurablePrimaryErrorCodes() { return enableConfigurablePrimaryErrorCodes; }

	public HashMap<Integer, ResultRow> getConfigurablePrimaryErrorCodesMap()
	{
		return configurablePrimaryErrorCodesMap;
	}

	public boolean isEnableTranslateSecondaryErrorCodes()
	{
		return enableTranslateSecondaryErrorCodes;
	}

	public boolean isEnableConfigurableSecondaryErrorCodes()
	{
		return enableConfigurableSecondaryErrorCodes;
	}

	public ValidateTopupMethod getValidateTopupMethod()
	{
		return validateTopupMethod;
	}

	public GetAccountInformationMethod getAccountInformationMethod()
	{
		return getAccountInformationMethod;
	}

	public HashMap<Integer, ResultRow> getConfigurableSecondaryErrorCodesMap()
	{
		return configurableSecondaryErrorCodesMap;
	}

	public Integer getGetBalanceAndDateDASFirstId()
	{
		return getBalanceAndDateDASFirstId;
	}

	public Integer getGetBalanceAndDateDASLastId()
	{
		return getBalanceAndDateDASLastId;
	}

	public boolean isSetExternalData1AsERSReference() { return setExternalData1AsERSReference; }

	public ExpireFormat getUpdateOfferExpiryFormat()
	{
		return updateOfferExpiryFormat;
	}
	public boolean isReturnMockResponse() { return returnMockResponse; }

	public boolean isUseInputReferenceNumberEnabled() {
		return useInputReferenceNumberEnabled;
	}

	public void setUseInputReferenceNumberEnabled(boolean useInputReferenceNumberEnabled) {
		this.useInputReferenceNumberEnabled = useInputReferenceNumberEnabled;
	}

	public boolean isSubordinateDedicatedAccountDetailsFlagEnable() { return subordinateDedicatedAccountDetailsFlagEnable; }

	public boolean getSubordinateDedicatedAccountDetailsFlagValue() { return subordinateDedicatedAccountDetailsFlagValue; }

	public boolean isRequestRefillAccountBeforeFlagEnable() { return requestRefillAccountBeforeFlagEnable; }

	public boolean getRequestRefillAccountBeforeFlagValue() { return requestRefillAccountBeforeFlagValue; }

	public String getSourceMultiPlierKeyName() { return sourceMultiPlierKeyName; }

	public boolean isEnableSourceMultiPlier() { return enableSourceMultiPlier; }
	public String getProductSelectionKey() {
		return productSelectionKey;
	}

	public void setProductSelectionKey(String productSelectionKey) {
		this.productSelectionKey = productSelectionKey;
	}

	public boolean isEnableMessageCapabilityFlag() {
		return enableMessageCapabilityFlag;
	}

	public void setEnableMessageCapabilityFlag(boolean enableMessageCapabilityFlag) {
		this.enableMessageCapabilityFlag = enableMessageCapabilityFlag;
	}

	public MessageCapabilityFlag getMessageCapabilityFlag() {
		return messageCapabilityFlag;
	}

	public void setMessageCapabilityFlag(MessageCapabilityFlag messageCapabilityFlag) {
		this.messageCapabilityFlag = messageCapabilityFlag;
	}

	public boolean isFetchBalanceBeforeInGetBalanceAndDate() {
		return fetchBalanceBeforeInGetBalanceAndDate;
	}

	public boolean isReturnOfferInformation()
	{
		return returnOfferInformation;
	}

	public void setFetchBalanceBeforeInGetBalanceAndDate(boolean fetchBalanceBeforeInGetBalanceAndDate) {
		this.fetchBalanceBeforeInGetBalanceAndDate = fetchBalanceBeforeInGetBalanceAndDate;
	}

	public boolean isUseCustomTransactionId()
	{
		return useCustomTransactionId;
	}

	public void setUseCustomTransactionId(boolean useCustomTransactionId)
	{
		this.useCustomTransactionId = useCustomTransactionId;
	}

	public String getCustomOriginOperatorIdExtraFieldName()
	{
		return customOriginOperatorIdExtraFieldName;
	}

	public void setCustomOriginOperatorIdExtraFieldName(String customOriginOperatorIdExtraFieldName)
	{
		this.customOriginOperatorIdExtraFieldName = customOriginOperatorIdExtraFieldName;
	}

	public String getCustomTransactionIdExtraFieldName()
	{
		return customTransactionIdExtraFieldName;
	}

	public void setCustomTransactionIdExtraFieldName(String customTransactionIdExtraFieldName) {
		this.customTransactionIdExtraFieldName = customTransactionIdExtraFieldName;
	}

	public boolean isNegotiatedCapabilitiesDeleteOfferEnable()
	{
		return negotiatedCapabilitiesDeleteOfferEnable;
	}

	public void setNegotiatedCapabilitiesDeleteOfferEnable(boolean negotiatedCapabilitiesDeleteOfferEnable)
	{
		this.negotiatedCapabilitiesDeleteOfferEnable = negotiatedCapabilitiesDeleteOfferEnable;
	}

	public boolean isOverrideRequestParamsInTopUp() {
		return overrideRequestParamsInTopUp;
	}

	public List<String> getOverrideParamKeys() {
		return overrideParamKeys;
	}
}
