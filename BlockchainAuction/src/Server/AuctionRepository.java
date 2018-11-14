/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import Auction.Auction;
import static Server.AuctionManager.messageClient;
import static Server.AuctionManager.messageRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author SHUBHAM
 */
public class AuctionRepository {

   
    public static void main(String[] args) throws SocketException, IOException {
        
      //All auctions
      ArrayList<Auction> ac= new ArrayList<>();
      
      DatagramSocket serverSocket = new DatagramSocket(9876);
         
      while(true){
            
          //Receber informação por udp
          byte[] receivebuffer = new byte[1024];
          byte[] sendbuffer  = new byte[1024];
          DatagramPacket recvdpkt = new DatagramPacket(receivebuffer, receivebuffer.length);
          serverSocket.receive(recvdpkt);
          InetAddress IP = recvdpkt.getAddress();
          int portno = recvdpkt.getPort();
          String clientdata = new String(recvdpkt.getData());
          System.out.println("\nClient : "+ clientdata);
          
          ComputeMessageType(clientdata,serverSocket,ac);
          /*//Mandar informação por udp, para a mesma pessoa que nos enviou
          System.out.print("\nServer : ");
          BufferedReader serverRead = new BufferedReader(new InputStreamReader (System.in) );
          String serverdata = serverRead.readLine();
          sendbuffer = serverdata.getBytes();
          DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, IP,portno);
          serverSocket.send(sendPacket); 
          
          //Terminar
          if(serverdata.equalsIgnoreCase("bye"))
          {
              System.out.println("connection ended by server");
              break;
          }*/    
      }
        //serverSocket.close();
    }
    
    //Fazer o que é suposto para todas as mensagens
    public static void ComputeMessageType(String msg,DatagramSocket serverSocket,ArrayList<Auction> ac) throws IOException{
        String type = msg.substring(0,3);
        String []param;
        //cta -> create auction
        //tta -> terminate auction
        //gba -> get bids of a auction
        //gbc -> get bids done by a client
        //lga -> list going auctions
        //lta -> list terminated auctions
        //coa -> check outcome auction
        //vlr -> validate receipt (?)
        //err -> error message
        
        switch(type){
            case "cta": 
                //Criar leilão
                param = msg.split(("/"));
                param[2]=param[2].replaceAll("[^0-9]+", "");
                param[3]=param[3].replaceAll("[^0-9]+", "");
                param[4]=param[4].replaceAll("[^0-9]+", "");
                System.out.println(Arrays.toString(param));
                
                //String auctionName, int creatorID, int auctionID, int timeToFinish
                Auction a = new Auction(param[1],Integer.parseInt(param[2]),Integer.parseInt(param[3]),Integer.parseInt(param[4]));
                ac.add(a);
                
                //Devolver mensagem
                messageManager("rta",serverSocket);
                
            case "tta" : 
                //Terminar leilão
                param = msg.split(("/"));
                param[2]=param[2].replaceAll("[^0-9]+", "");
                param[3]=param[3].replaceAll("[^0-9]+", "");
                param[4]=param[4].replaceAll("[^0-9]+", "");
                System.out.println(Arrays.toString(param));
                   
                for(int i=0; i<ac.size();i++){
                    if(ac.get(i).getAuctionID()==Integer.parseInt(param[3]))  ac.get(i).setAuctionFinished(true);
                 }
              
                //Devolver mensagem
                messageManager("rtt",serverSocket);
                
            case "gba" : 
            case "gbc" :  
            case  "lga": 
                //Terminar leilão
                param = msg.split(("/"));
                System.out.println(Arrays.toString(param));
                
                String retMsg = "rlg";
                   
                for(int i=0; i<ac.size();i++){
                    if(!ac.get(i).isAuctionFinished()) retMsg+="/"+ac.get(i).getAuctionName();
                 }
              
                //Devolver mensagem
                messageManager(retMsg,serverSocket);
            case  "coa": 
            case  "vlr":
                    //Mandar mensagem ao AuctionRepository para ele fazer o pretendido e devolver valores
                    messageRepository(msg,serverSocket);
                    break;
            case "end": //Temporary
                    serverSocket.close();
                    System.exit(1);
        }
    }
    
    public static void messageManager(String m, DatagramSocket serverSocket) throws UnknownHostException, IOException{
        InetAddress s_ip = InetAddress.getByName("127.0.0.1");
        int s_port = 9877;
        byte[] sendbuffer  = new byte[1024];

        sendbuffer = m.getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,s_ip ,s_port);
        serverSocket.send(sendPacket);
    }
}

