package com.seamless.ers.links.uciplink.ucip;

import com.seamless.common.ExtendedProperties;
import com.seamless.common.StringUtils;
import com.seamless.common.locale.ERSInvalidCurrencyException;
import com.seamless.common.locale.Locale;
import com.seamless.common.uciplib.common.*;
import com.seamless.common.uciplib.v40.requests.*;
import com.seamless.common.uciplib.v40.responses.*;
import com.seamless.common.uciplib.v50.requests.GetBalanceAndDateRequest;
import com.seamless.common.uciplib.v50.responses.GetBalanceAndDateResponse;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.utils.CurrencyHandler;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;
import com.seamless.ers.links.uciplink.utils.ExpireFormat;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmPoint;

import java.net.ConnectException;
import java.util.Date;

/**
 * Implementation of UCIPLINK for UCIP 4.0
 */
public class UCIPAdaptor40 extends IUCIPAdaptor
{
	public static final String TRANSACTION_TYPE_KEY = "transactionType";
	public static final String SUBSCRIBER_NUMBER = "subscriberNumber";
	public static final String VOUCHER_SERIAL = "voucherSerial";
	public static final String CREDIT = "Credit";
	public static final String DEBIT = "Debit";

    /**
     * Parametrized constructor
     *
     * @param config
     * @param properties
     */
    public UCIPAdaptor40(UCIPLinkConfig config, ExtendedProperties properties)
    {
        super(config, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UCIPResponse addToAccountFAF(String subscriberNumber, String fafNumber, String transactionId)
    {
        EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor40::addToAccountFAF");

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
            String err = "The connection to CS is down: " + e.getMessage();
            logger.info(err);
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
        EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor40::removeAccountFAF");

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
    public UCIPResponse getAccountDetails(String subscriberNumber, String transactionId, ERSHashtableParameter extraFields)
    {
        EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor40::getAccountDetails");

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
                if(config.isTransformCurrencyEnabled()) {
                    logger.debug(config.getModuleProperties("locale.custom_currency.").toString());
                    try {
                        Locale.getInstance().getFractionDigits(response.getCurrency1());
                    } catch (Exception e) {
                        //Currency is not based on ISO standards
                        response.setCurrency1(config.getModuleProperties("locale.custom_currency.").keySet().toArray()[0].toString());
                    }
                }
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
        EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor40::getAccountFAF");

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
        EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor40::getBalance");

        GetBalanceAndDateRequest request = null;
        GetBalanceAndDateResponse response = null;
        UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

        try
        {
            request = new GetBalanceAndDateRequest(subscriberNumber);
            resetExternalDataParameters();
            setDefaultVariables(request);
            request.setOriginTransactionID(transactionId);

            if (dedicatedAccountId != null)
            {
                DedicatedAccountSelection[] das = {new DedicatedAccountSelection(dedicatedAccountId)};
                request.setDedicatedAccountSelection(das);
            }

            response = (GetBalanceAndDateResponse) makeRequest(request);

            result.setUcipResponseCode(response.getResponseCode());

            if (response.isOkResponse())
            {
                if (dedicatedAccountId == null)
                {
                    if (response.getCurrency1() != null)
                    {
                        // Get main account balance
                        if(config.isTransformCurrencyEnabled()) {
                            logger.debug(config.getModuleProperties("locale.custom_currency.").toString());
                            try {
                                Locale.getInstance().getFractionDigits(response.getCurrency1());
                            } catch (Exception e) {
                                //Currency is not based on ISO standards
                                response.setCurrency1(config.getModuleProperties("locale.custom_currency.").keySet().toArray()[0].toString());
                            }
                        }
                        Amount balance = CurrencyHandler.createAmount(response.getAccountValue1(), response.getCurrency1());
                        result.setBalance(balance);
                    }
                }
                else
                {
                    if(config.isTransformCurrencyEnabled()) {
                        logger.debug(config.getModuleProperties("locale.custom_currency.").toString());
                        try {
                            Locale.getInstance().getFractionDigits(response.getCurrency1());
                        } catch (Exception e) {
                            //Currency is not based on ISO standards
                            response.setCurrency1(config.getModuleProperties("locale.custom_currency.").keySet().toArray()[0].toString());
                        }
                    }
                    Amount balance = getDedicatedAccountBalance(response.getDedicatedAccountInformation(), dedicatedAccountId, response.getCurrency1());
                    result.setBalance(balance);
                }
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

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UCIPResponse refill(String subscriberNumber, Amount amount, String transactionId, String transactionType, String profileId, Integer dedicatedAccountId, ERSHashtableParameter extraFields)
    {
        EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor40::refill");

        RefillRequest request = null;
        RefillResponse response = null;
        String originalCurrency = amount.getCurrency();
        UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

        try
        {
            if(config.isTransformCurrencyEnabled())
                originalCurrency = config.getTransformCurrencyCode();

            request = new RefillRequest(subscriberNumber, CurrencyHandler.getAmountValueAsLong(amount), originalCurrency, profileId);
            extraFields.put(REFILL_KEY, REFILL_VALUE);
            extraFields.put(TRANSACTION_TYPE_KEY, CREDIT);
            resetExternalDataParameters();
            setDefaultVariables(request, extraFields);
            request.setOriginNodeType(getOriginNodeType(extraFields));
            request.setOriginTransactionID(transactionId);
            request.setTransactionType(CREDIT);
            request.setRequestRefillAccountAfterFlag(true);
            request.setVoucherSerialNumber(extraFields.get(VOUCHER_SERIAL));
            if (extraFields.get(VALIDATE_SUBSCRIBER_LOCATION) != null)
            {
                request.setValidateSubscriberLocation(StringUtils.parseBoolean(extraFields.get(VALIDATE_SUBSCRIBER_LOCATION)));
            }
            response = (RefillResponse) makeRequest(request);
            result.setUcipResponseCode(response.getResponseCode());
            setDedicatedAccountBeforeRefill(dedicatedAccountId, response, result,originalCurrency);
            setDedicatedAccountAfterRefill(response, result,originalCurrency);
            setDedicatedAccountsTotal(response, result);
            setDedicatedAccountsPromotion(response, result);
        } catch (ConnectException e)
        {
            String err = "The connection to the CS is down: " + e.getMessage();
            logger.error(err);
            result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
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
     * For setting dedicated account promotions
     *
     * @param response
     * @param result
     */
    private void setDedicatedAccountsPromotion(RefillResponse response, UCIPResponse result)
    {
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
    }

    /**
     * For setting dedicated account Totals
     *
     * @param response
     * @param result
     */
    private void setDedicatedAccountsTotal(RefillResponse response, UCIPResponse result)
    {
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
    }

    /**
     * This method is setting dedicated accounts after refill
     *
     * @param response
     * @param result
     * @throws ERSInvalidCurrencyException
     */
    private void setDedicatedAccountAfterRefill(RefillResponse response, UCIPResponse result,String originalCurrency) throws ERSInvalidCurrencyException
    {
        if (response.getAccountAfterRefill() != null)
        {
            Amount balanceAfter = CurrencyHandler.createAmount(response.getAccountAfterRefill().getAccountValue1(), originalCurrency);
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
    }

    /**
     * This method is setting dedicated accounts before refill
     *
     * @param dedicatedAccountId
     * @param response
     * @param result
     * @throws ERSInvalidCurrencyException
     */
    private void setDedicatedAccountBeforeRefill(Integer dedicatedAccountId, RefillResponse response, UCIPResponse result, String originalCurrency) throws ERSInvalidCurrencyException
    {
        if (response.getAccountBeforeRefill() != null)
        {
            Amount balance;
            if (dedicatedAccountId == null)
            {
                balance = CurrencyHandler.createAmount(response.getAccountBeforeRefill().getAccountValue1(), originalCurrency);
            }
            else
            {
                balance = getDedicatedAccountBalance(response.getAccountBeforeRefill().getDedicatedAccountInformation(), dedicatedAccountId, originalCurrency);
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UCIPResponse updateServiceClass(String subscribernumber, int serviceClass, String reference, ERSHashtableParameter extraFields)
    {
        EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor40::updateServiceClass");

        String action = "SetOriginal";
        UpdateServiceClassRequest request = null;
        UpdateServiceClassResponse response = null;
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
        EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor40::updateBalance");
        UpdateBalanceAndDateRequest request;
        UpdateBalanceAndDateResponse response;
        UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

        try
        {
            String originalCurrency = relativeBalance.getCurrency();
            if (config.isTransformCurrencyEnabled())
            {
                originalCurrency = config.getTransformCurrencyCode();
            }


            if (dedicatedAccountId == null)
            {
                request = new UpdateBalanceAndDateRequest(subscriberNumber, CurrencyHandler.getAmountValueAsLong(relativeBalance), originalCurrency);
                extraFields.put(UPDATE_BALANCE_KEY,UPDATE_BALANCE_VALUE);
                resetExternalDataParameters();
                setDefaultVariables(request,extraFields);
                response = (UpdateBalanceAndDateResponse) makeRequest(request);
                result.setUcipResponseCode(response.getResponseCode());
            }
            else
            {
                request = new UpdateBalanceAndDateRequest(subscriberNumber, 0L, originalCurrency);
                if(config.getEnableDataBundle())
                {
                    loadDAInformationForDataBundle(request,extraFields);
                }
                else
                {
                    loadDAInformation(request,dedicatedAccountId,relativeBalance,extraFields);
                }

                resetExternalDataParameters();
                setDefaultVariables(request, extraFields);
                request.setOriginTransactionID(transactionId);

                response = (UpdateBalanceAndDateResponse) makeRequest(request);
                DedicatedAccountChangeInformation[] dedicatedAccountChangeInforamtionList = response.getDedicatedAccountChangeInformation();
                result.setUcipResponseCode(response.getResponseCode());
                if(response.getDedicatedAccountChangeInformation()!=null)
                {
                    result.getDedicatedAccountChangeInformation().put(dedicatedAccountChangeInforamtionList[0].getDedicatedAccountID().toString(), dedicatedAccountChangeInforamtionList[0]);
                }

            }

        } catch (ConnectException e)
        {
            String err = "The connection to the CS is down: " + e.getMessage();
            logger.error(err);
            result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
        } catch (NumberFormatException e)
        {
            logger.error("Invalid adjustment amount ", e);
            result.setErsResponseCode(ERSWSLinkResultCodes.INVALID_AMOUNT);

        }
        catch (IndexOutOfBoundsException e)
        {
            logger.error("Either 'daAmount' or 'validity' is missing for one of the dedicated account Ids ", e);
            result.setErsResponseCode(ERSWSLinkResultCodes.INVALID_PARAMETER_FOR_VAS_OPERATION);

        }
        catch (Exception e)
        {
            logger.error("UCIP Request failed for UpdateBalance: ", e);
            result.setErsResponseCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
        } finally
        {
            point.collect();
        }

        return result;
    }

    @Override
    public UCIPResponse updateAccumulators(String subscriberNumber, String transactionId, AccumulatorUpdateInformation[] info, ERSHashtableParameter extraFields)
    {
        EtmPoint point = EtmManager.getEtmMonitor().createPoint(
                "UCIPAdaptor40::updateAccumulators");
        point.collect();
        return new UCIPResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
    }

    @Override
    public UCIPResponse getCapabilities()
    {
        EtmPoint point = EtmManager.getEtmMonitor().createPoint(
                "UCIPAdaptor40::getCapabilities");
        point.collect();
        return new UCIPResponse(ERSWSLinkResultCodes.UNSUPPORTED_OPERATION);
    }

    @Override
    public UCIPResponse deleteOffer(String transactionId, String subscriberNumber, OfferProductsDTO offerProductDTO, ERSHashtableParameter extraFields) {
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
        EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor40::redeemVoucher");

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

    /**
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
        EtmPoint point = EtmManager.getEtmMonitor().createPoint("UCIPAdaptor40::updateOffer");

        UpdateOfferRequest request;
        UpdateOfferResponse response;
        String originalCurrency = amount.getCurrency();
        UCIPResponse result = new UCIPResponse(ERSWSLinkResultCodes.INTERNAL_FAILED);

        try
        {
            if (config.isTransformCurrencyEnabled())
            {
                originalCurrency = config.getTransformCurrencyCode();
            }

            request = new UpdateOfferRequest(subscriberNumber);
            request.setOriginTransactionID(transactionId);
            request.setOfferID(offerProductDTO.getOfferID());
            request.setOriginTimeStamp(formatter.parse(formatter.format(offerProductDTO.getStartDate(offerProductDTO.getValidityList().get(0)))));
            Date expiryDate = formatter.parse(formatter.format(offerProductDTO.getEndDate(offerProductDTO.getValidityList().get(0))));
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
            resetExternalDataParameters();
            setDefaultVariables(request, extraFields);
            request.setSubscriberNumberNAI(subscriberNumberNAI);

            if (extraFields.get(VALIDATE_SUBSCRIBER_LOCATION) != null)
            {
                request.setValidateSubscriberLocation(StringUtils.parseBoolean(extraFields.get(VALIDATE_SUBSCRIBER_LOCATION)));
            }

            response = (UpdateOfferResponse) makeRequest(request);

            result.setUcipResponseCode(response.getResponseCode());
            result.setUpdateOfferResponse(response);

        } catch (ConnectException e)
        {
            String err = "The connection to the CS is down: " + e.getMessage();
            logger.error(err);
            result.setErsResponseCode(ERSWSLinkResultCodes.LINK_DOWN);
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

}
