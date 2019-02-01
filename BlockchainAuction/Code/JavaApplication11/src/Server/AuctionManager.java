package Server;
import Auction.Auction;
import Auction.Bid;
import Client.SecurityClient;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
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
import sun.security.util.IOUtils;

/**
 *
 * Auction Manager Class
 * <br>
 * Servidor "rendevouz" que age como intermediário entre os clientes e o servidor repositório
 * 
 */

public class AuctionManager {
    
    //Client Keys
    private static final ArrayList<PublicKey> clientKeys = new ArrayList<>();
    private static final ArrayList<Certificate> clientCert = new ArrayList<>();
    private static final ArrayList<SecretKey> clientSecret = new ArrayList<>();
    private static KeyPair kp;
    
    //Manager
    private static Certificate repoCert = null;
    private static SecretKey repoKey = null;
    private static PublicKey pubRepo = null;
    private static boolean agreement = false;
    private static boolean clientAgreement = false;
   
    //Client ID
    private static int clientID=0;
    private static boolean atLeastOneClient=false;
    public static void newClientID() {
         clientID++;
         atLeastOneClient=true;
    }
    
    public static void main(String[] args) throws SocketException, IOException, NoSuchAlgorithmException, UnknownHostException, CertificateEncodingException, CertificateException, KeyStoreException, SignatureException, InvalidKeyException, GeneralSecurityException {
     
     //Cria par de chaves 
     kp = SecurityManager.generateKey();
     
     //Criar certificado
     X509Certificate cert = SecurityManager.generateCert(kp,"CN=Server_Manager, L=Aveiro, C=PT", 100, "SHA1withRSA");
     //SecurityManager.printCertificateSpecs(cert);
     
     DatagramSocket serverSocket = new DatagramSocket(9877);
     
     //Informação relativa ao cliente
     boolean client = false;
     InetAddress ClientIP=null;
     int ClientPort=0;
     
      while(true){
          
          //Receber informação
          byte[] receivebuffer = new byte[1024];
          byte[] sendbuffer;
          DatagramPacket recvdpkt = new DatagramPacket(receivebuffer, receivebuffer.length);
          serverSocket.receive(recvdpkt); 
          
          //ReceivedMsg = new String(recvdpkt.getData());
          
          byte[] receivedBytes = recvdpkt.getData();
          String ReceivedMsg = "";
          
          if(!(clientSecret.isEmpty() || new String(receivedBytes).contains("Type") || !atLeastOneClient)){
                byte[] IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
                byte[] msg = Arrays.copyOfRange(receivedBytes, 16, recvdpkt.getLength()); //Msg igual

                //Decriptar mensagem
                if(recvdpkt.getPort()==9876 && agreement){
                    msg = SecurityManager.decryptMsgSym(msg, repoKey, IV);
                    ReceivedMsg = new String(msg);
                }else{
                    msg = SecurityManager.decryptMsgSym(msg, clientSecret.get(clientID-1), IV);
                    ReceivedMsg = new String(msg);
                }
          
            }else{
              ReceivedMsg = new String(receivedBytes);
              //System.out.println(ReceivedMsg);
            }
            
          //Obter endereço do client
          if((!recvdpkt.getAddress().toString().equals("127.0.0.1")) && (recvdpkt.getPort()!=9876 )){
            ClientIP = recvdpkt.getAddress();
            ClientPort = recvdpkt.getPort();
            client=true;
          }
          
          JSONObject recMsg = new JSONObject(ReceivedMsg);
          System.out.println("Client : " + recMsg.toString());
          
          //Ver tipo de mensagem
          ComputeMessageType(ClientIP,ClientPort,serverSocket, recMsg, cert);
      }
    }
    
    /**
     * Dependendo do tipo de mensagem recebida executa o pedido pela mesma.Tipos de mensagens 
    <ul>
    <li>init - Troca de certificados</li>
    <li>cta - Criar leilão</li>
    <li>tta - Terminar leilão</li>
    <li>gba - Ver bids feitos em um leilão</li>
    <li>gbc - Ver bids feitos por um cliente</li>
    <li>lga - Listar todos os leilões ativos</li>
    <li>lta - Listar todos os leilões inativos</li>
    <li>coa - Ver resultado de um leilão</li>
    <li>vlr - Validar recibo</li>
    <li>rct - Reposta do Repositório a criar leilão</li>
    <li>rtt - Reposta do Repositório a terminar leilão</li>
    <li>rgb - Reposta do Repositório a ver bids feitos em um leilão</li>
    <li>rgc - Reposta do Repositório a ver bids feitos por um cliente</li>
    <li>rlg - Reposta do Repositório a listar todos os leilões ativos</li>
    <li>rlt - Reposta do Repositório a listar todos os leilões inativos</li>
    <li>rco - Reposta do Repositório a ver resultado de um leilão</li>
    <li>rvl - Reposta do Repositório a validar recibo</li>
    <li>end - Terminar tudo</li>
        </ul>
     * 
     * @param ClientIP IP do cliente
     * @param ClientPort Número da porta do cliente
     * @param serverSocket Socket do Auction Manager
     * @param msg Mensagem a ser interpretada
     * @param cert Certificado
     * @throws IOException 
     * @throws java.net.UnknownHostException 
     * @throws java.security.cert.CertificateEncodingException 
     * @throws java.security.KeyStoreException 
     */
    private static void ComputeMessageType(InetAddress ClientIP, int ClientPort, DatagramSocket serverSocket, JSONObject msg, X509Certificate cert) throws IOException, UnknownHostException, CertificateEncodingException, CertificateException, KeyStoreException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, GeneralSecurityException{
        String type = msg.getString(("Type"));
        Gson gson = new Gson();
        String retMsg="";
        JSONObject retJSON;

        switch(type){
            case "init_server":
                    //Mandar certificado 
                    messageRepositoryCert(serverSocket,cert);
                    
                    //Receber certificado do manager
                    byte[] receivebuffer = new byte[32768];
                    DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                    serverSocket.receive(receivePacket);
                    byte[] certificateBytes = receivePacket.getData();
                    
                    //Guardar certificado do manager
                    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    X509Certificate certificate = (X509Certificate)(certificateFactory.generateCertificate( new ByteArrayInputStream(certificateBytes)));
                    repoCert = certificate;
                    
                    //Validar certificado
                    try{
                        certificate.checkValidity();
                        pubRepo = certificate.getPublicKey();
                        retMsg = "Valid certificate";
                        retJSON = new JSONObject("{ \"Type\":\"cert_server\",\"Message\":"+retMsg+"}");
                        messageRepository(serverSocket, retJSON);
                    }catch(IOException | CertificateExpiredException | CertificateNotYetValidException | JSONException e){
                        retMsg = "Invalid certificate";
                        retJSON = new JSONObject("{ \"Type\":\"cert_server\",\"Message\":"+retMsg+"}");
                        messageRepository(serverSocket, retJSON);
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
                    byte[] symKey = SecurityRepository.decryptMsg(pubRepo,dataKey);
                    
                    //Verificar assinatura
                    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                    byte[] digest = messageDigest.digest(dataHash);
                    
                    //Obter chave simétrica
                    SecretKey symetricKey = new SecretKeySpec(dataHash, 0, dataHash.length, "AES");
                    
                    if(Arrays.equals(digest, symKey)){
                        repoKey=symetricKey;
                        agreement = true;
                        //System.out.println("Assinatura validada !");
                    }else{
                        System.out.println("Assinatura inválida !");
                    }
                    
                    break;
                    
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
                    //System.out.println(certificate.toString());
                    
                    //Validar certificado
                    try{
                        certificateClient.checkValidity();
                        clientKeys.add(certificateClient.getPublicKey());
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
                    byte[] receivebuffer2Client = new byte[64000];
                    DatagramPacket receivePacket2Client = new DatagramPacket(receivebuffer2Client, receivebuffer2Client.length);
                    serverSocket.receive(receivePacket2Client);
                    String SymReceivedMsgClient = new String(receivePacket2Client.getData());
                    //System.out.println(SymReceivedMsg);
                    JSONObject symMsgClient = new JSONObject(SymReceivedMsgClient);
                    JSONArray dataClient = symMsgClient.getJSONArray("Sym");
                    JSONArray data2Client = symMsgClient.getJSONArray("Data");
                    
                    //Decifrar mensagem
                    byte[] dataKeyClient = gson.fromJson(dataClient.toString(), byte[].class);
                    byte[] dataHashClient = gson.fromJson(data2Client.toString(),byte[].class);
                    byte[] symKeyClient = SecurityManager.decryptMsg(clientKeys.get(0),dataKeyClient);
                    
                    //Verificar assinatura
                    MessageDigest messageDigestClient = MessageDigest.getInstance("SHA-256");
                    byte[] digestClient = messageDigestClient.digest(dataHashClient);
                    
                    //Obter chave simétrica
                    SecretKey symetricKeyClient = new SecretKeySpec(dataHashClient, 0, dataHashClient.length, "AES");
                    //System.out.println(Arrays.toString(symetricKey.toString().getBytes()));
                    
                    if(Arrays.equals(digestClient, symKeyClient)){
                        clientAgreement=true;
                        clientSecret.add(symetricKeyClient);
                        //System.out.println("Assinatura validada !");
                    }else{
                        System.out.println("Assinatura inválida !");
                        //System.out.println(Arrays.toString(digest));
                        //System.out.println(Arrays.toString(symKey));
                    }
                                        
                    break;
                    
            case "decrypt":
                    JSONArray encrypted = msg.getJSONArray("Sign");
                    byte[] decrypted = SecurityManager.decryptMsg(kp.getPublic(), gson.fromJson(encrypted.toString(), byte[].class));
                    String sendDecrypted = gson.toJson(decrypted);
                    messageRepository(serverSocket,new JSONObject("{ \"Type\":\"decrypt\",\"decrypt\":"+sendDecrypted+"}"));
                    break;
                    
            case "bid":
                    double amount = msg.getDouble("Amount");
                    System.out.println(amount);
                    
                    //Receber chave simétrica
                    byte[] receiveHighest = new byte[64000];
                    DatagramPacket receivePacketHighest = new DatagramPacket(receiveHighest, receiveHighest.length);
                    serverSocket.receive(receivePacketHighest);
                    
                    //Decriptar
                    byte[] receivedBytes = receivePacketHighest.getData();
                    byte[] IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
                    byte[] msgEnc = Arrays.copyOfRange(receivedBytes, 16, receivePacketHighest.getLength()); //Msg igual
                
                    //Decriptar mensagem
                    msgEnc = SecurityRepository.decryptMsgSym(msgEnc, repoKey, IV);
                    String Highest = new String(msgEnc);

                    JSONObject HighestJSON = new JSONObject(Highest);
                    int highestBid = HighestJSON.getInt("Highest");
                    String auctionType = HighestJSON.getString("AuctionType");
                    
                    JSONArray datasigned = msg.getJSONArray("Sign");
                    byte[] signed = gson.fromJson(datasigned.toString(), byte[].class); //Hash

                    String ret = validateBids(amount,signed,auctionType,highestBid);
                    retJSON = new JSONObject(ret); 
                    messageRepository(serverSocket,retJSON);
                    break;
                    
            case "clientID":
                    newClientID();
                    retJSON = new JSONObject("{ \"Type\":\"clientID\",\"clientID\":"+clientID+"}");
                    messageRepository(serverSocket,msg);
                    messageClient(ClientIP, ClientPort,serverSocket, retJSON);
                    break;
            case "cta":
                    //Mandar mensagem ao AuctionRepository para ele fazer o pretendido e devolver valores
                    messageRepository(serverSocket, msg);
                    break;
                
            case "rct":
                    //Mandar mensagem ao Cliente a informar que o pretendido foi feito;
                    retMsg = "Operation completed with success!";
                    retJSON = new JSONObject("{ \"Type\":\"ret\",\"Message\":"+retMsg+"}");
                    messageClient(ClientIP, ClientPort,serverSocket, retJSON);
                    break;
            case "SUCCESS":
                    //Mandar mensagem ao Cliente a informar que o pretendido foi feito;
                    retJSON = new JSONObject("{ \"Type\":\"SUCCESS\"}");
                    messageClient(ClientIP, ClientPort,serverSocket, retJSON);
                    break;
            default:
                    retMsg = "ERROR - Invalid message";
                    retJSON = new JSONObject("{ \"Type\":\"ret\",\"Message\":"+retMsg+"}");
                    messageClient(ClientIP, ClientPort,serverSocket, retJSON);
                    break;  
        }
    }
    
    /**
     * Função que manda mensagem para o repositório.
     * 
     * @param serverSocket Socket do Auction Manager
     * @param msg Mensagem a enviar ao repositório
     * @throws UnknownHostException
     * @throws IOException 
     */
    private static void messageRepository(DatagramSocket serverSocket, JSONObject msg) throws UnknownHostException, IOException, GeneralSecurityException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9876;
        byte[] sendbuffer;
        
        if(agreement){
            //Gerar IV
            Gson gson = new Gson();
            byte[] initializationVector = new byte[16];
            SecureRandom secRan = new SecureRandom(); 
            secRan.nextBytes(initializationVector);

            //Encriptrar
            sendbuffer = msg.toString().getBytes();
            sendbuffer = SecurityRepository.encryptMsgSym(sendbuffer, repoKey,initializationVector);

            byte [] sendbufferIV = new byte[sendbuffer.length+initializationVector.length];
            System.arraycopy(initializationVector, 0, sendbufferIV, 0, initializationVector.length);
            System.arraycopy(sendbuffer, 0, sendbufferIV, initializationVector.length, sendbuffer.length);

            DatagramPacket sendPacket = new DatagramPacket(sendbufferIV, sendbufferIV.length,ServerIP ,ServerPort);
            serverSocket.send(sendPacket);
        }else{
            sendbuffer = msg.toString().getBytes();        
            DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
            serverSocket.send(sendPacket);
        }
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
     * Envia o certificado do repositorio para o Servidor Manager
     * 
     * @param clientSocket Socket do Manager
     * @param cert Certificado do Manager
     * @throws java.net.UnknownHostException
     * @throws IOException
     * @throws java.security.cert.CertificateEncodingException
     */
    private static void messageRepositoryCert(DatagramSocket clientSocket, X509Certificate cert) throws UnknownHostException, IOException, CertificateEncodingException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9876;
        byte[] sendbuffer;
        sendbuffer = cert.getEncoded();
        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, ServerIP ,ServerPort);
        clientSocket.send(sendPacket);
    }
    
    /**
     * Valida bids, neste momento apenas verifica se no caso de um leilão ingles a bid é maior que a anterior
     * 
     * @param amount Valor da bid
     * @param signed bid assinada
     * @return String onde é especificada a bid validada e a sua encriptação ou a não validação da bid
     */
    private static String validateBids(double amount, byte[] signed, String auctionType, double lastBid) throws IOException, GeneralSecurityException{
        //Fazer validações
        boolean validBid = true;
        if(auctionType.equals("English")){
            if(amount<=lastBid) validBid=false;
        }
        Gson gson = new Gson();
        
        String ret = "";
        if(validBid){
            byte[] signedEncrypted = SecurityManager.encryptMsg(signed, kp.getPrivate());
            String encrypted = ""+gson.toJson(signedEncrypted);
            ret = "{ \"Type\":\"ret\",\"Message\":Valid,\"Encrypted\":"+encrypted+"}";
        }
        else ret = "{ \"Type\":\"ret\",\"Message\":Invalid}";
        return ret;
    }
}

