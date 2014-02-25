package me.ryanhamshire.GriefPrevention.CommandHandling;

import org.bukkit.inventory.ItemStack;


public class BookViewData
{

    private ItemStack BookItem;
    private int ID = 0;

    public ItemStack getBookItem(){ return BookItem;}
    public int getEavesDropID(){ return ID;}
    public BookViewData(ItemStack pBookItem){
        ID = (int)(ViewBook.r.nextFloat()*32768);
        BookItem = pBookItem;

    }
}
