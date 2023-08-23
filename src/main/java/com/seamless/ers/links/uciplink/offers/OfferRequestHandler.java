package com.seamless.ers.links.uciplink.offers;


import com.seamless.common.ERSConfigurationException;
import com.seamless.common.ExtendedProperties;
import com.seamless.common.config.ConfigurationFileHandler;
import com.seamless.common.config.PropertiesUpdateHandler;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class executes processors based on rule ids and maintain the mapping with help of a property file,
 * and the rule ids have to be matched by the rule ids defined in the txe businesslogic property file.
 * <p>
 * The mapping between ruleIds and processors is defined with the following syntax in the property file:
 * <ul>
 * <li><rule identifier>.<method name>.<key>=<value>
 * </ul>
 * where method name is either validate, failed, and/or completed.
 */
public class OfferRequestHandler implements PropertiesUpdateHandler
{

    Logger logger = LoggerFactory.getLogger(OfferRequestHandler.class);
    UCIPLinkConfig config;
    HashMap<String, OfferProductsDTO> offerSKUMapp;
    OfferProductsDTO offerProductsDTO;
    List<String> mappingSKUList;


    /**
     * Parametrized constructor for OfferRequestHandler
     *
     * @param config
     * @param fileName
     * @throws ERSConfigurationException
     */
    public OfferRequestHandler(UCIPLinkConfig config, String fileName) throws ERSConfigurationException
    {
        this.config = config;
        config.addFile(fileName, new ConfigurationFileHandler()
        {
            public void loadConfiguration(File file) throws ERSConfigurationException
            {
                try
                {
                    logger.info("Loading configuration from: ");
                    ExtendedProperties properties = new ExtendedProperties();
                    properties.loadFromFile(file);
                    propertiesLoaded(properties);
                } catch (IOException e)
                {
                    throw new ERSConfigurationException("Failed to read: " + "SKU Mapping file", "SKU Mapping file", e);
                }
            }
        });
    }

    /**
     * Loading Offer Properties
     *
     * @param properties
     * @throws ERSConfigurationException
     */
    public void propertiesLoaded(ExtendedProperties properties) throws ERSConfigurationException
    {

        mappingSKUList = new ArrayList<String>(properties.getStringListProperty("updateOfferSkuList",new ArrayList<String>(), ","));
        ExtendedProperties offerProperties = properties.getSubProperties("product.");
        offerSKUMapp = new HashMap<String, OfferProductsDTO>();

        for (String sku : mappingSKUList)
        {
            List<String> dIdList = offerProperties.getStringListProperty(sku + ".daIDList", new ArrayList<String>(), ",");
            Integer offerId = offerProperties.getIntegerProperty(sku + ".offerID", 0, true);
            String packageName = offerProperties.getProperty(sku + ".packageName");
            String amount = offerProperties.getProperty(sku + ".amount");
            String mbValue = offerProperties.getProperty(sku + ".mb");
            List<String> validityList = offerProperties.getStringListProperty(sku + ".validityList", new ArrayList<String>(), ",");
            String validityType = offerProperties.getProperty(sku + ".validity_type");
            List<String> daAmountList = offerProperties.getStringListProperty(sku + ".daAmountList", new ArrayList<String>(), ",");

            offerProductsDTO = new OfferProductsDTO(dIdList, offerId, packageName, amount, daAmountList, mbValue, validityList, validityType);
            offerSKUMapp.put(sku, offerProductsDTO);
        }
    }

    public Map getOfferListMap()
    {
        return offerSKUMapp;
    }

    public List<String> getMappingSKUList()
    {
        return mappingSKUList;
    }
}
