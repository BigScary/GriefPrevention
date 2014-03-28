package me.ryanhamshire.GriefPrevention;


import java.util.Date;

public class MessageInfo {
    private String MessageText="";
    private Date MessageTimeStamp = new Date();

    public String getText(){ return MessageText;}
    public Date getTimeStamp(){ return MessageTimeStamp;}

    public MessageInfo(String pText){
        MessageText = pText;
    }


}
