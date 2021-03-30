/*
 * @(#)ProAbstractTCPListener.java	1.00 1st July 1999
 * 
 * Modification Log:
 * 1st  Jul 1999: Tanmay : Original Version
 * 28th Jan 2001: Tanmay : Added connection filter
 * 31st	Oct 2001: Tin Le tinle@cisco.com (http://tin.le.org): Increment Global requests_counter 
 *                         in run() to track how many times we are accessed.
 */


package proproxy;

import java.net.*;
import java.util.Stack;
import java.util.EmptyStackException;
import java.io.*;

/**
 * ProAbstractTCPListener<br>
 * The TCP connection pooler and dispatcher. Waits for a new connection, creates a new 
 * Processor for each new connection. Processors are cached in a
 * stack. Each Processor puts itself back to the cache when it is done
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
 * @version     1.20, 31st Oct, 2001
 */

public abstract class ProAbstractTCPListener implements Runnable
{
    ServerSocket            listenSocket;
    int                     iListenPort;
	ProConnectionFilter		ConnFilter;
    boolean                 bContinue;
    Thread                  listenThread;
    static Stack            processorStack = new Stack(); // cache of processors
    int                     iNumRequests = 0;

    private static int      iMaxSpares = 5;

    /**
     * Save the connection parameters
     */
    public ProAbstractTCPListener (int iListenPort, String sIncludeList, String sExcludeList)
    {
        this.iListenPort = iListenPort;
		ConnFilter = new ProConnectionFilter(sIncludeList, sExcludeList);
    }

    /**
     * The listener loop. 
     */
    public void run()
    {
        while(bContinue)
        {
            Socket  newSocket;
            Object  processor = null;
			
            // wait for a new connection
            try
            {
                newSocket = listenSocket.accept();
				// check for allowed IP
				if(!ConnFilter.isAllowed(newSocket.getInetAddress().getHostAddress()))
				{
					System.out.println("Rejected " + newSocket.getInetAddress().getHostAddress());
					newSocket.close();
					continue;
				}
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
                break;
            }

            if (!processorStack.isEmpty())
            {
                try
                {
                    processor = processorStack.pop();
                }
                catch (EmptyStackException ese)
                {
                    // no more free processors
                }
            }

            if (null == processor)
            {
                createAndProcessRequest(newSocket);
            }
            else
            {
                // hand over the connection and continue
                processRequest(processor, newSocket);
            }
            iNumRequests++;
			
			// increment Global request counts
			ProProxy.requests_counter++;

            pruneCache();            
        }

        bContinue = false;
    }

    public abstract void createAndProcessRequest(Socket newSocket);
    public abstract void processRequest(Object processor, Socket newSocket);
    
    /**
     * Check if there are too many processors in cache and remove extra ones
     */
    public void pruneCache()
    {
        // check if there are too many spares
        while(processorStack.size() > iMaxSpares)
        {
            try
            {
                processorStack.pop();   // pop out and let them be garbage
            }
            catch (EmptyStackException ese)
            {
                // no more free processors
            }                    
        }
    }

    
    /**
     * If listener thread is stopped and not listening, start it.
     */
    public void startListening()
    {
        if (!bContinue)
        {
            bContinue = true;
            try
            {
                listenSocket = new ServerSocket(iListenPort);
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
                bContinue = false;
                return;
            }
            listenThread = new Thread(this);
            listenThread.start();
        }
    }
    
    /**
     * If listener thread is active and listening, stop it.
     */
    public void stopListening()
    {
        if (bContinue)
        {
            bContinue = false;
            if (null != listenSocket)
            {
                try
                {
                    listenSocket.close();
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                }
            }
        }
    }

    /**
     * @return true if the listener thread is active and listening
     */
    public boolean isListening()
    {
        return bContinue;
    }
}
