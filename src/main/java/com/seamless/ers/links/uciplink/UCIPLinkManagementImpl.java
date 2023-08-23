package com.seamless.ers.links.uciplink;

import java.util.Date;
import java.util.List;

import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.AccountData;
import com.seamless.ers.interfaces.ersifextlink.ERSWSAccountManagementLink;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.*;
import com.seamless.ers.links.uciplink.commandProcessor.RequestProcessorHandler;

import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmPoint;

public class UCIPLinkManagementImpl implements ERSWSAccountManagementLink
{

	public UCIPLinkManagementImpl(RequestProcessorHandler reqProcessorHandler)
	{
	}

	@Override
	public AccountManagementResponse searchAccounts(String s, String s1, String s2, int i, int i1, String s3, TimestampParameter timestampParameter, TimestampParameter timestampParameter1, NumberParameter numberParameter) {
		return null;
	}

	public AccountManagementResponse addAccounts(List<AccountData> accounts, String reference)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkManagementImpl::addAccounts");
		
		try
		{
			AccountManagementResponse response = new AccountManagementResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION, false);
			return response;
		}
		finally
		{
			point.collect();
		}
	}

	public AccountManagementResponse searchAccounts(String accountTypeIdMatch,
			String accountIdMatch, String accountTypeMatch, int offset,
			int maxCount, String reference)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkManagementImpl::searchAccounts");

		try
		{
			AccountManagementResponse response = new AccountManagementResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION, false);
			return response;
		}
		finally
		{
			point.collect();
		}
	}

	public AccountManagementResponse updateAccounts(List<AccountData> accounts, String reference)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkManagementImpl::updateAccounts");

		try
		{
			AccountManagementResponse response = new AccountManagementResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION, false);
			return response;
		}
		finally
		{
			point.collect();
		}
	}

	public LinkStatusResponse getLinkStatus()
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkManagementImpl::getLinkStatus");
		
		try
		{
			LinkStatusResponse response = new LinkStatusResponse(ERSWSLinkResultCodes.OPERATION_NOT_AVAILABLE
					, false, "UCIPLink does not support account management");
			return response;
		}
		finally
		{
			point.collect();
		}
	}

	public TransactionListResponseData searchAccountTransactions(
			Account account, Date fromDate, Date untilDate, String reference,
			int offset, int maxCount)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkManagementImpl::searchAccountTransactions");
		
		try
		{
			TransactionListResponseData response = new TransactionListResponseData(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION, false);
			return response;
		}
		finally
		{
			point.collect();
		}
	}

	@Override
	public AccountManagementResponse getAccountById(String s, String s1) {
		return null;
	}

}
