package me.ryanhamshire.GriefPrevention.Configuration;


import me.ryanhamshire.GriefPrevention.Debugger;
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
    @Override
    public String toString(){
        return MaterialName + "," + String.valueOf(MatchID) + "," + String.valueOf(RequiredBlastPower);
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
        Debugger.Write("Reading SiegableData List From Node:" + Node, Debugger.DebugLevel.Verbose);
        List<?> testcontents = Source.getList(Node);

        List<String> retrievelist = Source.getStringList(Node);
        //getStringList() will return an empty String ArrayList, but will not return null
        //if there are in fact no elements.

        if(testcontents==null){
            //fill the string list with the contents of the passed default values.
          retrievelist = new ArrayList<String>();
            for(SiegeableData sd:Default){
                retrievelist.add(sd.toString()); //add the string representation.
            }
        }
        //we set the defaults to the string list, and then parse it after, rather than just setting the default and
        //breaking out because we want ot save those defaults to the configuration node in question.
        List<SiegeableData> result = new ArrayList<SiegeableData>();
        for(String iterate: retrievelist){
            result.add(new SiegeableData(iterate));

        }

        Target.set(Node,retrievelist);

        return result;


    }
    public static float getListPower(List<SiegeableData> list,Material testmat){
        for(SiegeableData loopdata:list){
            if(loopdata.doesMatch(testmat))
            {
                Debugger.Write("Material Match Found:" + testmat.name(), Debugger.DebugLevel.Verbose);
                return loopdata.getRequiredBlastPower();

            }


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
