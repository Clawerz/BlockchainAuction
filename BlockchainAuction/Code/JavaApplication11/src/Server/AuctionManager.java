package Server;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public static ArrayList<PublicKey> clientKeys = new ArrayList<>();
   
    public static void main(String[] args) throws SocketException, IOException, NoSuchAlgorithmException, UnknownHostException, CertificateEncodingException, CertificateException, KeyStoreException {
     
     //Cria par de chaves 
     KeyPair kp = SecurityManager.generateKey();
     //Criar certificado
     X509Certificate cert = SecurityManager.generateCert(kp,"CN=Auction_Manager, L=AV, C=PT", 100, "SHA1withRSA");
     SecurityManager.printCertificateSpecs(cert);
     
     
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
          String ReceivedMsg = new String(recvdpkt.getData());
          
          //Obter endereço do client
          if(!client){
            ClientIP = recvdpkt.getAddress();
            ClientPort = recvdpkt.getPort();
            client=true;
          }
          
          JSONObject recMsg = new JSONObject(ReceivedMsg);
          System.out.println("\nClient : " + recMsg.toString());
         
          //Ver tipo de mensagem
          ComputeMessageType(ClientIP,ClientPort,serverSocket, recMsg, cert);
          
          /*DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
          serverSocket.receive(receivePacket);
          String RepositoryMsg = new String(receivePacket.getData());
          System.out.print("\nRepository: " + RepositoryMsg);
          
          //Ver tipo de mensagem
          ComputeMessageType(ClientIP,ClientPort,serverSocket,RepositoryMsg);*/
      }
    }
    
    /**
     * Dependendo do tipo de mensagem recebida executa o pedido pela mesma.
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
     * @param ClientIP IP do cliente
     * @param ClientPort Número da porta do cliente
     * @param serverSocket Socket do Auction Manager
     * @param msg Mensagem a ser interpretada
     * @throws IOException 
     */
    public static void ComputeMessageType(InetAddress ClientIP, int ClientPort, DatagramSocket serverSocket, JSONObject msg, X509Certificate cert) throws IOException, UnknownHostException, CertificateEncodingException, CertificateException, KeyStoreException{
        String type = msg.getString(("Type"));
        String retMsg="";
        JSONObject retJSON;

        switch(type){
            case "init":
                    //Mandar certificado 
                    //Gson gson = new Gson();
                    //retJSON = new JSONObject("{ \"Type\":\"initCert\",\"Certificate\":"+gson.toJson(cert)+"}");
                    messageClientCert(ClientIP, ClientPort,serverSocket,cert);
                    
                    byte[] receivebuffer = new byte[32768];
                    DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                    serverSocket.receive(receivePacket);

                    byte[] certificateBytes = receivePacket.getData();
                    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    X509Certificate certificate = (X509Certificate)(certificateFactory.generateCertificate( new ByteArrayInputStream(certificateBytes)));
                    System.out.println(certificate.toString());
                    
                    try{
                        certificate.checkValidity();
                        clientKeys.add(certificate.getPublicKey());
                        retMsg = "Valid certificate";
                        retJSON = new JSONObject("{ \"Type\":\"cert\",\"Message\":"+retMsg+"}");
                        messageClient(ClientIP, ClientPort,serverSocket, retJSON);
                        System.out.println("Certificate valid");
                    }catch(IOException | CertificateExpiredException | CertificateNotYetValidException | JSONException e){
                        retMsg = "Invalid certificate";
                        retJSON = new JSONObject("{ \"Type\":\"cert\",\"Message\":"+retMsg+"}");
                        messageClient(ClientIP, ClientPort,serverSocket, retJSON);
                        System.out.println("Certificate invalid");
                    }
                    
                    break;
        
            case "cta":
                    //Mandar mensagem ao AuctionRepository para ele fazer o pretendido e devolver valores
                    messageRepository(serverSocket, msg);
                    break;
                
            case "rct":
                    //Mandar mensagem ao Cliente a informar que o pretendido foi feito;
                    retMsg = "Operation completed with sucess!";
                    retJSON = new JSONObject("{ \"Type\":\"ret\",\"Message\":"+retMsg+"}");
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
    public static void messageRepository(DatagramSocket serverSocket, JSONObject msg) throws UnknownHostException, IOException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9876;
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
    public static void messageClient(InetAddress ClientIP, int ClientPort,DatagramSocket serverSocket, JSONObject msg) throws UnknownHostException, IOException{
        byte[] sendbuffer  = new byte[1024];
        sendbuffer = msg.toString().getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, ClientIP ,ClientPort);
        serverSocket.send(sendPacket);
    }
    
    public static void messageClientCert(InetAddress ClientIP, int ClientPort,DatagramSocket serverSocket, X509Certificate cert) throws UnknownHostException, IOException, CertificateEncodingException{
        byte[] sendbuffer  = new byte[32768];
        sendbuffer = cert.getEncoded();
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, ClientIP ,ClientPort);
        serverSocket.send(sendPacket);
    }

    
}

