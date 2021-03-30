/*
 * @(#)ProConnectionFilter.java	1.00 28th Jan 2001
 * 
 * Modification Log:
 * 28th  Jan 2001: Tanmay : Original Version
 */


package proproxy;

import java.net.*;
import java.util.Stack;
import java.util.EmptyStackException;
import java.util.StringTokenizer;
import java.io.*;

/**
 * ProConnectionFilter<br>
 * The connection filter. Checks for IP address or host name in a inclusion and
 * exclusion list.
 * <br>
 * You are free to use this code and to make modifications provided
 * this notice is retained.
 * <p>
 * If you found this useful, please add a note of acknowledgement to my 
 * <a href="http://htmlgear.lycos.com/guest/control.guest?u=tanmaykm&i=1&a=sign" alt="guest book">guestbook</a>. 
 * If you would like to report a bug or suggest some improvements, 
 * you are most welcome. I will be happy to help you use this piece of code.
 * <p>
 *
 * @author 	    Tanmay K. Mohapatra
 * @version     1.00, 28th Jan, 2001
 */

public class ProConnectionFilter
{
	String [] asInclude;
	String [] asExclude;
	
	public ProConnectionFilter(String sIncludeList, String sExcludeList)
	{
		// parse the include list
		if(null != sIncludeList) asInclude = parseList(sIncludeList);
		// parse the exclude list
		if(null != sExcludeList) asExclude = parseList(sExcludeList);
	}

	public boolean isAllowed(String sIP)
	{
		int iIndex;
		
		if(null != asExclude)
		{
			for(iIndex=0; iIndex < asExclude.length; iIndex++)
			{
				if(asExclude[iIndex].equalsIgnoreCase(sIP)) return false;
			}
			return true;
		}
		
		if(null != asInclude)
		{
			for(iIndex=0; iIndex < asInclude.length; iIndex++)
			{
				if(asInclude[iIndex].equalsIgnoreCase(sIP)) return true;
			}
			return false;
		}
		
		return true;
	}

	private String [] parseList(String sList)
	{
		String [] asList = null;
		StringTokenizer st;
		Stack TempStack = new Stack();

		// parse the include list
		st = new StringTokenizer(sList, " ");

		while(st.hasMoreTokens())
		{
			String sOne = st.nextToken().trim();
			if(sOne.length() > 0) TempStack.push(sOne);
		}

		if(TempStack.size() > 0)
		{
			asList = new String[TempStack.size()];
			for(int iIndex=TempStack.size(); iIndex != 0; iIndex--)
			{
				asList[iIndex-1] = (String)TempStack.pop();
			}
		}

		return asList;
	}
}
