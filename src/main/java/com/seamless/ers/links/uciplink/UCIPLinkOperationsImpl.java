package com.seamless.ers.links.uciplink;

import com.seamless.ers.interfaces.ersifcommon.dto.accounts.AccountData;
import com.seamless.ers.interfaces.ersifextlink.dto.*;
import com.seamless.ers.interfaces.ersifextlink.dto.accounts.AccountInformationList;
import com.seamless.ers.interfaces.ersifextlink.dto.accounts.AccountInformationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.log4j.MDC;

import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifextlink.ERSWSAccountOperationsLink;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.links.uciplink.commandProcessor.RequestProcessorHandler;
import com.seamless.ers.links.uciplink.operations.processors.ERSGetAccountInformationProcessor;
import com.seamless.ers.links.uciplink.operations.processors.ERSMakeAccountTransactionProcessor;
import com.seamless.ers.links.uciplink.services.processors.LinkStatusProcessor;

import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmPoint;

import java.util.Collections;

public class UCIPLinkOperationsImpl implements ERSWSAccountOperationsLink
{

	private static final String ERS_REFERENCE = "ersReference";
	private Logger logger = LoggerFactory.getLogger(UCIPLinkOperationsImpl.class);
	private RequestProcessorHandler handler;
	
	

	public UCIPLinkOperationsImpl(RequestProcessorHandler reqProcessorHandler)
	{
		this.handler = reqProcessorHandler;
	}

	public AccountInformationResponse getAccountInformation(String principalId,
			Account account, String routingInfo, boolean wantBalance, String reference, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPLinkOperationsImpl::getAccountInformation");

		
		String nativeReference = handler.getConfig().createNativeReference(reference);
		
		try
		{
			MDC.put(ERS_REFERENCE, reference);
			StringBuilder accountString = new StringBuilder("");
			accountString.append(" account{ accountId=").append((account != null ? account.getAccountId() : null));
			accountString.append(", accountTypeId=").append((account != null ? account.getAccountTypeId() : null));
			accountString.append(", accountProviderId=").append((account != null ? account.getAccountProviderId() : null));
			accountString.append(", accountDescription=").append((account != null ? account.getAccountDescription() : null));
			accountString.append(", extraFields=").append((account != null ? account.getExtraFields() : null)).append(" }");

			logger.debug("begin getAccountInformation" 
					+ " principalId=" + principalId
					+ accountString.toString()
					+ " routingInfo=" + routingInfo 
					+ " wantBalance=" + wantBalance 
					+ " reference="+ reference
					+ " nativeReference=" + nativeReference
					+ " extraFields=" + extraFields);
			
			ERSGetAccountInformationProcessor processor = new ERSGetAccountInformationProcessor(handler.getConfig(),
					principalId, account, routingInfo, wantBalance, reference, nativeReference, extraFields);

			AccountInformationResponse response = handler.handleRequest(processor);
			response.setNativeReference(nativeReference);
			if(extraFields != null && extraFields.getParameters() != null)
				response.getFields().getParameters().putAll(extraFields.getParameters());

			logger.debug("end getAccountInformation"
					+ " resultCode=" + response.getResultCode()
					+ " reference="+ reference
					+ " nativeReference=" + nativeReference
					+ " extraFields=" + (response.getFields() != null ? response.getFields().toString() : null));

			return response;

		
		}
		catch (Exception e)
		{
			logger.error("Unexpected exception", e);
			
			AccountInformationResponse resp = new AccountInformationResponse();
			resp.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
			resp.setNativeReference(nativeReference);
			resp.setResultDescription(e.getMessage());
			return resp;
		}
		finally
		{
			point.collect();
			MDC.remove(ERS_REFERENCE);
		}
	}

	public AccountTransactionResponse makeAccountTransaction(String principalId, Account account, String password,
			AccountTransactionType transactionType, String routingInfo,	Amount amount, String reference, 
			ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkOperationsImpl::makeAccountTransaction");
		
		String nativeReference = handler.getConfig().createNativeReference(reference);
		
		try
		{
			MDC.put(ERS_REFERENCE, reference);
			logger.debug("begin makeAccountTransaction" 
					+ " principalId=" + principalId
					+ " accountId=" + (account != null ? account.getAccountId() : null) 
					+ " password=" + password 
					+ " transactionType=" + transactionType 
					+ " routingInfo=" + routingInfo 
					+ " amount=" + amount
					+ " extraFields=" + (extraFields != null ? extraFields.toString() : null) 
					+ " reference="+ reference
					+ " nativeReference=" + nativeReference);
			
			
			ERSMakeAccountTransactionProcessor processor = new ERSMakeAccountTransactionProcessor(
					principalId, account, transactionType, amount, reference, nativeReference,extraFields);

			AccountTransactionResponse response = handler.handleRequest(processor);
			response.setNativeReference(nativeReference);
			
			logger.debug("end makeAccountTransaction"
					+ " principalId=" + principalId
					+ " accountId=" + (account != null ? account.getAccountId() : null) 					
					+ " password=" + password 
					+ " transactionType=" + transactionType 
					+ " routingInfo=" + routingInfo 
					+ " amount=" + amount 
					+ " resultCode=" + response.getResultCode()
					+ " extraFields=" + (response.getFields() != null ? response.getFields().toString() : null)
					+ " reference="+ reference
					+ " nativeReference=" + nativeReference);
			
			return response;
		}
		catch (Exception e)
		{
			logger.error("Unexpected exception", e);
			
			AccountTransactionResponse resp = new AccountTransactionResponse();
			resp.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
			resp.setNativeReference(nativeReference);
			resp.setResultDescription(e.getMessage());
			return resp;
		}
		finally
		{
			point.collect();
			MDC.remove(ERS_REFERENCE);
		}

	}

	public LinkStatusResponse getLinkStatus()
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkOperationsImpl::getLinkStatus");
		
		try
		{
			LinkStatusProcessor processor = new LinkStatusProcessor();
			return handler.handleRequest(processor);
		}
		catch(Exception e)
		{
			logger.error("Unexpected exception", e);
			LinkStatusResponse response = new LinkStatusResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
			response.setResultDescription(e.getMessage());
			return response;
		}
		finally
		{
			point.collect();
		}
	}
	
	@Override
	public GetAccountCountersResponse getCounters(String principalId, String reference, Account account)
	{
		return new GetAccountCountersResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
	}

	@Override
	public GetAccountBalanceExpiryResponse getAccountExpiryDate(String s) {
		return null;
	}

	@Override
	public MakeAccountsTransactionsResponse makeAccountsTransactions(MakeAccountsTransactionsRequest makeAccountsTransactionsRequest) {
		{
			String reference = makeAccountsTransactionsRequest.getErsReference();
			EtmPoint point = EtmManager.getEtmMonitor().createPoint(
					"UCIPLinkOperationsImpl::makeAccountTransaction");

			String nativeReference = handler.getConfig().createNativeReference(reference);
			MakeAccountsTransactionsData makeAccountsTransactionsData = makeAccountsTransactionsRequest.getMakeAccountsTransactions().get(0);
			String principalId = makeAccountsTransactionsData.getPrincipalId();
			Account account = makeAccountsTransactionsData.getAcc();
			String password = makeAccountsTransactionsData.getPassword();
			AccountTransactionType transactionType = makeAccountsTransactionsData.getTransactionType();
			String routingInfo = makeAccountsTransactionsData.getRoutingInfo();
			Amount amount = makeAccountsTransactionsData.getAmount();
			ERSHashtableParameter extraFields = makeAccountsTransactionsData.getExtraField();
			try
			{
				MDC.put(ERS_REFERENCE, reference);
				logger.debug("begin makeAccountTransaction"
						+ " principalId=" + principalId
						+ " accountId=" + (account != null ? account.getAccountId() : null)
						+ " password=" + password
						+ " transactionType=" + transactionType
						+ " routingInfo=" + routingInfo
						+ " amount=" + amount
						+ " extraFields=" + (extraFields != null ? extraFields.toString() : null)
						+ " reference="+ reference
						+ " nativeReference=" + nativeReference);


				ERSMakeAccountTransactionProcessor processor = new ERSMakeAccountTransactionProcessor(
						principalId, account, transactionType, amount, reference, nativeReference,extraFields);

				AccountTransactionResponse response = handler.handleRequest(processor);
				response.setNativeReference(nativeReference);
				AccountData accountData = new AccountData();
				accountData.setAccount(account);
				response.setAccountData(accountData);

				logger.debug("end makeAccountTransaction"
						+ " principalId=" + principalId
						+ " accountId=" + (account != null ? account.getAccountId() : null)
						+ " password=" + password
						+ " transactionType=" + transactionType
						+ " routingInfo=" + routingInfo
						+ " amount=" + amount
						+ " resultCode=" + response.getResultCode()
						+ " extraFields=" + (response.getFields() != null ? response.getFields().toString() : null)
						+ " reference="+ reference
						+ " nativeReference=" + nativeReference);
				MakeAccountsTransactionsResponse makeAccountsTransactionsResponse = new MakeAccountsTransactionsResponse();
				makeAccountsTransactionsResponse.setAccountTransactionResponses(Collections.singletonList(response));
				makeAccountsTransactionsResponse.setErsReference(reference);
				makeAccountsTransactionsResponse.setResultCode(response.getResultCode());
				makeAccountsTransactionsResponse.setFields(response.getFields());

				return makeAccountsTransactionsResponse;
			}
			catch (Exception e)
			{
				logger.error("Unexpected exception", e);

				MakeAccountsTransactionsResponse makeAccountsTransactionsResponse = new MakeAccountsTransactionsResponse();
				AccountTransactionResponse resp = new AccountTransactionResponse();
				resp.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
				resp.setNativeReference(nativeReference);
				resp.setResultDescription(e.getMessage());
				makeAccountsTransactionsResponse.setAccountTransactionResponses(Collections.singletonList(resp));
				makeAccountsTransactionsResponse.setErsReference(reference);
				return makeAccountsTransactionsResponse;
			}
			finally
			{
				point.collect();
				MDC.remove(ERS_REFERENCE);
			}

		}
	}

	@Override
	public ExchangeTokenResponse doNotifyMaster(ERSHashtableParameter ersHashtableParameter) {
		return null;
	}

	@Override
	public ExchangeTokenResponse doNotifySalve(ERSHashtableParameter ersHashtableParameter) {
		return null;
	}

	@Override
	public ExchangeTokenResponse doGetToken(ERSHashtableParameter ersHashtableParameter) {
		return null;
	}

	@Override
	public ERSLinkResponse fetchUpsellOffers(ERSHashtableParameter ersHashtableParameter) {
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPLinkOperationsImpl::fetchUpsellOffers");
		try
		{
			return new ERSLinkResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
		}
		finally
		{
			point.collect();
		}
	}

	@Override
	public AccountInformationResponse getAccountInformation(AccountInformationRequest accountInformationRequest) {
		return null;
	}

	@Override
	public AccountInformationListResponse getAccountInformation(AccountInformationList accountInformationList) {
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPLinkOperationsImpl::getAccountInformation");
		try
		{
			return new AccountInformationListResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
		}
		finally
		{
			point.collect();
		}
	}


}
