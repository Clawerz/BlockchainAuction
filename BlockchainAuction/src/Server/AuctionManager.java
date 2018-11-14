/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import Auction.Auction;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

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
          
          //Ver tipo de mensagem
          ComputeMessageType(clientdata,c_IP,c_portno,serverSocket);
      }
    }
    
    //Fazer o que é suposto para todas as mensagens
    public static void ComputeMessageType(String msg, InetAddress c_ip, int c_portno,DatagramSocket serverSocket) throws IOException{
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
            case "cta": case "tta" : case "gba" : case "gbc" :  case  "lga": case  "coa": case  "vlr":
                    //Mandar mensagem ao AuctionRepository para ele fazer o pretendido e devolver valores
                    messageRepository(msg,serverSocket);
                    break;
            case "rta": case "rtta" : case  "rvlr":
                    //Mandar mensagem ao Cliente a informar queo pretendido foi feito;
                    messageClient(c_ip, c_portno,serverSocket, "Operation completed with sucess!");
                    break;
            case "rgba" : 
                    //Mandar mensagem ao Cliente com os bids feitos
                    break;
            case "rgbc" : 
                    //Mandar mensagem ao Cliente com os bids feitos
                    break;
            case  "rlg": 
                    System.out.println(msg);
                    //Mandar mensagem ao Cliente com os auctions ativos
                    param = msg.split(("/"));
                    String retMsg="";
                    for(int i=1; i<param.length;i++){
                        retMsg+=param[i];
                    }
                    messageClient(c_ip,c_portno,serverSocket,retMsg);
                    break;
            case  "rcoa": 
                    //Mandar mensagem ao Cliente com os auctions inativos
                    break;
            case "end": //Temporary
                    serverSocket.close();
                    System.exit(1);
            default:
                    messageClient(c_ip, c_portno,serverSocket, "ERROR - Invalid message");
                    break;
            
        }
    }
    
    public static void messageRepository(String m, DatagramSocket serverSocket) throws UnknownHostException, IOException{
        InetAddress s_ip = InetAddress.getByName("127.0.0.1");
        int s_port = 9876;
        byte[] sendbuffer  = new byte[1024];
        
        sendbuffer = m.getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,s_ip ,s_port);
        serverSocket.send(sendPacket);
    }
    
    public static void messageClient(InetAddress c_ip, int c_portno,DatagramSocket serverSocket, String m) throws UnknownHostException, IOException{
        byte[] sendbuffer  = new byte[1024];
        sendbuffer = m.getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,c_ip ,c_portno);
        serverSocket.send(sendPacket);
    }
  
}

