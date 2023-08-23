package com.seamless.ers.links.uciplink.services.processors;

import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter;
import com.seamless.ers.interfaces.ersifcommon.dto.accounts.Account;
import com.seamless.ers.interfaces.ersifextlink.ERSWSLinkResultCodes;
import com.seamless.ers.interfaces.ersifextlink.dto.ERSLinkResponse;
import com.seamless.ers.links.uciplink.commandProcessor.AbstractRequestProcessor;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;
import com.seamless.ers.links.uciplink.ucip.UCIPResponse;
import com.seamless.ers.links.uciplink.utils.UCIPUtils;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ERSDeleteOfferProcessor extends AbstractRequestProcessor<ERSLinkResponse>
{
    private static final Logger LOG = LoggerFactory.getLogger(ERSDeleteOfferProcessor.class);

    private String ersReferenceNumber;

    private String nativeReferenceNumber;

    private Account subscriberAccount;

    private ERSHashtableParameter extraFields;

    public ERSDeleteOfferProcessor(String ersReferenceNumber, String nativeReferenceNumber, Account subscriberAccount, ERSHashtableParameter extraFields)
    {
        super(new ERSLinkResponse());
        this.ersReferenceNumber = ersReferenceNumber;
        this.nativeReferenceNumber = nativeReferenceNumber;
        this.subscriberAccount = subscriberAccount;
        this.extraFields = extraFields;
    }

    @Override
    public String getRequestTypeId()
    {
        return getClass().getName();
    }

    @Override
    public void process()
    {
        EtmPoint point = EtmManager.getEtmMonitor().createPoint("ERSDeleteOfferProcessor::process");
        ERSLinkResponse response = new ERSLinkResponse();
        try
        {
            this.getConfig().formatMsisdn(subscriberAccount);
            LOG.info("Reference no = {}", ersReferenceNumber);
            LOG.info("Native reference no = {}", nativeReferenceNumber);
            LOG.info("Subscriber = {}", subscriberAccount);
            LOG.info("+++++++ extraFields : [ {} ] +++++++", extraFields);
            OfferProductsDTO offerProductsDTO = UCIPUtils.setupOfferInfo(extraFields);
            UCIPResponse ucipResponse = this.getConfig()
                                            .getUcipAdaptor()
                                            .getInstance()
                                            .deleteOffer(nativeReferenceNumber,
                                                        subscriberAccount.getAccountId(),
                                                        offerProductsDTO,
                                                        extraFields);
            LOG.info("isDeleteOfferSuccessful = {}", ucipResponse.isSuccessful());
            if(ucipResponse.isSuccessful())
            {
                response.setResultCode(ERSWSLinkResultCodes.SUCCESS);
            }
            else
            {
                response.setResultCode(ucipResponse.getErsResponseCode());
            }
        }
        catch(Exception e)
        {
            LOG.error("Problem occured while retrieving account information: ", e);
            response.setResultCode(ERSWSLinkResultCodes.INTERNAL_FAILED);
        }
        finally
        {
            setResult(response);
            if(point != null)
            {
                point.collect();
            }
        }
    }
}
