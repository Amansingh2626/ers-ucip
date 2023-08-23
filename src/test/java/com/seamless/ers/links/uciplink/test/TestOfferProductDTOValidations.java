package com.seamless.ers.links.uciplink.test;

import com.seamless.common.ERSConfigurationException;
import com.seamless.common.ExtendedProperties;
import com.seamless.ers.links.uciplink.offers.OfferProductsDTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.lang.reflect.Method;
import java.util.*;

/**
 * OfferProductsDTO Tester.
 *
 * @author Bilal Mirza
 * @version 1.0
 * @since <pre>Aug 5, 2016</pre>
 */
public class TestOfferProductDTOValidations
{
    HashMap<String, OfferProductsDTO> offerSKUMapp;
    OfferProductsDTO offerProductsDTO;
    ArrayList<String> mappingSKUList;
    Date startDate;
    Date endDate;

    @org.junit.Before
    public void before() throws Exception
    {
        ExtendedProperties properties = new ExtendedProperties();
        properties.loadFromFile("src/test/resources/conf/vas_mapping.properties");
        propertiesLoaded(properties);
    }

    @After
    public void after() throws Exception
    {
    }

    /**
     * Method: getDateForTime(String timeInterval)
     */
    @Test
    public void testStartAndEndDatesForHoursValidity() throws Exception
    {
        for(String sku: offerSKUMapp.keySet())
        {
            offerProductsDTO = offerSKUMapp.get(sku);
            checkProductValidities(offerProductsDTO);
        }
    }

    private void checkProductValidities(OfferProductsDTO offerProductsDTO)
    {
        for(String interval : offerProductsDTO.getValidityList())
        {
            endDate = offerProductsDTO.getEndDate(interval);
            startDate = offerProductsDTO.getStartDate(interval);

            System.out.println("================= Validity = " + interval + " =================");
            System.out.println("Start Date : " + startDate);
            System.out.println("End Date : " + endDate);

            Assert.assertTrue(startDate.getTime() <= endDate.getTime());
        }
    }

    public void propertiesLoaded(ExtendedProperties properties) throws ERSConfigurationException
    {

        mappingSKUList = new ArrayList<String>(properties.getStringListProperty("updateOfferSkuList",null, ","));
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

} 
