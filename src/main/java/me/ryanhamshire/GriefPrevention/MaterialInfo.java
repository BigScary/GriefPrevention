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

import java.util.regex.Pattern;

import org.bukkit.Material;

//represents a material or collection of materials
public class MaterialInfo
{
	public int typeID;
	public byte data;
	public boolean allDataValues;
	String description;
	private Pattern re;
	public int getTypeID(){ return typeID;}
	public byte getData(){ return data;}
	public boolean getallDataValues(){ return allDataValues;}
	public String getDescription(){ return description;}
	public MaterialInfo(int typeID, byte data, String description)
	{
		this.typeID = typeID;
		this.data = data;
		this.allDataValues = false;
		this.description = description;
	}
	public MaterialInfo(Material Source){
		this.typeID = Source.getId();
		this.data = 0;
		this.allDataValues=true;
		description = Source.name();
	}
	public MaterialInfo(MaterialInfo Source){
		this.typeID = Source.getTypeID();
		this.data = Source.getData();
		this.allDataValues= Source.getallDataValues();
		this.description = Source.getDescription();
		
	}
	public MaterialInfo(int typeID, String description)
	{
		this.typeID = typeID;
		this.data = 0;
		this.allDataValues = true;
		if(description==null || description.length()==0){
			description = Material.getMaterial(typeID).name();
		}
		this.description = description;
	}
	
	private MaterialInfo(int typeID, byte data, boolean allDataValues, String description)
	{
		this.typeID = typeID;
		this.data = data;
		this.allDataValues = allDataValues;
		if(description.startsWith("//")){
			re = Pattern.compile(description.substring(1));
		}
		this.description = description;
	}
	public Object clone(){
		return new MaterialInfo(this);
	}
	@Override
	public String toString()
	{
		String returnValue = String.valueOf(this.typeID) + ":" + (this.allDataValues?"*":String.valueOf(this.data));
		if(this.description != null) returnValue += ":" + this.description;
		
		return returnValue;
	}
	@Override
	public int hashCode(){
		return (typeID*data)/(typeID+data)^data;
	}
	@Override
	public boolean equals(Object other){
		if(other instanceof MaterialInfo){
			MaterialInfo castedelement = (MaterialInfo)other;
			if (this.typeID == castedelement.typeID &&
					((this.allDataValues || castedelement.allDataValues) ||
							this.data == castedelement.data)){
				if(re!=null){
					return re.matcher(castedelement.getDescription()).matches();
				}
				return true;
			}
		}
		return super.equals(other);
	}
	public static MaterialInfo fromString(String string)
	{
		if(string == null || string.isEmpty()) return null;
		
		String [] parts = string.split(":");
		//if only ID is specified, allow it.
		if(parts.length==1){
			try {
				return new MaterialInfo(Integer.parseInt(parts[0]),null);
			}
			catch(NumberFormatException ex){
				return null;
			}
		}
		
		try
		{
			int typeID = Integer.parseInt(parts[0]);
		
			byte data;
			boolean allDataValues;
			if(parts[1].equals("*"))
			{
				allDataValues = true;
				data = 0;
			}
			else
			{
				allDataValues = false;
				data = Byte.parseByte(parts[1]);
			}
			String Name = parts.length<2?parts[2]:"X" + String.valueOf(typeID) + "$" + String.valueOf(data);
			return new MaterialInfo(typeID, data, allDataValues, Name);
		}
		catch(NumberFormatException exception)
		{
			return null;
		}
	}
}
