/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Message;

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
}
