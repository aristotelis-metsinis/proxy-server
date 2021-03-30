/*
 * @(#)ProProxy.java	1.70 28th Jan, 2001
 * 
 * Modification Log:
 * 14th Feb 1999 : Tanmay : Original Version.
 * 18th Feb 1999 : Tanmay : ProProxy now prints current cache size too.
 * 19th Feb 1999 : Tanmay : ProProxy now prunes cache even when it is blocking on accept. 
 *                          And prints out total requests served.
 * 26th May 1999 : Tanmay : Supports both TCP and UDP. Command line format changed.
 * 01st Jul 1999 : Tanmay : Supports HTTP protocol.
 * 12th Mar 2000 : Tanmay : Support for compression in HTTP mode.
 * 27th Jan 2001 : Tanmay : Moved to configuration file based startup.
 * 28th Jan 2001 : Tanmay : Added connection filter.
 * 31st Oct 2001 : Tin Le tinle@cisco.com (http://tin.le.org): Added static VERSION field to 
 *                          make it easier to update. Added Global debugflg 
 *                          for use in other modules.
 */

package proproxy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * ProProxy<br>
 * Generic cascading proxy server. Can be used for any single socket 
 * network application - HTTP, telnet, blah blah..<br>
 * One of the few proxy servers that support both UDP and TCP sockets.<br>
 * <br>
 * Usage: java proproxy.ProProxy <configuration file name><br>
 * <br>
 * Configuration file format:<br>
 * # Debug mode (true or false). All messages will be logged in debug mode.<br>
 * # DEBUG = <br>
 * # Protocol for which this proxy is to be used<br>
 * # Any one of udp, tcp, http or chttp<br>
 * PROTOCOL = <br>
 * # The port number on which ProProxy will listen for incoming requests<br>
 * LISTEN_PORT = <br>
 * # If proproxy is being used in cascade mode (udp or tcp protocols) <br>
 * # the host address to which the requests are to be forwarded<br>
 * CASCADE_HOST =<br>
 * # If proproxy is being used in cascade mode (udp or tcp protocols) <br>
 * # the port number on the target machine to which the requests are to be forwarded<br>
 * CASCADE_PORT = <br>
 * <br>
 * # List of IP addresses who are not allowed to connect to proxy<br>
 * REJECT_IP_LIST = <br>
 * # List of IP addresses who are to be allowed to connect to proxy<br>
 * ALLOW_IP_LIST =<br>
 * <br>
 * <br>
 * If cascade port is not specified it is same as listen port.<br>
 * Cascade host and port are not used if protocol is http or chttp.<br>
 * Protocol chttp is HTTP mode with compression.<br>
 * If REJECT_IP_LIST list is specified, ALLOW_IP_LIST will be ignored.<br>
 * If neither is specified, all connections will be allowed.<br>
 * Lists can contain valid IP addresses separated by space.<br>
 * <br>
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
 * @version     1.70, 31st Oct, 2001
 */

public class ProProxy
{
	private static final String VERSION = "ProProxy Version 1.70";
	protected static boolean debugflg = false;
	protected static int requests_counter = 0;

	/**
	 * The main entry point for the application. 
	 * Creates a new ProListener thread and waits till the listener gets out.
	 * Prints out the number of active connections intermittently.
	 *
	 * @param args Array of parameters passed to the application via the command line.
	 */
	public static void main (String[] args)
	{
        String			sCascadeServer = null;
		String			sProtocol, sDebug;
		String			sIncludeList, sExcludeList;
        int				iCascadePort = 80, iListenPort = 80;
		FileInputStream ConfigStream;
		Properties		Config;
		
        // check if sufficient arguments are given
        if (args.length < 1)
        {
            printUsage();
            return;
        }
		
		Config = new Properties();
		
		try
		{
			ConfigStream = new java.io.FileInputStream(args[0]);
			Config.load(ConfigStream);
		}
		catch(Exception e)
		{
			System.err.println(e.toString()); // file not found and io exception
			return;
		}

		if(null != (sDebug = Config.getProperty("DEBUG")))
		{
			if(sDebug.equalsIgnoreCase("true"))
			{
				debugflg = true;
			}
		}
		
		if(null == (sProtocol = Config.getProperty("PROTOCOL")))
		{
			System.err.println("PROTOCOL not specified.");
			return;
		}
		sProtocol = sProtocol.trim();
		
		if(sProtocol.equalsIgnoreCase("tcp") || sProtocol.equalsIgnoreCase("udp"))
		{
			sCascadeServer = Config.getProperty("CASCADE_HOST"); // get cascade proxy server
			sCascadeServer = sCascadeServer.trim();
            if ( (null == sCascadeServer) || (sCascadeServer.length() == 0) )
            {
				System.err.println("CASCADE_HOST not specified.");
                return;
            }
		}

        iListenPort = Integer.parseInt(Config.getProperty("LISTEN_PORT", "0")); // get listen port
		if(0 >= iListenPort)
		{
			System.err.println("LISTEN_PORT not specified or invalid.");
			return;
		}
        iCascadePort = Integer.parseInt(Config.getProperty("CASCADE_PORT", "0"));
		if(0 >= iCascadePort) iCascadePort = iListenPort;
		
		sIncludeList = Config.getProperty("ALLOW_IP_LIST");
		sExcludeList = Config.getProperty("REJECT_IP_LIST");

        if (sProtocol.equalsIgnoreCase("tcp"))
        {
            handleTCP(iListenPort, sCascadeServer, iCascadePort, sIncludeList, sExcludeList);
        }
        else if (sProtocol.equalsIgnoreCase("udp"))
        {
            handleUDP(iListenPort, sCascadeServer, iCascadePort, sIncludeList, sExcludeList);
        }
        else if (sProtocol.equalsIgnoreCase("http"))
        {
            ProHTTPProcessor.setCompression(false);
            handleHTTP(iListenPort, sIncludeList, sExcludeList);
        }
        else if (sProtocol.equalsIgnoreCase("chttp"))
        {
            ProHTTPProcessor.setCompression(true);
            handleHTTP(iListenPort, sIncludeList, sExcludeList);
        }
        else
        {
            System.out.println("Unknown protocol " + sProtocol);
        }
	}

    public static void handleHTTP(int iListenPort, String sIncludeList, String sExcludeList)
    {
        ProHTTPListener proListener;
        
        // instantiate a listener
        proListener = new ProHTTPListener(iListenPort, sIncludeList, sExcludeList);
        proListener.startListening();   // start listener thread
        
        // wait for listener thread to stop
        while(proListener.isListening())
        {
            try
            {
                Thread.sleep(2000);
            }
            catch (InterruptedException ie)
            {
                break;
            }
            proListener.pruneCache();
            System.out.print("\rConnections: " + ProTCPProcessor.iNumProcessors + " Cache: " + ProTCPListener.processorStack.size() + " Requests: " + proListener.iNumRequests);
            System.gc();
        }
    }
    
    public static void handleUDP(int iListenPort, String sCascadeServer, int iCascadePort, String sIncludeList, String sExcludeList)
    {
        ProUDPListener proListener;
        
        // instantiate the listener
        proListener = new ProUDPListener(iListenPort, sCascadeServer, iCascadePort, sIncludeList, sExcludeList);
        proListener.startListening();   // start listener thread
        
        // wait for listener thread to stop
        while(proListener.isListening())
        {
            try
            {
                Thread.sleep(2000);
            }
            catch (InterruptedException ie)
            {
                break;
            }
            System.out.print("\rRequests: " + proListener.iNumRequests);
            System.gc();
        }
    }
    
    public static void handleTCP(int iListenPort, String sCascadeServer, int iCascadePort, String sIncludeList, String sExcludeList)
    {
        ProTCPListener proListener;
        
        // instantiate a listener
        proListener = new ProTCPListener(iListenPort, sCascadeServer, iCascadePort, sIncludeList, sExcludeList);
        proListener.startListening();   // start listener thread
        
        // wait for listener thread to stop
        while(proListener.isListening())
        {
            try
            {
                Thread.sleep(2000);
            }
            catch (InterruptedException ie)
            {
                break;
            }
            proListener.pruneCache();
            System.out.print("\rConnections: " + ProTCPProcessor.iNumProcessors + " Cache: " + ProTCPListener.processorStack.size() + " Requests: " + proListener.iNumRequests);
            System.gc();
        }
    }

    private static void printUsage()
    {
        System.out.println(VERSION);
        System.out.println("Usage:");
        System.out.println("java proproxy.ProProxy <configuration file name>");
		System.out.println("");
        System.out.println("Configuration file format:");
        System.out.println("# Debug mode (true or false). All messages will be logged in debug mode.");
        System.out.println("DEBUG = ");
		System.out.println("");
        System.out.println("# Protocol for which this proxy is to be used. (udp, tcp, http or chttp)");
        System.out.println("PROTOCOL = ");
		System.out.println("");
        System.out.println("# Port on which to listen for incoming requests");
        System.out.println("LISTEN_PORT = ");
		System.out.println("");
        System.out.println("# If used in cascade mode (udp or tcp protocols)");
        System.out.println("# the host address to which requests are to be forwarded");
        System.out.println("CASCADE_HOST =");
		System.out.println("");
        System.out.println("# If used in cascade mode (udp or tcp protocols) the port number on");
		System.out.println("# target machine to which requests are to be forwarded");
        System.out.println("CASCADE_PORT = ");
		System.out.println("");
		System.out.println("# List of IP addresses who are not allowed to connect to proxy");
		System.out.println("REJECT_IP_LIST = ");
		System.out.println("# List of IP addresses who are to be allowed to connect to proxy");
		System.out.println("ALLOW_IP_LIST = ");		
		System.out.println("");
        System.out.println("If cascade port is not specified it is same as listen port.");
        System.out.println("Cascade host and port are not used if protocol is http.");
        System.out.println("Protocol chttp is HTTP mode with compression.");
		System.out.println("");
		System.out.println("If REJECT_IP_LIST list is specified, ALLOW_IP_LIST will be ignored.");
		System.out.println("If neither is specified, all connections will be allowed.");
		System.out.println("Lists can contain valid IP addresses separated by space.");

        return;
    }
}
