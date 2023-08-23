package com.seamless.ers.links.uciplink.test;

import static org.junit.Assert.assertEquals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

import com.seamless.common.ExtendedProperties;
import com.seamless.ers.links.uciplink.UCIPNumberFormatter;

public class TestNumberFormatting
{

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Test
	public void testLiberiaMSISDNFormatting() throws Exception
	{
		ExtendedProperties properties = new ExtendedProperties();
		properties
				.loadFromFile("src/test/resources/conf/liberia_formatting.properties");
		UCIPNumberFormatter config = new UCIPNumberFormatter(properties);
		assertEquals("8812345678", config.formatMsisdn("2318812345678"));
		assertEquals("6123456", config.formatMsisdn("2316123456"));
	}

	@Test
	public void testSwedishMSISDNFormatting() throws Exception
	{
		ExtendedProperties properties = new ExtendedProperties();
		properties
				.loadFromFile("src/test/resources/conf/sweden_formatting.properties");
		UCIPNumberFormatter config = new UCIPNumberFormatter(properties);
		assertEquals("708369925", config.formatMsisdn("46708369925"));
	}
	
	@Test
	public void testBrasilMSISDNFormatting() throws Exception
	{
		ExtendedProperties properties = new ExtendedProperties();
		properties
				.loadFromFile("src/test/resources/conf/brasil_formatting.properties");
		UCIPNumberFormatter config = new UCIPNumberFormatter(properties);
		assertEquals("7910000001", config.formatMsisdn("557910000001"));
	}
}
