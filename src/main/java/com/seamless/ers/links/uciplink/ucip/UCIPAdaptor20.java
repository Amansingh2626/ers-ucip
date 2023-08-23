package com.seamless.ers.links.uciplink.ucip;

import java.net.ConnectException;

import com.seamless.common.uciplib.common.AccumulatorUpdateInformation;
import com.seamless.ers.interfaces.ersifextlink.dto.ERSLinkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.common.ExtendedProperties;
import com.seamless.common.StringUtils;
import com.seamless.common.locale.ERSInvalidCurrencyException;
import com.seamless.common.uciplib.common.DedicatedAccountInformation;
import com.seamless.common.uciplib.common.DedicatedAccountRefillInformation;
import com.seamless.common.uciplib.common.FafInformation;
import com.seamless.common.uciplib.v22.requests.AdjustmentTRequest;
import com.seamless.common.uciplib.v22.requests.BalanceEnquiryTRequest;
import com.seamless.common.uciplib.v22.requests.GetAccountDetailsTRequest;
import com.seamless.common.uciplib.v22.requests.GetFaFListTRequest;
import com.seamless.common.uciplib.v22.requests.RefillTRequest;
import com.seamless.common.uciplib.v22.requests.StandardVoucherRefillTRequest;
import com.seamless.common.uciplib.v22.requests.UpdateServiceClassTRequest;
import com.seamless.common.uciplib.v22.requests.AdjustmentTRequest.AccountInformation;
import com.seamless.common.uciplib.v22.responses.AdjustmentTResponse;
import com.seamless.common.uciplib.v22.responses.BalanceEnquiryTResponse;
import com.seamless.common.uciplib.v22.responses.GetAccountDetailsTResponse;
import com.seamless.common.uciplib.v22.responses.GetFaFListTResponse;
import com.seamless.common.uciplib.v22.responses.RefillTResponse;
import com.seamless.common.uciplib.v22.responses.StandardVoucherRefillTResponse;
import com.seamless.common.uciplib.v22.responses.UpdateFaFListTResponse;
import com.seamless.common.uciplib.v22.responses.UpdateServiceClassTResponse;
import com.seamless.common.uciplib.v34.requests.RefillRequest;
import com.seamless.common.uciplib.v34.responses.RefillResponse;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.utils.CurrencyHandler;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;

import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmPoint;


public class UCIPAdaptor20 extends IUCIPAdaptor
{
	private final static Logger logger = LoggerFactory.getLogger(UCIPAdaptor20.class);
		
	
	public UCIPAdaptor20(UCIPLinkConfig config, ExtendedProperties properties)
	{
		super(config, properties);			
	}

	@Override
	public UCIPResponse addToAccountFAF(String subscriberNumber, String fafNumber,String transactionId) 
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPAdaptor20::addToAccountFAF");
		
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		
		try
		{
			FafInformation[] fafInformation = new FafInformation[1];
			
			fafInformation[0] = new FafInformation();
			fafInformation[0].setFafNumber(fafNumber);
			fafInformation[0].setFafIndicator(config.getFafIndicator());
			fafInformation[0].setOwner("1");
			com.seamless.common.uciplib.v22.requests.UpdateFaFListTRequest request = new com.seamless.common.uciplib.v22.requests.UpdateFaFListTRequest(subscriberNumber,"ADD",fafInformation);
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);
			com.seamless.common.uciplib.v22.responses.UpdateFaFListTResponse response = (UpdateFaFListTResponse)makeRequest(request);
			result.setUcipResponseCode(response.getResponseCode());
			
			
			
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch(Exception e)
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
	

	@Override
	public UCIPResponse removeAccountFAF(String subscriberNumber, String fafNumber,String transactionId) 
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPAdaptor20::removeAccountFAF");
		
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		
		try
		{
			FafInformation[] fafInformation = new FafInformation[1];
			
			fafInformation[0] = new FafInformation();
			fafInformation[0].setFafNumber(fafNumber);
			fafInformation[0].setFafIndicator(config.getFafIndicator());
			fafInformation[0].setOwner("1");
			com.seamless.common.uciplib.v22.requests.UpdateFaFListTRequest request = new com.seamless.common.uciplib.v22.requests.UpdateFaFListTRequest(subscriberNumber,"DEL",fafInformation);
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);
			com.seamless.common.uciplib.v22.responses.UpdateFaFListTResponse response = (UpdateFaFListTResponse)makeRequest(request);
			result.setUcipResponseCode(response.getResponseCode());
			
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch(Exception e)
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


	@Override
	public UCIPResponse getAccountDetails(String subscriberNumber, String transactionId,  ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPAdaptor20::getAccountDetails");
		
		GetAccountDetailsTRequest 	request 	= null;
		GetAccountDetailsTResponse 	response 	= null;
		UCIPResponse				result 		= new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		
		try
		{
			request 	= new GetAccountDetailsTRequest(subscriberNumber);
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);			
			response 	= (GetAccountDetailsTResponse)makeRequest(request);
			
			result.setUcipResponseCode(response.getResponseCode());
			result.setDefaultServiceClass(ifNull(response.getServiceClassCurrent(), 0));
			result.setCurrentServiceClass(ifNull(response.getServiceClassCurrent(), 0));
			result.setLanguageInISO6391(config.getLanguageInISO6391(response.getCurrentLanguageID()));
			result.setSupervisionExpiryDate(response.getSupervisionDate());
			result.setServiceFeeExpiryDate(response.getServiceFeeDate());

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
		catch(Exception e)
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

	

	@Override
	public UCIPResponse getAccountFAF(String subscriberNumber, String transactionId) 
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPAdaptor20::getAccountFAF");
		
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		
		try		
		{
			GetFaFListTRequest request = new GetFaFListTRequest(subscriberNumber, getRequestedOwner());
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);
			GetFaFListTResponse response = (GetFaFListTResponse)makeRequest(request);
			result.setErsResponseCode(response.getResponseCode());
			result.setFafList(response.getFafInformation());
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch(Exception e)
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

	

	@Override
	public UCIPResponse getBalance(String subscriberNumber, Integer dedicatedAccountId, Integer dedicatedAccountLastId, String transactionId, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPAdaptor20::getBalance");
		
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);		
		
		try
		{
			BalanceEnquiryTRequest request = new BalanceEnquiryTRequest(subscriberNumber);
			resetExternalDataParameters();
			setDefaultVariables(request);
			request.setOriginTransactionID(transactionId);			
			
			
			BalanceEnquiryTResponse response = (BalanceEnquiryTResponse)makeRequest(request);
			result.setErsResponseCode(response.getResponseCode());
			result.setLanguageInISO6391(config.getLanguageInISO6391(response.getCurrentLanguageID()));
			
			

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

	
	
	public UCIPResponse refill(String subscriberNumber, Amount amount, String transactionId, String transactionType, String profileId, Integer dedicatedAccountId, ERSHashtableParameter extraFields) 
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPAdaptor20::refill");
		
		RefillTRequest request = null;
		RefillTResponse response = null;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		
		try
		{
			request 	= new RefillTRequest(subscriberNumber, CurrencyHandler.getAmountValueAsLong(amount), amount.getCurrency());
			request.setPaymentProfileID(profileId);

			resetExternalDataParameters();
			setDefaultVariables(request,extraFields);
			request.setOriginTransactionID(transactionId);
			
			response 	= (RefillTResponse)makeRequest(request);
			
			result.setUcipResponseCode(response.getResponseCode()); 
			
			if (response.getAccountValueAfter1() != null)
			{
				Amount balanceAfter = CurrencyHandler.createAmount(response.getAccountValueAfter1(),
												 response.getCurrency1());
				result.setBalanceAfter(balanceAfter); /* Set balance after transaction */

				result.setSupervisionExpiryDateAfter(response.getSupervisionDateAfter());
			}
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch(Exception e)
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

	
	@Override
	public UCIPResponse updateBalance(String subscriberNumber,
			Amount relativeBalance, String transactionId,
			String transactionType, Integer dedicatedAccountId,ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPAdaptor20::updateBalance");
		
		AdjustmentTRequest request = null;
		AdjustmentTResponse response = null;
		UCIPResponse result = new UCIPResponse(
				ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{

			if (dedicatedAccountId == null)
			{
				request = new AdjustmentTRequest(subscriberNumber,
						CurrencyHandler.getAmountValueAsLong(relativeBalance),
						relativeBalance.getCurrency());
			}
			else
			{
				request = new AdjustmentTRequest(subscriberNumber, 0L,
						relativeBalance.getCurrency());
				AccountInformation info = request.new AccountInformation();
				info.setDedicatedAcccountID(dedicatedAccountId);
				info.setAdjustmentAmount(CurrencyHandler
						.getAmountValueAsLong(relativeBalance));
				request.setDedicatedAccountInformation(info);
			}

			resetExternalDataParameters();
			setDefaultVariables(request,extraFields);

			request.setOriginTransactionID(transactionId);
			response = (AdjustmentTResponse) makeRequest(request);
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
				"UCIPAdaptor20::updateAccumulators");
		point.collect();
		return new UCIPResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
	}

	@Override
	public UCIPResponse getCapabilities()
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
				"UCIPAdaptor20::getCapabilities");
		point.collect();
		return new UCIPResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
	}

	@Override
	public UCIPResponse deleteOffer(String transactionId, String subscriberNumber, OfferProductsDTO offerProductDTO, ERSHashtableParameter extraFields)
	{
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

	public UCIPResponse updateServiceClass(String subscribernumber, int serviceClass,String transactionId,ERSHashtableParameter extraFields) 
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPAdaptor20::updateServiceClass");
		
		UpdateServiceClassTRequest 	request 	= null;
		UpdateServiceClassTResponse	response	= null;
		UCIPResponse				result 		= new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		
		try
		{
			request 	= new UpdateServiceClassTRequest(subscribernumber, serviceClass);
			resetExternalDataParameters();
			setDefaultVariables(request,extraFields);
			request.setOriginTransactionID(transactionId);
			response 	= (UpdateServiceClassTResponse)makeRequest(request);
			result.setUcipResponseCode(response.getResponseCode());
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch(Exception e)
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

	@Override
	public UCIPResponse redeemVoucher(String subscriberNumber,
			String voucherCode, String transactionId, ERSHashtableParameter extraFields)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint(
		"UCIPAdaptor20::redeemVoucher");
	
		StandardVoucherRefillTRequest request = null;
		StandardVoucherRefillTResponse response = null;
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);
		
		try
		{
			request 	= new StandardVoucherRefillTRequest(subscriberNumber, voucherCode);

			resetExternalDataParameters();
			setDefaultVariables(request,extraFields);
			request.setOriginTransactionID(transactionId);
			
			response 	= (StandardVoucherRefillTResponse)makeRequest(request);
			
			result.setUcipResponseCode(response.getResponseCode()); 
			
			if (response.getAccountValueAfter1() != null)
			{
				Amount balanceAfter = CurrencyHandler.createAmount(response.getAccountValueAfter1(), response.getCurrency1());
				result.setBalanceAfter(balanceAfter); /* Set balance after transaction */

				result.setServiceFeeExpiryDateAfter(response.getServiceFeeDateAfter());
			}
		}
		catch (ConnectException e)
		{
			String err = "The connection to the CS is down: " + e.getMessage();
			logger.error(err);
			result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
		}
		catch(Exception e)
		{
			logger.error("UCIP Request failed for redeem voucher: ", e);
			result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
		}
		finally
		{
			point.collect();
		}
		
		return result;
	}	
	public UCIPResponse updateOffer(
			String subscriberNumber,
			Amount amount,
			String transactionId,
			String transactionType,
			String profileId,
			Integer dedicatedAccountId,
			ERSHashtableParameter extraFields,OfferProductsDTO offerProductDTO, String currency)
	{
		EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor20::refill");

		RefillRequest request;
		RefillResponse response;
		String originalCurrency = amount.getCurrency();
		UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

		try
		{
			if(config.isTransformCurrencyEnabled())
			{
				originalCurrency = config.getTransformCurrencyCode();
			}

			request = new RefillRequest(subscriberNumber, CurrencyHandler.getAmountValueAsLong(amount), originalCurrency, profileId);
			resetExternalDataParameters();
			setDefaultVariables(request, extraFields);
			request.setOriginTransactionID(transactionId);
			request.setTransactionType(transactionType);
			request.setRequestRefillAccountAfterFlag(true);
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
							dedicatedAccountId,
							originalCurrency);
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
						response.getAccountAfterRefill().getAccountValue1(),
						originalCurrency);
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
	
	private
	Amount getDedicatedAccountBalance(DedicatedAccountInformation[] daInfoList, int dedicatedAccountId, String currency) throws ERSInvalidCurrencyException
	{
		if (daInfoList != null)
		{
			// get Dedicated account balance
			for (DedicatedAccountInformation da: daInfoList)
			{
				if (da.getDedicatedAccountID().equals(dedicatedAccountId) &&
					da.getDedicatedAccountValue1() != null &&
					currency != null)
				{
					return CurrencyHandler.createAmount(da.getDedicatedAccountValue1(), currency);
				}
			}
		}
		
		return null;
	}
}
