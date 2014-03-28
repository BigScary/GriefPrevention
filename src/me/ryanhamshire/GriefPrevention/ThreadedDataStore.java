package me.ryanhamshire.GriefPrevention;

import org.bukkit.World;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * wraps a normal DataStore. Any functions that do not return a value
 * are run on a separate thread.
 */
public class ThreadedDataStore extends DataStore{

    private DataStore InternalStore = null;
    public DataStore getInternalStore(){ return InternalStore;}
    private Queue<Runnable> DataCallQueue= new ConcurrentLinkedDeque<Runnable>();
    private Thread DataCallThread = null;
    private void DataCallThreadRoutine()
    {

        while(true){

            if(DataCallQueue.size() > 0){
                Runnable runit = DataCallQueue.poll();
                runit.run();

            }
            try {
            Thread.sleep(50); //yield and wait.
            }catch(InterruptedException exx){

                }
        }


    }

    public ThreadedDataStore(DataStore Internal){
        if(Internal==null) throw new IllegalArgumentException("DataStore cannot be null");
        InternalStore = Internal;
    }
    private void RunThreaded(Runnable target){
        if(DataCallThread==null){
            DataCallThread = new Thread(new Runnable(){public void run(){DataCallThreadRoutine();}});
            DataCallThread.run();
        }
        DataCallQueue.add(target);
    }
    @Override
    void deleteClaimFromSecondaryStorage(final Claim claim) {
        RunThreaded(new Runnable(){public void run(){
            InternalStore.deleteClaimFromSecondaryStorage(claim);
        }
    });
    }

    @Override
    public boolean deletePlayerData(String playerName) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<PlayerData> getAllPlayerData() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    PlayerData getPlayerDataFromStorage(String playerName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasPlayerData(String playerName) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void incrementNextClaimID() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void saveGroupBonusBlocks(String groupName, int amount) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void savePlayerData(String playerName, PlayerData playerData) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getNextClaimID() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setNextClaimID(long nextClaimID2) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void WorldLoaded(World worldload) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void writeClaimToStorage(Claim claim) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
