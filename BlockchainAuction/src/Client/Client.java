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

/**
 *
 * Client Class
 * <br>
 * Cliente no leilão
 * 
 */
public class Client {

    
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
            String name = "";
            String time = "";
            String auctionType = "";
            
            String sendMsg = "";
            
            System.out.print("\n\nEscolha uma opção");
            System.out.print("\n1-Criar leilão");
            System.out.print("\n20-Sair");
            System.out.print("\nOpção: ");
            
            String option = br.readLine();
            switch (option){
                    case "1":
                        messageType = "cta";
                        System.out.print("\nNome: ");
                        name = br.readLine();
                        System.out.print("\nDuração(minutos): ");
                        time = br.readLine();
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
                        sendMsg = messageType + "-" + name + "-" + "22-33" + "-" + time + "-" + auctionType;
                        break;
                    default:
                        sendMsg = "exit";
                        exit = true;
                        break;
            }
            //Mensagem a mandar
            //String SendMsg = br.readLine();
            
            messageManager(clientSocket,sendMsg + "-");

            DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
            clientSocket.receive(receivePacket);
            String serverData = new String(receivePacket.getData());
            System.out.print("\nServer: " + serverData);

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
    public static void messageManager(DatagramSocket clientSocket, String msg) throws UnknownHostException, IOException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9877;
        byte[] sendbuffer  = new byte[1024];

        sendbuffer = msg.getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
        clientSocket.send(sendPacket);
        
        if(msg.equals(("end"))){
            clientSocket.close();
            System.exit(1);
        }
    }
}
