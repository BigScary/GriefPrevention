package me.ryanhamshire.GriefPrevention.claim;

import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.player.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Visualization;
import me.ryanhamshire.GriefPrevention.VisualizationType;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 5/19/2017.
 *
 * @author RoboMWM
 */
public class ClaimManager
{
    //in-memory cache for claim data
    ArrayList<Claim> claims = new ArrayList<Claim>();
    ConcurrentHashMap<Long, ArrayList<Claim>> chunksToClaimsMap = new ConcurrentHashMap<Long, ArrayList<Claim>>();

    public void changeClaimOwner(Claim claim, UUID newOwnerID)
    {
        //determine current claim owner
        PlayerData ownerData = null;
        if(!claim.isAdminClaim())
        {
            ownerData = this.getPlayerData(claim.ownerID);
        }

        //determine new owner
        PlayerData newOwnerData = null;

        if(newOwnerID != null)
        {
            newOwnerData = this.getPlayerData(newOwnerID);
        }

        //transfer
        //TODO: use copy constructor
        claim.setOwnerID(newOwnerID);
        this.saveClaim(claim);

        //adjust blocks and other records
        if(ownerData != null)
        {
            ownerData.getClaims().remove(claim);
        }

        if(newOwnerData != null)
        {
            newOwnerData.getClaims().add(claim);
        }
    }

    //adds a claim to the datastore, making it an effective claim
    public void addClaim(Claim newClaim, boolean writeToStorage)
    {
        //add it and mark it as added
        this.claims.add(newClaim);
        ArrayList<Long> chunkHashes = newClaim.getChunkHashes();
        for(Long chunkHash : chunkHashes)
        {
            ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkHash);
            if(claimsInChunk == null)
            {
                claimsInChunk = new ArrayList<Claim>();
                this.chunksToClaimsMap.put(chunkHash, claimsInChunk);
            }

            claimsInChunk.add(newClaim);
        }

        newClaim.inDataStore = true;

        //except for administrative claims (which have no owner), update the owner's playerData with the new claim
        if(!newClaim.isAdminClaim() && writeToStorage)
        {
            PlayerData ownerData = this.getPlayerData(newClaim.ownerID);
            ownerData.getClaims().add(newClaim);
        }

        //make sure the claim is saved to disk
        if(writeToStorage)
        {
            this.saveClaim(newClaim);
        }
    }

    void deleteClaim(Claim claim, boolean fireEvent, boolean releasePets)
    {
        //mark as deleted so any references elsewhere can be ignored
        claim.inDataStore = false;

        //remove from memory
        for(int i = 0; i < this.claims.size(); i++)
        {
            if(claims.get(i).id.equals(claim.id))
            {
                this.claims.remove(i);
                break;
            }
        }

        ArrayList<Long> chunkHashes = claim.getChunkHashes();
        for(Long chunkHash : chunkHashes)
        {
            ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkHash);
            if(claimsInChunk != null)
            {
                for(int j = 0; j < claimsInChunk.size(); j++)
                {
                    if(claimsInChunk.get(j).id.equals(claim.id))
                    {
                        claimsInChunk.remove(j);
                        break;
                    }
                }
            }
        }

        //remove from secondary storage
        this.deleteClaimFromSecondaryStorage(claim);

        //update player data
        if(claim.ownerID != null)
        {
            PlayerData ownerData = this.getPlayerData(claim.ownerID);
            for(int i = 0; i < ownerData.getClaims().size(); i++)
            {
                if(ownerData.getClaims().get(i).id.equals(claim.id))
                {
                    ownerData.getClaims().remove(i);
                    break;
                }
            }
            this.savePlayerData(claim.ownerID, ownerData);
        }

        if(fireEvent)
        {
            ClaimDeletedEvent ev = new ClaimDeletedEvent(claim);
            Bukkit.getPluginManager().callEvent(ev);
        }

        //optionally set any pets free which belong to the claim owner
        if(releasePets && claim.ownerID != null && claim.parent == null)
        {
            for(Chunk chunk : claim.getChunks())
            {
                Entity[] entities = chunk.getEntities();
                for(Entity entity : entities)
                {
                    if(entity instanceof Tameable)
                    {
                        Tameable pet = (Tameable)entity;
                        if(pet.isTamed())
                        {
                            AnimalTamer owner = pet.getOwner();
                            if(owner != null)
                            {
                                UUID ownerID = owner.getUniqueId();
                                if(ownerID != null)
                                {
                                    if(ownerID.equals(claim.ownerID))
                                    {
                                        pet.setTamed(false);
                                        pet.setOwner(null);
                                        if(pet instanceof InventoryHolder)
                                        {
                                            InventoryHolder holder = (InventoryHolder)pet;
                                            holder.getInventory().clear();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    //Utilities useful for claims

    //turns a location into a string, useful in data storage
    public String locationStringDelimiter = ";";
    String locationToString(Location location)
    {
        StringBuilder stringBuilder = new StringBuilder(location.getWorld().getName());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockX());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockY());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockZ());

        return stringBuilder.toString();
    }

    //turns a location string back into a location
    public Location locationFromString(String string, List<World> validWorlds) throws Exception
    {
        //split the input string on the space
        String [] elements = string.split(locationStringDelimiter);

        //expect four elements - world name, X, Y, and Z, respectively
        if(elements.length < 4)
        {
            throw new Exception("Expected four distinct parts to the location string: \"" + string + "\"");
        }

        String worldName = elements[0];
        String xString = elements[1];
        String yString = elements[2];
        String zString = elements[3];

        //identify world the claim is in
        World world = null;
        for(World w : validWorlds)
        {
            if(w.getName().equalsIgnoreCase(worldName))
            {
                world = w;
                break;
            }
        }

        if(world == null)
        {
            throw new Exception("World not found: \"" + worldName + "\"");
        }

        //convert those numerical strings to integer values
        int x = Integer.parseInt(xString);
        int y = Integer.parseInt(yString);
        int z = Integer.parseInt(zString);

        return new Location(world, x, y, z);
    }
    //gets the claim at a specific location
    //ignoreHeight = TRUE means that a location UNDER an existing claim will return the claim
    //cachedClaim can be NULL, but will help performance if you have a reasonable guess about which claim the location is in
    synchronized public Claim getClaimAt(Location location, boolean ignoreHeight, Claim cachedClaim)
    {
        //check cachedClaim guess first.  if it's in the datastore and the location is inside it, we're done
        if(cachedClaim != null && cachedClaim.inDataStore && cachedClaim.contains(location, ignoreHeight, true)) return cachedClaim;

        //find a top level claim
        Long chunkID = getChunkHash(location);
        ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkID);
        if(claimsInChunk == null) return null;

        for(Claim claim : claimsInChunk)
        {
            if(claim.inDataStore && claim.contains(location, false))
            {
                return claim;
            }
        }

        //if no claim found, return null
        return null;
    }

    //finds a claim by ID
    public synchronized Claim getClaim(long id)
    {
        for(Claim claim : this.claims)
        {
            if(claim.inDataStore && claim.getID() == id) return claim;
        }

        return null;
    }

    //returns a read-only access point for the list of all land claims
    //if you need to make changes, use provided methods like .deleteClaim() and .createClaim().
    //this will ensure primary memory (RAM) and secondary memory (disk, database) stay in sync
    public Collection<Claim> getClaims()
    {
        return Collections.unmodifiableCollection(this.claims);
    }

    public Collection<Claim> getClaims(int chunkx, int chunkz)
    {
        ArrayList<Claim> chunkClaims = this.chunksToClaimsMap.get(getChunkHash(chunkx, chunkz));
        if(chunkClaims != null)
        {
            return Collections.unmodifiableCollection(chunkClaims);
        }
        else
        {
            return Collections.unmodifiableCollection(new ArrayList<Claim>());
        }
    }

    //gets an almost-unique, persistent identifier for a chunk
    static Long getChunkHash(long chunkx, long chunkz)
    {
        return (chunkz ^ (chunkx << 32));
    }

    //gets an almost-unique, persistent identifier for a chunk
    static Long getChunkHash(Location location)
    {
        return getChunkHash(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    //creates a claim.
    //if the new claim would overlap an existing claim, returns a failure along with a reference to the existing claim
    //if the new claim would overlap a WorldGuard region where the player doesn't have permission to build, returns a failure with NULL for claim
    //otherwise, returns a success along with a reference to the new claim
    //use ownerName == "" for administrative claims
    //for top level claims, pass parent == NULL
    //DOES adjust claim blocks available on success (players can go into negative quantity available)
    //DOES check for world guard regions where the player doesn't have permission
    //does NOT check a player has permission to create a claim, or enough claim blocks.
    //does NOT check minimum claim size constraints
    //does NOT visualize the new claim for any players
    synchronized public CreateClaimResult createClaim(World world, int x1, int x2, int y1, int y2, int z1, int z2, UUID ownerID, Claim parent, Long id, Player creatingPlayer)
    {
        CreateClaimResult result = new CreateClaimResult();

        int smallx, bigx, smally, bigy, smallz, bigz;

        if(y1 < GriefPrevention.instance.config_claims_maxDepth) y1 = GriefPrevention.instance.config_claims_maxDepth;
        if(y2 < GriefPrevention.instance.config_claims_maxDepth) y2 = GriefPrevention.instance.config_claims_maxDepth;

        //determine small versus big inputs
        if(x1 < x2)
        {
            smallx = x1;
            bigx = x2;
        }
        else
        {
            smallx = x2;
            bigx = x1;
        }

        if(y1 < y2)
        {
            smally = y1;
            bigy = y2;
        }
        else
        {
            smally = y2;
            bigy = y1;
        }

        if(z1 < z2)
        {
            smallz = z1;
            bigz = z2;
        }
        else
        {
            smallz = z2;
            bigz = z1;
        }

        //creative mode claims always go to bedrock
        if(GriefPrevention.instance.config_claims_worldModes.get(world) == ClaimsMode.Creative)
        {
            smally = 0;
        }

        //create a new claim instance (but don't save it, yet)
        Claim newClaim = new Claim(
                new Location(world, smallx, smally, smallz),
                new Location(world, bigx, bigy, bigz),
                ownerID,
                new ArrayList<String>(),
                new ArrayList<String>(),
                new ArrayList<String>(),
                new ArrayList<String>(),
                id);

        newClaim.parent = parent;

        //ensure this new claim won't overlap any existing claims
        ArrayList<Claim> claimsToCheck = this.claims;

        for(int i = 0; i < claimsToCheck.size(); i++)
        {
            Claim otherClaim = claimsToCheck.get(i);

            //if we find an existing claim which will be overlapped
            if(otherClaim.id != newClaim.id && otherClaim.inDataStore && otherClaim.overlaps(newClaim))
            {
                //result = fail, return conflicting claim
                result.succeeded = false;
                result.claim = otherClaim;
                return result;
            }
        }

        //otherwise add this new claim to the data store to make it effective
        this.addClaim(newClaim, true);

        //then return success along with reference to new claim
        result.succeeded = true;
        result.claim = newClaim;
        return result;
    }

    //extends a claim to a new depth
    //respects the max depth config variable
    synchronized public void extendClaim(Claim claim, int newDepth)
    {
        if(newDepth < GriefPrevention.instance.config_claims_maxDepth) newDepth = GriefPrevention.instance.config_claims_maxDepth;

        if(claim.parent != null) claim = claim.parent;

        //adjust to new depth
        claim.lesserBoundaryCorner.setY(newDepth);
        claim.greaterBoundaryCorner.setY(newDepth);

        //save changes
        this.saveClaim(claim);
    }

    //deletes all claims owned by a player
    synchronized public void deleteClaimsForPlayer(UUID playerID, boolean releasePets)
    {
        //make a list of the player's claims
        ArrayList<Claim> claimsToDelete = new ArrayList<Claim>();
        for(int i = 0; i < this.claims.size(); i++)
        {
            Claim claim = this.claims.get(i);
            if((playerID == claim.ownerID || (playerID != null && playerID.equals(claim.ownerID))))
                claimsToDelete.add(claim);
        }

        //delete them one by one
        for(int i = 0; i < claimsToDelete.size(); i++)
        {
            Claim claim = claimsToDelete.get(i);
            claim.removeSurfaceFluids(null);

            this.deleteClaim(claim, releasePets);

            //if in a creative mode world, delete the claim
            if(GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
            {
                GriefPrevention.instance.restoreClaim(claim, 0);
            }
        }
    }

    //tries to resize a claim
    //see CreateClaim() for details on return value
    synchronized public CreateClaimResult resizeClaim(Claim claim, int newx1, int newx2, int newy1, int newy2, int newz1, int newz2, Player resizingPlayer)
    {
        //try to create this new claim, ignoring the original when checking for overlap
        CreateClaimResult result = this.createClaim(claim.getLesserBoundaryCorner().getWorld(), newx1, newx2, newy1, newy2, newz1, newz2, claim.ownerID, claim.parent, claim.id, resizingPlayer);

        //if succeeded
        if(result.succeeded)
        {
            //copy permissions from old claim
            ArrayList<String> builders = new ArrayList<String>();
            ArrayList<String> containers = new ArrayList<String>();
            ArrayList<String> accessors = new ArrayList<String>();
            ArrayList<String> managers = new ArrayList<String>();
            claim.getPermissions(builders, containers, accessors, managers);

            for(int i = 0; i < builders.size(); i++)
                result.claim.setPermission(builders.get(i), ClaimPermission.Build);

            for(int i = 0; i < containers.size(); i++)
                result.claim.setPermission(containers.get(i), ClaimPermission.Inventory);

            for(int i = 0; i < accessors.size(); i++)
                result.claim.setPermission(accessors.get(i), ClaimPermission.Access);

            for(int i = 0; i < managers.size(); i++)
            {
                result.claim.managers.add(managers.get(i));
            }

            //save those changes
            this.saveClaim(result.claim);

            //make original claim ineffective (it's still in the hash map, so let's make it ignored)
            claim.inDataStore = false;
        }

        return result;
    }

    void resizeClaimWithChecks(Player player, PlayerData playerData, int newx1, int newx2, int newy1, int newy2, int newz1, int newz2)
    {
        //for top level claims, apply size rules and claim blocks requirement
        if(playerData.claimResizing.parent == null)
        {
            //measure new claim, apply size rules
            int newWidth = (Math.abs(newx1 - newx2) + 1);
            int newHeight = (Math.abs(newz1 - newz2) + 1);
            boolean smaller = newWidth < playerData.claimResizing.getWidth() || newHeight < playerData.claimResizing.getHeight();

            if(!player.hasPermission("griefprevention.adminclaims") && !playerData.claimResizing.isAdminClaim() && smaller)
            {
                if(newWidth < GriefPrevention.instance.config_claims_minWidth || newHeight < GriefPrevention.instance.config_claims_minWidth)
                {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeClaimTooNarrow, String.valueOf(GriefPrevention.instance.config_claims_minWidth));
                    return;
                }

                int newArea = newWidth * newHeight;
                if(newArea < GriefPrevention.instance.config_claims_minArea)
                {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeClaimInsufficientArea, String.valueOf(GriefPrevention.instance.config_claims_minArea));
                    return;
                }
            }

            //make sure player has enough blocks to make up the difference
            if(!playerData.claimResizing.isAdminClaim() && player.getName().equals(playerData.claimResizing.getOwnerName()))
            {
                int newArea =  newWidth * newHeight;
                int blocksRemainingAfter = playerData.getRemainingClaimBlocks() + playerData.claimResizing.getArea() - newArea;

                if(blocksRemainingAfter < 0)
                {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeNeedMoreBlocks, String.valueOf(Math.abs(blocksRemainingAfter)));
                    this.tryAdvertiseAdminAlternatives(player);
                    return;
                }
            }
        }

        //special rule for making a top-level claim smaller.  to check this, verifying the old claim's corners are inside the new claim's boundaries.
        //rule: in any mode, shrinking a claim removes any surface fluids
        Claim oldClaim = playerData.claimResizing;
        boolean smaller = false;
        if(oldClaim.parent == null)
        {
            //temporary claim instance, just for checking contains()
            Claim newClaim = new Claim(
                    new Location(oldClaim.getLesserBoundaryCorner().getWorld(), newx1, newy1, newz1),
                    new Location(oldClaim.getLesserBoundaryCorner().getWorld(), newx2, newy2, newz2),
                    null, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), null);

            //if the new claim is smaller
            if(!newClaim.contains(oldClaim.getLesserBoundaryCorner(), true, false) || !newClaim.contains(oldClaim.getGreaterBoundaryCorner(), true, false))
            {
                smaller = true;

                //remove surface fluids about to be unclaimed
                oldClaim.removeSurfaceFluids(newClaim);
            }
        }

        //ask the datastore to try and resize the claim, this checks for conflicts with other claims
        CreateClaimResult result = GriefPrevention.instance.dataStore.resizeClaim(playerData.claimResizing, newx1, newx2, newy1, newy2, newz1, newz2, player);

        if(result.succeeded)
        {
            //decide how many claim blocks are available for more resizing
            int claimBlocksRemaining = 0;
            if(!playerData.claimResizing.isAdminClaim())
            {
                UUID ownerID = playerData.claimResizing.ownerID;
                if(playerData.claimResizing.parent != null)
                {
                    ownerID = playerData.claimResizing.parent.ownerID;
                }
                if(ownerID == player.getUniqueId())
                {
                    claimBlocksRemaining = playerData.getRemainingClaimBlocks();
                }
                else
                {
                    PlayerData ownerData = this.getPlayerData(ownerID);
                    claimBlocksRemaining = ownerData.getRemainingClaimBlocks();
                    OfflinePlayer owner = GriefPrevention.instance.getServer().getOfflinePlayer(ownerID);
                    if(!owner.isOnline())
                    {
                        this.clearCachedPlayerData(ownerID);
                    }
                }
            }

            //inform about success, visualize, communicate remaining blocks available
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimResizeSuccess, String.valueOf(claimBlocksRemaining));
            Visualization visualization = Visualization.FromClaim(result.claim, player.getEyeLocation().getBlockY(), VisualizationType.Claim, player.getLocation());
            Visualization.Apply(player, visualization);

            //if resizing someone else's claim, make a log entry
            if(!player.getUniqueId().equals(playerData.claimResizing.ownerID) && playerData.claimResizing.parent == null)
            {
                GriefPrevention.AddLogEntry(player.getName() + " resized " + playerData.claimResizing.getOwnerName() + "'s claim at " + GriefPrevention.getfriendlyLocationString(playerData.claimResizing.lesserBoundaryCorner) + ".");
            }

            //if in a creative mode world and shrinking an existing claim, restore any unclaimed area
            if(smaller && GriefPrevention.instance.creativeRulesApply(oldClaim.getLesserBoundaryCorner()))
            {
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
                GriefPrevention.instance.restoreClaim(oldClaim, 20L * 60 * 2);  //2 minutes
                GriefPrevention.AddLogEntry(player.getName() + " shrank a claim @ " + GriefPrevention.getfriendlyLocationString(playerData.claimResizing.getLesserBoundaryCorner()));
            }

            //clean up
            playerData.claimResizing = null;
            playerData.lastShovelLocation = null;
        }
        else
        {
            if(result.claim != null)
            {
                //inform player
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlap);

                //show the player the conflicting claim
                Visualization visualization = Visualization.FromClaim(result.claim, player.getEyeLocation().getBlockY(), VisualizationType.ErrorClaim, player.getLocation());
                Visualization.Apply(player, visualization);
            }
            else
            {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlapRegion);
            }
        }
    }

    //gets all the claims "near" a location
    Set<Claim> getNearbyClaims(Location location)
    {
        Set<Claim> claims = new HashSet<Claim>();

        Chunk lesserChunk = location.getWorld().getChunkAt(location.subtract(150, 0, 150));
        Chunk greaterChunk = location.getWorld().getChunkAt(location.add(300, 0, 300));

        for(int chunk_x = lesserChunk.getX(); chunk_x <= greaterChunk.getX(); chunk_x++)
        {
            for(int chunk_z = lesserChunk.getZ(); chunk_z <= greaterChunk.getZ(); chunk_z++)
            {
                Chunk chunk = location.getWorld().getChunkAt(chunk_x, chunk_z);
                Long chunkID = getChunkHash(chunk.getBlock(0,  0,  0).getLocation());
                ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkID);
                if(claimsInChunk != null)
                {
                    for(Claim claim : claimsInChunk)
                    {
                        if(claim.inDataStore && claim.getLesserBoundaryCorner().getWorld().equals(location.getWorld()))
                        {
                            claims.add(claim);
                        }
                    }
                }
            }
        }

        return claims;
    }

    //deletes all the land claims in a specified world
    void deleteClaimsInWorld(World world, boolean deleteAdminClaims)
    {
        for(int i = 0; i < claims.size(); i++)
        {
            Claim claim = claims.get(i);
            if(claim.getLesserBoundaryCorner().getWorld().equals(world))
            {
                if(!deleteAdminClaims && claim.isAdminClaim()) continue;
                this.deleteClaim(claim, false, false);
                i--;
            }
        }
    }
}
