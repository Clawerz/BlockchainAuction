package Server;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
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
   
    //Client ID
    private static int clientID=0;
    public static void newClientID() {
         clientID++;
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
          byte[] sendbuffer  = new byte[1024];
          DatagramPacket recvdpkt = new DatagramPacket(receivebuffer, receivebuffer.length);
          serverSocket.receive(recvdpkt); 
          String ReceivedMsg = "";
          ReceivedMsg = new String(recvdpkt.getData());
          /*if(!clientSecret.isEmpty()){
            //Obter IV
            byte[] receivedBytes = recvdpkt.getData();
            byte[] IV = Arrays.copyOfRange(receivedBytes, 0, 16);
            byte[] msg = Arrays.copyOfRange(receivedBytes, 16, receivedBytes.length);

            //Decriptar mensagem
            msg = SecurityManager.decryptMsgSym(msg, clientSecret.get(clientID), IV);
            ReceivedMsg = new String(msg);
            System.out.println(ReceivedMsg+"FDS QUE GAJO INTELIGENTE");
            System.exit(1);
          
           }else{
             ReceivedMsg = new String(recvdpkt.getData());
           }*/
          
          //Obter endereço do client
          if((!recvdpkt.getAddress().toString().equals("127.0.0.1")) && (recvdpkt.getPort()!=9876 )){
            ClientIP = recvdpkt.getAddress();
            ClientPort = recvdpkt.getPort();
            client=true;
          }
          
          JSONObject recMsg = new JSONObject(ReceivedMsg);
          System.out.println("\nClient : " + recMsg.toString());
          
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
        String retMsg="";
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
                    //System.out.println(SymReceivedMsg);
                    JSONObject symMsg = new JSONObject(SymReceivedMsg);
                    JSONArray data = symMsg.getJSONArray("Sym");
                    JSONArray data2 = symMsg.getJSONArray("Data");
                    Gson gson = new Gson();
                    
                    //Decifrar mensagem
                    byte[] dataKey = gson.fromJson(data.toString(), byte[].class);
                    byte[] dataHash = gson.fromJson(data2.toString(),byte[].class);
                    byte[] symKey = SecurityManager.decryptMsg(clientKeys.get(0),dataKey);
                    
                    //Verificar assinatura
                    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                    byte[] digest = messageDigest.digest(dataHash);
                    
                    //Obter chave simétrica
                    SecretKey symetricKey = new SecretKeySpec(symKey, 0, symKey.length, "AES");
                    //System.out.println(Arrays.toString(symetricKey.toString().getBytes()));
                    
                    if(Arrays.equals(digest, symKey)){
                        clientSecret.add(symetricKey);
                        //System.out.println("Assinatura validada !");
                    }else{
                        System.out.println("Assinatura inválida !");
                        //System.out.println(Arrays.toString(digest));
                        //System.out.println(Arrays.toString(symKey));
                    }
                    
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
    private static void messageRepository(DatagramSocket serverSocket, JSONObject msg) throws UnknownHostException, IOException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9876;
        byte[] sendbuffer;
        
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
        byte[] sendbuffer;
        sendbuffer = msg.toString().getBytes();        
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

