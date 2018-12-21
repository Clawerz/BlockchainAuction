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
    /*private static int clientID=0;

    public Client(int clientID) {
        clientID++;
    }*/
    private static String type = "";
    private static String messageType = "";
    private static String sendMsg = "";
    private static JSONObject sendObj=null;
    private static DatagramSocket clientSocket;
    private static DatagramPacket receivePacket;
    private static byte[] receivebuffer = new byte[1024];
    private static String serverData;
    private static JSONObject rec;
    
    public static void main(String[] args) throws SocketException, IOException {
        
        boolean exit = false;
        
        BufferedReader br =new BufferedReader(new InputStreamReader(System.in));
      
        InetAddress IP = InetAddress.getByName("127.0.0.1");

        clientSocket = new DatagramSocket();
        byte[] sendbuffer = new byte[1024];
        
        
        
        
        int clientID = 0;
            
        //Convert to json, cuidado com a maneria como se constroio a mesnagem
        sendMsg = "{ \"Type\":\"clientID\"}";
        sendObj = new JSONObject(sendMsg);
        //System.out.println(obj.getString("Type"));
        messageManager(clientSocket,sendObj);
                        
        receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
            clientSocket.receive(receivePacket);
            serverData = new String(receivePacket.getData());
            
            rec = new JSONObject(serverData);
            for(int i=1; i < rec.names().length();i++){
                List tmp = rec.names().toList();
                clientID = rec.getInt("clientID");
                System.out.print("\nO seu ID é " + clientID);
            }
        
        
        while(!exit)
        {
            
            
            String auctionName = "";
            
            
            int auctionID = 0;
            int userID = 0;
            double amount = 0;
            
            
            
            System.out.print("\n\nEscolha uma opção");
            System.out.print("\n1-Criar leilão");
            System.out.print("\n2-Terminar leilão");
            System.out.print("\n3-Ver leilões ativos");
            System.out.print("\n4-Ver leilões concluídos");
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
                        boolean firstTime = true;
                        while(auctionName.equals("")){
                            if(!firstTime){
                                System.out.print("\nERRO! O nome não pode estar vazio!");
                            }
                            System.out.print("\nNome: ");
                            auctionName = br.readLine();
                            firstTime = false;
                        }
                        int auctionTime = 0;
                        while(auctionTime == 0){
                            System.out.print("\nDuração(minutos): ");
                            try{
                                auctionTime = Integer.parseInt(br.readLine());
                            }catch(NumberFormatException e){
                                System.out.print("\nERRO! Insira um número!");
                            }
                        }
                        
                        String auctionType = "";
                        int optionType = 0;
                        while(auctionType.equals("")){
                            System.out.print("\nTipo");
                            System.out.print("\n1-Leilão Inglês");
                            System.out.print("\n2-Leilão Cego");
                            System.out.print("\nOpção: ");
                            try{
                                optionType = Integer.parseInt(br.readLine());
                            }catch(NumberFormatException e){
                                    System.out.print("\nERRO! Insira um número!");
                            }
                            switch (optionType){
                                case 1:
                                    auctionType = "english";
                                    break;
                                case 2:
                                    auctionType = "blind";
                                    break;
                                default:
                                    System.out.print("\nERRO! Opção inválida!");
                                    break;
                            }
                        }
                        //Convert to json, cuidado com a maneria como se constroio a mesnagem
                        sendMsg = "{ \"Type\":"+messageType+",\"clientID\":"+clientID+",\"Name\":\""+auctionName+ "\",\"Time\":"+auctionTime+",\"AuctionType\":"+auctionType+"}";
                        sendObj = new JSONObject(sendMsg);
                        //System.out.println(obj.getString("Type"));
                        messageManager(clientSocket,sendObj);
                        
                        receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                        clientSocket.receive(receivePacket);
                        serverData = new String(receivePacket.getData());

                        rec = new JSONObject(serverData);
                        type = rec.getString(("Type"));
                        switch(type){
                            case "SUCCESS":
                                System.out.print("\nLeilão criado.");
                                break;
                            default:
                                System.out.print("\nERRO!");
                                break;
                        }
                        break;
                        
                    case "2":
                        boolean existAuctions = getActiveAuctionsByClient(clientID);
                        
                        if(existAuctions){
                            messageType = "tta";
                            auctionID = 0;
                            while(auctionID == 0){
                                System.out.print("\nInsira o ID do leilão desejado: ");
                                try{
                                    auctionID = Integer.parseInt(br.readLine());
                                }catch(NumberFormatException e){
                                    System.out.print("\nERRO! Insira um número!");
                                }
                            }
                            sendMsg = "{ \"Type\":"+messageType + ",\"ClientID\":"+clientID + ",\"AuctionID\":"+auctionID+"}";
                            sendObj = new JSONObject(sendMsg);
                            messageRepository(clientSocket,sendObj);

                            receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                            clientSocket.receive(receivePacket);
                            serverData = new String(receivePacket.getData());

                            rec = new JSONObject(serverData);
                            String type = rec.getString(("Type"));
                            switch(type){
                                case "SUCCESS":
                                    if(rec.getBoolean("SUCCESS")){
                                        System.out.print("\nLeilão terminado com sucesso.");
                                    }
                                    else{
                                        System.out.print("\nERRO! Leilão não encontrado!");
                                    }
                                break;
                            }
                        }
                        break;
                        
                    case "3":
                        getActiveAuctions();
                        break;
                        
                    case "4":
                        getInactiveAuctions();
                        break;
                        
                    case "5":
                        existAuctions = false;
                        int optionActiveInactive = 0;
                        
                        while(optionActiveInactive == 0){
                            System.out.print("\nEscolha entre");
                            System.out.print("\n1-Leilões ativos");
                            System.out.print("\n2-Leilões inativos");
                            System.out.print("\nOpção: ");
                            try{
                                optionActiveInactive = Integer.parseInt(br.readLine());
                            }catch(NumberFormatException e){
                                    System.out.print("\nERRO! Insira um número!");
                            }
                        }
                        switch (optionActiveInactive){
                            case 1:
                                existAuctions = getActiveAuctions();
                                break;
                            case 2:
                                existAuctions = getInactiveAuctions();
                                break;
                            default:
                                System.out.print("\nERRO! Opção inválida! De volta ao menu inicial.");
                                break;
                        }
                        if(existAuctions){
                            auctionID = 0;
                            while(auctionID == 0){
                                System.out.print("\nInsira o ID do leilão desejado: ");
                                try{
                                    auctionID = Integer.parseInt(br.readLine());
                                }catch(NumberFormatException e){
                                        System.out.print("\nERRO! Insira um número!");
                                }
                            }
                            messageType = "gba";
                            sendMsg = "{ \"Type\":"+messageType+ ",\"AuctionID\":"+auctionID+"}";
                            sendObj = new JSONObject(sendMsg);
                            messageRepository(clientSocket,sendObj);
                            
                            receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                            clientSocket.receive(receivePacket);
                            serverData = new String(receivePacket.getData());

                            rec = new JSONObject(serverData);
                            type = rec.getString(("Type"));
                            switch(type){
                                case "Bids":
                                    if(rec.getJSONArray("Bids").length() != 0){
                                        for(int i=0; i < rec.getJSONArray("Bids").length();i++){
                                            System.out.print("\nLicitação" + i + ": " + rec.getJSONArray("Bids").getDouble(i));
                                        }
                                    }else
                                    {
                                        System.out.print("\nNão foi feita nenhuma licitação.");
                                    }
                                break;
                            }
                        }
                        break;
                        
                    case "6":
                        messageType = "gbc";
                        userID = 0;
                        while(userID == 0){
                            System.out.print("\nInsira o ID do utilizador: ");
                            try{
                                userID = Integer.parseInt(br.readLine());
                            }catch(NumberFormatException e){
                                System.out.print("\nERRO! Insira um número!");
                            }
                        }
                        sendMsg = "{ \"Type\":"+messageType+ ",\"ClientID\":"+userID+"}";
                        sendObj = new JSONObject(sendMsg);
                        messageRepository(clientSocket,sendObj);
                        
                        receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                        clientSocket.receive(receivePacket);
                        serverData = new String(receivePacket.getData());

                        rec = new JSONObject(serverData);
                        type = rec.getString(("Type"));
                        switch(type){
                            case "Bids":
                                if(rec.getJSONArray("Values").length() != 0){
                                    for(int i=0; i < rec.getJSONArray("Values").length();i++){
                                        System.out.print("\nLicitação" + i + ": " + rec.getJSONArray("Values").getDouble(i));
                                    }
                                }else
                                {
                                    System.out.print("\nEste utilizador não existe ou não fez nenhuma licitação.");
                                }
                            break;
                        }
                        break;
                        
                    case "7":
                        existAuctions = getInactiveAuctions();
                        
                        if(existAuctions){
                            auctionID = 0;
                            while(auctionID == 0){
                                System.out.print("\nInsira o ID do leilão desejado: ");
                                try{
                                    auctionID = Integer.parseInt(br.readLine());
                                }catch(NumberFormatException e){
                                    System.out.print("\nERRO! Insira um número!");
                                }
                            }
                            messageType = "coa";
                            sendMsg = "{ \"Type\":"+messageType+ ",\"AuctionID\":"+auctionID+"}";                            sendObj = new JSONObject(sendMsg);
                            messageRepository(clientSocket,sendObj);

                            receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                            clientSocket.receive(receivePacket);
                            serverData = new String(receivePacket.getData());

                            rec = new JSONObject(serverData);
                            type = rec.getString(("Type"));
                            switch(type){
                                case "AuctionOutcome":
                                    if(!(rec.getInt("AuctionID") == 0)){
                                        if(rec.getString("Bought").equals("true")){
                                            System.out.print("\nNome do leilão: " + rec.getString("AuctionName"));
                                            System.out.print("\nID do leilão: " + rec.getInt("AuctionID"));
                                            System.out.print("\nID do Comprador: " + rec.getInt("BuyerID"));
                                            System.out.print("\nValor: " + rec.getDouble("Value") + "€");
                                        }else{
                                            System.out.print("\nNome do leilão: " + rec.getString("AuctionName"));
                                            System.out.print("\nID do leilão: " + rec.getInt("AuctionID"));
                                            System.out.print("\nNinguém comprou.");
                                        }
                                    }else{
                                        System.out.print("\nERRO! O leilão com ID " + auctionID + " não existe!");
                                    }
                                break;
                            }
                        }
                        break;
                        
                    case "8":
                        //TODO: Não faz nada por agora, pois ainda não sabemos como validar recibos
                        messageType = "vlr";
                        sendMsg = "{ \"Type\":"+messageType+"}";
                        sendObj = new JSONObject(sendMsg);
                        messageRepository(clientSocket,sendObj);
                        
                        receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                        clientSocket.receive(receivePacket);
                        serverData = new String(receivePacket.getData());

                        rec = new JSONObject(serverData);
                        type = rec.getString(("Type"));
                        switch(type){
                            case "ret":
                                for(int i=1; i < rec.names().length();i++){
                                    List tmp = rec.names().toList();
                                    System.out.print("\nServer: " + rec.getString(tmp.get(i).toString()));
                                }
                            break;
                        }
                        break;
                        
                    case "9":
                        //TODO: Com leilões criados e ativos, se se puser um ID que não existe, o programa continua para a frente, não devia.
                        existAuctions = getActiveAuctions();
                        
                        if(existAuctions){
                            messageType = "bid";
                            auctionID = 0;
                            while(auctionID == 0){
                                System.out.print("\nInsira o ID do leilão desejado: ");
                                try{
                                    auctionID = Integer.parseInt(br.readLine());
                                }catch(NumberFormatException e){
                                    System.out.print("\nERRO! Insira um número!");
                                }
                            }
                            amount = 0.0;
                            while(amount == 0.0){
                                System.out.print("\nValor(€): ");
                                try{
                                    amount = Double.parseDouble(br.readLine());
                                }catch(NumberFormatException e){
                                    System.out.print("\nERRO! Insira um número! (Usar \".\" em vez de \",\" para separar a parte inteira da parte decimal)");
                                }
                            }
                            sendMsg = "{ \"Type\":"+messageType+",\"AuctionID\":"+auctionID+",\"Amount\":"+amount+",\"ClientID\":"+clientID+"}";
                            sendObj = new JSONObject(sendMsg);
                            messageRepository(clientSocket,sendObj);

                            receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                            clientSocket.receive(receivePacket);
                            serverData = new String(receivePacket.getData());

                            rec = new JSONObject(serverData);
                            type = rec.getString(("Type"));
                            switch(type){
                                case "ret":
                                    for(int i=1; i < rec.names().length();i++){
                                        List tmp = rec.names().toList();
                                        System.out.print("\nServer: " + rec.getString(tmp.get(i).toString()));
                                    }
                                break;
                                default:
                                    for(int i=1; i < rec.names().length();i++){
                                    List tmp = rec.names().toList();
                                    System.out.print("\nServer: " + rec.getString(tmp.get(i).toString()));
                                    }
                                    break;
                            }
                        }
                        break;
                        
                    case "0":
                        sendMsg = "exit";
                        exit = true;
                        messageManager(clientSocket,sendObj);
                        break;
                    default:
                        System.out.print("\nERRO! Opção inválida!");
            }

            /*receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
            clientSocket.receive(receivePacket);
            serverData = new String(receivePacket.getData());
            
            rec = new JSONObject(serverData);
            String type = rec.getString(("Type"));
            switch(type){
                case "ret":
                for(int i=1; i < rec.names().length();i++){
                    List tmp = rec.names().toList();
                    System.out.print("\nServer: " + rec.getString(tmp.get(i).toString()));
                }
                break;
            }*/

        }
        
        clientSocket.close();
        
    }
    
    private static boolean getActiveAuctionsByClient(int clientID) throws IOException{
        messageType = "lgaClient";
        sendMsg = "{ \"Type\":"+messageType+",\"ClientID\":" + clientID + "}";
        sendObj = new JSONObject(sendMsg);
        messageRepository(clientSocket,sendObj);

        receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
        clientSocket.receive(receivePacket);
        serverData = new String(receivePacket.getData());

        rec = new JSONObject(serverData);
        type = rec.getString(("Type"));
        switch(type){
            case "ActiveAuctions":
                int activeAuctionTotal = rec.getInt("ActiveAuctionsTotal");
                if(activeAuctionTotal == 0){
                    System.out.print("\nNão existem leilões ativos.");
                    return false;
                    }
                else{
                    System.out.print("\nLeilões ativos:");
                    for(int i=1; i<=activeAuctionTotal; i++){
                        System.out.print("\n" + rec.getInt("AuctionID" + i) + "-" + rec.getString("AuctionName" + i));
                    }
                    return true;
                }
            default:
                System.out.print("\nERRO!");
                return false;
        }
    }
    
    private static boolean getActiveAuctions() throws IOException{
        messageType = "lga";
        sendMsg = "{ \"Type\":"+messageType+"}";
        sendObj = new JSONObject(sendMsg);
        messageRepository(clientSocket,sendObj);

        receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
        clientSocket.receive(receivePacket);
        serverData = new String(receivePacket.getData());

        rec = new JSONObject(serverData);
        type = rec.getString(("Type"));
        switch(type){
            case "ActiveAuctions":
                int activeAuctionTotal = rec.getInt("ActiveAuctionsTotal");
                if(activeAuctionTotal == 0){
                    System.out.print("\nNão existem leilões ativos.");
                    return false;
                    }
                else{
                    System.out.print("\nLeilões ativos:");
                    for(int i=1; i<=activeAuctionTotal; i++){
                        System.out.print("\n" + rec.getInt("AuctionID" + i) + "-" + rec.getString("AuctionName" + i));
                    }
                    return true;
                }
            default:
                System.out.print("\nERRO!");
                return false;
        }
    }
    
    private static boolean getInactiveAuctions() throws IOException{
        messageType = "lta";
        sendMsg = "{ \"Type\":"+messageType+"}";
        sendObj = new JSONObject(sendMsg);
        messageRepository(clientSocket,sendObj);

        receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
        clientSocket.receive(receivePacket);
        serverData = new String(receivePacket.getData());

        rec = new JSONObject(serverData);
        type = rec.getString(("Type"));
        switch(type){
            case "InactiveAuctions":
                int inactiveAuctionTotal = rec.getInt("InactiveAuctionsTotal");
                if(inactiveAuctionTotal == 0){
                    System.out.print("\nNão existem leilões concluídos.");
                    return false;
                    }
                else{
                    System.out.print("\nLeilões concluídos:");
                    for(int i=1; i<=inactiveAuctionTotal; i++){
                        System.out.print("\n" + rec.getInt("AuctionID" + i) + "-" + rec.getString("AuctionName" + i));
                    }
                    return true;
                }
            default:
                System.out.print("\nERRO!");
                return false;
        }
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
