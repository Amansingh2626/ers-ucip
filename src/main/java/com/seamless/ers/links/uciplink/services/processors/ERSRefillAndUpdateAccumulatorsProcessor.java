package com.seamless.ers.links.uciplink.services.processors;

import com.seamless.common.uciplib.common.AccumulatorUpdateInformation;
import com.seamless.common.uciplib.common.DedicatedAccountRefillInformation;
import com.seamless.common.uciplib.common.TreeDefinedFieldInformation;
import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountTransactionResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;
import com.seamless.ers.links.uciplink.utils.UCIPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ERSRefillAndUpdateAccumulatorsProcessor extends AbstractRequestProcessor<AccountTransactionResponse>
{
    Logger logger = LoggerFactory.getLogger(ERSRefillAndUpdateAccumulatorsProcessor.class);
    private static final String STAGE1 = "Refill";
    private static final String STAGE2 = "UpdateAccumulators";

    private Account receiverAccount;
    private Amount amount;
    private String reference;
    private String refillProfileId;
    private String transactionType;
    private String nativeReference;
    private ERSHashtableParameter extraFields;

    /**
     * Parametrized constuctor for ERSRefillAndUpdateAccumulatorsProcessor
     * @param principalId
     * @param account
     * @param amount
     * @param reference
     * @param nativeReference
     * @param extraFields
     */
    public ERSRefillAndUpdateAccumulatorsProcessor(String principalId, Account account, Amount amount, String reference, String nativeReference, ERSHashtableParameter extraFields)
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
        response.getFields().put("STAGE1", STAGE1);
        response.getFields().put("STAGE2", STAGE2);

        String originalCurrency = amount.getCurrency();

        if(reference != null)
            extraFields.put("reference", reference);


        try
        {
            transactionType = extraFields.get("transactionType");
            String productSKUExtaField = extraFields.get("productSKU");
            String transactionType = extraFields.get("TRANSACTION_TYPE_KEY");

            logger.debug("Product SKU received from TXE " + productSKUExtaField);

            logger.debug("Going to call Refill ");
            UCIPResponse ucipRefillResponse = getConfig().getUcipAdaptor().getInstance().refill(receiverAccount.getAccountId(), amount, nativeReference, transactionType, refillProfileId, null,extraFields);

            if (ucipRefillResponse != null)
            {
                response.getFields().put("ucipResponseCode", String.valueOf(ucipRefillResponse.getUcipResponseCode()));
                if(getConfig().isTransformCurrencyEnabled()) {
                    transformAmountCurrency(amount, originalCurrency);
                    transformUcipResponseCurrency(ucipRefillResponse, originalCurrency);
                }

                if (ucipRefillResponse.getErsResponseCode() == ERSWSLinkResultCodes.SUCCESS || ucipRefillResponse.getErsResponseCode() == 1 || ucipRefillResponse.getErsResponseCode() == 2)
                {
                    response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
                    response.getFields().put("STAGE1_RESULT", "SUCCESS");
                    response.setBalanceBefore(ucipRefillResponse.getBalance());
                    response.setBalanceAfter(ucipRefillResponse.getBalanceAfter());


                    if (ucipRefillResponse.getSupervisionExpiryDateBefore() != null)
                    {
                        response.getFields().put(
                                "supervisionExpiryDateBefore",
                                getConfig().formatResponseDate(ucipRefillResponse.getSupervisionExpiryDateBefore()));
                    }

                    if (ucipRefillResponse.getSupervisionExpiryDateAfter() != null)
                    {
                        response.getFields().put(
                                "supervisionExpiryDateAfter",
                                getConfig().formatResponseDate(ucipRefillResponse.getSupervisionExpiryDateAfter()));
                    }
                    setRefillValueDedicatedAccountsBefore(ucipRefillResponse, response.getFields());

                    setRefillValueDedicatedAccountsAfter(ucipRefillResponse, response.getFields());

                    setRefillValueDedicatedAccountsPromotion(ucipRefillResponse, response.getFields());

                    if(getConfig().isPopulateResponseWithTreeDefinedFields())
                    {
                        populateFieldsWithTreeDefinedFields(ucipRefillResponse, response.getFields());
                    }

                    try
                    {
                        //UPDATE ACCUMULATORS CALL NOW

                        logger.debug("Going to call UpdateAccumulators ");
                        nativeReference = getConfig().createNativeReference(reference);
                        logger.debug("Generated new native reference: " + nativeReference);
                        final AccumulatorUpdateInformation[] accumulatorUpdateInformations = UCIPUtils.extractAccumulatorUpdateInformationParameters(extraFields);
                        UCIPResponse updateAccumulatorsResponse = getConfig().getUcipAdaptor().getInstance().updateAccumulators(receiverAccount.getAccountId(), nativeReference, accumulatorUpdateInformations, extraFields);

                        if (updateAccumulatorsResponse != null)
                        {
                            if (updateAccumulatorsResponse.getErsResponseCode() == ERSWSLinkResultCodes.SUCCESS || updateAccumulatorsResponse.getErsResponseCode() == 1 || updateAccumulatorsResponse.getErsResponseCode() == 2)
                            {
                                response.getFields().put("STAGE2_RESULT", "SUCCESS");
                                //extract any necessary parameters here
                            }
                            else
                            {
                                response.getFields().put("STAGE2_RESULT", "FAILURE");
                                logger.info("updateAccumulators Response Not Successful .");
                                logger.debug("ucip code: " + updateAccumulatorsResponse.getUcipResponseCode());
                                logger.debug("ers code: " + updateAccumulatorsResponse.getErsResponseCode());
                                logger.warn("No rollback as Refill call was successful");
                                // If response is not successful we are not reversing
                                // transaction because the earlier refill call was successful.

                            }
                        }
                        else
                        {
                            response.getFields().put("STAGE2_RESULT", "FAILURE");
                            logger.info("updateAccumulators Response is NUll .");
                            logger.warn("No rollback as Refill call was successful");
                            // If response is null, that is not successful we are not reversing
                            // transaction because the earlier refill call was successful.
                        }
                    }
                    catch (Exception e)
                    {
                        //If any exception occurs due to updateAccumulators, we are not reversing
                        //transaction since the earlier refill call was successful.
                        logger.error("Problem occurred while making updateAccumulators call: ", e);
                        logger.warn("No rollback as Refill call was successful");
                        response.getFields().put("STAGE2_RESULT", "FAILURE");
                    }
                }
                else
                {
                    response.getFields().put("STAGE1_RESULT", "FAILURE");
                    logger.info("Refill response not successful.");
                    //if response is not successful reverse transaction
                    response.setResultCode(ucipRefillResponse.getErsResponseCode());
                    logger.debug("ucip code: " + ucipRefillResponse.getUcipResponseCode());
                    logger.debug("ers code: " + ucipRefillResponse.getErsResponseCode());
                }
            }
            else
            {
                logger.info("No response from AIR node! check connectivity for receiver account.");
                response.setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
                response.getFields().put("STAGE1_RESULT", "FAILURE");
            }

        } catch (Exception e)
        {
            logger.error("Problem occurred while making refill call: ", e);
            response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
            response.getFields().put("STAGE1_RESULT", "FAILURE");
        }

        return response;
    }


    public String getRequestTypeId()
    {
        return getClass().getName();

    }

    public void setRefillValueDedicatedAccountsAfter(UCIPResponse ucipResponse, ERSHashtableParameter extraFields)
    {
        for (Map.Entry<Integer, Long> entry : ucipResponse.getDedicatedAccountValuesAfter().entrySet())
        {
            int dedicatedAccountId = entry.getKey();
            Long refillAmount1 = entry.getValue();
            if (refillAmount1 != null)
            {
                String extraFieldName = getConfig().getRefillValueTotalDAName(dedicatedAccountId);
                if (extraFieldName != null)
                {
                    extraFields.put(extraFieldName, refillAmount1.toString());
                }
            }
        }
    }

    public void setRefillValueDedicatedAccountsBefore(UCIPResponse ucipResponse, ERSHashtableParameter extraFields)
    {
        for (Map.Entry<Integer, Long> entry : ucipResponse.getDedicatedAccountValuesBefore().entrySet())
        {
            int dedicatedAccountId = entry.getKey();
            Long refillAmount1 = entry.getValue();
            if (refillAmount1 != null)
            {
                String extraFieldName = getConfig().getRefillValueTotalDAName(dedicatedAccountId);
                if (extraFieldName != null)
                {
                    extraFields.put(extraFieldName + "Before", refillAmount1.toString());
                }
            }
        }
    }

    public void setRefillValueDedicatedAccountsPromotion(UCIPResponse ucipResponse, ERSHashtableParameter extraFields)
    {
        for (Map.Entry<Integer, DedicatedAccountRefillInformation> entry : ucipResponse.getDedicatedAccountRefillValuePromotionMap().entrySet())
        {
            int dedicatedAccountId = entry.getKey();
            DedicatedAccountRefillInformation value = entry.getValue();
            String extraFieldName = getConfig().getRefillValuePromotionDAName(dedicatedAccountId);
            extraFieldName = extraFieldName + "_";
            if (extraFieldName != null)
            {
                if (value.getDedicatedAccountID() != null)
                {
                    extraFields.put(extraFieldName + "dedicatedAccountID", value.getDedicatedAccountID().toString());
                }
                if (value.getRefillAmount1() != null)
                {
                    extraFields.put(extraFieldName + "refillAmount1", value.getRefillAmount1().toString());
                }
                if (value.getExpiryDateExtended() != null)
                {
                    extraFields.put(extraFieldName + "expiryDateExtended", value.getExpiryDateExtended().toString());
                }
            }
        }
    }

    private void populateFieldsWithTreeDefinedFields(UCIPResponse ucipResponse, ERSHashtableParameter extraFields)
    {
        Map<String, TreeDefinedFieldInformation> treeDefinedFieldInformationMap = ucipResponse.getTreeDefinedFieldInformationMap();

        if(treeDefinedFieldInformationMap != null)
        {
            String prefix = getConfig().getTreeDefinedFieldsResponsePrefix();
            for(Map.Entry<String, TreeDefinedFieldInformation> entry: treeDefinedFieldInformationMap.entrySet())
            {
                TreeDefinedFieldInformation treeDefinedFieldInformation = entry.getValue();
                if(treeDefinedFieldInformation != null)
                {
                    extraFields.put(prefix + entry.getKey(), treeDefinedFieldInformation.getTreeDefinedFieldValue());
                }
            }
        }
    }


}

