package com.seamless.ers.links.uciplink;

import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifextlink.ERSWSAccountServicesLink;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.*;
import com.seamless.ers.interfaces.ersifextlink.dto.accounts.AccountInformationRequest;
import com.seamless.ers.links.uciplink.UCIPLinkConfig.TopupProductSettings;
import com.seamless.ers.links.uciplink.commandProcessor.RequestProcessorHandler;
import com.seamless.ers.links.uciplink.services.processors.*;

import com.sun.xml.bind.v2.TODO;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.log4j.MDC;
import se.seamless.module.productmanagement.api.model.PaginatedProductsResponseModel;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UCIPLinkServicesImpl implements ERSWSAccountServicesLink
{
	private static final String ERS_REFERENCE = "ersReference";
	private Logger logger = LoggerFactory.getLogger(UCIPLinkServicesImpl.class);	
	private RequestProcessorHandler handler;
	private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	public UCIPLinkServicesImpl(RequestProcessorHandler reqProcessorHandler)
	{
		this.handler = reqProcessorHandler;
	}

	public ERSLinkResponse activateAccount(String principalId, Account account,
			String password, String routingInfo, String reference,
			ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::activateAccount");
		
		try
		{
			return new ERSLinkResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
		}
		finally
		{
			point.collect();
		}
	}
	
	public ERSLinkResponse changeAccountPassword(String principalId,
			Account account, String oldPassword, String newPassword,
			String routingInfo, String reference,
			ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::changeAccountPassword");
		
		try
		{
			return new ERSLinkResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
		}
		finally
		{
			point.collect();
		}
	}

	public ERSLinkResponse deactivateAccount(String principalId,
			Account account, String routingInfo, String reference,
			ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::deactivateAccount");
		
		try
		{
			return new ERSLinkResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
		}
		finally
		{
			point.collect();
		}
	}

	public ERSLinkResponse doAccountMiscOperation(String principalId,
			Account account, String operationId, String routingInfo,
			String reference, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::doAccountMiscOperation");
		
		try
		{
			return new ERSLinkResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
		}
		finally
		{
			point.collect();
		}
	}

	public AccountTransactionResponse redeemVoucherOnAccount(
			String principalId, Account account, String routingInfo,
			String voucherActivationCode, String reference,
			ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::redeemVoucherOnAccount");
		
		try
		{
			return new AccountTransactionResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
		}
		finally
		{
			point.collect();
		}
	}

	public AccountTransactionResponse topup(String principalId, Account account, 
			String routingInfo, Amount amount, String reference, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::topup");

		String nativeReference = handler.getConfig().createNativeReference(reference);
		Boolean isEnableDataBundle=handler.getConfig().getEnableDataBundle();
		String productSKUExtaField=extraFields.get("productSKU");
		String logType = "Airtime Topup";
		logger.debug("Product SKU from USSD APP "+productSKUExtaField);
	    logger.debug("principalId : " + principalId);
	    AccountTransactionResponse response = new AccountTransactionResponse();

		long startTime = 0;
		long endTime = 0;
		try
		{
			MDC.put(ERS_REFERENCE, reference);
			logger.info("Begin topup" 
					+ " principalId=" + principalId
					+ " accountId=" + (account != null ? account.getAccountId() : null) 
					+ " routingInfo=" + routingInfo 
					+ " amount=" + amount
					+ " extraFields=" + (extraFields != null ? extraFields.toString() : null) 
					+ " reference="+ reference
					+ " nativeReference=" + nativeReference);
			
	    
			if(isEnableDataBundle && extraFields.get("bundle_flow_type") != null && extraFields.get("bundle_flow_type").equalsIgnoreCase("REFILL_THEN_UPDATE_OFFER"))
			{
				logType = "Bundle";
				startTime = System.currentTimeMillis();
				ERSRefillAndUpdateOfferProcessor processor = new ERSRefillAndUpdateOfferProcessor(principalId, account, amount, reference, nativeReference, extraFields);
				response = handler.handleRequest(processor);
				endTime = System.currentTimeMillis();
			}
			else if(isEnableDataBundle && extraFields.get("bundle_flow_type") != null && extraFields.get("bundle_flow_type").equalsIgnoreCase("UPDATE_OFFER_THEN_REFILL"))
			{
				logType = "Bundle";
				ERSUpdateOfferAndRefillProcessor processor = new ERSUpdateOfferAndRefillProcessor(principalId, account, amount, reference, nativeReference, extraFields);
				startTime = System.currentTimeMillis();
				response = handler.handleRequest(processor);
				endTime = System.currentTimeMillis();
			}
			else if(isEnableDataBundle && extraFields.get("bundle_flow_type") != null && extraFields.get("bundle_flow_type").equalsIgnoreCase("UPDATE_ACCUMULATORS_THEN_REFILL"))
			{
				logType = "Bundle";
				ERSUpdateAccumulatorsAndRefillProcessor processor = new ERSUpdateAccumulatorsAndRefillProcessor(principalId, account, amount, reference, nativeReference, extraFields);
				startTime = System.currentTimeMillis();
				response = handler.handleRequest(processor);
				endTime = System.currentTimeMillis();
			}
			else if(isEnableDataBundle && extraFields.get("bundle_flow_type") != null && extraFields.get("bundle_flow_type").equalsIgnoreCase("REFILL_THEN_UPDATE_ACCUMULATORS"))
			{
				logType = "Bundle";
				ERSRefillAndUpdateAccumulatorsProcessor processor = new ERSRefillAndUpdateAccumulatorsProcessor(principalId, account, amount, reference, nativeReference, extraFields);
				startTime = System.currentTimeMillis();
				response = handler.handleRequest(processor);
				endTime = System.currentTimeMillis();
			}
			else if(isEnableDataBundle && extraFields.get("bundle_flow_type") != null && extraFields.get("bundle_flow_type").equalsIgnoreCase("UPDATE_OFFER"))
			{
				logType = "Bundle";
				ERSUpdateOffer2Processor processor = new ERSUpdateOffer2Processor(principalId, account, amount, reference, nativeReference, extraFields);
				startTime = System.currentTimeMillis();
				response = handler.handleRequest(processor);
				endTime = System.currentTimeMillis();
			}
			else if(isEnableDataBundle && handler.getConfig().getMappedSKUList().contains(productSKUExtaField))
			{
				/*
				 * To check databundle property is true
				 * Then it should have skuMappinglist.
				 */
				logType = "Bundle";
				ERSUpdateBalanceAndUpdateOfferProcessor processor=new ERSUpdateBalanceAndUpdateOfferProcessor
							   (principalId,account,amount,reference,nativeReference,extraFields);
				logger.debug("Request is for Data Bundle, Going to call UpdateBalance Processor ");
				startTime = System.currentTimeMillis();
				response = handler.handleRequest(processor);
				endTime = System.currentTimeMillis();
		     }
			else if(isEnableDataBundle && extraFields.get("bundle_flow_type") != null && extraFields.get("bundle_flow_type").equalsIgnoreCase("VOUCHER_RECHARGE")){
				logger.debug("Request is for Voucher Recharge , Going to call VoucherRecharge processor ");
				startTime = System.currentTimeMillis();
				RefillVoucherRechargeProcessor processor = new RefillVoucherRechargeProcessor(principalId, account, amount, reference, nativeReference, extraFields);
				response = handler.handleRequest(processor);
				endTime = System.currentTimeMillis();
			}
			else
			{
				logger.debug("Request is for TOPUP , Going to call TOPUP processor ");
				startTime = System.currentTimeMillis();
				ERSTopupProcessor processor = new ERSTopupProcessor(principalId, account, amount, reference, nativeReference, extraFields);
				response = handler.handleRequest(processor);
				endTime = System.currentTimeMillis();
			}	
            
		
			response.setNativeReference(nativeReference);
			if(extraFields != null && extraFields.getParameters() != null)
				response.getFields().getParameters().putAll(extraFields.getParameters());
			response.getFields().put("externalReference", nativeReference);
			logger.info("end topup"
					+ " principalId=" + principalId
					+ " accountId=" + (account != null ? account.getAccountId() : null) 					
					+ " amount=" + amount 
					+ " resultCode=" + response.getResultCode()
					+ " extraFields=" + (response.getFields() != null ? response.getFields().toString() : null)
					+ " reference="+ reference
					+ " nativeReference=" + nativeReference); 
			
			return response;
		}
		catch (Exception e)
		{
			logger.error("Topup Unexpected exception", e);
			
			response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
			response.setResultDescription(e.getMessage());
			response.setNativeReference(nativeReference);
			response.getFields().put("externalReference", nativeReference);
			return response;
		}
		finally
		{
			String resultStatausMsg = response.getResultString();
			resultStatausMsg = resultStatausMsg.substring(resultStatausMsg.indexOf("(")+1, resultStatausMsg.lastIndexOf(")"));
			StringBuilder responseBuilder = new StringBuilder();
			responseBuilder.append("transactionMetrics ")
						   .append(logType)
					       .append("{EndTime=").append(simpleDateFormat.format(new Date(startTime)))
					 	   .append("|ErsReference=").append(reference)
					       .append("|ReceiverMSISDN=").append(response.getFields().getParameters().get("RECEIVER_MSISDN"))
					       .append("|TransactionAmount=").append(amount.getValue())
					       .append("|ResultCode=").append(response.getResultCode())
					       .append("|ResultStatus=").append(resultStatausMsg)
					       .append("|SenderMSISDN=").append(response.getFields().getParameters().get("SENDER_MSISDN"))
					       .append("|SenderResellerId=null")
					       .append("|TransactionProfile=").append(response.getFields().getParameters().get("TRANSACTION_PROFILE"))
						   .append("|UcipReference=").append(response.getFields().getParameters().get("externalReference"))
						   .append("|UcipResponseCode=").append(response.getFields().getParameters().get("ucipResponseCode"))
						   .append("|TimeTaken=").append(endTime-startTime).append("}");
			logger.info(responseBuilder.toString());
			point.collect();
			MDC.remove(ERS_REFERENCE);
		}
		
	}

	public AccountTransactionResponse updateOffer(Account account,
												  String reference, ERSHashtableParameter extraFields) {
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPLinkServicesImpl::updateOffer");
		AccountTransactionResponse response = new AccountTransactionResponse();
		long startTime = System.currentTimeMillis();
		long endTime = 0;
		try {
			MDC.put(ERS_REFERENCE, reference);
			logger.info("begin updateOffer"
					+ " reference={} extraFields={}",reference,(extraFields != null ? extraFields.toString() : null));
			String nativeReference = handler.getConfig().createNativeReference(reference);
			ERSUpdateOfferProcessor processor = new ERSUpdateOfferProcessor(account, null, nativeReference, extraFields);
			response = handler.handleRequest(processor);
			endTime = System.currentTimeMillis();
		} catch (Exception e) {
			logger.error("updateOffer Unexpected exception", e);

			response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
			response.setResultDescription(e.getMessage());
			return response;
		} finally {
			String resultStatausMsg = response.getResultString();
			resultStatausMsg = resultStatausMsg.substring(resultStatausMsg.indexOf("(")+1, resultStatausMsg.lastIndexOf(")"));
			StringBuilder responseBuilder = new StringBuilder();
			responseBuilder.append("transactionMetrics ")
					.append("{EndTime=").append(simpleDateFormat.format(new Date(startTime)))
					.append("|ErsReference=").append(reference)
					.append("|ReceiverMSISDN=").append(response.getFields().getParameters().get("RECEIVER_MSISDN"))
					.append("|ResultCode=").append(response.getResultCode())
					.append("|ResultStatus=").append(resultStatausMsg)
					.append("|SenderMSISDN=").append(response.getFields().getParameters().get("SENDER_MSISDN"))
					.append("|SenderResellerId=null")
					.append("|TransactionProfile=").append(response.getFields().getParameters().get("TRANSACTION_PROFILE"))
					.append("|UcipReference=").append(response.getFields().getParameters().get("externalReference"))
					.append("|UcipResponseCode=").append(response.getFields().getParameters().get("ucipResponseCode"))
					.append("|TimeTaken=").append(endTime-startTime).append("}");
			logger.info("Response:{}",responseBuilder);
			point.collect();
			MDC.remove(ERS_REFERENCE);
		}
		return response;
	}
	
	
	public AccountTransactionResponse reverseTopup(String principalId,
			Account senderAccount, Account receiverAccount, String routingInfo,
			Amount amount, String topupNativeReference, String reference,
			ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::reverseTopup");
		
		String nativeReference = handler.getConfig().createNativeReference(reference);
		

		try
		{
			MDC.put(ERS_REFERENCE, reference);
			logger.info("begin reverseTopup" 
					+ " principalId=" + principalId
					+ " senderAccount=" + (senderAccount != null ? senderAccount.getAccountId() : null) 
					+ " receiverAccount=" + (receiverAccount != null ? receiverAccount.getAccountId() : null) 
					+ " routingInfo=" + routingInfo 
					+ " amount=" + amount
					+ " topupNativeReference=" + topupNativeReference
					+ " reference="+ reference
					+ " nativeReference="+ nativeReference
					+ " extraFields=" + (extraFields != null ? extraFields.toString() : null));
			

			ERSReverseTopupProcessor processor = new ERSReverseTopupProcessor(principalId, senderAccount, receiverAccount, routingInfo, amount, topupNativeReference, reference, extraFields, nativeReference);

			AccountTransactionResponse response = handler.handleRequest(processor);
			response.setNativeReference(nativeReference);
			
			logger.info("end reverseTopup"
					+ " principalId=" + principalId
					+ " senderAccount=" + (senderAccount != null ? senderAccount.getAccountId() : null) 
					+ " receiverAccount=" + (receiverAccount != null ? receiverAccount.getAccountId() : null)
					+ " routingInfo=" + routingInfo 
					+ " amount=" + amount
					+ " topupNativeReference=" + topupNativeReference
					+ " reference="+ reference
					+ " nativeReference="+ nativeReference
					+ " extraFields=" + (extraFields != null ? extraFields.toString() : null)					
					+ " resultCode=" + response.getResultCode());
			
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

	public AccountTransactionResponse validateTopupReversal(String principalId,
			Account senderAccount, Account receiverAccount, String routingInfo,
			Amount amount, String nativeReference, String reference,
			ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::validateTopupReversal");
		
		AccountTransactionResponse response = new AccountTransactionResponse();

		try
		{
			response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
		}
		finally
		{
			point.collect();
		}
		
		return response;
	}
			

	public LinkStatusResponse getLinkStatus()
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::getLinkStatus");
		
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

	public AccountInformationResponse generateAccount(String accountType, String currency,
			Integer accountIdLength, String accountPrefix,
			Boolean generatePassword, Integer passwordLength,
			Integer lifeInDays, String reference, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::generateAccount");
		
		try
		{
			AccountInformationResponse response = new AccountInformationResponse();
			response.setResultCode(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
			return response;
		}
		finally
		{
			point.collect();
		}
	}

	public AccountInformationResponse validateAccount(String accountType, String accountId, 
			String password, String reference)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::validateAccount");

		
		String nativeReference = handler.getConfig().createNativeReference(reference);
		
		
		try
		{
			MDC.put(ERS_REFERENCE, reference);
			logger.debug("begin validateAccount" 
					+ " accountType=" + accountType
					+ " accountId=" + accountId 
					+ " password=" + password 
					+ " reference="+ reference
					+ " nativeReference=" + nativeReference);

			// TODO: 15/08/2022 as the extraFields are not present so initializing it , you can pass here accordingly.

			ERSValidateAccountProcessor processor = new ERSValidateAccountProcessor(
					accountType, accountId, reference, nativeReference, new ERSHashtableParameter());

			AccountInformationResponse response = handler.handleRequest(processor);
			response.setNativeReference(nativeReference);
			
			logger.debug("end validateAccount"
					+ " accountType=" + accountType
					+ " accountId=" + accountId 					
					+ " password=" + password 
					+ " resultCode=" + response.getResultCode()
					+ " extraFields=" + (response.getFields() != null ? response.getFields().toString() : null)
					+ " reference="+ reference
					+ " nativeReference=" + nativeReference);
			
			return response;
		}
		catch (Exception e)
		{
			logger.error("Unexpected exception", e);
			
			AccountInformationResponse resp = new AccountInformationResponse();
			resp.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
			resp.setResultDescription(e.getMessage());
			resp.setNativeReference(nativeReference);
			return resp;
		}
		finally
		{
			point.collect();
			MDC.remove(ERS_REFERENCE);
		}
	}


	public AccountTransactionResponse validateTopup(String principalId, Account senderAccount,
			Account receiverAccount, String routingInfo, Amount amount, 
			String reference, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::validateTopup");

		String productSKU = extraFields.get("productSKU");
		try
		{
			MDC.put(ERS_REFERENCE, reference);
			TopupProductSettings settings = handler.getConfig().getTopupProductSettings(productSKU);
			if (settings == null || 
				!settings.isValidateTopupAccount())
			{
				AccountTransactionResponse response = new AccountTransactionResponse();
				response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
				return response;
			}
			else
			{
				logger.debug("begin topup" 
						+ " principalId=" + principalId
						+ " accountId=" + (receiverAccount != null ? receiverAccount.getAccountId() : null) 
						+ " routingInfo=" + routingInfo 
						+ " amount=" + amount
						+ " extraFields=" + (extraFields != null ? extraFields.toString() : null) 
						+ " reference="+ reference);
				
				String nativeReference = handler.getConfig().createNativeReference(reference);
				ERSValidateTopupProcessor processor = 
					new ERSValidateTopupProcessor(
							settings, 
							principalId, 
							senderAccount, 
							receiverAccount, 
							amount, 
							reference, 
							nativeReference, extraFields);
							//principalId, receiverAccount, amount, reference, reference, extraFields);

				AccountTransactionResponse response = handler.handleRequest(processor);
				response.setNativeReference(nativeReference);
				if(extraFields != null && extraFields.getParameters() != null)
					response.getFields().getParameters().putAll(extraFields.getParameters());

				logger.debug("end topup"
						+ " principalId=" + principalId
						+ " accountId=" + (receiverAccount != null ? receiverAccount.getAccountId() : null) 					
						+ " amount=" + amount 
						+ " resultCode=" + response.getResultCode()
						+ " extraFields=" + (response.getFields() != null ? response.getFields().toString() : null)
						+ " reference="+ reference);
				
				return response;
			}
		}
		catch (Exception e)
		{
			logger.error("Unexpected exception", e);
			
			AccountTransactionResponse resp = new AccountTransactionResponse();
			resp.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
			resp.setResultDescription(e.getMessage());
			resp.setNativeReference(reference);
			return resp;
		}
		finally
		{
			point.collect();
			MDC.remove(ERS_REFERENCE);
		}
	}

	@Override
	public PaginatedProductsResponseModel getPackagesByMsisdn(Account account, Account account1, String s, ERSHashtableParameter ersHashtableParameter) {
		return null;
	}


	/**
	 * This method observes the 'operationId' parameter to decide which processor should
	 * be invoked against this particular call. All required parameters must be sent to 
	 * the target processor through ERSHashtableParameter.
	 * @param principalId The id of entity, who invoked this processor.
	 * @param account The account on which the operation is supposed to be performed.
	 * @param routingInfo Specific information about the node on which this operation should 
	 * be performed.
	 * @param operationId String representing the unique id of the operation. The operation id 
	 * could possible be any one from those listed in the 'extraFields' explanation.
	 * @param reference Transaction id generated by ERS.
	 * @param extraFields Hash table should be filled in with parameters according to the 
	 * following: 
	 * 
	 * <style>
	 * table
	 * {
	 * 		margin: 20px;
	 * 		border: black 0px solid;
	 * 		border-width: 0px 1px 0px 1px;
	 * }
	 * th
	 * {
	 * 		padding: 2px 10px;
	 * 		border: black 0px solid;
	 * 		border-width: 1px 0px 1px 0px;
	 * }
	 * td
	 * {
	 * 		padding: 1px 10px;
	 * 		border-bottom: black 1px solid;
	 * }
	 * </style>
	 * 
	 * <table cellspacing="0">
	 * <tr>
	 *   <th>OperationId</th>
	 *   <th>Description</th>
	 *   <th>Response object</th>
	 *   <th>Parameters</th>
	 * </tr>
	 * <tr>
	 *   <td>CHANGE_SERVICECLASS</td>
	 *   <td>Change current permanent service class</td>
	 *   <td>AccountClassResponse.java</td>
	 *   <td>ACCOUNT_CLASS_ID This string should contain digits which could be casted to service class id</td>
	 * </tr>
	 * <tr>
	 *   <td>GET_SERVICECLASS</td>
	 *   <td>Get current permanent service class</td>
	 *   <td>AccountClassResponse.java</td>
	 *   <td>None</td>
	 * </tr> 
	 * <tr>
	 *   <td>ADD_FRIEND_AND_FAMILY</td>
	 *   <td>Add number to friend and family list</td>
	 *   <td>To be updated</td>
	 *   <td>To be updated</td>
	 * </tr> 
	 * <tr>
	 *   <td>REMOVE_FRIEND_AND_FAMILY</td>
	 *   <td>Remove number to friend and family list</td>
	 *   <td>To be updated</td>
	 *   <td>To be updated</td>
	 * </tr> 
	 * <tr>
	 *   <td>GET_FRIEND_AND_FAMILY</td>
	 *   <td>Get friend and family list</td>
	 *   <td>AccountFAFResponse.java</td>
	 *   <td>None</td>
	 * </tr> 
	 * <tr>
	 *   <td>GET_BONUS_INCOMING_CALL</td>
	 *   <td>Get Bonus Incoming Call service status</td>
	 *   <td>To be updated</td>
	 *   <td>To be updated</td>
	 * </tr> 
	 * <tr>
	 *   <td>ACTIVATE_BONUS_INCOMING_CALL</td>
	 *   <td>Activate Bonus Incoming Call service</td>
	 *   <td>To be updated</td>
	 *   <td>To be updated</td>
	 * </tr> 
	 * <tr>
	 *   <td>DEACTIVATE_BONUS_INCOMING_CALL</td>
	 *   <td>De-activate Bonus Incoming Call service</td>
	 *   <td>To be updated</td>
	 *   <td>To be updated</td>
	 * </tr>
	 * </table>
	 */
	public AccountVASOperationResponse performVASOperation(String principalId,
			Account account, String routingInfo, String operationId,
			String reference, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::performVASOperation::" + operationId);
		
		String nativeReference = handler.getConfig().createNativeReference(reference);
		
		
		try
		{
			MDC.put(ERS_REFERENCE, reference);
			logger.debug("begin performVASOperation" 
					+ " principalId=" + principalId
					+ " accountId=" + (account != null ? account.getAccountId() : null) 
					+ " routingInfo=" + routingInfo 
					+ " operationId=" + operationId 
					+ " extraFields=" + (extraFields != null ? extraFields.toString() : null) 
					+ " reference="+ reference
					+ " nativeReference=" + nativeReference);
			
			ERSPerformVASProcessor processor = new ERSPerformVASProcessor(principalId,
					account, routingInfo, operationId, reference, extraFields, nativeReference);

			AccountVASOperationResponse response = handler.handleRequest(processor);
			response.setNativeReference(nativeReference);
			
			logger.debug("end performVASOperation"
					+ " principalId=" + principalId
					+ " accountId=" + (account != null ? account.getAccountId() : null) 					
				+ " operationId=" + operationId 
					+ " resultCode=" + response.getResultCode()
					+ " extraFields=" + (response.getFields() != null ? 
							response.getFields().toString() : null)
							+ " reference="+ reference
							+ " nativeReference=" + nativeReference
				);
			
			return response;
		}
		catch (Exception e)
		{
			logger.error("Unexpected exception", e);
			
			AccountVASOperationResponse resp = new AccountVASOperationResponse();
			resp.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
			resp.setResultDescription(e.getMessage());
			resp.setNativeReference(nativeReference);
			return resp;
		}
		finally
		{
			point.collect();
			MDC.remove(ERS_REFERENCE);
		}
	}

	public AccountVASOperationResponse validateVASOperation(String principalId,
			Account account, String routingInfo, String operationId,
			String reference, ERSHashtableParameter extraFields) 
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPLinkServicesImpl::validateVASOperation::" + operationId);
		
		try
		{
			logger.debug("begin validateVASOperation" 
					+ " principalId=" + principalId
					+ " accountId=" + (account != null ? account.getAccountId() : null) 
					+ " routingInfo=" + routingInfo 
					+ " operationId=" + operationId 
					+ " reference="+ reference
					+ " extraFields=" + (extraFields != null ? extraFields.toString() : null));
			
			return new AccountVASOperationResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION, operationId);
		}
		finally
		{
			point.collect();
		}
	}

	@Override
	public ERSLinkResponse createExpiryInfo(String s, String s1, String s2) {
		return null;
	}

	@Override
	public GetCustomerPlanResponse getCustomerPlans(String s) {
		return null;
	}

	@Override
	public GetSubscriberBalanceResponse getSubsciberBalance(String s) {
		return null;
	}

	@Override
	public GetSubscriberMigratedResponse getSubsciberMigrated(String s) {
		return null;
	}

	@Override
	public GetCustomerBundlesAndAvailableBundlesResp getCustomerBundlesAndAvailableBundles(String s) {
		return null;
	}

	@Override
	public GetCustomerBundlesAndAvailableBundlesV2Resp getCustomerBundlesAndAvailableBundlesV2(String s) {
		return null;
	}

	@Override
	public ChangeAlternatePhoneNumberResp changeAlternatePhoneNumber(String s, String s1, String s2) {
		return null;
	}

	@Override
	public AccountTransactionResponse topup(AccountInformationRequest accountInformationRequest) {
		return null;
	}

	@Override
	public AccountTransactionResponse reverseTopup(AccountInformationRequest accountInformationRequest) {
		return null;
	}

	@Override
	public ERSLinkResponse postReverseOrder(ERSHashtableParameter ersHashtableParameter) {
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPLinkServicesImpl::postReverseOrder");
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
	public ERSLinkResponse deleteOffer(Account account, String reference, ERSHashtableParameter ersHashtableParameter)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPLinkServicesImpl::deleteOffer");
		ERSLinkResponse response = new ERSLinkResponse();
		String nativeReference = null;
		try
		{
			MDC.put(ERS_REFERENCE, reference);
			nativeReference = handler.getConfig().createNativeReference(reference);
			ERSDeleteOfferProcessor processor = new ERSDeleteOfferProcessor(reference, nativeReference, account, ersHashtableParameter);
			response = handler.handleRequest(processor);
			return response;
		}
		catch (Exception e)
		{
			logger.error("Delete offer unexpected exception", e);
			response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
			response.setResultDescription(e.getMessage());
			response.setReference(reference);
			response.setNativeReference(nativeReference);
			return response;
		}
		finally
		{
			MDC.remove(ERS_REFERENCE);
			if(point != null)
			{
				point.collect();
			}
		}
	}
}
