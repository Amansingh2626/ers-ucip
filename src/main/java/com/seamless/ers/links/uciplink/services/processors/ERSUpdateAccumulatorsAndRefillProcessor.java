package com.seamless.ers.links.uciplink.services.processors;

import com.seamless.common.uciplib.common.AccumulatorUpdateInformation;
import com.seamless.common.uciplib.common.DedicatedAccountChangeInformation;
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

import java.util.*;


public class ERSUpdateAccumulatorsAndRefillProcessor extends AbstractRequestProcessor<AccountTransactionResponse>
{
    Logger logger = LoggerFactory.getLogger(ERSUpdateAccumulatorsAndRefillProcessor.class);
    private static final String STAGE1 = "UpdateAccumulators";
    private static final String STAGE2 = "Refill";

    private Account receiverAccount;
    private Amount amount;
    private String reference;
    private String refillProfileId;
    private String transactionType;
    private String nativeReference;
    private ERSHashtableParameter extraFields;

    /**
     * Parametrized constuctor for ERSUpdateAccumulatorsAndRefillProcessor
     * @param principalId
     * @param account
     * @param amount
     * @param reference
     * @param nativeReference
     * @param extraFields
     */
    public ERSUpdateAccumulatorsAndRefillProcessor(String principalId, Account account, Amount amount, String reference, String nativeReference, ERSHashtableParameter extraFields)
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
        logger.debug("Request received to topup - info: [" + receiverAccount + " : " + amount + "]");

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

            logger.debug("Going to call UpdateAccumulators ");
            final AccumulatorUpdateInformation[] accumulatorUpdateInformations = UCIPUtils.extractAccumulatorUpdateInformationParameters(extraFields);
            UCIPResponse updateAccumulatorResponse = getConfig().getUcipAdaptor().getInstance().updateAccumulators(receiverAccount.getAccountId(), nativeReference, accumulatorUpdateInformations, extraFields);

            if (updateAccumulatorResponse != null)
            {
                if (updateAccumulatorResponse.getErsResponseCode() == ERSWSLinkResultCodes.SUCCESS || updateAccumulatorResponse.getErsResponseCode() == 1 || updateAccumulatorResponse.getErsResponseCode() == 2)
                {
                    //extract any parameters needed here from updateAccumulatorsResponse
                    response.getFields().put("STAGE1_RESULT", "SUCCESS");

                    logger.debug("Going to call Refill ");
                    nativeReference = getConfig().createNativeReference(reference);
                    logger.debug("Generated new native reference: " + nativeReference);
                    try
                    {
                        UCIPResponse ucipRefillResponse = getConfig().getUcipAdaptor().getInstance().refill(receiverAccount.getAccountId(), amount, this.nativeReference, transactionType, refillProfileId, null, extraFields);

                        if (ucipRefillResponse != null)
                        {
                            response.getFields().put("ucipResponseCode", String.valueOf(ucipRefillResponse.getUcipResponseCode()));
                            if (getConfig().isTransformCurrencyEnabled())
                            {
                                transformAmountCurrency(amount, originalCurrency);
                                transformUcipResponseCurrency(ucipRefillResponse, originalCurrency);
                            }

                            if (ucipRefillResponse.getErsResponseCode() == ERSWSLinkResultCodes.SUCCESS || ucipRefillResponse.getErsResponseCode() == 1 || ucipRefillResponse.getErsResponseCode() == 2)
                            {
                                response.getFields().put("STAGE2_RESULT", "SUCCESS");
                                response.setBalanceBefore(ucipRefillResponse.getBalance());
                                response.setBalanceAfter(ucipRefillResponse.getBalanceAfter());
                                response.setResultCode(ERSWSLinkResultCodes.SUCCESS);

                                if (ucipRefillResponse.getSupervisionExpiryDateBefore() != null)
                                {
                                    response.getFields().put("supervisionExpiryDateBefore", getConfig().formatResponseDate(ucipRefillResponse.getSupervisionExpiryDateBefore()));
                                }

                                if (ucipRefillResponse.getSupervisionExpiryDateAfter() != null)
                                {
                                    response.getFields().put("supervisionExpiryDateAfter", getConfig().formatResponseDate(ucipRefillResponse.getSupervisionExpiryDateAfter()));
                                }
                                setRefillValueDedicatedAccountsBefore(ucipRefillResponse, response.getFields());

                                setRefillValueDedicatedAccountsAfter(ucipRefillResponse, response.getFields());

                                setRefillValueDedicatedAccountsPromotion(ucipRefillResponse, response.getFields());

                                if (getConfig().isPopulateResponseWithTreeDefinedFields())
                                {
                                    populateFieldsWithTreeDefinedFields(ucipRefillResponse, response.getFields());
                                }

                            }
                            else
                            {

                                logger.warn("Refill call failed.");
                                response.setResultCode(ucipRefillResponse.getErsResponseCode());
                                logger.debug("ucip code: " + ucipRefillResponse.getUcipResponseCode());
                                logger.debug("ers code: " + ucipRefillResponse.getErsResponseCode());
                                response.getFields().put("STAGE2_RESULT", "FAILURE");
                                // reset accumulators
                                logger.debug("Going to Reset Accumulators");
                                nativeReference = getConfig().createNativeReference(reference);
                                logger.debug("Generated new native reference: " + nativeReference);
                                try
                                {
                                    List<AccumulatorUpdateInformation> accs = new ArrayList<>();
                                    for(AccumulatorUpdateInformation accumulatorUpdateInformation : accumulatorUpdateInformations)
                                    {
                                        if(accumulatorUpdateInformation.getAccumulatorValueAbsolute() != null)
                                        {
                                            AccumulatorUpdateInformation accInfo = new AccumulatorUpdateInformation(accumulatorUpdateInformation.getAccumulatorID());
                                            accInfo.setAccumulatorValueAbsolute(0);
                                            accs.add(accInfo);
                                        }
                                        else if(accumulatorUpdateInformation.getAccumulatorValueRelative() != null)
                                        {
                                            int val = accumulatorUpdateInformation.getAccumulatorValueRelative() * -1;
                                            AccumulatorUpdateInformation accInfo = new AccumulatorUpdateInformation(accumulatorUpdateInformation.getAccumulatorID());
                                            accInfo.setAccumulatorValueRelative(val);
                                            accs.add(accInfo);
                                        }
                                    }

                                    if(!accs.isEmpty())
                                    {
                                        UCIPResponse resetUpdateAccumulatorResponse = getConfig().getUcipAdaptor().getInstance().updateAccumulators(receiverAccount.getAccountId(), nativeReference, accs.toArray(new AccumulatorUpdateInformation[accs.size()]), extraFields);
                                        if(resetUpdateAccumulatorResponse != null)
                                        {
                                            final int ucipResponseCode = resetUpdateAccumulatorResponse.getUcipResponseCode();
                                            logger.warn("Resetting accumulators returned with response: " + ucipResponseCode);
                                        }
                                        else
                                        {
                                            logger.warn("Resetting update accumulators returned null");
                                        }
                                    }
                                    else
                                    {
                                        logger.warn("No accumulators found to need resetting");
                                    }


                                }
                                catch (Exception e)
                                {
                                    logger.error("Resetting accumulators failed: " + e.getMessage() , e);
                                }


                            }
                        }
                        else
                        {
                            logger.warn("No response from AIR node! check connectivity for receiver account.");
                            response.setResultCode(ERSWSLinkResultCodes.LINK_DOWN);
                            response.getFields().put("STAGE2_RESULT", "FAILURE");

                            logger.debug("Going to Reset Accumulators");
                            nativeReference = getConfig().createNativeReference(reference);
                            logger.debug("Generated new native reference: " + nativeReference);
                            try
                            {
                                List<AccumulatorUpdateInformation> accs = new ArrayList<>();
                                for(AccumulatorUpdateInformation accumulatorUpdateInformation : accumulatorUpdateInformations)
                                {
                                    if(accumulatorUpdateInformation.getAccumulatorValueAbsolute() != null)
                                    {
                                        AccumulatorUpdateInformation accInfo = new AccumulatorUpdateInformation(accumulatorUpdateInformation.getAccumulatorID());
                                        accInfo.setAccumulatorValueAbsolute(0);
                                        accs.add(accInfo);
                                    }
                                    else if(accumulatorUpdateInformation.getAccumulatorValueRelative() != null)
                                    {
                                        int val = accumulatorUpdateInformation.getAccumulatorValueRelative() * -1;
                                        AccumulatorUpdateInformation accInfo = new AccumulatorUpdateInformation(accumulatorUpdateInformation.getAccumulatorID());
                                        accInfo.setAccumulatorValueRelative(val);
                                        accs.add(accInfo);
                                    }
                                }

                                if(!accs.isEmpty())
                                {
                                    UCIPResponse resetUpdateAccumulatorResponse = getConfig().getUcipAdaptor().getInstance().updateAccumulators(receiverAccount.getAccountId(), nativeReference, accs.toArray(new AccumulatorUpdateInformation[accs.size()]), extraFields);
                                    if(resetUpdateAccumulatorResponse != null)
                                    {
                                        final int ucipResponseCode = resetUpdateAccumulatorResponse.getUcipResponseCode();
                                        logger.warn("Resetting accumulators returned with response: " + ucipResponseCode);
                                    }
                                    else
                                    {
                                        logger.warn("Resetting update accumulators returned null");
                                    }
                                }
                                else
                                {
                                    logger.warn("No accumulators found to need resetting");
                                }


                            }
                            catch (Exception e)
                            {
                                logger.error("Resetting accumulators failed: " + e.getMessage() , e);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        logger.error("Problem occurred while making Refill call: ", e);
                        response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
                        response.getFields().put("STAGE2_RESULT", "FAILURE");
                    }
                }
                else
                {
                    logger.info("UpdateAccumulators Response Not Successful.");
                    //if response is not successful reverse transaction
                    response.setResultCode(updateAccumulatorResponse.getErsResponseCode());
                    logger.debug("ucip code: " + updateAccumulatorResponse.getUcipResponseCode());
                    logger.debug("ers code: " + updateAccumulatorResponse.getErsResponseCode());
                    response.getFields().put("STAGE1_RESULT", "FAILURE");
                }
            }
            else
            {
                logger.info("UpdateAccumulators response is null .");
                response.setResultCode(ERSWSLinkResultCodes.LINK_ERROR);
                response.getFields().put("STAGE1_RESULT", "FAILURE");
            }

        }
        catch (Exception e)
        {
            logger.error("Problem occurred while processing UpdateAccumulators + Refill call: ", e);
            response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
            response.getFields().put("STAGE1_RESULT", "FAILURE");
        }

        return response;
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
                response.getFields().put("Update balance dedicatedAccountActiveValue1", dedicatedAccountInfo.getDedicatedAccountActiveValue1().toString());
            }
            response.getFields().put("Update balance DedicatedAccountID", dedicatedAccountInfo.getDedicatedAccountID().toString());
            response.getFields().put("Update balance DedicatedAccountUnit", dedicatedAccountInfo.getDedicatedAccountUnit().toString());
            if(dedicatedAccountInfo.getDedicatedAccountValue1()!=null)
            {
                response.getFields().put("Update balance DedicatedAccountValue1", dedicatedAccountInfo.getDedicatedAccountValue1().toString());
            }
            response.getFields().put("Update balanceExpiryDate", dedicatedAccountInfo.getExpiryDate().toString());
            if(dedicatedAccountInfo.getOfferID()!=null)
            {
                response.getFields().put("Update balance OfferID", dedicatedAccountInfo.getOfferID().toString());
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

