/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.GriefPrevention;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;

//ordered list of material info objects, for fast searching
public class MaterialCollection
{
	HashMap<Integer,List<MaterialInfo>> materials = new HashMap<Integer,List<MaterialInfo>>();
	
	public List<MaterialInfo> getMaterials(){
		
		List<MaterialInfo> Result = new ArrayList<MaterialInfo>();
		for(List<MaterialInfo> listiterate:materials.values()){
			Result.addAll(listiterate);
		}
		return Result;
		
		
	}
	
	
	public void add(Material m){
		add(new MaterialInfo(m));
	}
	public void remove(MaterialInfo mi) {
		if(!materials.containsKey(mi.getTypeID())){
			return; //nothing to remove, since the id isn't even here.
		}
		List<MaterialInfo> mlist = this.materials.get(mi.typeID);
		for(int i=0;i<mlist.size();i++){
			if(mlist.get(i).equals(mi)){
				mlist.remove(i);
			}
		}
		
	}
	public void add(MaterialInfo material)
	{
		if(!materials.containsKey(material.getTypeID())){
			materials.put(material.getTypeID(),new ArrayList<MaterialInfo>());
		}
		List<MaterialInfo> mlist = this.materials.get(material.typeID);
		mlist.add(material);
		
	}
	public boolean contains(Material m){
		return contains(new MaterialInfo(m));
	}
	public boolean contains(MaterialInfo material)
	{
		if(this.materials.containsKey(material.typeID))
		{
			return 	this.materials.get(material.typeID).contains(material);
		}
		
		
		return false;
	}
	public MaterialCollection(){
		
	}
	public List<String> GetList(){
		ArrayList<String> buildresult = new ArrayList<String>();
		for(MaterialInfo mi:getMaterials()){
			buildresult.add(mi.toString());
		}
		return buildresult;
	}
	public MaterialCollection(MaterialCollection Source){
		for(MaterialInfo iterate:Source.getMaterials()){
			this.add((MaterialInfo)(iterate.clone()));
		}
	}
	public MaterialCollection(List<MaterialInfo> materialsAdd){
		for(MaterialInfo iterate:materialsAdd){
			add(iterate);
		}
	}
	
	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		for(List<MaterialInfo> iteratelist:materials.values()){
			
			for(MaterialInfo iteratemat:iteratelist){
			stringBuilder.append(iteratemat.toString() + " ");
			}
		}
		
		return stringBuilder.toString();
	}
	
	public int size()
	{
		return this.materials.size();
	}

	public void clear() 
	{
		this.materials.clear();
	}
	public Object clone(){
		return new MaterialCollection(this);
	}
	
}
