package Server;
import Auction.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Auction Repository Class
 * <br>
 * Tem e gera a informação sobre todos os leilões
 */
public class AuctionRepository {

    private static int auctionID=0;
    public static void newAuctionID() {
        auctionID++;
    }
   
    public static void main(String[] args) throws SocketException, IOException {
        
        //All auctions
        ArrayList<Auction> AuctionList= new ArrayList<>();

        DatagramSocket serverSocket = new DatagramSocket(9876);

        //Informação relativa ao cliente
        boolean client = false;
        InetAddress ClientIP=null;
        int ClientPort=0;

        while(true){

            //Receber informação por udp
            byte[] receivebuffer = new byte[1024];
            byte[] sendbuffer  = new byte[1024];
            DatagramPacket recvdpkt = new DatagramPacket(receivebuffer, receivebuffer.length);
            serverSocket.receive(recvdpkt);
            ClientIP = recvdpkt.getAddress();
            ClientPort = recvdpkt.getPort();

            String ReceivedMsg = new String(recvdpkt.getData());
            JSONObject recMsg = new JSONObject(ReceivedMsg);
            System.out.println(recMsg.toString());
            //System.out.println("\nManager : "+ ReceivedMsg);

            ComputeMessageType(serverSocket,AuctionList,recMsg,ClientIP, ClientPort);

        }
    }
    
    /**
     * Dependendo do tipo de mensagem recebida executa o pretendido.
     * Tipos de mensagens 
     * <ul>
        *<li>cta - Criar leilão</li>
        *<li>tta - Terminar leilão</li>
        *<li>gba - Ver bids feitos em um leilão</li>
        *<li>gbc - Ver bids feitos por um cliente</li>
        *<li>lga - Listar todos os leilões ativos</li>
        *<li>lta - Listar todos os leilões inativos</li>
        *<li>coa - Ver resultado de um leilão</li>
        *<li>vlr - Validar recibo</li>
        *<li>rct - Reposta do Repositório a criar leilão</li>
        *<li>rtt - Reposta do Repositório a terminar leilão</li>
        *<li>rgb - Reposta do Repositório a ver bids feitos em um leilão</li>
        *<li>rgc - Reposta do Repositório a ver bids feitos por um cliente</li>
        *<li>rlg - Reposta do Repositório a listar todos os leilões ativos</li>
        *<li>rlt - Reposta do Repositório a listar todos os leilões inativos</li>
        *<li>rco - Reposta do Repositório a ver resultado de um leilão</li>
        *<li>rvl - Reposta do Repositório a validar recibo</li>
        *<li>end - Terminar tudo</li>
        </ul>
     * 
     * @param AuctionList Lista de leilões
     * @param msg Mensagem a ser interpretada
     * @param ClientIP IP do cliente
     * @param ClientPort Número da porta do cliente
     * @param serverSocket Socket do Auction Repository
     * @throws IOException 
     */
    
    private static void ComputeMessageType(DatagramSocket serverSocket,ArrayList<Auction> AuctionList, JSONObject msg, InetAddress ClientIP, int ClientPort) throws IOException{
        String type = msg.getString(("Type"));
        String retMsg="";
        JSONObject retJSON;
        
        switch(type){
            case "cta": 
                //leilão inglês ou leilão cego
                boolean englishAuction=true;
                newAuctionID();
                if(msg.getString("AuctionType").equals("blind") ){
                    englishAuction = false;
                }

                Auction a = new Auction(msg.getInt("clientID"), msg.getString("Name"), auctionID, msg.getInt("Time"), englishAuction);
                AuctionList.add(a);
                
                //Devolver mensagem
                retJSON = new JSONObject("{ \"Type\":\"SUCCESS\"}");
                messageManager(serverSocket,retJSON);
                break;
                
            case "tta" : 
                //Terminar leilão
                boolean found = false;
                double value = 0;
                //Percorrer todos os leilões procurar pelo ID que foi fornecido na mensagem, quando encontrar mudar o estado desse leilão
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).getAuctionID() == msg.getInt("AuctionID")) {
                        if(AuctionList.get(i).getCreatorID() == msg.getInt("ClientID")){
                            for(int j=0; j<AuctionList.get(i).getBids().size();j++){
                                if(AuctionList.get(i).getBids().get(j).getValue()>value){
                                    value = AuctionList.get(i).getBids().get(j).getValue();
                                    AuctionList.get(i).setWinnerBid(AuctionList.get(i).getBids().get(j));
                                    AuctionList.get(i).setBuyerID(AuctionList.get(i).getBids().get(j).getClientID());

                                }
                            }
                            AuctionList.get(i).setAuctionFinished(true);
                            found = true;
                            break;
                        }
                    }
                 }
                
                retJSON = new JSONObject("{ \"Type\":\"SUCCESS\",\"SUCCESS\":"+found+"}");
                messageClient(ClientIP,ClientPort,serverSocket,retJSON);
                break;
                
            case "gba" : 
                //Listar todos os bids de um client
                retMsg="";
                String arrayBidsValues = "";
                //Percorrer todos os leilões, os que tiverem ativos são adicionados á string
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).getAuctionID() == msg.getInt("AuctionID")){
                        /*for(int j=0; j<AuctionList.get(i).getBids().size(); j++){
                            retMsg+= AuctionList.get(i).getBids().get(j).getValue()+" ";
                        }*/
                        for(int j=0; j<AuctionList.get(i).getBids().size(); j++){
                            if(j!=AuctionList.get(i).getBids().size()-1){
                                arrayBidsValues += AuctionList.get(i).getBids().get(j).getValue() + ",";
                            }else{
                                arrayBidsValues += AuctionList.get(i).getBids().get(j).getValue();
                            }
                        }
                    }
                 }
                
                System.out.println("bids -> "+ retMsg);
              
                //Devolver mensagem
                retJSON = new JSONObject("{ \"Type\":\"Bids\",\"Message\":\"Operation completed with sucess!\",\"Bids\":["+ arrayBidsValues +"]}");
                messageClient(ClientIP,ClientPort,serverSocket,retJSON);
                break;
                
            case "gbc" : 
                //Listar todos os bids de um client
                retMsg="";
                arrayBidsValues = "";
                //Percorrer todos os leilões, os que tiverem ativos são adicionados á string
                for(int i=0; i<AuctionList.size();i++){
                        for(int j=0; j<AuctionList.get(i).getBids().size(); j++){
                            if(AuctionList.get(i).getBids().get(j).getClientID()==msg.getInt("ClientID")){
                                if(j!=AuctionList.get(i).getBids().size()-1){
                                    arrayBidsValues += AuctionList.get(i).getBids().get(j).getValue() + ",";
                                }else{
                                    arrayBidsValues += AuctionList.get(i).getBids().get(j).getValue();
                                }
                            }
                        }
                 }  
              
                //Devolver mensagem
                retJSON = new JSONObject("{ \"Type\":\"Bids\",\"Message\":\"Operation completed with sucess!\",\"Values\":["+arrayBidsValues+"]}");
                messageClient(ClientIP,ClientPort,serverSocket,retJSON);
                break;
                
            case  "lga": 
                //Listar todos os leilões ativos
                retMsg="";
                //Percorrer todos os leilões, os que tiverem ativos são adicionados á string
                int activeAuctionTotal = 0;
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).isAuctionFinished()==false){
                        activeAuctionTotal++;
                        String auctionName = AuctionList.get(i).getAuctionName();
                        int auctionID = AuctionList.get(i).getAuctionID();
                        retMsg += ",\"AuctionName" + activeAuctionTotal + "\":\"" + auctionName + "\",\"AuctionID" + activeAuctionTotal + "\":" + auctionID + "";
                    }
                 }
                
                //Devolver mensagem
                retJSON = new JSONObject("{ \"Type\":\"ActiveAuctions\",\"Message\":\"Operation completed with sucess!\",\"ActiveAuctionsTotal\":" + activeAuctionTotal + retMsg + "}");
                
                messageClient(ClientIP,ClientPort,serverSocket,retJSON);
                break;
                
            case  "lgaClient": 
                //Listar todos os leilões ativos de um cliente
                retMsg="";
                //Percorrer todos os leilões, os que tiverem ativos são adicionados á string
                activeAuctionTotal = 0;
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).getCreatorID() == msg.getInt("ClientID")){
                        if(AuctionList.get(i).isAuctionFinished()==false){
                            activeAuctionTotal++;
                            String auctionName = AuctionList.get(i).getAuctionName();
                            int auctionID = AuctionList.get(i).getAuctionID();
                            retMsg += ",\"AuctionName" + activeAuctionTotal + "\":\"" + auctionName + "\",\"AuctionID" + activeAuctionTotal + "\":" + auctionID + "";
                        }
                    }
                 }
                
                //Devolver mensagem
                retJSON = new JSONObject("{ \"Type\":\"ActiveAuctions\",\"Message\":\"Operation completed with sucess!\",\"ActiveAuctionsTotal\":" + activeAuctionTotal + retMsg + "}");
                
                messageClient(ClientIP,ClientPort,serverSocket,retJSON);
                break;
                
            case  "lta": 
                //Listar todos os leilões inativos
                retMsg="";
                //Percorrer todos os leilões, os que tiverem inativos são adicionados á string
                int inactiveAuctionTotal = 0;
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).isAuctionFinished()==true){
                        inactiveAuctionTotal++;
                        String auctionName = AuctionList.get(i).getAuctionName();
                        int auctionID = AuctionList.get(i).getAuctionID();
                        retMsg += ",\"AuctionName" + inactiveAuctionTotal + "\":\"" + auctionName + "\",\"AuctionID" + inactiveAuctionTotal + "\":" + auctionID + "";
                    }
                 }
              
                //Devolver mensagem
                retJSON = new JSONObject("{ \"Type\":\"InactiveAuctions\",\"Message\":\"Operation completed with sucess!\",\"InactiveAuctionsTotal\":"+inactiveAuctionTotal+ retMsg + "}");
                messageClient(ClientIP,ClientPort,serverSocket,retJSON);
                break;
                
            case  "coa":
                //Ver resultado de um leilão
                retMsg="";
                
                found = false;
                //Percorrer todos os leilões procurar pelo ID que foi fornecido na mensagem, quando encontrar obter toda a informação sobre o leilão
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).getAuctionID() == msg.getInt("AuctionID")){
                        if(AuctionList.get(i).getBuyerID() != 0){
                            retMsg= ",\"Bought\":\"true\"" + ",\"AuctionName\":" + AuctionList.get(i).getAuctionName() + ",\"AuctionID\":" + AuctionList.get(i).getAuctionID() + ",\"BuyerID\":" + AuctionList.get(i).getBuyerID() + ",\"Value\":" + AuctionList.get(i).getWinnerBid().getValue();
                        }else{
                            retMsg= ",\"Bought\":\"false\"" + ",\"AuctionName\":" + AuctionList.get(i).getAuctionName() + ",\"AuctionID\":" + AuctionList.get(i).getAuctionID();
                        }
                        found = true;
                        break;
                    }
                }
                if(!found){
                    retMsg= ",\"AuctionID\":0";
                }
                //Devolver mensagem
                retJSON = new JSONObject("{ \"Type\":\"AuctionOutcome\",\"Message\":\"Operation completed with sucess!\"" + retMsg + "}");
                messageClient(ClientIP,ClientPort,serverSocket,retJSON);
                break;
                
            case  "vlr":
                    //Não faz nada por agora, pois ainda não sabemos como validar recibos
                    //Devolver mensagem
                    retJSON = new JSONObject("{ \"Type\":\"ret\",\"Message\":\"Ainda não foi implementado\"}");
                    messageClient(ClientIP,ClientPort,serverSocket,retJSON);
                    break;
                
            case "bid":              
                //Enviar cryptopuzzle
                byte[] puzzle = SecurityRepository.puzzleGenerate();
                Gson gson = new Gson();   
                String json = ""+gson.toJson(puzzle);
                String sendMsg = "{ \"Type\":\"Puzzle\",\"Data\":"+json+"}";
                JSONObject sendObj = new JSONObject(sendMsg); 
                messageClient(ClientIP,ClientPort,serverSocket,sendObj);
                
                //Verificar cryptopuzzle
                byte[] receivebufferBid = new byte[32768];
                DatagramPacket receivePacketBid = new DatagramPacket(receivebufferBid, receivebufferBid.length);
                serverSocket.receive(receivePacketBid);
                String puzzleReceivedMsg = new String(receivePacketBid.getData());
                JSONObject puzzleMsg = new JSONObject(puzzleReceivedMsg);
                JSONArray data = puzzleMsg.getJSONArray("Data");
                byte[] solution = gson.fromJson(data.toString(), byte[].class);
                
                //Enviar resposta ao cliente
                if(Arrays.equals(solution, puzzle)){
                    System.out.println("ENVIADO");
                    sendMsg = "{ \"Type\":\"Puzzle\",\"Result\":\"SUCESS\"}";
                    sendObj = new JSONObject(sendMsg);
                    System.out.println(sendObj.toString());
                    messageClient(ClientIP,ClientPort,serverSocket,sendObj);
                }
                
                //Esperar bid
                byte[] receivebufferBidFinal = new byte[32768];
                DatagramPacket receivePacketBidFinal = new DatagramPacket(receivebufferBidFinal, receivebufferBidFinal.length);
                serverSocket.receive(receivePacketBidFinal);
                String ReceivedMsg = new String(receivePacketBidFinal.getData());
                msg = new JSONObject(ReceivedMsg);
                
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).getAuctionID() == msg.getInt("AuctionID"))
                    {
                        AuctionList.get(i).addBid(new Bid(msg.getDouble("Amount"),msg.getInt("ClientID")));
                    }
                }
                
                //Devolver mensagem
                retJSON = new JSONObject("{ \"Type\":\"ret\",\"Message\":\"Operation completed with sucess!\"}");
                messageClient(ClientIP,ClientPort,serverSocket,retJSON);
                break;

                
            case "end":
                    serverSocket.close();
                    System.exit(1);
                    break;
        }
    }
    
    /**
     * Função que manda mensagem para o manager.
     * 
     * @param serverSocket Socket do Auction Repository
     * @param msg Mensagem a enviar ao manager
     * @throws UnknownHostException
     * @throws IOException
     */
    private static void messageManager(DatagramSocket serverSocket, JSONObject msg) throws UnknownHostException, IOException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9877;
        byte[] sendbuffer  = new byte[1024];

        sendbuffer = msg.toString().getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
        serverSocket.send(sendPacket);
    }
    
       /**
     * Função que manda mensagem para o cliente.
     * 
     * @param ClientIP IP do cliente
     * @param ClientPort Número da porta do cliente
     * @param serverSocket Socket do Auction Manager
     * @param msg Mensagem a enviar ao cliente
     * @throws UnknownHostException
     * @throws IOException 
     */
    private static void messageClient(InetAddress ClientIP, int ClientPort,DatagramSocket serverSocket, JSONObject msg) throws UnknownHostException, IOException{
        byte[] sendbuffer  = new byte[1024];
        sendbuffer = msg.toString().getBytes();        
        System.out.println(ClientPort);
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, ClientIP ,ClientPort);
        serverSocket.send(sendPacket);
    }
}

