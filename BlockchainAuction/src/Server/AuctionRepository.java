package Server;
import Auction.Auction;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Auction Repository Class
 * <br>
 * Tem e gera a informação sobre todos os leilões
 */
public class AuctionRepository {

   
    public static void main(String[] args) throws SocketException, IOException {
        
      //All auctions
      ArrayList<Auction> AuctionList= new ArrayList<>();
      
      DatagramSocket serverSocket = new DatagramSocket(9876);
         
      while(true){
            
          //Receber informação por udp
          byte[] receivebuffer = new byte[1024];
          byte[] sendbuffer  = new byte[1024];
          DatagramPacket recvdpkt = new DatagramPacket(receivebuffer, receivebuffer.length);
          serverSocket.receive(recvdpkt);
          InetAddress ManIP = recvdpkt.getAddress();
          int ManPort = recvdpkt.getPort();
          
          String ReceivedMsg = new String(recvdpkt.getData());
          System.out.println("\nManager : "+ ReceivedMsg);
          
          ComputeMessageType(serverSocket,AuctionList,ReceivedMsg);
           
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
     * @param serverSocket Socket do Auction Repository
     * @throws IOException 
     */
    
    public static void ComputeMessageType(DatagramSocket serverSocket,ArrayList<Auction> AuctionList, String msg) throws IOException{
        String type = msg.substring(0,3);
        String []param;
        String retMsg="";
        
        switch(type){
            case "cta": 
                //Criar leilão
                param = msg.split(("-"));
                
                //Substituir todos os caracteres que não sejam números por nada, fazer isto por causa de ter convertido de byte para string então o caracter '\0' vai fazer parte da string.
                param[2]=param[2].replaceAll("[^0-9]+", "");
                param[3]=param[3].replaceAll("[^0-9]+", "");
                param[4]=param[4].replaceAll("[^0-9]+", "");;
                
                //String auctionName, int creatorID, int auctionID, int timeToFinish
                Auction a = new Auction(param[1],Integer.parseInt(param[2]),Integer.parseInt(param[3]),Integer.parseInt(param[4]));
                AuctionList.add(a);
                
                //Devolver mensagem
                messageManager(serverSocket,"rct");
                
            case "tta" : 
                //Terminar leilão
                param = msg.split(("-"));
                
                //Substituir todos os caracteres que não sejam números por nada, fazer isto por causa de ter convertido de byte para string então o caracter '\0' vai fazer parte da string.
                param[2]=param[2].replaceAll("[^0-9]+", "");
                param[3]=param[3].replaceAll("[^0-9]+", "");
                param[4]=param[4].replaceAll("[^0-9]+", "");
                   
                //Percorrer todos os leilões procurar pelo ID que foi fornecido na mensagem, quando encontrar mudar o estado desse leilão
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).getAuctionID()==Integer.parseInt(param[3]))  AuctionList.get(i).setAuctionFinished(true);
                 }
              
                //Devolver mensagem
                messageManager(serverSocket,"rgc");
                
            case "gba" : 
                //Listar todos os bids de um client
                param = msg.split(("-"));
                
                //Substituir todos os caracteres que não sejam números por nada, fazer isto por causa de ter convertido de byte para string então o caracter '\0' vai fazer parte da string.
                param[2]=param[2].replaceAll("[^0-9]+", "");
                
                retMsg = "rlg";
                   
                //Percorrer todos os leilões, os que tiverem ativos são adicionados á string
                for(int i=0; i<AuctionList.size();i++){
                    for(int j=0;i<AuctionList.get(i).getBids().size();j++){
                        if(AuctionList.get(i).getAuctionID()==Integer.parseInt(param[2])) retMsg+="-"+AuctionList.get(i).getBids().toString();
                    }
                 }
              
                //Devolver mensagem
                messageManager(serverSocket,retMsg);
                
            case "gbc" : 
                //Listar todos os bids de um client
                param = msg.split(("-"));
                
                //Substituir todos os caracteres que não sejam números por nada, fazer isto por causa de ter convertido de byte para string então o caracter '\0' vai fazer parte da string.
                param[2]=param[2].replaceAll("[^0-9]+", "");
                
                retMsg = "rlg";
                   
                //Percorrer todos os leilões, os que tiverem ativos são adicionados á string
                for(int i=0; i<AuctionList.size();i++){
                    for(int j=0;i<AuctionList.get(i).getBids().size();j++){
                        if(AuctionList.get(i).getBids().get(j).getClientID()==Integer.parseInt(param[2])) retMsg+="-"+AuctionList.get(i).getBids().get(j).toString();
                    }
                 }
              
                //Devolver mensagem
                messageManager(serverSocket,retMsg);
                
            case  "lga": 
                //Listar todos os leilões ativos
                param = msg.split(("-"));
                
                retMsg = "rlg";
                   
                //Percorrer todos os leilões, os que tiverem ativos são adicionados á string
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).isAuctionFinished()==false) retMsg+="-"+AuctionList.get(i).getAuctionName();
                 }
              
                //Devolver mensagem
                messageManager(serverSocket,retMsg);
                
            case  "lta": 
                //Listar todos os leilões inativos
                param = msg.split(("-"));
                
                retMsg = "rlt";
                   
                //Percorrer todos os leilões, os que tiverem inativos são adicionados á string
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).isAuctionFinished()==true) retMsg+="-"+AuctionList.get(i).getAuctionName();
                 }
              
                //Devolver mensagem
                messageManager(serverSocket,retMsg);
                
            case  "coa":
                //Ver resultado de um leilão
                param = msg.split(("-"));
                
                //Substituir todos os caracteres que não sejam números por nada, fazer isto por causa de ter convertido de byte para string então o caracter '\0' vai fazer parte da string.
                param[2]=param[2].replaceAll("[^0-9]+", "");
                   
                retMsg="rco";
                
                //Percorrer todos os leilões procurar pelo ID que foi fornecido na mensagem, quando encontrar obeter toda a informação sobre o leilão
                for(int i=0; i<AuctionList.size();i++){
                    if(AuctionList.get(i).getAuctionID()==Integer.parseInt(param[3]))  retMsg+="-"+AuctionList.get(i).getBuyerID()+"-"+AuctionList.get(i).getWinnerBid();
                 }
              
                //Devolver mensagem
                messageManager(serverSocket,"rtt");
                
            case  "vlr":
                    //Não faz nada por agora, pois ainda não sabemos como validar recibos
                    break;
                
            case "end":
                    serverSocket.close();
                    System.exit(1);
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
    public static void messageManager(DatagramSocket serverSocket, String msg) throws UnknownHostException, IOException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9877;
        byte[] sendbuffer  = new byte[1024];

        sendbuffer = msg.getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
        serverSocket.send(sendPacket);
    }
}

