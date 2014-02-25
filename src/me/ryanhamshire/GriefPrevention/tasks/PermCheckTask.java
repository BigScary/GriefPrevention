package me.ryanhamshire.GriefPrevention.tasks;


import org.bukkit.entity.Player;
//thread-safe task wrapper for Permission checks.
public class PermCheckTask implements Runnable{
    public boolean CheckResult=false;
    private Player testPlayer;
    private String testPermission;
    public PermCheckTask(Player pPlayer,String pPermission){
        testPlayer = pPlayer;
        testPermission = pPermission;

    }
    public void run(){
        CheckResult = testPlayer.hasPermission(testPermission);
    }


}
