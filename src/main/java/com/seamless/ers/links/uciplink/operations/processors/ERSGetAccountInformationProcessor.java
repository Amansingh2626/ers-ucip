package com.seamless.ers.links.uciplink.operations.processors;

import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.config.GetAccountInformationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.common.uciplib.common.ServiceOffering;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.AccountData;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountInformationResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;

public class ERSGetAccountInformationProcessor extends
		AbstractRequestProcessor<AccountInformationResponse>
{
	private static final Logger logger = LoggerFactory.getLogger(ERSGetAccountInformationProcessor.class);

	private UCIPLinkConfig config;
	private String principalId;

	private Account account;

	private String routingInfo;

	private boolean wantBalance;

	/*
	 * The native reference used to talk to the CS.
	 */
	private String nativeReference;

	@SuppressWarnings("unused")
	private String reference;

	ERSHashtableParameter extraFields;

	public ERSGetAccountInformationProcessor(UCIPLinkConfig config, String principalId,
			Account account, String routingInfo, boolean wantBalance,
			String reference, String nativeReference , ERSHashtableParameter extraFields)
	{
		super(new AccountInformationResponse());

		this.config = config;
		this.principalId = principalId;
		this.account = account;
		this.routingInfo = routingInfo;
		this.wantBalance = wantBalance;
		this.reference = reference;
		this.nativeReference = nativeReference;
		this.extraFields = extraFields;
	}

	public void process()
	{
		setResult(_process());
	}

	protected AccountInformationResponse _process()
	{
		this.getConfig().formatMsisdn(account);

		logger.debug("AccountInformation requested-Info [ principalId:" + principalId + ", account:"
				+ account + ", routingInfo:" + routingInfo + ", wantBalance:" + wantBalance + ", ERS reference:"+reference+", native reference:"+nativeReference+"]");

		AccountInformationResponse response = new AccountInformationResponse();

		UCIPResponse ucipResponse = null;

		if(config.isSimulateAccountInformation())
		{
			ucipResponse = new UCIPResponse();
			ucipResponse.setErsResponseCode(ERSWSLinkResultCodes.SUCCESS);
			Amount amount = new Amount(0, getConfig().getDefaultCurreny());
			ucipResponse.setBalance(amount);
			ucipResponse.setBalanceBefore(amount);
			ucipResponse.setBalanceAfter(amount);
		}
		else
		{
			if(getConfig().getAccountInformationMethod() == GetAccountInformationMethod.GET_ACCOUNT_DETAILS)
			{
				ucipResponse = getConfig().getUcipAdaptor().getInstance().getAccountDetails(account.getAccountId(), nativeReference, extraFields);
			}
			else if(getConfig().getAccountInformationMethod() == GetAccountInformationMethod.GET_BALANCE_AND_DATE)
			{
				ucipResponse = getConfig().getUcipAdaptor().getInstance().getBalance(account.getAccountId(), getConfig().getGetBalanceAndDateDASFirstId(),
						getConfig().getGetBalanceAndDateDASLastId(),
						nativeReference, extraFields);
			}

		}
		if (ucipResponse.isSuccessful())
		{
			response = getAccountInformationResponse(response, ucipResponse);
			return response;
		}
		else
		{
			logger.debug("Failed to retrieve account information!");
			response.setResultCode(ucipResponse.getErsResponseCode());
			return response;
		}

	}
	private AccountInformationResponse getAccountInformationResponse(AccountInformationResponse response, UCIPResponse ucipResponse)
	{
		if(getConfig().isTransformCurrencyEnabled()) {
			logger.debug(getConfig().getModuleProperties("locale.custom_currency.").toString());
			transformUcipResponseCurrency(ucipResponse,
					getConfig().getModuleProperties("locale.custom_currency.").keySet().toArray()[0].toString());
			logger.debug("Setting currency: " + ucipResponse);
		}

		AccountData accountData = new AccountData(account);
		accountData.setFields(new ERSHashtableParameter());

		accountData.getFields().put("accountClassId", String.valueOf(ucipResponse.getCurrentServiceClass()));

		accountData.setAccountClassId(String.valueOf(ucipResponse.getCurrentServiceClass()));

		// POC changes
		logger.debug("Segment from ucipAcccountDetailsResponse :"+ucipResponse.getSegment());
		if(ucipResponse.getSegment()!= null){
			accountData.getFields().put("segment", ucipResponse.getSegment());
		}

		accountData.setLanguageCode(ucipResponse .getLanguageInISO6391());
		accountData.setAccountLinkTypeId(getConfig().getLinkTypeId(ucipResponse.getCurrentServiceClass()));
		logger.debug("account link type ID set in accountData: " + accountData.getAccountLinkTypeId());
		accountData.setAccountExpiry(ucipResponse.getServiceFeeExpiryDate());
		accountData.setAccountClassExpiry(ucipResponse.getSupervisionExpiryDate());
		accountData.setStatus(getConfig().getAccountStatus(ucipResponse.getServiceFeeExpiryDate(),
				ucipResponse.getSupervisionExpiryDate()));

		ServiceOffering[] serviceOfferings = ucipResponse.getServiceOfferings();
		if (serviceOfferings != null)
		{
			logger.debug("Service offerings response");

			for (int indexOfferings = 0; indexOfferings < serviceOfferings.length; indexOfferings++)
			{
				if (serviceOfferings[indexOfferings].isServiceOfferingActiveFlag())
				{
					accountData.getFields().put("serviceOffering_" + serviceOfferings[indexOfferings].getServiceOfferingID(), "true");
				}
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
							.getSupervisionPeriodExpiryFlag().toString());
			accountData.getFields().put(
					"serviceFeePeriodExpiryFlag",
					ucipResponse.getAccountFlags()
							.getServiceFeePeriodExpiryFlag().toString());
		}

		if(ucipResponse.getTemporaryBlockedFlag() != null)
		{
			accountData.getFields().put(
					"temporaryBlockedFlag", ucipResponse.getTemporaryBlockedFlag().toString());
		}

		if (ucipResponse.getPamInformationList() != null)
		{
			accountData.getFields().put("pamInformationList",
					ucipResponse.getPamInformationList().toString());
		}

			/* Fix PT-272 -  as accountLinkTypeId is not available in
			 * ASAccount to put this value in extra fields
			 * */

		if(accountData.getAccountLinkTypeId() != null){
			accountData.getFields().put("accountLinkTypeId", accountData.getAccountLinkTypeId());
		}

		response.setAccountData(accountData);

		if (wantBalance)
		{
			response.getAccountData().setBalance(ucipResponse.getBalance());

		}

		response.setRetryable(false);
		response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
		return response;
	}
	public String getRequestTypeId()
	{
		return getClass().getName();
	}

}
