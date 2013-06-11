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

//ordered list of material info objects, for fast searching
public class MaterialCollection
{
	HashMap<Integer,MaterialInfo> materials = new HashMap<Integer,MaterialInfo>();
	
	void Add(MaterialInfo material)
	{
		int i;
		this.materials.put(material.typeID, material);
	}
	
	boolean Contains(MaterialInfo material)
	{
		if(this.materials.containsKey(material.typeID))
		{
			return this.materials.get(material.typeID).allDataValues ||
					this.materials.get(material.typeID).data==material.data;
		}
		
		
		return false;
	}
	
	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		for(int i = 0; i < this.materials.size(); i++)
		{
			stringBuilder.append(this.materials.get(i).toString() + " ");
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
}
