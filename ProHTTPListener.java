/*
 * @(#)ProHTTPListener.java	1.00 1st Jul 1999
 * 
 * Modification Log:
 * 01st Jul 1999: Tanmay : Original Version
 * 28th Jan 2001 : Tanmay : Added connection filter.
 */


package proproxy;

import java.net.*;
import java.util.Stack;
import java.util.EmptyStackException;
import java.io.*;

/**
 * ProHTTPListener<br>
 * The HTTP connection listener. Waits for a new connection, creates a new 
 * ProHTTPProcessor for each new connection. ProHTTPProcessors are cached in a
 * stack. Each ProHTTPProcessor puts itself back to the cache when it is done
 * so that it can be used for the next request.
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
 * @version     1.10, 28th Jan, 2001
 */

public class ProHTTPListener extends ProAbstractTCPListener
{

    /**
     * Save the connection parameters
     */
    public ProHTTPListener (int iListenPort, String sIncludeList, String sExcludeList)
    {
        super(iListenPort, sIncludeList, sExcludeList);
    }
    
    public void createAndProcessRequest(Socket newSocket)
    {
        ProHTTPProcessor processor = new ProHTTPProcessor(processorStack);
        processor.processReq(newSocket);
    }
    
    public void processRequest(Object processor, Socket newSocket)
    {
        ((ProHTTPProcessor)processor).processReq(newSocket);
    }
}
