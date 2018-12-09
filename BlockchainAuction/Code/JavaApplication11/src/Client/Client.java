/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import org.json.*;

/**
 *
 * Client Class
 * <br>
 * Cliente no leilão
 * 
 */
public class Client {
    private static int clientID=0;

    public Client(int clientID) {
        clientID++;
    }
    
    
    public static void main(String[] args) throws SocketException, IOException {
        
        boolean exit = false;
        
        BufferedReader br =new BufferedReader(new InputStreamReader(System.in));
      
        InetAddress IP = InetAddress.getByName("127.0.0.1");

        DatagramSocket clientSocket = new DatagramSocket();
        
        while(!exit)
        {
            byte[] sendbuffer = new byte[1024];
            byte[] receivebuffer = new byte[1024];
            
            String messageType = "";
            String auctionName = "";
            String auctionTime = "";
            String auctionType = "";
            String userID = "";
            double amount = 0;
            
            String sendMsg = "";
            JSONObject sendObj=null;
            
            System.out.print("\n\nEscolha uma opção");
            System.out.print("\n1-Criar leilão");
            System.out.print("\n2-Terminar leilão");
            System.out.print("\n3-Ver leilões ativos");
            System.out.print("\n4-Ver leilões inativos");
            System.out.print("\n5-Ver licitações de um leilão");
            System.out.print("\n6-Ver licitações feitas por um cliente");
            System.out.print("\n7-Ver resultado de um leilão");
            System.out.print("\n8-Validar recibo");
            System.out.print("\n9-Licitar em leilão");
            System.out.print("\n0-Sair");
            System.out.print("\nOpção: ");
            
            String option = br.readLine();
            switch (option){
                    case "1":
                        messageType = "cta";
                        System.out.print("\nNome: ");
                        auctionName = br.readLine();
                        System.out.print("\nDuração(minutos): ");
                        auctionTime = br.readLine();
                        System.out.print("\nTipo");
                        System.out.print("\n1-Leilão Inglês");
                        System.out.print("\n2-Leilão Cego");
                        System.out.print("\nOpção: ");
                        String optionType = br.readLine();
                        
                        switch (optionType){
                            case "1":
                                auctionType = "english";
                                break;
                            case "2":
                                auctionType = "blind";
                                break;
                        }
                        //Convert to json, cuidado com a maneria como se constroio a mesnagem
                        sendMsg = "{ \"Type\":"+messageType+",\"Name\":"+auctionName+ ",\"Time\":"+auctionTime+",\"AuctionType\":"+auctionType+"}";
                        sendObj = new JSONObject(sendMsg);
                        //System.out.println(obj.getString("Type"));
                        messageManager(clientSocket,sendObj);
                        break;
                        
                    case "2":
                        messageType = "tta";
                        System.out.print("\nNome: ");
                        auctionName = br.readLine();
                        sendMsg = "{ \"Type\":"+messageType+ ",\"Name\":"+auctionName+"}";
                        sendObj = new JSONObject(sendMsg);
                        messageRepository(clientSocket,sendObj);
                        break;
                        
                    case "3":
                        messageType = "lga";
                        sendMsg = "{ \"Type\":"+messageType+"}";
                        sendObj = new JSONObject(sendMsg);
                        messageRepository(clientSocket,sendObj);
                        break;
                        
                    case "4":
                        messageType = "lta";
                        sendMsg = "{ \"Type\":"+messageType+"}";
                        sendObj = new JSONObject(sendMsg);
                        messageRepository(clientSocket,sendObj);
                        break;
                        
                    case "5":
                        messageType = "gba";
                        System.out.print("\nNome: ");
                        auctionName = br.readLine();
                        sendMsg = "{ \"Type\":"+messageType+ ",\"Name\":"+auctionName+"}";
                        sendObj = new JSONObject(sendMsg);
                        messageRepository(clientSocket,sendObj);
                        break;
                        
                    case "6":
                        messageType = "gbc";
                        System.out.print("\nClient ID: ");
                        userID = br.readLine();
                        sendMsg = "{ \"Type\":"+messageType+ ",\"ClientID\":"+userID+"}";
                        sendObj = new JSONObject(sendMsg);
                        messageRepository(clientSocket,sendObj);
                        break;
                        
                    case "7":
                        messageType = "coa";
                        System.out.print("\nNome: ");
                        auctionName = br.readLine();
                        sendMsg = "{ \"Type\":"+messageType+ ",\"Name\":"+auctionName+"}";
                        sendObj = new JSONObject(sendMsg);
                        messageRepository(clientSocket,sendObj);
                        break;
                        
                    case "8":
                        messageType = "vlr";
                        sendMsg = "{ \"Type\":"+messageType+"}";
                        sendObj = new JSONObject(sendMsg);
                        messageRepository(clientSocket,sendObj);
                        break;
                        
                    case "9":
                        messageType = "bid";
                        System.out.print("\nNome: ");
                        auctionName = br.readLine();
                        System.out.print("\nAmount: ");
                        amount = Double.parseDouble(br.readLine());
                        sendMsg = "{ \"Type\":"+messageType+",\"Name\":"+auctionName+",\"Amount\":"+amount+",\"ClientID\":"+clientID+"}";
                        sendObj = new JSONObject(sendMsg);
                        messageRepository(clientSocket,sendObj);
                        break;
                        
                    default:
                        sendMsg = "exit";
                        exit = true;
                        messageManager(clientSocket,sendObj);
                        break;
            }

            DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
            clientSocket.receive(receivePacket);
            String serverData = new String(receivePacket.getData());
            
            JSONObject rec = new JSONObject(serverData);
            for(int i=1; i < rec.names().length();i++){
                List tmp = rec.names().toList();
                System.out.print("\nServer: " + rec.getString(tmp.get(i).toString()));
            }

        }
        
        clientSocket.close();
        
    }
    
     /**
     * Função que manda mensagem para o manager.
     * 
     * @param clientSocket Socket do Cliente
     * @param msg Mensagem a enviar ao manager
     * @throws UnknownHostException
     * @throws IOException
     */
    public static void messageManager(DatagramSocket clientSocket, JSONObject msg) throws UnknownHostException, IOException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9877;
        byte[] sendbuffer  = new byte[1024];

        sendbuffer = msg.toString().getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
        clientSocket.send(sendPacket);
        
        if(msg.equals(("end"))){
            clientSocket.close();
            System.exit(1);
        }
    }
    
    
    /**
     * Função que manda mensagem para o repositorio.
     * 
     * @param clientSocket Socket do Cliente
     * @param msg Mensagem a enviar ao manager
     * @throws UnknownHostException
     * @throws IOException
     */
    public static void messageRepository(DatagramSocket clientSocket, JSONObject msg) throws UnknownHostException, IOException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9876;
        byte[] sendbuffer  = new byte[1024];

        sendbuffer = msg.toString().getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
        clientSocket.send(sendPacket);
        
        if(msg.equals(("end"))){
            clientSocket.close();
            System.exit(1);
        }
    }
}
