package com.seamless.ers.links.uciplink;

import java.util.List;

import com.seamless.common.ERSConfigurationException;
import com.seamless.common.ExtendedProperties;
import com.seamless.common.StringUtils;

public class UCIPNumberFormatter
{
	private List<String> msisdnStripPrefixes = null;
	private String msisdnAddPrefix = null;
	private int msisdnSignificantDigits;
	private int strip_msisdn_zeros = 0;

	public 
	UCIPNumberFormatter(ExtendedProperties properties)
		throws ERSConfigurationException
	{
		msisdnSignificantDigits = StringUtils.parseInt(properties.getProperty("msisdn_significant"), 0);
		msisdnStripPrefixes = properties.getStringListProperty("msisdn_strip_prefixes", null, " , ");
		msisdnAddPrefix = properties.getProperty("msisdn_add_prefix", null, false);
		strip_msisdn_zeros = StringUtils.parseInt(properties.getProperty("strip_msisdn_zeros"), 0);
	}
	
	public String formatMsisdn(String msisdn)
	{
		if (msisdn == null)
		{
			return null;
		}

		if (strip_msisdn_zeros > 0)
		{
		   for(int count = 0; count < strip_msisdn_zeros; count++)
		   {
			   if(msisdn.startsWith("0")) msisdn = msisdn.substring(1);
		   }
		}

		if (msisdnStripPrefixes != null)
		{
			for (String prefix : msisdnStripPrefixes)
			{
				if (msisdn.startsWith(prefix))
				{
					msisdn = msisdn.substring(prefix.length());
					break;
				}
			}
		}
		if (msisdnSignificantDigits > 0)
		{
			int pos = msisdn.length() - msisdnSignificantDigits;
			if (pos > 0)
			{
				msisdn = msisdn.substring(pos);
			}
		}
		if (msisdnAddPrefix != null)
		{
			msisdn = msisdnAddPrefix + msisdn;
		}
		return msisdn;
	}
}
