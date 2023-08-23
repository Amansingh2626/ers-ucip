package com.seamless.ers.links.uciplink.config;

public class ResultRow
{
	private Integer ucipCode;
	private Integer ersCode;
	private String description;

	public ResultRow(Integer ucipCode, Integer ersCode, String description)
	{
		this.ucipCode = ucipCode;
		this.ersCode = ersCode;
		this.description = description;
	}

	public Integer getUcipCode()
	{
		return ucipCode;
	}

	public void setUcipCode(Integer ucipCode)
	{
		this.ucipCode = ucipCode;
	}

	public Integer getErsCode()
	{
		return ersCode;
	}

	public void setErsCode(Integer ersCode)
	{
		this.ersCode = ersCode;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	@Override
	public String toString()
	{
		return "ResultRow{" + "ucipCode='" + ucipCode + '\'' + ", ersCode='" + ersCode + '\'' + ", description='" + description + '\'' + '}';
	}
}
