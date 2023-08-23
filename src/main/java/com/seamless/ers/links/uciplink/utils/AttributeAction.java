package com.seamless.ers.links.uciplink.utils;

public enum AttributeAction
{
	ADD("ADD"),
	SET("SET"),
	DELETE("DELETE"),
	CLEAR("CLEAR");

	private String value;
	AttributeAction(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}

	public static AttributeAction lookup(String value)
	{
		final AttributeAction[] values = AttributeAction.values();
		for(AttributeAction val : values)
		{
			if(val.getValue().equalsIgnoreCase(value))
			{
				return val;
			}
		}

		return null;
	}

	@Override
	public String toString()
	{
		return "AttributeAction[" + "value='" + value + '\'' + ']';
	}
}
