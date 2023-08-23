package com.seamless.ers.links.uciplink.services.processors;

import com.seamless.ers.interfaces.ersifcommon.dto.Amount;
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.AccountTransactionResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;
import com.seamless.ers.links.uciplink.utils.UCIPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.seamless.ers.links.uciplink.utils.UCIPUtils.setupOfferInfo;

public class ERSUpdateOffer2Processor extends AbstractRequestProcessor<AccountTransactionResponse>
{
    Logger logger = LoggerFactory.getLogger(ERSUpdateOffer2Processor.class);
    private static final String STAGE1 = "UpdateOffer";

    private Account receiverAccount;
    private Amount amount;
    private String reference;
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
    public ERSUpdateOffer2Processor(String principalId, Account account, Amount amount, String reference, String nativeReference, ERSHashtableParameter extraFields)
    {
        super(new AccountTransactionResponse());

        this.receiverAccount = account;
        this.amount = amount;
        this.reference = reference;
        this.nativeReference = nativeReference;
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


        if (this.getConfig().isValidateSubscriberLocation() != null)
        {
            extraFields.put("validateSubscriberLocation", this.getConfig().isValidateSubscriberLocation().toString());
        }

        AccountTransactionResponse response = new AccountTransactionResponse();
        response.getFields().put("STAGE1", STAGE1);
        response.setFields(new ERSHashtableParameter());

        String currency = null;
        if(this.getConfig().isUpdateOffer2CurrencyEnable())
        {
            currency = amount.getCurrency();

        }

        if(reference != null)
            extraFields.put("reference", reference);


        try
        {
            transactionType = extraFields.get("transactionType");
            // Get receiver account information, it will confirm that receiver
            // account exists in the system.
            //HashMap<String, OfferProductsDTO> offerSKUMapp = (HashMap<String, OfferProductsDTO>) getConfig().getOfferListMap();
            String productSKUExtaField = extraFields.get("productSKU");
            String transactionType = extraFields.get("TRANSACTION_TYPE_KEY");

            logger.debug("Product SKU received from TXE " + productSKUExtaField);

            final OfferProductsDTO offerProductsDTO = setupOfferInfo(extraFields);

            logger.debug("Going to call UpdateOffer ");
            UCIPResponse updateOfferResponse = getConfig().getUcipAdaptor().getInstance().updateOffer(receiverAccount.getAccountId(), amount, nativeReference, transactionType,null, 0, extraFields, offerProductsDTO, currency);

            if (updateOfferResponse != null)
            {
                if (updateOfferResponse.getErsResponseCode() == ERSWSLinkResultCodes.SUCCESS || updateOfferResponse.getErsResponseCode() == 1 || updateOfferResponse.getErsResponseCode() == 2)
                {
                    response.getFields().put("STAGE1_RESULT", "SUCCESS");
                    UCIPUtils.updateOfferResponseSuccess(response, updateOfferResponse);
                }
                else
                {
                    logger.info("UpdateOffer Response Not Successful.");
                    //if response is not successful reverse transaction
                    response.setResultCode(updateOfferResponse.getErsResponseCode());
                    logger.debug("ucip code: " + updateOfferResponse.getUcipResponseCode());
                    logger.debug("ers code: " + updateOfferResponse.getErsResponseCode());
                    response.getFields().put("STAGE1_RESULT", "FAILURE");
                }
            }
            else
            {
                logger.info("UpdateOffer response is null .");
                response.setResultCode(ERSWSLinkResultCodes.LINK_ERROR);
                response.getFields().put("STAGE1_RESULT", "FAILURE");
            }

        }
        catch (Exception e)
        {
            logger.error("Problem occurred while processing UpdateOffer call: ", e);
            response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
            response.getFields().put("STAGE1_RESULT", "FAILURE");
        }

        return response;
    }


    public String getRequestTypeId()
    {
        return getClass().getName();

    }


}

