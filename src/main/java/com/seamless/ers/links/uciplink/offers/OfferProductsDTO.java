package com.seamless.ers.links.uciplink.offers;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This OfferProductsDTO is for keeping data for every offer from configurations
 */
public class OfferProductsDTO
{
    private List<String> daIDList;
    private Integer offerID;
    private String packageName;
    private String amount;
    private List<String> daAmountList;
    private String mb;
    private List<String> validityList;
    private String validityType;
    private Date startDate;
    private Date endDate;
    private Integer offerType;


    /**
     * Parametrized version of OfferProductsDTO
     * @param daID
     * @param offerID
     * @param packageName
     * @param amount
     * @param daAmount
     * @param mb
     * @param validityList validity in case of multiple
     * @param validityType
     */
    public OfferProductsDTO(List<String> daID, Integer offerID, String packageName, String amount, List<String> daAmount, String mb, List<String> validityList, String validityType)
    {
        this.daIDList = daID;
        this.offerID = offerID;
        this.packageName = packageName;
        this.amount = amount;
        this.daAmountList = daAmount;
        this.mb = mb;
        this.validityList = validityList;
        this.validityType = validityType;
    }

    public OfferProductsDTO()
    {
    }

    public String getPackageName()
    {
        return packageName;
    }


    public void setPackageName(String packageName)
    {
        this.packageName = packageName;
    }


    public String getAmount()
    {
        return amount;
    }


    public void setAmount(String amount)
    {
        this.amount = amount;
    }


    public String getMb()
    {
        return mb;
    }


    public void setMb(String mb)
    {
        this.mb = mb;
    }


    public List<String> getValidityList()
    {
        return this.validityList;
    }

    public void setValidityList(List<String> validityList)
    {
        this.validityList = validityList;
    }

    public String getValidityType()
    {
        return validityType;
    }


    public void setValidityType(String validityType)
    {
        this.validityType = validityType;
    }

    public List<String> getDaIDList()
    {
        return daIDList;
    }

    public void setDaIDList(List<String> daIDList)
    {
        this.daIDList = daIDList;
    }

    public Integer getOfferID()
    {
        return offerID;
    }

    public void setOfferID(Integer offerID)
    {
        this.offerID = offerID;
    }

    public List<String> getDaAmountList()
    {
        return daAmountList;
    }

    public void setDaAmountList(List<String> daAmountList)
    {
        this.daAmountList = daAmountList;
    }

    public Integer getOfferType()
    {
        return offerType;
    }

    public void setOfferType(Integer offerType)
    {
        this.offerType = offerType;
    }

    /**
     * Get Start Date against provided validity
     * @param validity
     * @return
     */
    public Date getStartDate(String validity)
    {
        if ("HOURS".equalsIgnoreCase(validityType))
        {
            startDate = getDateForTime(validity.split("-")[0]);
        }
        else if("SECONDS".equalsIgnoreCase(validityType))
        {
            startDate = new Date();
        }
        else if("MINUTES".equalsIgnoreCase(validityType))
        {
            startDate = new Date();
        }
        else
        {
            startDate = new Date();
        }

        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    /**
     * Calculates end Date based on Validity provided
     *
     * @return Date
     */
    public Date getEndDate(String validity)
    {
        Calendar calEndDate = Calendar.getInstance();
        Calendar calStartDate = Calendar.getInstance();
        startDate = getStartDate(validity);
        calStartDate.setTime(startDate);

        if ("HOURS".equalsIgnoreCase(validityType))
        {
            endDate = getDateForTime(validity.split("-")[1]);
            calEndDate.setTime(endDate);
        }
        else if("SECONDS".equalsIgnoreCase(validityType))
        {
            final String valueInSeconds = validity;
            calEndDate.setTime(startDate);
            calEndDate.add(Calendar.SECOND, Integer.parseInt(valueInSeconds));
            endDate = calEndDate.getTime();
        }
        else if("MINUTES".equalsIgnoreCase(validityType))
        {
            final String valueInMinutes = validity;
            calEndDate.setTime(startDate);
            calEndDate.add(Calendar.MINUTE, Integer.parseInt(valueInMinutes));
            endDate = calEndDate.getTime();
        }
        else
        {
            calEndDate.setTime(new Date());
            calEndDate.add(Calendar.DATE, Integer.parseInt(validity));
            endDate = calEndDate.getTime();
        }

        if(endDate.getTime() < startDate.getTime())
        {
            calEndDate.add(Calendar.DATE, 1);
            endDate = calEndDate.getTime();
        }
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    /**
     * Calculate End date based on time interval provided in form of 3am-5pm
     *
     * @param timeInterval
     * @return
     */
    public static Date getDateForTime(String timeInterval)
    {
        Calendar cal = Calendar.getInstance();

        if (timeInterval.contains("am"))
        {
            cal.set(Calendar.HOUR, Integer.parseInt(timeInterval.substring(0, timeInterval.indexOf("am"))));
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.AM_PM, Calendar.AM);
        }
        else if (timeInterval.contains("pm"))
        {
            cal.set(Calendar.HOUR, Integer.parseInt(timeInterval.substring(0, timeInterval.indexOf("pm"))));
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.AM_PM, Calendar.PM);
        }

        if (cal.getTime().before(new Date()))
        {
            cal.add(Calendar.DATE, 1);
        }
        return cal.getTime();
    }
}
