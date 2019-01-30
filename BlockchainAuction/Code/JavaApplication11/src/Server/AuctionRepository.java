package Server;
import Auction.*;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Auction Repository Class
 * <br>
 * Tem e gera a informação sobre todos os leilões
 */
public class AuctionRepository {

    //Client Keys
    private static final ArrayList<PublicKey> clientKeys = new ArrayList<>();
    private static final ArrayList<Certificate> clientCert = new ArrayList<>();
    private static final ArrayList<SecretKey> clientSecret = new ArrayList<>();
    private static KeyPair kp;
    
    private static int auctionID=0;
    public static void newAuctionID() {
        auctionID++;
    }
    //Client ID
    private static int clientID=0;
    public static void newClientID() {
         clientID++;
    }
   
    public static void main(String[] args) throws SocketException, IOException, NoSuchAlgorithmException, UnknownHostException, GeneralSecurityException, InterruptedException {
        
        //Cria par de chaves 
        kp = SecurityRepository.generateKey();

        //Criar certificado
        X509Certificate cert = SecurityRepository.generateCert(kp,"CN=Server_Repository, L=Aveiro, C=PT", 100, "SHA1withRSA");
        //SecurityManager.printCertificateSpecs(cert);
     
        //All auctions
        ArrayList<Auction> AuctionList= new ArrayList<>();

        DatagramSocket serverSocket = new DatagramSocket(9876);

        //Informação relativa ao cliente
        boolean client = false;
        InetAddress ClientIP=null;
        int ClientPort=0;

        while(true){

            //Receber informação por udp
            byte[] receivebuffer= new byte[1024];
            byte[] sendbuffer;
            DatagramPacket recvdpkt = new DatagramPacket(receivebuffer, receivebuffer.length);
            serverSocket.receive(recvdpkt);
            ClientIP = recvdpkt.getAddress();
            ClientPort = recvdpkt.getPort();
            byte[] receivedBytes = recvdpkt.getData();
            //Thread.sleep(2000);
            /*int i=0;
            while(i<receivedBytes.length){
                if(receivedBytes[i]!=0) i++;
                else break;
            }*/
            String ReceivedMsg = "";
            //Thread.sleep(1000);
            //System.out.println(Arrays.toString(receivedBytes));
            if(!(clientSecret.isEmpty() || new String(receivedBytes).contains("Type"))){
                byte[] IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
                byte[] msg = Arrays.copyOfRange(receivedBytes, 16, recvdpkt.getLength()); //Msg igual
                //Thread.sleep(2000);
                //System.out.println(Arrays.toString(IV));
                //System.out.println(Arrays.toString(msg));
                //Decriptar mensagem
                msg = SecurityRepository.decryptMsgSym(msg, clientSecret.get(clientID-1), IV);
                ReceivedMsg = new String(msg);
          
            }else{
              ReceivedMsg = new String(receivedBytes);
              System.out.println(ReceivedMsg);
            }
          
            JSONObject recMsg = new JSONObject(ReceivedMsg);
            //System.out.println("\nManager : "+ ReceivedMsg);

            ComputeMessageType(serverSocket,AuctionList,recMsg,ClientIP, ClientPort,cert);

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
    
    private static void ComputeMessageType(DatagramSocket serverSocket,ArrayList<Auction> AuctionList, JSONObject msg, InetAddress ClientIP, int ClientPort, X509Certificate cert) throws IOException, UnknownHostException, CertificateEncodingException, CertificateException, GeneralSecurityException, InterruptedException{
        String type = msg.getString(("Type"));
        String retMsg="";
        Gson gson = new Gson();
        JSONObject retJSON;
        
        switch(type){
            case "init":
                    //Mandar certificado 
                    messageClientCert(ClientIP, ClientPort,serverSocket,cert);
                    
                    //Receber certificado do cliente
                    byte[] receivebuffer = new byte[32768];
                    DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                    serverSocket.receive(receivePacket);
                    byte[] certificateBytes = receivePacket.getData();
                    
                    //Guardar certificado do cliente
                    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    X509Certificate certificate = (X509Certificate)(certificateFactory.generateCertificate( new ByteArrayInputStream(certificateBytes)));
                    clientCert.add(certificate);
                    //System.out.println(certificate.toString());
                    
                    //Validar certificado
                    try{
                        certificate.checkValidity();
                        clientKeys.add(certificate.getPublicKey());
                        retMsg = "Valid certificate";
                        retJSON = new JSONObject("{ \"Type\":\"cert\",\"Message\":"+retMsg+"}");
                        messageClient(ClientIP, ClientPort,serverSocket, retJSON);
                        //System.out.println("Certificate valid");
                    }catch(IOException | CertificateExpiredException | CertificateNotYetValidException | JSONException e){
                        retMsg = "Invalid certificate";
                        retJSON = new JSONObject("{ \"Type\":\"cert\",\"Message\":"+retMsg+"}");
                        messageClient(ClientIP, ClientPort,serverSocket, retJSON);
                        //System.out.println("Certificate invalid");
                    }
                    
                    //Receber chave simétrica
                    byte[] receivebuffer2 = new byte[64000];
                    DatagramPacket receivePacket2 = new DatagramPacket(receivebuffer2, receivebuffer.length);
                    serverSocket.receive(receivePacket2);
                    String SymReceivedMsg = new String(receivePacket2.getData());
                    JSONObject symMsg = new JSONObject(SymReceivedMsg);
                    JSONArray data = symMsg.getJSONArray("Sym");
                    JSONArray data2 = symMsg.getJSONArray("Data");
                    
                    //Decifrar mensagem
                    byte[] dataKey = gson.fromJson(data.toString(), byte[].class); //Hash
                    byte[] dataHash = gson.fromJson(data2.toString(),byte[].class); //Chave
                    byte[] symKey = SecurityRepository.decryptMsg(clientKeys.get(0),dataKey);
                    
                    //Verificar assinatura
                    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                    byte[] digest = messageDigest.digest(dataHash);
                    
                    //Obter chave simétrica
                    //SecretKey symetricKey = new SecretKey()
                    SecretKey symetricKey = new SecretKeySpec(dataHash, 0, dataHash.length, "AES");

                    //System.out.println("key inside"+Arrays.toString(symetricKey.getEncoded()));
                    
                    if(Arrays.equals(digest, symKey)){
                        clientSecret.add(symetricKey);
                        //System.out.println(Arrays.toString(symetricKey.toString().getBytes()));
                        //System.out.println("Assinatura validada !");
                    }else{
                        System.out.println("Assinatura inválida !");
                        //System.out.println(Arrays.toString(digest));
                        //System.out.println(Arrays.toString(symKey));
                    }
                    
                    break;
                    
            case "clientID":
                    newClientID();
                    break;
                    
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
                String json = ""+gson.toJson(puzzle);
                String sendMsg = "{ \"Type\":\"Puzzle\",\"Data\":"+json+"}";
                JSONObject sendObj = new JSONObject(sendMsg); 
                messageClient(ClientIP,ClientPort,serverSocket,sendObj);
                
                //Verificar cryptopuzzle
                byte[] receivebufferBid = new byte[32768];
                DatagramPacket receivePacketBid = new DatagramPacket(receivebufferBid, receivebufferBid.length);
                serverSocket.receive(receivePacketBid);
                //String puzzleReceivedMsg = new String(receivePacketBid.getData());
                
                //Decriptar
                byte[] receivedBytes = receivePacketBid.getData();
                /*Thread.sleep(2000);
                int s=0;
                while(s<receivedBytes.length){
                    if(receivedBytes[s]!=0) s++;
                    else break;
                }*/
                
                byte[] IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
                byte[] msgEnc = Arrays.copyOfRange(receivedBytes, 16, receivePacketBid.getLength()); //Msg igual
                //Decriptar mensagem
                msgEnc = SecurityRepository.decryptMsgSym(msgEnc, clientSecret.get(clientID-1), IV);
                String puzzleReceivedMsg = new String(msgEnc);
                
                JSONObject puzzleMsg = new JSONObject(puzzleReceivedMsg);
                JSONArray dataPuzzle = puzzleMsg.getJSONArray("Data");
                byte[] solution = gson.fromJson(dataPuzzle.toString(), byte[].class);
                
                //Enviar resposta ao cliente
                if(Arrays.equals(solution, puzzle)){
                    System.out.println("ENVIADO");
                    sendMsg = "{ \"Type\":\"Puzzle\",\"Result\":\"SUCESS\"}";
                    sendObj = new JSONObject(sendMsg);
                    System.out.println(sendObj.toString());
                    messageClient(ClientIP,ClientPort,serverSocket,sendObj);
                }
                
                //Manda leilões ativos
                byte[] receivebufferBidActive = new byte[32768];
                DatagramPacket receivePacketBidActive = new DatagramPacket(receivebufferBidActive, receivebufferBidActive.length);
                serverSocket.receive(receivePacketBidActive);
                //String ReceivedMsg = new String(receivePacketBidActive.getData());
                //Decriptar
                receivedBytes = receivePacketBidActive.getData();
                /*Thread.sleep(1000);
                //System.out.println("ativos"+Arrays.toString(receivedBytes));
                s=0;
                while(s<receivedBytes.length){
                    if(receivedBytes[s]!=0) s++;
                    else break;
                }*/
                
                IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
                msgEnc = Arrays.copyOfRange(receivedBytes, 16, receivePacketBidActive.getLength()); //Msg igual
                //Decriptar mensagem
                msgEnc = SecurityRepository.decryptMsgSym(msgEnc, clientSecret.get(clientID-1), IV);
                String ReceivedMsg = new String(msgEnc);
                
                msg = new JSONObject(ReceivedMsg);
                //System.out.println(msg.toString());
                //Listar todos os leilões ativos
                retMsg="";
                //Percorrer todos os leilões, os que tiverem ativos são adicionados á string
                int activeAuctionTotalBid = 0;
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).isAuctionFinished()==false){
                        activeAuctionTotalBid++;
                        String auctionName = AuctionList.get(i).getAuctionName();
                        int auctionID = AuctionList.get(i).getAuctionID();
                        retMsg += ",\"AuctionName" + activeAuctionTotalBid + "\":\"" + auctionName + "\",\"AuctionID" + activeAuctionTotalBid + "\":" + auctionID + "";
                    }
                 }
                
                //Devolver mensagem
                retJSON = new JSONObject("{ \"Type\":\"ActiveAuctions\",\"Message\":\"Operation completed with sucess!\",\"ActiveAuctionsTotal\":" + activeAuctionTotalBid + retMsg + "}");
                messageClient(ClientIP,ClientPort,serverSocket,retJSON);
                
                //Esperar bid
                byte[] receivebufferBidFinal = new byte[32768];
                DatagramPacket receivePacketBidFinal = new DatagramPacket(receivebufferBidFinal, receivebufferBidFinal.length);
                serverSocket.receive(receivePacketBidFinal);
                //ReceivedMsg = new String(receivePacketBidFinal.getData());
                
                //Decriptar
                receivedBytes = receivePacketBidFinal.getData();
                /*Thread.sleep(2000);
                //System.out.println(Arrays.toString(receivedBytes));
                //System.out.println(Arrays.toString(receivedBytes));
                s=0;
                while(s<receivedBytes.length){
                    if(receivedBytes[s]!=0) s++;
                    else break;
                }*/
                
                IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
                msgEnc = Arrays.copyOfRange(receivedBytes, 16, receivePacketBidFinal.getLength()); //Msg igual
                //Decriptar mensagem
                msgEnc = SecurityRepository.decryptMsgSym(msgEnc, clientSecret.get(clientID-1), IV);
                ReceivedMsg = new String(msgEnc);
                
                msg = new JSONObject(ReceivedMsg);
                //System.out.println(msg.toString());
                
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
    
    /**
     * Função que manda mensagem para o cliente com o certificado do servidor.
     * 
     * @param ClientIP IP do cliente
     * @param ClientPort Número da porta do cliente
     * @param serverSocket Socket do Auction Manager
     * @param cert Certificado do servidor
     * @throws UnknownHostException
     * @throws IOException 
     * @throws java.security.cert.CertificateEncodingException 
     */
    private static void messageClientCert(InetAddress ClientIP, int ClientPort,DatagramSocket serverSocket, X509Certificate cert) throws UnknownHostException, IOException, CertificateEncodingException{
        byte[] sendbuffer;
        sendbuffer = cert.getEncoded();
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, ClientIP ,ClientPort);
        serverSocket.send(sendPacket);
    }
}

