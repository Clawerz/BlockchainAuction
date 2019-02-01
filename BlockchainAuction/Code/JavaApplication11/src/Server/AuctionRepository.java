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
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import javax.crypto.KeyGenerator;
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
            
    //All auctions
    private static ArrayList<Auction> AuctionList= new ArrayList<>();
    
    //Client Keys
    private static final ArrayList<PublicKey> clientKeys = new ArrayList<>();
    private static final ArrayList<Certificate> clientCert = new ArrayList<>();
    private static final ArrayList<SecretKey> clientSecret = new ArrayList<>();
    private static KeyPair kp;
    private static boolean clientAgreement = false;

    
    //Manager
    private static Certificate managerCert = null;
    private static SecretKey managerKey = null;
    private static PublicKey pubManager = null;
    private static boolean simetricKeyGen = false;
    
    private static int auctionID=0;
    
    public static void newAuctionID() {
        auctionID++;
    }
    
    //Client ID
    private static int clientID=0;
    public static void newClientID() {
         clientID++;
    }
    
    public static void newAuctionList(){
        AuctionList= new ArrayList<>();
    }
    
    
   
    public static void main(String[] args) throws SocketException, IOException, NoSuchAlgorithmException, UnknownHostException, GeneralSecurityException, InterruptedException {
        
        newAuctionList();
        
        //Cria par de chaves 
        kp = SecurityRepository.generateKey();

        //Criar certificado
        X509Certificate cert = SecurityRepository.generateCert(kp,"CN=Server_Repository, L=Aveiro, C=PT", 100, "SHA1withRSA");
        //SecurityManager.printCertificateSpecs(cert);

        DatagramSocket serverSocket = new DatagramSocket(9876);
        
        initCommunicationManager(serverSocket,cert);
        
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
            String ReceivedMsg = "";

            if(!(clientSecret.isEmpty() || new String(receivedBytes).contains("Type"))){
                byte[] IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
                byte[] msg = Arrays.copyOfRange(receivedBytes, 16, recvdpkt.getLength()); //Msg igual

                //Decriptar mensagem
                if(recvdpkt.getPort()==9877){
                    msg = SecurityManager.decryptMsgSym(msg, managerKey, IV);
                    ReceivedMsg = new String(msg);
                }else{
                    msg = SecurityManager.decryptMsgSym(msg, clientSecret.get(clientID-1), IV);
                    ReceivedMsg = new String(msg);
                }
          
            }else{
              ReceivedMsg = new String(receivedBytes);
              //System.out.println(ReceivedMsg);
            }
            
            JSONObject recMsg = new JSONObject(ReceivedMsg);
            ComputeMessageType(serverSocket,recMsg,ClientIP, ClientPort,cert);

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
    
    private static void ComputeMessageType(DatagramSocket serverSocket, JSONObject msg, InetAddress ClientIP, int ClientPort, X509Certificate cert) throws IOException, UnknownHostException, CertificateEncodingException, CertificateException, GeneralSecurityException, InterruptedException{
        
        String type = msg.getString(("Type"));
        String retMsg="";
        Gson gson = new Gson();
        JSONObject retJSON;
        
        switch(type){
            
            case "init":
                
                    //Mandar certificado 
                    messageClientCert(ClientIP, ClientPort,serverSocket,cert);
                    
                    //Receber certificado do cliente
                    byte[] receivebufferClient = new byte[32768];
                    DatagramPacket receivePacketClient = new DatagramPacket(receivebufferClient, receivebufferClient.length);
                    serverSocket.receive(receivePacketClient);
                    byte[] certificateBytesClient = receivePacketClient.getData();
                    
                    //Guardar certificado do cliente
                    CertificateFactory certificateFactoryClient = CertificateFactory.getInstance("X.509");
                    X509Certificate certificateClient = (X509Certificate)(certificateFactoryClient.generateCertificate( new ByteArrayInputStream(certificateBytesClient)));
                    clientCert.add(certificateClient);
                    
                    //Validar certificado
                    try{
                        certificateClient.checkValidity();
                        clientKeys.add(certificateClient.getPublicKey());
                        retMsg = "Valid certificate";
                        retJSON = new JSONObject("{ \"Type\":\"cert\",\"Message\":"+retMsg+"}");
                        messageClient(ClientIP, ClientPort,serverSocket, retJSON);
                    }catch(IOException | CertificateExpiredException | CertificateNotYetValidException | JSONException e){
                        retMsg = "Invalid certificate";
                        retJSON = new JSONObject("{ \"Type\":\"cert\",\"Message\":"+retMsg+"}");
                        messageClient(ClientIP, ClientPort,serverSocket, retJSON);
                    }
                    
                    //Receber chave simétrica
                    byte[] receivebuffer2Client = new byte[64000];
                    DatagramPacket receivePacket2Client = new DatagramPacket(receivebuffer2Client, receivebuffer2Client.length);
                    serverSocket.receive(receivePacket2Client);
                    String SymReceivedMsgClient = new String(receivePacket2Client.getData());
                    JSONObject symMsgClient = new JSONObject(SymReceivedMsgClient);
                    JSONArray dataClient = symMsgClient.getJSONArray("Sym");
                    JSONArray data2Client = symMsgClient.getJSONArray("Data");
                    
                    //Decifrar mensagem
                    byte[] dataKeyClient = gson.fromJson(dataClient.toString(), byte[].class); //Hash
                    byte[] dataHashClient = gson.fromJson(data2Client.toString(),byte[].class); //Chave
                    byte[] symKeyClient = SecurityRepository.decryptMsg(clientKeys.get(0),dataKeyClient);
                    
                    //Verificar assinatura
                    MessageDigest messageDigestClient = MessageDigest.getInstance("SHA-256");
                    byte[] digestClient = messageDigestClient.digest(dataHashClient);
                    
                    //Obter chave simétrica
                    SecretKey symetricKeyClient = new SecretKeySpec(dataHashClient, 0, dataHashClient.length, "AES");
                    
                    if(Arrays.equals(digestClient, symKeyClient)){
                        clientAgreement = true;
                        clientSecret.add(symetricKeyClient);
                        //System.out.println("Assinatura validada !");
                    }else{
                        System.out.println("Assinatura inválida !");
                    }
                    
                    break;
                    
            case "clientID":
                    
                //Incrementa número de cliente
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
                
                JSONObject toClient = new JSONObject();
                JSONArray toClientJSON = new JSONArray();
                JSONObject toClient2 = new JSONObject();
                JSONArray toClientJSON2 = new JSONArray();

                //Pedir ao repo para desencriptar todos os bids
                //Validar a assinatura de todos os bids
                //Mandar ao cliente todos os bids feitos e seus autores
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).getAuctionID() == msg.getInt("AuctionID")) {
                        for(int j=0; j<AuctionList.get(i).getBids_signed_encrypted().size();j++){
                            
                            //Para cada bid enviar uma mensagem a pedir para ser desencriptada
                            retMsg=""+gson.toJson( AuctionList.get(i).getBids_signed_encrypted().get(j));
                            retJSON = new JSONObject("{ \"Type\":\"decrypt\",\"Sign\":"+retMsg+"}");
                            messageManager(serverSocket,retJSON);
                            
                            //Receber bid desencriptada
                            byte[] receiveDecrypted = new byte[32768];
                            DatagramPacket receivePacketDecrypted = new DatagramPacket(receiveDecrypted, receiveDecrypted.length);
                            serverSocket.receive(receivePacketDecrypted);
                            
                            //Decriptar
                            byte[] receivedBytes = receivePacketDecrypted.getData();
                            byte[] IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
                            byte[] msgEnc = Arrays.copyOfRange(receivedBytes, 16, receivePacketDecrypted.getLength()); //Msg igual

                            //Decriptar mensagem
                            msgEnc = SecurityRepository.decryptMsgSym(msgEnc, managerKey, IV);
                            String ReceivedMsg = new String(msgEnc);
                            JSONObject decrypted = new JSONObject(ReceivedMsg);
                            JSONArray encrypted = decrypted.getJSONArray("decrypt");
                            byte[] signedMessage = gson.fromJson(encrypted.toString(), byte[].class);
                            
                            //byte[] signedMessage = gson.fromJson(decrypted.toString(), byte[].class);
                            String dataFromSignature = ",\"AuctionID\":"+AuctionList.get(i).getAuctionID()+",\"Amount\":"+AuctionList.get(i).getBids().get(j).getValue()+",\"ClientID\":"+AuctionList.get(i).getBids().get(j).getClientID()+"}";
                            //Validar assinatura
                            if(SecurityRepository.verifySign(signedMessage, dataFromSignature.getBytes(), clientCert.get(clientID-1))){
                                if(AuctionList.get(i).isEnglishAuction()){
                                    toClient.put("Valor",AuctionList.get(i).getBids().get(j).getValue());
                                    toClient.put("Cliente",AuctionList.get(i).getBids().get(j).getClientID());
                                }else{
                                    toClient.put("Valor",AuctionList.get(i).getBids().get(j).getValue());
                                }
                                System.out.println("Bid validada");
                            }
                        }
                        toClientJSON.put(toClient);
                    }
                }
                
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
                
                //De seguida mandar verificação da blockchain
                for(int i=0; i<AuctionList.size();i++){
                    for(int j=AuctionList.get(i).getBlockChain().size()-1; j>0; j--){
                        String list = AuctionList.get(i).getBids().toString();
                        AuctionList.get(i).getBids().remove(j);
                        if(SecurityRepository.verifyHash(AuctionList.get(i).getBids().toString().getBytes(), AuctionList.get(i).getBlockChain().get(j))){
                            System.out.println("Verifica"+j);
                            toClient2.put("Lista",list);
                            toClient2.put("Chain",AuctionList.get(i).getBlockChain().get(j));
                        }
                        toClientJSON2.put(toClient2);
                    }         
                }
                
                retJSON = new JSONObject("{ \"Type\":\"SUCCESS\",\"SUCCESS\":"+toClientJSON.toString()+", \"Chain\":"+toClientJSON2.toString()+"}");
                messageClient(ClientIP,ClientPort,serverSocket,retJSON);
                
                
                
                break;
                
            case "gba" : 
                
                //Listar todos os bids de um client
                retMsg="";
                String arrayBidsValues = "";
                
                //Percorrer todos os leilões, os que tiverem ativos são adicionados á string
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).getAuctionID() == msg.getInt("AuctionID")){
                        for(int j=0; j<AuctionList.get(i).getBids().size(); j++){
                            if(j!=AuctionList.get(i).getBids().size()-1){
                                arrayBidsValues += AuctionList.get(i).getBids().get(j).getValue() + ",";
                            }else{
                                arrayBidsValues += AuctionList.get(i).getBids().get(j).getValue();
                            }
                        }
                    }
                 }
                              
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
                
                //Decriptar
                byte[] receivedBytes = receivePacketBid.getData();
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
                    sendMsg = "{ \"Type\":\"Puzzle\",\"Result\":\"SUCESS\"}";
                    sendObj = new JSONObject(sendMsg);
                    System.out.println(sendObj.toString());
                    messageClient(ClientIP,ClientPort,serverSocket,sendObj);
                } 
                
                //Manda leilões ativos
                byte[] receivebufferBidActive = new byte[32768];
                DatagramPacket receivePacketBidActive = new DatagramPacket(receivebufferBidActive, receivebufferBidActive.length);
                serverSocket.receive(receivePacketBidActive);
                
                //Decriptar
                receivedBytes = receivePacketBidActive.getData();
                IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
                msgEnc = Arrays.copyOfRange(receivedBytes, 16, receivePacketBidActive.getLength()); //Msg igual
                
                //Decriptar mensagem
                msgEnc = SecurityRepository.decryptMsgSym(msgEnc, clientSecret.get(clientID-1), IV);
                String ReceivedMsg = new String(msgEnc);
                JSONObject msg3 = new JSONObject(ReceivedMsg);
                                
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
                
                //Decriptar
                receivedBytes = receivePacketBidFinal.getData();
                IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
                msgEnc = Arrays.copyOfRange(receivedBytes, 16, receivePacketBidFinal.getLength()); //Msg igual
                
                //Decriptar mensagem
                msgEnc = SecurityRepository.decryptMsgSym(msgEnc, clientSecret.get(clientID-1), IV);
                ReceivedMsg = new String(msgEnc);
                System.out.println(ReceivedMsg);
                
                //Manda Bid para o Manager verificar a bid
                JSONObject msg2 = new JSONObject(ReceivedMsg);
                messageManager(serverSocket,msg2);
                
                ReceivedMsg = "{ \"Type\":\"bid\",\"Highest\":"+getHighest(msg2.getInt("AuctionID"))+",\"AuctionType\":"+getAuctionType(msg2.getInt("AuctionID"))+"}";
                JSONObject msg4 = new JSONObject(ReceivedMsg);
                messageManager(serverSocket,msg4);
                
                //Esperar validação do manager
                byte[] receivebufferBidValid = new byte[32768];
                DatagramPacket receivePacketBidValid = new DatagramPacket(receivebufferBidValid, receivebufferBidValid.length);
                serverSocket.receive(receivePacketBidValid);
                
                //Decriptar
                receivedBytes = receivePacketBidValid.getData();
                IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
                msgEnc = Arrays.copyOfRange(receivedBytes, 16, receivePacketBidValid.getLength()); //Msg igual
                
                //Decriptar mensagem
                msgEnc = SecurityRepository.decryptMsgSym(msgEnc, managerKey, IV);
                ReceivedMsg = new String(msgEnc);
                
                JSONObject receivedValid = new JSONObject(ReceivedMsg);
                String validBid = receivedValid.getString("Message");
                System.out.println(receivedValid.toString());
                if(validBid.equals("Valid")){
                    for(int i=0; i<AuctionList.size();i++){
                        if(AuctionList.get(i).getAuctionID() == msg2.getInt("AuctionID"))
                        {
                            //Adicionar á blockChain
                            byte[] hash = SecurityRepository.hash(AuctionList.get(i).getBids().toString().getBytes());
                            AuctionList.get(i).addChain(hash);
                            
                            Bid b = new Bid(msg2.getDouble("Amount"),msg2.getInt("ClientID"));
                            AuctionList.get(i).addBid(b);
                            JSONArray datasigned = receivedValid.getJSONArray("Encrypted");
                            byte[] signed = gson.fromJson(datasigned.toString(), byte[].class); //Hash
                            AuctionList.get(i).addBidSignedEncrypted(signed); 
                        }
                    }
                    //Devolver mensagem
                    retJSON = new JSONObject("{ \"Type\":\"ret\",\"Message\":\"Operation completed with sucess!\"}");
                }else{
                    //Devolver mensagem
                    retJSON = new JSONObject("{ \"Type\":\"ret\",\"Message\":\"Invalid bid!\"}");
                }  
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
    private static void messageManager(DatagramSocket serverSocket, JSONObject msg) throws UnknownHostException, IOException, GeneralSecurityException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9877;
        byte[] sendbuffer  = new byte[1024];
        
        //Gerar IV
        Gson gson = new Gson();
        byte[] initializationVector = new byte[16];
        SecureRandom secRan = new SecureRandom(); 
        secRan.nextBytes(initializationVector);

        //Encriptrar
        sendbuffer = msg.toString().getBytes();
        sendbuffer = SecurityRepository.encryptMsgSym(sendbuffer, managerKey,initializationVector);

        byte [] sendbufferIV = new byte[sendbuffer.length+initializationVector.length];
        System.arraycopy(initializationVector, 0, sendbufferIV, 0, initializationVector.length);
        System.arraycopy(sendbuffer, 0, sendbufferIV, initializationVector.length, sendbuffer.length);

        DatagramPacket sendPacket = new DatagramPacket(sendbufferIV, sendbufferIV.length,ServerIP ,ServerPort);
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
    private static void messageClient(InetAddress ClientIP, int ClientPort,DatagramSocket serverSocket, JSONObject msg) throws UnknownHostException, IOException, GeneralSecurityException{
        byte[] sendbuffer;
        
        if(clientAgreement){
            //Gerar IV
            Gson gson = new Gson();
            byte[] initializationVector = new byte[16];
            SecureRandom secRan = new SecureRandom(); 
            secRan.nextBytes(initializationVector);

            //Encriptrar
            sendbuffer = msg.toString().getBytes();
            sendbuffer = SecurityRepository.encryptMsgSym(sendbuffer, clientSecret.get(clientID-1),initializationVector);

            byte [] sendbufferIV = new byte[sendbuffer.length+initializationVector.length];
            System.arraycopy(initializationVector, 0, sendbufferIV, 0, initializationVector.length);
            System.arraycopy(sendbuffer, 0, sendbufferIV, initializationVector.length, sendbuffer.length);

            DatagramPacket sendPacket = new DatagramPacket(sendbufferIV, sendbufferIV.length,ClientIP ,ClientPort);
            serverSocket.send(sendPacket);
        }else{
            sendbuffer = msg.toString().getBytes();        
            DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ClientIP ,ClientPort);
            serverSocket.send(sendPacket);
        }
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
    
    /**
     * Envia o certificado do Repositorio para o Servidor Manager
     * 
     * @param clientSocket Socket do Cliente
     * @param cert Certificado do Repo
     * @throws java.net.UnknownHostException
     * @throws IOException
     * @throws java.security.cert.CertificateEncodingException
     */
    private static void messageManagerCert(DatagramSocket clientSocket, X509Certificate cert) throws UnknownHostException, IOException, CertificateEncodingException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9877;
        byte[] sendbuffer;
        sendbuffer = cert.getEncoded();
        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, ServerIP ,ServerPort);
        clientSocket.send(sendPacket);
    }
    
     /**
     * Função que inicia a comunicação.Começa por mandar uma mensagem a dizer init, de seguida espera a mensagem do servidor com o certificado, valida o certificado do servidor, manda o seu certificado e fica á espera da confirmação de validação por parte do servidor, quando recebida, ativa a possibildiade de geração de chave simetrica.
     * 
     * @param clientSocket Socket do Cliente
     * @param managerKey Chave publica do manager
     * @throws UnknownHostException
     * @throws java.security.cert.CertificateException
     * @throws java.security.KeyStoreException
     * @throws IOException
     */
    private static void initCommunicationManager(DatagramSocket serverSocket, X509Certificate cert) throws IOException, CertificateException, KeyStoreException, GeneralSecurityException{
        String sendMsg = "{ \"Type\":init_server}";
        JSONObject sendObj = new JSONObject(sendMsg);
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9877;
        byte[] sendbuffer  = new byte[1024];
        sendbuffer = sendObj.toString().getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
        serverSocket.send(sendPacket);
        
        //Manager 
        byte[] receivebuffer = new byte[32768];
        DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
        serverSocket.receive(receivePacket);
        
        byte[] certificateBytes = receivePacket.getData();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate)(certificateFactory.generateCertificate( new ByteArrayInputStream(certificateBytes)));
        managerCert = certificate;
        
        try{
            certificate.checkValidity();
            pubManager = certificate.getPublicKey();
            messageManagerCert(serverSocket, cert);
        }catch(CertificateExpiredException | CertificateNotYetValidException e){
            System.out.println("Certificate not valid ");
        }
        
        serverSocket.receive(receivePacket);
        String ReceivedMsg = new String(receivePacket.getData());
        JSONObject recMsg = new JSONObject(ReceivedMsg);
        System.out.println("Server : " + recMsg.toString());
        String type = recMsg.getString(("Type"));
        if(type.equals("cert_server")){
           String msg = recMsg.getString(("Message"));
           if(msg.equals("Valid certificate")){
               simetricKeyGen = true;
           }
        }
        
        //Ver algoritmos a usar
        //Gerar chave simétrica
        if(simetricKeyGen){
            try {
                managerKey= KeyGenerator.getInstance("AES").generateKey();
   
                //Assinar chave
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                byte[] digest = messageDigest.digest(managerKey.getEncoded());
                
                //Encriptar
                byte[] symKey = SecurityRepository.encryptMsg(digest,kp.getPrivate());

                //Envia chave simetrica
                Gson gson = new Gson();
                String json = ""+gson.toJson(symKey);
                String json2 = ""+gson.toJson(managerKey.getEncoded());
                sendMsg = "{ \"Sym\":"+json+",\"Data\":"+json2+"}";
                sendObj = new JSONObject(sendMsg);                               
            } catch (NoSuchAlgorithmException ex) {
                System.out.println("Impossible to generate Symetric Key.");
            }
        }
        sendbuffer = sendObj.toString().getBytes();
        sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
        serverSocket.send(sendPacket);
    }

    /**
     * Função que devolve maior bid feita num leilão
     * 
     * @param AuctionID ID do leilão a verificar
     * @return maior bid
     */
    public static double getHighest(int AuctionID) {
        double highestBid = 0;
        for(int i=0; i<AuctionList.size();i++){
            if(AuctionList.get(i).getAuctionID() == AuctionID){      
                for(int j=0; j<AuctionList.get(i).getBids().size();j++)
                    if(AuctionList.get(i).getBids().get(j).getValue()>highestBid) highestBid = AuctionList.get(i).getBids().get(j).getValue(); 
            }
        }  
        return highestBid;
    }
    
    /**
     * Função que devolve tipo de um leilão
     * 
     * @param AuctionID ID do leilão a verificar
     * @return tipo do leilão
     */
    public static String getAuctionType(int AuctionID) {
        String type = "Blind";
        for(int i=0; i<AuctionList.size();i++){
            if(AuctionList.get(i).getAuctionID() == AuctionID){      
                if(AuctionList.get(i).isEnglishAuction()) type = "English";
            }
        }  
        return type;
    }
    
}

