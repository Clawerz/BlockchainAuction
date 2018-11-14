/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Message;

import Auction.Auction;
import java.lang.String;

/**
 *
 * @author Filipe
 */
public class Message {
    private String msg;
    
    public Message(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public String getDestination(String msg){
        
       // Position 0 -> ip
       // Position 1 -> port
       String [] destination = new String[1];
       return "";
    }
    
    public Auction createAuction(String input){
        String[] div = input.split(" ");
        
        //auctionName,creatorID,auctionID,timeToFinish
        Auction a = new Auction(div[0],Integer.parseInt(div[1]),Integer.parseInt(div[2]),Integer.parseInt(div[3]));
        return a;
    }
}
