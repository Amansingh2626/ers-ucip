package com.seamless.ers.links.uciplink.ucip;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.seamless.common.uciplib.common.*;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.config.ResultCodesResolver;
import com.seamless.ers.links.uciplink.config.ResultRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.common.uciplib.v31.responses.UpdateOfferResponse;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;

public class UCIPResponse
{
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	public static final int SUCCESS			 = 0;
	public static final int XML_RPC_FAILURE  = 1;
	public static final int INTERNAL_FAILURE = 2;
	
	/*
	 * Response returned by UCIP Link
	 */
	int 	ucipResponseCode 		= -1;
	
	/* Balance of account after refill / adjust  */
	Amount	balanceAfter 			= null;
	/* Balance of account before refill / adjust  */
	Amount	balanceBefore 			= null;
	/* Expiry date of account after refill/adjust */
	Date serviceFeeExpiryDateAfter = null;
	/* Expiry date of account before refill/adjust */
	Date serviceFeeExpiryDateBefore = null;
	/* Current balance of account, for those 
	   functions which does not require a change in the account balance */
	Amount 	balance					= null;
	
	/* Actual (default) Service Class of the Account */
	int 	defaultServiceClass		= -1;
	
	/* Current Service Class of the Account */ 
	int 	currentServiceClass 	= -1;
	
	/* Response Code for ERS */
	int 	ersResponseCode 		= -1;

	String secondaryNativeResultCode = "";
	
	/* dedicatedAccountValues after refill*/
	java.util.HashMap<Integer, Long> dedicatedAccountValuesAfter = new HashMap<Integer, Long>();
	
	
	/* dedicatedAccountValues before refill*/
	java.util.HashMap<Integer, Long> dedicatedAccountValuesBefore = new HashMap<Integer, Long>();
	
	/* dedicated account values in refill value total */
	java.util.HashMap<Integer, Long> dedicatedAccountRefillValueTotal = new HashMap<Integer, Long>();
	
	/* dedicated account values in refill value promotion */
	java.util.HashMap<Integer, DedicatedAccountRefillInformation> dedicatedAccountRefillValuePromotion = new HashMap<Integer, DedicatedAccountRefillInformation>();
	
	/* dedicated account values in refill value promotion */
	java.util.HashMap<String, DedicatedAccountChangeInformation> dedicatedAccountChangeInformation = new HashMap<String, DedicatedAccountChangeInformation>();
	
	/* Friends and family list associated with the account*/
	FafInformation[] fafList		= null;
	
	/* Service offering flags for the account */
	ServiceOffering[] serviceOfferings = null;
	
	/* cs languageIDCurrent converted to ISO6391 */
	String languageInISO6391 = "en";

	/* supervision expiry date for get account details */
	Date supervisionExpiryDate;

	/* service fee expiry date for get account details */
	Date serviceFeeExpiryDate;

	/**
	 * Supervision expiry date from accountBeforeRefill in RefillResponse
	 */
	Date supervisionExpiryDateBefore;
	
	/**
	 * Supervision expiry date from accountAfterRefill in RefillResponse
	 */
	Date supervisionExpiryDateAfter;
	
	/* Account flags */
	AccountFlags accountFlags;
	
	/**
	 * List of PAM information for the subscriber
	 */
	PamInformation[] pamInformationList;
	
	/**
	 * A flag indicating whether the subscriber and operator has access to subscriber and account data
	 * 
	 */
	
	Boolean temporaryBlockedFlag;
	
	private UpdateOfferResponse updateOfferResponse;
	
	/* Service class for the account */
	String segment;

	Map<String, TreeDefinedFieldInformation> treeDefinedFieldInformationMap;

	private OfferInformation[] offerInformation;

	Amount transactionAmount;

	private String info5;

	private String info7;

	public String getSegment() {
		return segment;
	}

	public void setSegment(String segment) {
		this.segment = segment;
	}

	/**
	 * Default constructor
	 */
	public UCIPResponse()
	{
		
	}
	
	/**
	 * Overloaded constructor with error code from UCIPAdaptor.
	 * @param ersResponseCode initial response code
	 */
	public UCIPResponse(int ersResponseCode)
	{
		setErsResponseCode(ersResponseCode);		
	}

	public
	int translateUcipResponseCode(int ucipCode)
	{
		int res;
		
		switch (ucipCode)
		{
		case 0:	// success
		case 1: // OK but supervision period exceeded
		case 2: // OK but service fee period exceeded
			res = ERSWSLinkResultCodes.SUCCESS;
			break;
		case 102:	// Subscriber not found
			res = ERSWSLinkResultCodes.ACCOUNT_NOT_FOUND;
			break;
		case 103:	// Account barred from refill
		case 104:	// Temporary blocked
			res = ERSWSLinkResultCodes.ACCOUNT_BLOCKED;
			break;
		case 106:	// Dedicated account negative
		case 124:	// Below minimum balance
			res = ERSWSLinkResultCodes.INSUFFICIENT_CREDIT;
			break;
		case 117:	// Service class change not allowed
		case 155:	// Invalid new service class
			res = ERSWSLinkResultCodes.INVALID_ACCOUNT_CLASS_CHANGE;
			break;
		case 123:	// Max credit limit exceeded
		case 153:	// Dedicated account max credit limit exceeded
			res = ERSWSLinkResultCodes.CREDIT_TOO_HIGH;
			break;
		case 126:	// Account not active
			res = ERSWSLinkResultCodes.INVALID_ACCOUNT_STATUS;
			break;
		case 100:	// Other error
		case 105:	// Dedicated account not allowed
		case 115:	// Refill not accepted
		case 120:	// Invalid refill profile
		case 121:	// Supervision period too long
		case 122:	// Service fee period too long
		case 127:	// Accumulator not available
		case 129:	// FAF number doesnt exist
			res = ERSWSLinkResultCodes.FAF_NUMBER_NOT_EXISTS;
			break;
		case 130:	// FAF number not allow
			res = ERSWSLinkResultCodes.INVALID_FAF_NUMBER;
			break;
		case 134:	// Accumulator overflow
		case 135:	// Accumulator underflow
		case 136:	// Date adjustment error
		case 137:	// Get date and balance not allowed
		case 139:	// Dedicated account not defined
		case 140:	// Invalid old service class
		case 154:	// Invalid old service class date
		case 160:	// operation not allowed from current location
		case 161:	// Failed to get location information
		case 999:	// other error no retry
			res = ERSWSLinkResultCodes.LINK_ERROR;
			break;
			
		default:
			log.warn("Got unknown UCIP result code " + ucipCode + ", translated it to LINK_ERROR");
			res = ERSWSLinkResultCodes.LINK_ERROR;
			break;
		}

		return res;
	}
	
	public int translateSecondaryUcipResponseCode(int secondaryUcipCode)
	{
		int res;

		switch (secondaryUcipCode)
		{
			case 9000:
				res = 1565;
				break;
			case 8001:
				res = 90;
				break;
			default:
				log.warn("Got unknown secondary UCIP result code " + secondaryUcipCode + ", translated it to LINK_ERROR");
				res = ERSWSLinkResultCodes.LINK_ERROR;
		}

		return res;
	}
	
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("[ ");
		sb.append("ucipResponseCode: "+ucipResponseCode);
		sb.append(" , balanceAfter: "+balanceAfter);
		sb.append(" , balanceBefore: "+balanceBefore);
		sb.append(" , expiryDateBefore: "+ serviceFeeExpiryDateBefore);
		sb.append(" , expiryDateAfter: "+ serviceFeeExpiryDateAfter);
		sb.append(" , balance: "+balance);
		sb.append(" , defaultServiceClass: "+defaultServiceClass);
		sb.append(" , currentServiceClass: "+currentServiceClass);
		sb.append(" , ersResponseCode: "+ersResponseCode);
		sb.append(" , dedicatedAccountValuesBefore: "+dedicatedAccountValuesBefore.toString());
		sb.append(" , dedicatedAccountValuesAfter: "+dedicatedAccountValuesAfter.toString());
		sb.append(" , dedicatedAccountRefillValueTotalValues: " + dedicatedAccountRefillValueTotal.toString());
		sb.append(" , dedicatedAccountRefillValuePromotionValues: " + dedicatedAccountRefillValuePromotion.toString());
		sb.append(" , segment: " + segment);
		sb.append(" , info5: " + info5);
		sb.append(" , info7: " + info7);
		sb.append(" ]");
		return sb.toString();
	}
	
	
	public boolean isSuccessful()
	{
		return ersResponseCode == ERSWSLinkResultCodes.SUCCESS;
	}
	
	public int getUcipResponseCode()
	{
		return ucipResponseCode;
	}
	
	public void setUcipResponseCode(int ucipResponseCode)
	{
		this.ucipResponseCode = ucipResponseCode;
		int ersCode;

		try
		{
			UCIPLinkConfig ucipLinkConfig = (UCIPLinkConfig) UCIPLinkConfig.getInstance();
			boolean configurablePrimaryErrorCodesEnabled = ucipLinkConfig.isEnableConfigurablePrimaryErrorCodes();
			boolean translateSecondaryErrorCodesEnabled = ucipLinkConfig.isEnableTranslateSecondaryErrorCodes();
			boolean configurableSecondaryErrorCodesEnabled = ucipLinkConfig.isEnableConfigurableSecondaryErrorCodes();
			ResultCodesResolver resultCodesResolver = new ResultCodesResolver();

			Map<String, TreeDefinedFieldInformation> treeDefinedFieldInformationMap = this.getTreeDefinedFieldInformationMap();
			if (translateSecondaryErrorCodesEnabled && treeDefinedFieldInformationMap != null && treeDefinedFieldInformationMap.get("SResponseCode") != null)
			{
				TreeDefinedFieldInformation treeDefinedFieldInformation = treeDefinedFieldInformationMap.get("SResponseCode");
				String secondaryResultCodeString = treeDefinedFieldInformation.getTreeDefinedFieldValue();
				setSecondaryNativeResultCode(secondaryResultCodeString);
				if (secondaryResultCodeString == null || secondaryResultCodeString.isEmpty() || secondaryResultCodeString.equals("0"))
				{
					// business as usual
					setErsResponseCode(translateUcipResponseCode(ucipResponseCode));
				}
				else
				{
					int secondaryResultCode = new Integer(secondaryResultCodeString);
					if (configurableSecondaryErrorCodesEnabled)
					{
						HashMap<Integer, ResultRow> configurableSecondaryErrorCodesMap = ucipLinkConfig.getConfigurableSecondaryErrorCodesMap();
						ersCode = resultCodesResolver.getMapping(secondaryResultCode, configurableSecondaryErrorCodesMap);
					}
					else
					{
						ersCode = translateSecondaryUcipResponseCode(secondaryResultCode);
					}
					setErsResponseCode(ersCode);

				}
			}
			else if (configurablePrimaryErrorCodesEnabled)
			{
				HashMap<Integer, ResultRow> configurablePrimaryErrorCodesMap = ucipLinkConfig.getConfigurablePrimaryErrorCodesMap();
				ersCode = resultCodesResolver.getMapping(ucipResponseCode, configurablePrimaryErrorCodesMap);
				setErsResponseCode(ersCode);
			}
			else
			{
				setErsResponseCode(translateUcipResponseCode(ucipResponseCode));
			}
		}
		catch (Exception e)
		{
			log.debug("Exception caught while trying to use configured error codes from file");
			setErsResponseCode(translateUcipResponseCode(ucipResponseCode));
		}
	}

	private void setSecondaryNativeResultCode(String ersCode)
	{
		this.secondaryNativeResultCode = ersCode;
	}

	public int getErsResponseCode()
	{
		return ersResponseCode;
	}
	
	public void setErsResponseCode(int ersResponseCode)
	{
		this.ersResponseCode = ersResponseCode;
	}

	public Amount getBalanceAfter()
	{
		return balanceAfter;
	}

	public void setBalanceAfter(Amount balanceAfter)
	{
		this.balanceAfter = balanceAfter;
	}

	public Amount getBalance()
	{
		return balance;
	}

	public void setBalance(Amount balance)
	{
		this.balance = balance;
	}

	public int getDefaultServiceClass()
	{
		return defaultServiceClass;
	}

	public void setDefaultServiceClass(int defaultServiceClass)
	{
		this.defaultServiceClass = defaultServiceClass;
	}

	public int getCurrentServiceClass()
	{
		return currentServiceClass;
	}

	public void setCurrentServiceClass(int currentServiceClass)
	{
		this.currentServiceClass = currentServiceClass;
	}
	public Long getDedicatedAccountValueAfter(Integer dedicatedAccountId)
	{
		if(dedicatedAccountValuesAfter == null)
			return null;
		return dedicatedAccountValuesAfter.get(dedicatedAccountId);
		
	}
	public void setDedicatedAccountValueAfter(Integer dedicatedAccountId,Long value)
	{
		if(dedicatedAccountValuesAfter == null)
			dedicatedAccountValuesAfter = new HashMap<Integer, Long>();
		dedicatedAccountValuesAfter.put(dedicatedAccountId, value);
	}

	public Long getDedicatedAccountRefillValueTotal(Integer dedicatedAccountId)
	{
		return dedicatedAccountRefillValueTotal.get(dedicatedAccountId);
	}
	
	public HashMap<Integer, Long> getDedicatedAccountValuesAfterMap() {
		return dedicatedAccountValuesAfter;
	}

	public void setDedicatedAccountRefillValueTotal(Integer dedicatedAccountId, Long value)
	{
		if(dedicatedAccountRefillValueTotal == null)
			dedicatedAccountRefillValueTotal = new HashMap<Integer, Long>();
		dedicatedAccountRefillValueTotal.put(dedicatedAccountId, value);
	}

	public DedicatedAccountRefillInformation getDedicatedAccountRefillValuePromotion(DedicatedAccountRefillInformation dedicatedAccountId)
	{
		return dedicatedAccountRefillValuePromotion.get(dedicatedAccountId);
	}
	
	public void setDedicatedAccountRefillValuePromotion(Integer dedicatedAccountId, DedicatedAccountRefillInformation value)
	{
		if(dedicatedAccountRefillValuePromotion == null)
			dedicatedAccountRefillValuePromotion = new HashMap<Integer, DedicatedAccountRefillInformation>();
		dedicatedAccountRefillValuePromotion.put(dedicatedAccountId, value);
	}
	
	public HashMap<Integer, DedicatedAccountRefillInformation> getDedicatedAccountRefillValuePromotionMap() {
		return dedicatedAccountRefillValuePromotion;
	}
	
	public FafInformation[] getFafList()
	{
		return fafList;
	}

	public void setFafList(FafInformation[] fafList)
	{
		this.fafList = fafList;
	}

	public Amount getBalanceBefore()
	{
		return balanceBefore;
	}

	public void setBalanceBefore(Amount balanceBefore)
	{
		this.balanceBefore = balanceBefore;
	}

	public Date getServiceFeeExpiryDateAfter()
	{
		return serviceFeeExpiryDateAfter;
	}

	public void setServiceFeeExpiryDateAfter(Date date)
	{
		this.serviceFeeExpiryDateAfter = date;
	}

	public Date getServiceFeeExpiryDateBefore()
	{
		return serviceFeeExpiryDateBefore;
	}

	public void setServiceFeeExpiryDateBefore(Date date)
	{
		this.serviceFeeExpiryDateBefore = date;
	}

	public String getLanguageInISO6391()
	{
		return languageInISO6391;
	}

	public void setLanguageInISO6391(String languageInISO6391)
	{
		this.languageInISO6391 = languageInISO6391;
	}

	public void setSupervisionExpiryDate(Date supervisionExpiryDate)
	{
		this.supervisionExpiryDate = supervisionExpiryDate;
		
	}
	
	public Date getSupervisionExpiryDate()
	{
		return supervisionExpiryDate;
		
	}

	public void setServiceFeeExpiryDate(Date serviceFeeExpiryDate)
	{
		this.serviceFeeExpiryDate = serviceFeeExpiryDate;
	}
	
	public Date getServiceFeeExpiryDate()
	{
		return this.serviceFeeExpiryDate;
	}

	/**
	 * @return the supervisionExpiryDateBefore
	 */
	public Date getSupervisionExpiryDateBefore()
	{
		return supervisionExpiryDateBefore;
	}

	/**
	 * @param supervisionExpiryDateBefore the supervisionExpiryDateBefore to set
	 */
	public void setSupervisionExpiryDateBefore(Date supervisionExpiryDateBefore)
	{
		this.supervisionExpiryDateBefore = supervisionExpiryDateBefore;
	}

	public void setSupervisionExpiryDateAfter(Date date)
	{
		this.supervisionExpiryDateAfter = date;
	}
	
	public Date getSupervisionExpiryDateAfter()
	{
		return supervisionExpiryDateAfter;
	}

	public ServiceOffering[] getServiceOfferings() 
	{
		return serviceOfferings;
	}

	public void setServiceOfferings(ServiceOffering[] serviceOfferings) 
	{
		this.serviceOfferings = serviceOfferings;
	}

	public AccountFlags getAccountFlags() 
	{
		return accountFlags;
	}

	public void setAccountFlags(AccountFlags accountFlags) 
	{
		this.accountFlags = accountFlags;
	}

	public PamInformation[] getPamInformationList()
	{
		return pamInformationList;
	}

	public void setPamInformationList(PamInformation[] pamInformationList)
	{
		this.pamInformationList = pamInformationList;
	}

	public Boolean getTemporaryBlockedFlag()
	{
		return temporaryBlockedFlag;
	}

	public void setTemporaryBlockedFlag(Boolean temporaryBlockedFlag)
	{
		this.temporaryBlockedFlag = temporaryBlockedFlag;
	}

	public java.util.HashMap<Integer, Long> getDedicatedAccountValuesBefore()
	{
		return dedicatedAccountValuesBefore;
	}

	public void setDedicatedAccountValuesBefore(java.util.HashMap<Integer, Long> dedicatedAccountValuesBefore)
	{
		this.dedicatedAccountValuesBefore = dedicatedAccountValuesBefore;
	}
	public Long getDedicatedAccountValueBefore(Integer dedicatedAccountId)
	{
		if(dedicatedAccountValuesBefore == null)
			return null;
		return dedicatedAccountValuesBefore.get(dedicatedAccountId);
		
	}
	public void setDedicatedAccountValueBefore(Integer dedicatedAccountId,Long value)
	{
		if(dedicatedAccountValuesBefore == null)
			dedicatedAccountValuesBefore = new HashMap<Integer, Long>();
		dedicatedAccountValuesBefore.put(dedicatedAccountId, value);
	}
	
	public java.util.HashMap<Integer, Long> getDedicatedAccountValuesAfter()
	{
		return dedicatedAccountValuesAfter;
	}

	public java.util.HashMap<String, DedicatedAccountChangeInformation> getDedicatedAccountChangeInformation() {
		return dedicatedAccountChangeInformation;
	}

	public void setDedicatedAccountChangeInformation(
			java.util.HashMap<String, DedicatedAccountChangeInformation> dedicatedAccountChangeInformation) {
		this.dedicatedAccountChangeInformation = dedicatedAccountChangeInformation;
	}

	public UpdateOfferResponse getUpdateOfferResponse() {
		return updateOfferResponse;
	}

	public void setUpdateOfferResponse(UpdateOfferResponse updateOfferResponse) {
		this.updateOfferResponse = updateOfferResponse;
	}

	public Map<String,TreeDefinedFieldInformation> getTreeDefinedFieldInformationMap()
	{
		return treeDefinedFieldInformationMap;
	}

	public void setTreeDefinedFields(String treeDefinedFieldName, TreeDefinedFieldInformation treeDefinedFieldInformation)
	{
		if(treeDefinedFieldInformationMap == null)
			treeDefinedFieldInformationMap = new HashMap<String, TreeDefinedFieldInformation>();
		treeDefinedFieldInformationMap.put(treeDefinedFieldName, treeDefinedFieldInformation);
	}

	public String getSecondaryNativeResultCode()
	{
		return secondaryNativeResultCode;
	}

	public OfferInformation[] getOfferInformation() { return offerInformation; }

	public void setOfferInformation(OfferInformation[] offerInformation) { this.offerInformation = offerInformation; }

	public Amount getTransactionAmount() { return transactionAmount; }

	public void setTransactionAmount(Amount transactionAmount) { this.transactionAmount = transactionAmount; }

	public String getInfo5() {
		return info5;
	}

	public void setInfo5(String info5) {
		this.info5 = info5;
	}

	public String getInfo7() {
		return info7;
	}

	public void setInfo7(String info7) {
		this.info7 = info7;
	}
}
