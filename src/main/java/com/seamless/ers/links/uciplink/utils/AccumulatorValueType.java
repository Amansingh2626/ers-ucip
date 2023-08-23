package com.seamless.ers.links.uciplink.utils;

public enum AccumulatorValueType
{
	VALUE_ABSOLUTE("VALUE_ABSOLUTE"),
	VALUE_RELATIVE("VALUE_RELATIVE"),
	START_DATE("START_DATE");

	private String value;
	AccumulatorValueType(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}

	public static AccumulatorValueType lookup(String value)
	{
		final AccumulatorValueType[] values = AccumulatorValueType.values();
		for(AccumulatorValueType val : values)
		{
			if(val.getValue().equalsIgnoreCase(value))
			{
				return val;
			}
		}

		return null;
	}

}
