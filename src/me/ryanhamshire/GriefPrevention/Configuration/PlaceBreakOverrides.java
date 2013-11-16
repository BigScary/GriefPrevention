package me.ryanhamshire.GriefPrevention.Configuration;


import me.ryanhamshire.GriefPrevention.Debugger;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.MaterialCollection;
import me.ryanhamshire.GriefPrevention.MaterialInfo;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

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
        private ClaimBehaviourData Rule;
        private String Name;
        public String getName(){ return Name;}
        public ClaimBehaviourData getRule(){ return Rule;}
        public MaterialCollection getMaterials(){ return Materials;}
        public OverrideData(String pName,MaterialCollection pMaterials,ClaimBehaviourData pRule){
            Name = pName;
            Materials=pMaterials;

            Rule = pRule;

        }
        public OverrideData(String pName,FileConfiguration Source,FileConfiguration Target,String useNode,OverrideData defaults){
            Debugger.Write("OverrideData Constructor, Name=" + pName + " Node=" + useNode,Debugger.DebugLevel.Verbose);
            Materials = new MaterialCollection();
            List<String> readStrings = Source.getStringList(useNode + ".Materials");
            if(defaults!=null) Debugger.Write("OverrideData constructor: default has " + String.valueOf(defaults.getMaterials().size()),Debugger.DebugLevel.Verbose);
            Debugger.Write("Override Data read in " + String.valueOf(readStrings.size()) + " Materials." , Debugger.DebugLevel.Verbose);
            GriefPrevention.instance.parseMaterialListFromConfig(readStrings, Materials);
            if(readStrings.size()==0) Materials = defaults==null?new MaterialCollection():defaults.getMaterials();
            Debugger.Write("Materials Count:" + String.valueOf(Materials.size()),Debugger.DebugLevel.Verbose);
            Name = pName;

            readStrings = Materials.GetList();

            Debugger.Write("Persisting Override Data values to configuration.",Debugger.DebugLevel.Verbose);
            Target.set(useNode + ".Materials", readStrings);

            Target.set(useNode + ".Name",Name);
            Rule = new ClaimBehaviourData("OverrideRule",Source,Target,useNode,defaults==null?null:defaults.getRule());

        }
        public boolean testBlock(Block blockTest){

            if(!Materials.hasMaterial(blockTest.getType())) return false; //not the right kind of material.

            return true;

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
        OverrideData addoverride = new OverrideData(pName,mc,data);
        addDirect(ID,addoverride);
        return this;


    }
    public static PlaceBreakOverrides Default = new PlaceBreakOverrides().AddOverride("TNT",Material.TNT.getId(),
            new ClaimBehaviourData("TNT Override",PlacementRules.Neither,PlacementRules.Both, ClaimBehaviourData.ClaimBehaviourMode.RequireBuild)
            .setSiegeOverrides(ClaimBehaviourData.SiegePVPOverrideConstants.Allow, ClaimBehaviourData.SiegePVPOverrideConstants.Deny));


    private void addDirect(int ID,OverrideData additem){
        Debugger.Write("Adding direct:" + String.valueOf(ID) + " od:" + additem.getName(),Debugger.DebugLevel.Verbose);
        if(!BreakOverrides.containsKey(ID))
            BreakOverrides.put(ID,new ArrayList<OverrideData>());

        BreakOverrides.get(ID).add(additem);


    }
    public ClaimBehaviourData getBehaviourforBlock(Block blockfor){

        Integer blockID = blockfor.getTypeId();
        Debugger.Write("getBehaviourforBlock called on id:" + String.valueOf(blockID) + " material Name:" + blockfor.getType().name(), Debugger.DebugLevel.Verbose);
        if(!BreakOverrides.containsKey(blockID)) {
            Debugger.Write("BreakOverrides does not contain ID key...", Debugger.DebugLevel.Verbose) ;
            return null; //not in the HashMap, so nothing to return. we will return null.
        }

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
        Debugger.Write("Copying existing PlaceBreakOverrides",Debugger.DebugLevel.Verbose);
        this.BreakOverrides = Source.BreakOverrides;

    }
    public OverrideData getOverrideByName(String pName){
        Debugger.Write("Looking for Override by name of " + pName,Debugger.DebugLevel.Verbose);
        Debugger.Write("getOverrideByName: BreakOverrides has " + String.valueOf(this.BreakOverrides.size()) + " values.",Debugger.DebugLevel.Verbose);
        for(List<OverrideData> iterateList:this.BreakOverrides.values()){
            Debugger.Write("List in BreakOverrides has " + String.valueOf(iterateList.size()) + " values.",Debugger.DebugLevel.Verbose);
             for(OverrideData od:iterateList){
                 Debugger.Write("overrideData named:" + od.getName(),Debugger.DebugLevel.Verbose);
                if(od.getName().trim().equalsIgnoreCase(pName)) {
                    Debugger.Write("Found "  + od.getName(),Debugger.DebugLevel.Verbose);
                    return od;

                }
            }

        }
        return null;



    }




    public Set<String> getOverrideNames(){
        Set<String> Buildresult = new HashSet<String>();
        for(List<OverrideData> iterate:this.BreakOverrides.values()){
            for(OverrideData element:iterate){
                if(!Buildresult.contains(element.getName()))
                Buildresult.add(element.getName());
            }
        }
        return Buildresult;
    }
    public PlaceBreakOverrides(FileConfiguration Source,FileConfiguration Target,String NodeSource,PlaceBreakOverrides Defaults){

        //Default would be something like "GriefPrevention.Claims.Rules.Overrides
        Debugger.Write("PlaceBreakOverrides Constructor", Debugger.DebugLevel.Verbose);
        ConfigurationSection getSection;
        getSection = Source.getConfigurationSection(NodeSource);
        Set<String> OverrideNames = new HashSet<String>();
        if(getSection!=null)
            OverrideNames = getSection.getKeys(false);
        if(OverrideNames.size()==0){
            //init to defaults...
            Debugger.Write("No entries in " + NodeSource, Debugger.DebugLevel.Verbose);
            Copy(Defaults);

            Debugger.Write("using defaults...",Debugger.DebugLevel.Verbose);
            OverrideNames = getOverrideNames();

        }
        //create a new OverrideData from "GriefPrevention.Claims.Rules.<OverrideName>" for each entry.

        for(String makeoverride:OverrideNames){
            OverrideData usedefault = null;

            OverrideData od;
            if(Defaults!=null) usedefault = Defaults.getOverrideByName(makeoverride);
            if(!Source.isConfigurationSection(NodeSource + "." + makeoverride)){
                Debugger.Write("using Default for " + makeoverride + " as no Config node exists.", Debugger.DebugLevel.Verbose);

            }
            od = new OverrideData(makeoverride,Source,Target,NodeSource + "." + makeoverride,usedefault);

            for(MaterialInfo iterate:od.getMaterials().getMaterials()){
                Integer grabmat = iterate.getTypeID();
                Debugger.Write("Adding Material:" + iterate.getTypeID(),Debugger.DebugLevel.Verbose);
                //does this material exist as a key?
                if(!BreakOverrides.containsKey(grabmat)){
                    BreakOverrides.put(grabmat,new ArrayList<OverrideData>());
                }


                BreakOverrides.get(grabmat).add(od);

            }


        }

    }



}
