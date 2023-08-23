package com.seamless.ers.links.uciplink.services.processors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.common.uciplib.common.DedicatedAccountChangeInformation;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountTransactionResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;

public class ERSUpdateBalanceAndUpdateOfferProcessor extends AbstractRequestProcessor<AccountTransactionResponse>
{
    Logger logger = LoggerFactory.getLogger(ERSUpdateBalanceAndUpdateOfferProcessor.class);

    private Account receiverAccount;
    private Amount amount;
    private String reference;
    private String refillProfileId;
    private String transactionType;
    private String nativeReference;
    private ERSHashtableParameter extraFields;

    /**
     * Parametrized constuctor for ERSUpdateBalanceAndUpdateOfferProcessor
     * @param principalId
     * @param account
     * @param amount
     * @param reference
     * @param nativeReference
     * @param extraFields
     */
    public ERSUpdateBalanceAndUpdateOfferProcessor(String principalId, Account account, Amount amount, String reference, String nativeReference, ERSHashtableParameter extraFields)
    {
        super(new AccountTransactionResponse());

        this.receiverAccount = account;
        this.amount = amount;
        this.reference = reference;
        this.nativeReference = nativeReference;
        this.refillProfileId = extraFields.get("refillProfileId");
        this.extraFields = extraFields;
    }

    public void process()
    {
        setResult(_process());
    }

    protected AccountTransactionResponse _process()
    {
        logger.debug("Request recieved to topup - info: [" + receiverAccount + " : " + amount + "]");

        this.getConfig().formatMsisdn(receiverAccount);

        if (refillProfileId == null)
        {
            refillProfileId = getConfig().getRefillProfileID();
        }

        if (this.getConfig().isValidateSubscriberLocation() != null)
        {
            extraFields.put("validateSubscriberLocation", this.getConfig().isValidateSubscriberLocation().toString());
        }

        AccountTransactionResponse response = new AccountTransactionResponse();
        response.setFields(new ERSHashtableParameter());

        String originalCurrency = amount.getCurrency();

        if(reference != null)
            extraFields.put("reference", reference);


        try
        {
            transactionType = extraFields.get("transactionType");
            // Get receiver account information, it will confirm that receiver
            // account exists in the system.
            HashMap<String, OfferProductsDTO> offerSKUMapp = (HashMap<String, OfferProductsDTO>) getConfig().getOfferListMap();
            String productSKUExtaField = extraFields.get("productSKU");
            String transactionType = extraFields.get("TRANSACTION_TYPE_KEY");

            logger.debug("Product SKU received from TXE " + productSKUExtaField);

            OfferProductsDTO offerProductsDTO = offerSKUMapp.get(productSKUExtaField);

            if (offerProductsDTO.getDaIDList().size() > 1)
            {
                extraFields.put("externalData3", offerProductsDTO.getDaIDList().get(1));
            }

            logger.debug("Going to call Update Balance ");
            UCIPResponse ucipResponseUpdateBalance = getConfig().getUcipAdaptor().getInstance().updateBalance(receiverAccount.getAccountId(), amount, nativeReference, transactionType, Integer.parseInt(offerProductsDTO.getDaIDList().get(0)), extraFields);

            if (ucipResponseUpdateBalance != null)
            {

                if (getConfig().isTransformCurrencyEnabled())
                {
                    transformAmountCurrency(amount, originalCurrency);
                    transformUcipResponseCurrency(ucipResponseUpdateBalance, originalCurrency);
                }

                if (ucipResponseUpdateBalance.getErsResponseCode() == ERSWSLinkResultCodes.SUCCESS)
                {
                    logger.debug("Successfully Called Update Balance ");


                    setUpdateBalanceResponse(ucipResponseUpdateBalance, response);

                    /**
                     * update offer call.
                     */
                    logger.debug("Transaction Type " + transactionType + " Excluded Update offer Transaction Types " + getConfig().getExcludUpdateOfferlist());
                    if ((getConfig().getExcludUpdateOfferlist() == null || !getConfig().getExcludUpdateOfferlist().contains(transactionType)))
                    {
                        logger.debug("Going to call update Offer");
                        UCIPResponse updateOfferResponse = getConfig().getUcipAdaptor().getInstance().updateOffer(receiverAccount.getAccountId(), amount, nativeReference, originalCurrency, transactionType, Integer.parseInt(offerProductsDTO.getDaIDList().get(0)), extraFields, offerProductsDTO, null);

                        if (updateOfferResponse != null)
                        {
                            if (updateOfferResponse.getErsResponseCode() == ERSWSLinkResultCodes.SUCCESS)
                            {
                                updateOfferResponse(response, updateOfferResponse);
                            }
                            else
                            {
                                logger.info("updateOffer Response Not Successful .");
                                /**
                                 * If response is not successful now reverse transaction with adjusting balance.
                                 */
                            }
                        }
                        else
                        {

                            logger.info("updateOffer Response is NUll .");
                            /**
                             * If response is null that is not successful now reverse transaction with adjusting balance.
                             *
                             */
                        }
                    }

                    if (ucipResponseUpdateBalance.getSupervisionExpiryDateBefore() != null)
                    {
                        response.getFields().put("supervisionExpiryDateBefore", getConfig().formatResponseDate(ucipResponseUpdateBalance.getSupervisionExpiryDateBefore()));
                    }

                    if (ucipResponseUpdateBalance.getSupervisionExpiryDateAfter() != null)
                    {
                        response.getFields().put("supervisionExpiryDateAfter", getConfig().formatResponseDate(ucipResponseUpdateBalance.getSupervisionExpiryDateAfter()));
                    }
                }
                else
                {
                    response.setResultCode(ucipResponseUpdateBalance.getErsResponseCode());
                }
            }
            else
            {
                logger.info("No response from AIR node! check connectivity for receiver account.");
                response.setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
            }

        } catch (Exception e)
        {
            logger.error("Problem occured while retrieving account information: ", e);
            response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
        }

        return response;
    }

    private void updateOfferResponse(AccountTransactionResponse response, UCIPResponse updateOfferResponse)
    {
        response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
        response.getFields().put("UpdateOffer ExpiryDate ", updateOfferResponse.getUpdateOfferResponse().getExpiryDate().toString());
        response.getFields().put("UpdateOffer Response Code ", updateOfferResponse.getUpdateOfferResponse().getResponseCode().toString());
        response.getFields().put("UpdateOffer Offer ID ", updateOfferResponse.getUpdateOfferResponse().getOfferID().toString());
        response.getFields().put("UpdateOffer Offer Type ", updateOfferResponse.getUpdateOfferResponse().getOfferType().toString());
        response.getFields().put("UpdateOffer Transaction ID ", updateOfferResponse.getUpdateOfferResponse().getOriginTransactionID().toString());
        response.getFields().put("UpdateOffer Start Date ", updateOfferResponse.getUpdateOfferResponse().getStartDate().toString());
    }

    public String getRequestTypeId()
    {
        return getClass().getName();

    }

    public void setUpdateBalanceResponse(UCIPResponse ucipResponse, AccountTransactionResponse response)
    {


        HashMap<String, DedicatedAccountChangeInformation> dedicatedAccount = ucipResponse.getDedicatedAccountChangeInformation();
        Set<String> dedicatedAccountKeySet = dedicatedAccount.keySet();
        for (
                Iterator iterator = dedicatedAccountKeySet.iterator(); iterator.hasNext(); )
        {
            String key = (String) iterator.next();
            DedicatedAccountChangeInformation dedicatedAccountInfo = dedicatedAccount.get(key);
            if(dedicatedAccountInfo.getDedicatedAccountActiveValue1()!=null)
            {
                response.getFields().put("Update balane dedicatedAccountActiveValue1", dedicatedAccountInfo.getDedicatedAccountActiveValue1().toString());
            }
            response.getFields().put("Update balane DedicatedAccountID", dedicatedAccountInfo.getDedicatedAccountID().toString());
            response.getFields().put("Update balane DedicatedAccountUnit", dedicatedAccountInfo.getDedicatedAccountUnit().toString());
            if(dedicatedAccountInfo.getDedicatedAccountValue1()!=null)
            {
                response.getFields().put("Update balane DedicatedAccountValue1", dedicatedAccountInfo.getDedicatedAccountValue1().toString());
            }
            response.getFields().put("Update balaneExpiryDate", dedicatedAccountInfo.getExpiryDate().toString());
            if(dedicatedAccountInfo.getOfferID()!=null)
            {
                response.getFields().put("Update balane OfferID", dedicatedAccountInfo.getOfferID().toString());
            }
        }


    }


}

