package Server;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.json.JSONObject;

/**
 *
 * Auction Manager Class
 * <br>
 * Servidor "rendevouz" que age como intermediário entre os clientes e o servidor repositório
 * 
 */
public class AuctionManager {

    private static int clientID=0;

    public static void newClientID() {
         clientID++;
    }
   
    public static void main(String[] args) throws SocketException, IOException {
        
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
          if((!recvdpkt.getAddress().toString().equals("127.0.0.1")) && (recvdpkt.getPort()!=9876 )){
            ClientIP = recvdpkt.getAddress();
            ClientPort = recvdpkt.getPort();
            client=true;
          }
          
          JSONObject recMsg = new JSONObject(ReceivedMsg);
          System.out.println("\nClient : " + recMsg.toString());
          
          //Ver tipo de mensagem
          ComputeMessageType(ClientIP,ClientPort,serverSocket, recMsg);
          
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
    public static void ComputeMessageType(InetAddress ClientIP, int ClientPort, DatagramSocket serverSocket, JSONObject msg) throws IOException{
        String type = msg.getString(("Type"));
        String retMsg="";
        JSONObject retJSON;

        switch(type){
            case "clientID":
                    newClientID();
                    retJSON = new JSONObject("{ \"Type\":\"clientID\",\"clientID\":"+clientID+"}");
                    messageClient(ClientIP, ClientPort,serverSocket, retJSON);
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
  
}

