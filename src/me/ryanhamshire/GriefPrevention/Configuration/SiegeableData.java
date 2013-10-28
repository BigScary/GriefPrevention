package me.ryanhamshire.GriefPrevention.Configuration;


import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class SiegeableData {
    private String MaterialName;
    public String getMaterialName(){ return MaterialName;}
    private int MatchID;
    private float RequiredBlastPower=1;
    public int getMatchID(){return MatchID;}
    public float getRequiredBlastPower(){return RequiredBlastPower;}
    public SiegeableData(String pMaterialName){
        MaterialName = pMaterialName;
        //Name,ID,RequiredPower
        String[] splitresult= MaterialName.split(",");
        MaterialName = splitresult[0];
        if(splitresult.length > 1)
        {
            if(splitresult[1].length() >0)
                MatchID = Integer.parseInt(splitresult[1]);

            if(splitresult.length > 2){
                RequiredBlastPower = Float.parseFloat(splitresult[2]);
            }
        }


    }
    public boolean doesMatch(Block testblock){
        return doesMatch(testblock.getType());
    }
    public boolean doesMatch(Material testmaterial){

        return (MaterialName.length() > 0 && (MaterialName.equalsIgnoreCase("*") ||
                MaterialName.equalsIgnoreCase(testmaterial.name())))
                || MatchID > 0 && MatchID==testmaterial.getId();
    }

    public static List<SiegeableData> readList(FileConfiguration Source,FileConfiguration Target,String Node,List<SiegeableData> Default){

        List<String> retrievelist = Source.getStringList(Node);
        List<SiegeableData> result = retrievelist==null?Default:new ArrayList<SiegeableData>();
        for(String iterate: retrievelist){
            result.add(new SiegeableData(iterate));

        }

        Target.set(Node,retrievelist);

        return result;


    }
    public static float getListPower(List<SiegeableData> list,Material testmat){
        for(SiegeableData loopdata:list){
            if(loopdata.doesMatch(testmat))
                loopdata.getRequiredBlastPower();




        }
        return -1;
    }
    public static boolean CheckList(List<SiegeableData> list,Material testmat){
        for(SiegeableData loopdata:list){
            if(loopdata.doesMatch(testmat))
                return true;




        }
        return false;
    }


}
