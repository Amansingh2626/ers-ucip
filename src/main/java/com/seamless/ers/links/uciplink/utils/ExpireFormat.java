package com.seamless.ers.links.uciplink.utils;

public enum ExpireFormat
{
	DATE("DATE"),
	DATETIME("DATETIME"),
	DATE_RELATIVE("DATE_RELATIVE");

	ExpireFormat(String value)
	{
		this.value = value;
	}

	private String value;

	public static ExpireFormat lookup(String expiryFormat)
	{
		for(ExpireFormat format : ExpireFormat.values())
		{
			if(format.value.equalsIgnoreCase(expiryFormat))
			{
				return format;
			}
		}
		return null;
	}

}
