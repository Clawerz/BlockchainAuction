/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 *
 * @author SHUBHAM
 */
public class AuctionManager {

   
    public static void main(String[] args) throws SocketException, IOException {
        
     DatagramSocket serverSocket = new DatagramSocket(9877);
     
     //Informação relativa ao cliente
     boolean clientIP = false;
     InetAddress c_IP=null;
     int c_portno=0;
     
      while(true){
          
          //Receber informação
          byte[] receivebuffer = new byte[1024];
          byte[] sendbuffer  = new byte[1024];
          DatagramPacket recvdpkt = new DatagramPacket(receivebuffer, receivebuffer.length);
          serverSocket.receive(recvdpkt);    
          String clientdata = new String(recvdpkt.getData());
          
          //Obter endereço do client
          if(!clientIP){
            c_IP = recvdpkt.getAddress();
            c_portno = recvdpkt.getPort();
            clientIP=true;
          }
          
          System.out.println("\nClient : "+ clientdata);
          
          //Decidir para quem mandamos informação
          InetAddress IP;
          int portno;
          
          System.out.println(clientdata);
          if(clientdata.contains("cl")){
               IP = c_IP;
               portno = c_portno;
          }else{
              IP = InetAddress.getByName("127.0.0.1");
              portno = 9876;
          }
          
          //Mandar informação
          System.out.print("\nServer : ");
          BufferedReader serverRead = new BufferedReader(new InputStreamReader (System.in) );
          String serverdata = serverRead.readLine();
          sendbuffer = serverdata.getBytes();
          DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,IP ,portno);
          serverSocket.send(sendPacket); 
          
          //Terminar
          if(serverdata.equalsIgnoreCase("bye"))
          {
              System.out.println("connection ended by server");
              break;
          }    
      }
        serverSocket.close();
    }        
}

