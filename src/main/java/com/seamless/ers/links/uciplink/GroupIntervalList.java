package com.seamless.ers.links.uciplink;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seamless.common.ExtendedProperties;


/*
service_class_range.GROUP_A.1.values=1,2,3,4
service_class_range.GROUP_A.2.range=10, 50

service_class_range.GROUP_B.1.values=500, 510
service_class_range.GROUP_B.2.range=600, 700
 */

/**
 * The class use the sweep and prune algorithm for mappings of group identities to intervals.
 * <p>
 * A group can contain one or more values as either fixed values or ranges as the following example configuration:
 * <p>
 * service_class_range.GROUP_A.1.values=1,2,3,4<p>
 * service_class_range.GROUP_A.2.range=10, 50<p>
 * service_class_range.GROUP_B.1.values=500, 510<p>
 * service_class_range.GROUP_B.2.range=600, 700<p>
 *
 */
public class GroupIntervalList
{
	static Logger log = LoggerFactory.getLogger(GroupIntervalList.class);

	List<Interval> intervalList = new ArrayList<Interval>();
	HashMap<Integer, Interval> lookupMap = new HashMap<Integer, Interval>();
	
	
	/**
	 * A data class to contain an intervalue.
	 *
	 */
	public class Interval
	{
		private String identity;
		private int min;
		private int max;
		
		public Interval(String identity, int min, int max)
		{
			this.identity = identity;
			this.min = min;
			this.max = max;
		}
		
		public boolean isRange() 
		{
			return min != max;
		}
		
		public boolean isFixed() 
		{
			return min == max;
		}
		
		public String getIdentity()
		{
			return identity;
		}
		
		@Override
		public String toString()
		{
			return identity + "[" + min + ", " + max + "]";
		}

		public int getMin()
		{
			return min;
		}
		
		public int getMax()
		{
			return max;
		}
		
	}
	
	public GroupIntervalList(ExtendedProperties props) throws Exception
	{		
		
		ExtendedProperties groupProperties = new ExtendedProperties("service_class_range.", props);
		
		Set<String> groupIds = groupProperties.truncatedKeys(".");
		for (String groupId : groupIds)
		{
			int index = 1;
			while (true)
			{
				ExtendedProperties mappingProperties = new ExtendedProperties(groupId + "." + index + ".", groupProperties);
				Map<String, String> valueMap = mappingProperties.getListProperty("values", null, ",", true, false);
				Map<String, String> rangeMap = mappingProperties.getListProperty("range", null, ",", true, false);
						
				log.debug("Read service_class_range property");

				if (valueMap != null && rangeMap != null)
				{
					throw new Exception("Syntax error while reading service class range group: " + groupId + " - A group index can only contain either a range or a value list");
				}
				
				if (valueMap != null)
				{
					log.debug("Value map for groupId: " + valueMap);
				}
				
				if (rangeMap != null)
				{
					log.debug("Range map for groupId: " + rangeMap);
				}
				
				if (valueMap != null)
				{
					for (String value : valueMap.values())
					{
						addInterval(groupId, Integer.valueOf(value), Integer.valueOf(value));
					}
				}
				else if (rangeMap != null)
				{
					if (rangeMap.size() != 2)
					{
						throw new Exception("Syntax error while reading service class range group: " + groupId + " - A range must have min and max set");
					}
					else
					{
						Iterator<String> iter = rangeMap.values().iterator();
						String min = iter.next();
						String max = iter.next();
						addInterval(groupId, Integer.valueOf(min), Integer.valueOf(max));
					}
					
				}
				else
				{
					break;
				}
				
				index++;
			}
		}		
		initialize();
	}
	
	/**
	 * Adds an interval with a group identity.
	 * 
	 * @param identity the group identity.
	 * @param min the min value.
	 * @param max the max value.
	 */
	public void addInterval(String identity, int min, int max)
	{
		Interval interval = new Interval(identity, min, max); 
		lookupMap.put(min, interval);

		if (min != max)
		{
			intervalList.add(interval);
		}
	}	

	
	/**
	 * Searches for an interval in O(n) in a sorted list.
	 * @param value the given value to match an interval for.
	 * @return the interval if found, otherwise null.
	 */
	public Interval findInterval(int value)
	{
		Interval result = lookupMap.get(value);
		
		if (result != null)
		{
			return result;
		}
		
		for (Iterator<Interval> iter = intervalList.iterator(); iter.hasNext();)
		{
			Interval interval = iter.next();


			if (value < interval.getMin())
			{
				break;
			}

			if (value <= interval.getMax())
			{
				result = interval;
				break;
			}

		}

		return result;
	}
	
	
	/**
	 * Initializes the range list using sweep and prune 1D algorithm. 
	 */
	@SuppressWarnings("unchecked")
	private void initialize()
	{

		Comparator sweepAndPruneComparator = new Comparator()
		{

			public int compare(Object o1, Object o2)
			{
				Interval interval1 = (Interval) o1;
				Interval interval2 = (Interval) o2;

				int comparison = interval1.getMin() - interval2.getMin();

				if (comparison == 0)
				{
					if (interval1.isRange())
					{
						return 1;
					}
					else
					{
						return -1;
					}
				}

				return comparison;
			}

		};

		Collections.sort(intervalList, sweepAndPruneComparator);
	}
}
