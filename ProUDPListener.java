/*
 * @(#)ProUDPListener.java	1.10 28th Jan 2001
 * 
 * Modification Log:
 * 26th May 1999 : Tanmay : Original Version
 * 28th Jan 2001 : Tanmay : Added connection filter.
 * 31st Oct 2001 : Tin Le tinle@cisco.com (http://tin.le.org): Added Global requests_counter 
 *                          to track num of requests.
 */


package proproxy;

import java.net.*;
import java.util.Stack;
import java.util.EmptyStackException;
import java.io.*;

/**
 * ProUDPListener<br>
 * The UDP connection listener. Waits for a new UDP packet, changes the target
 * host and port and relays it again.
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


public class ProUDPListener implements Runnable
{
    DatagramSocket          dgSocket;
    DatagramPacket          dgPacket;
    InetAddress             cascadeHost;
    String                  sCascadeServer;
    int                     iCascadePort, iListenPort;
	ProConnectionFilter		ConnFilter;
    boolean                 bContinue;
    public int              iNumRequests = 0;
    static int              iMAX_LEN = 1024;
    byte []                 bData;
    Thread                  listenThread;

    /**
     * Save the connection parameters
     */
    public ProUDPListener (int iListenPort, String sCascadeServer, int iCascadePort, String sIncludeList, String sExcludeList)
    {
        this.iListenPort = iListenPort;
        this.sCascadeServer = sCascadeServer;
        this.iCascadePort = iCascadePort;
		ConnFilter = new ProConnectionFilter(sIncludeList, sExcludeList);
    }

    /**
     * The listener loop. 
     */
    public void run()
    {
        while(bContinue)
        {
            try
            {
                //uncomment short circuit for test, set cascadehost to localhost
                //byte [] testData = new String("test data").getBytes();
                //dgPacket.setData(testData);
                //dgPacket.setLength(testData.length);
                //dgPacket.setAddress(cascadeHost);
                //dgPacket.setPort(iCascadePort);
                //dgSocket.send(dgPacket);

                dgSocket.receive(dgPacket);
                //System.out.println("Got a request");
				// check for allowed IP
				if(!ConnFilter.isAllowed(dgPacket.getAddress().getHostAddress()))
				{
					continue;
				}
                dgPacket.setAddress(cascadeHost);
                dgPacket.setPort(iCascadePort);
                dgSocket.send(dgPacket);

                // uncomment for test
                //System.out.println(new String(dgPacket.getData(), 0, dgPacket.getLength()));
            }
            catch (Exception e)
            {
                System.out.println(e);
                break;
            }
            iNumRequests++;
			ProProxy.requests_counter = iNumRequests;
        }

        bContinue = false;
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
                dgSocket = new DatagramSocket(iListenPort);
                bData = new byte[iMAX_LEN];
                dgPacket = new DatagramPacket(bData, iMAX_LEN);
                cascadeHost = InetAddress.getByName(sCascadeServer);
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
            if (null != dgSocket)
            {
                dgSocket.close();
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
