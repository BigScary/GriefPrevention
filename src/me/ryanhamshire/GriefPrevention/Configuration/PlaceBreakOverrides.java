package me.ryanhamshire.GriefPrevention.Configuration;


import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.MaterialCollection;
import me.ryanhamshire.GriefPrevention.MaterialInfo;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * GriefPrevention:
 *   Rules:
 *     Overrides:
 *       SiegeTNT:
 *         Materials=[TNT,LEVER]
 *         DataMask="*"
 *         Claims:
 *           etc...
 *         Wilderness:
 *           etc...
 *
 */
public class PlaceBreakOverrides {

    private class OverrideData
    {

        private MaterialCollection Materials = new MaterialCollection();
        private String DataMask = "*";
        private ClaimBehaviourData Rule;
        private String Name;
        public String getName(){ return Name;}
        public String getDataMask() { return DataMask;}
        public ClaimBehaviourData getRule(){ return Rule;}
        public MaterialCollection getMaterials(){ return Materials;}
        public OverrideData(String pName,MaterialCollection pMaterials,String pDataMask,ClaimBehaviourData pRule){
            Name = pName;
            Materials=pMaterials;
            DataMask = pDataMask;
            Rule = pRule;
        }
        public OverrideData(String pName,FileConfiguration Source,FileConfiguration Target,String useNode){
            List<String> readStrings = Source.getStringList(useNode + ".Materials");
            GriefPrevention.instance.parseMaterialListFromConfig(readStrings,Materials);
            Name = pName;
            DataMask = Source.getString(useNode + ".DataMask");
            readStrings = Materials.GetList();
            Target.set(useNode + ".Materials",readStrings);
            Target.set(useNode + ".DataMask",DataMask);
            Target.set(useNode + ".Name",Name);
            Rule = new ClaimBehaviourData("OverrideRule",Source,Target,useNode,ClaimBehaviourData.getAll("OverrideRule"));

        }
        public boolean testBlock(Block blockTest){

            if(!Materials.hasMaterial(blockTest.getType())) return false; //not the right kind of material.

            //otherwise we need to test the Type.
            else if(DataMask.equals("*") || DataMask.length()==0) return true;


            else if(DataMask.startsWith("!")){

                String testval = DataMask.substring(1);
                try {
                    return Byte.parseByte(testval)!=blockTest.getData();
                }                                                       catch(NumberFormatException nfe){}


            }else {
                try {
                    return Byte.parseByte(DataMask)==blockTest.getData();
                }                                                         catch(NumberFormatException nfe){}

            }


            return false;

        }
    }


    HashMap<Integer,List<OverrideData>> BreakOverrides = new HashMap<Integer, List<OverrideData>>();
    //index each Material to the ClaimBehaviourData that is the first Override to list that Material.
    //the list can have multiple entries because it lists all the Rules that apply to a given material, without taking their additional
    //criteria into account. The first item in the List to return true on "TestBlock()" will have it's rule used.

    /**
     * Fluent method for Adding Overrides; adds a single block override.
     * @param ID
     * @param data
     * @return
     */
    public PlaceBreakOverrides AddOverride(String pName,int ID,ClaimBehaviourData data){

        MaterialCollection mc = new MaterialCollection();
        mc.add(Material.getMaterial(ID));
        OverrideData addoverride = new OverrideData(pName,mc,"*",data);
        addDirect(ID,addoverride);
        return this;


    }
    public static PlaceBreakOverrides Default = new PlaceBreakOverrides().AddOverride("TNT",Material.TNT.getId(),
            new ClaimBehaviourData("TNT Override",PlacementRules.Neither,PlacementRules.Both, ClaimBehaviourData.ClaimBehaviourMode.RequireBuild)
            .setSiegeOverrides(ClaimBehaviourData.SiegePVPOverrideConstants.Allow, ClaimBehaviourData.SiegePVPOverrideConstants.Deny));


    private void addDirect(int ID,OverrideData additem){

        if(!BreakOverrides.containsKey(ID))
            BreakOverrides.put(ID,new ArrayList<OverrideData>());

        BreakOverrides.get(ID).add(additem);


    }
    public ClaimBehaviourData getBehaviourforBlock(Block blockfor){

        Integer blockID = blockfor.getTypeId();
        if(!BreakOverrides.containsKey(blockID)) return null; //not in the HashMap, so nothing to return. we will return null.

        List<OverrideData> datalist = BreakOverrides.get(blockID);
        for(OverrideData od:datalist){

            if(od.testBlock(blockfor)){
                return od.getRule();
            }

        }
         return null;

    }
    private PlaceBreakOverrides(){

    }
    private void Copy(PlaceBreakOverrides Source){
        this.BreakOverrides = Source.BreakOverrides;

    }
    public PlaceBreakOverrides(FileConfiguration Source,FileConfiguration Target,String NodeSource,PlaceBreakOverrides Defaults){

        //Default would be something like "GriefPrevention.Claims.Rules.Overrides

        ConfigurationSection getSection;
        getSection = Source.getConfigurationSection(NodeSource);
        Set<String> OverrideNames = new HashSet<String>();
        if(getSection!=null)
            OverrideNames = getSection.getKeys(false);
        if(OverrideNames.size()==0){
            //init to defaults...
            Copy(Defaults);
            return;
        }
        //create a new OverrideData from "GriefPrevention.Claims.Rules.<OverrideName>" for each entry.
        for(String makeoverride:OverrideNames){
            OverrideData od = new OverrideData(makeoverride,Source,Target,NodeSource = "." + makeoverride);
            for(MaterialInfo iterate:od.getMaterials().getMaterials()){
                Integer grabmat = iterate.getTypeID();
                //does this material exist as a key?
                if(!BreakOverrides.containsKey(grabmat)){
                    BreakOverrides.put(grabmat,new ArrayList<OverrideData>());
                }


                BreakOverrides.get(grabmat).add(od);

            }


        }

    }



}
