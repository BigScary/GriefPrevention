package me.ryanhamshire.GriefPrevention;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;

import org.bukkit.Location;
import org.bukkit.World;

public class ClaimArray implements Iterable<Claim> {

	public static ArrayList<String> getChunks(Claim claim) {
		String world = claim.getLesserBoundaryCorner().getWorld().getName();

		int lx = claim.getLesserBoundaryCorner().getBlockX();
		int lz = claim.getLesserBoundaryCorner().getBlockZ();
		int gx = claim.getGreaterBoundaryCorner().getBlockX();
		int gz = claim.getGreaterBoundaryCorner().getBlockZ();
		// Let's make sure the lowest value of X is in lx.
		if (gx < lx) {
			int tx = gx;
			gx = lx;
			lx = tx;
		}
		// Let's make sure the lowest value of Z is in lz.
		if (gz < lz) {
			int tz = gz;
			gz = lz;
			lz = tz;
		}
		ArrayList<String> chunks = new ArrayList<String>();
		for (int tx = lx; (tx >> 4) <= (gx >> 4); tx += 16) {
			for (int tz = lz; (tz >> 4) <= (gz >> 4); tz += 16) {
				int chunkX = tx >> 4;
				int chunkZ = tz >> 4;
				chunks.add(world + ";" + chunkX + "," + chunkZ);
			}
		}
		return chunks;
	}

	ConcurrentMap<String, ArrayList<Claim>> chunkmap = new ConcurrentHashMap<String, ArrayList<Claim>>();
	ConcurrentMap<Long, Claim> claimmap = new ConcurrentHashMap<Long, Claim>();
	private ArrayList<Claim> claims = new ArrayList<Claim>();

	ConcurrentMap<String, ArrayList<Claim>> claimworldmap = new ConcurrentHashMap<String, ArrayList<Claim>>();

	public void add(Claim newClaim) {
		addClaimWorld(newClaim);
		claims.add(newClaim);
		claimmap.put(newClaim.getID(), newClaim);
		ArrayList<String> chunks = getChunks(newClaim);
		for (String chunk : chunks) {
			ArrayList<Claim> aclaims = chunkmap.get(chunk);
			if (aclaims == null) {
				aclaims = new ArrayList<Claim>();
				aclaims.add(newClaim);
				chunkmap.put(chunk, aclaims);
			} else {
				int k = 0;
				if (!aclaims.contains(newClaim)) {

					aclaims.add(newClaim);
				}
				chunkmap.put(chunk, aclaims);
			}
		}
	}

	private void addClaimWorld(Claim c) {
		String usekey = c.getLesserBoundaryCorner().getWorld().getName();
		if (!claimworldmap.containsKey(usekey))
			claimworldmap.put(usekey, new ArrayList<Claim>());

		if (!claimworldmap.get(usekey).contains(c))
			claimworldmap.get(usekey).add(c);
		Debugger.Write("Claim added to world mapping owned by " + c.getOwnerName() + " to world:" + c.getLesserBoundaryCorner().getWorld(), DebugLevel.Verbose);

	}

	public Claim get(int i) {
		return claims.get(i);
	}

	public List<Claim> getClaims(String chunk) {
		if (!chunkmap.containsKey(chunk))
			return new ArrayList<Claim>();
		return chunkmap.get(chunk);
	}

	public List<Claim> getClaimsInChunk(Location ChunkLocation) {

		String chunkstr = GriefPrevention.instance.dataStore.getChunk(ChunkLocation);
		return getClaims(chunkstr);
	}

	public Claim getID(long i) {
		return claimmap.get(i);
	}



	public List<Claim> getWorldClaims(String worldname) {
		if (!claimworldmap.containsKey(worldname))
			return new ArrayList<Claim>();
		return claimworldmap.get(worldname);
	}

	public List<Claim> getWorldClaims(World w) {
		return getWorldClaims(w.getName());
	}

	@SuppressWarnings("unchecked")
	public Iterator<Claim> iterator() {
		// TODO Auto-generated method stub
		return ((ArrayList<Claim>) claims.clone()).iterator();
	}

	private void removeClaimWorld(Claim c) {
		if (c == null)
			return;
		String usekey = c.getLesserBoundaryCorner().getWorld().getName();
		if (claimworldmap.containsKey(usekey)) {
			claimworldmap.get(usekey).remove(c);
			Debugger.Write("Claim removed from world mapping owned by " + c.getOwnerName() + " to world:" + c.getLesserBoundaryCorner().getWorld(), DebugLevel.Verbose);
		}

	}

	public void removeID(Long i) {
		Claim claim = claimmap.remove(i);
		if (claim == null)
			return;
		this.removeClaimWorld(claim);
		ArrayList<String> chunks = getChunks(claim);
		claims.remove(claim);
		for (String chunk : chunks) {
			ArrayList<Claim> aclaims = chunkmap.get(chunk);
			if (aclaims != null) {
				Debugger.Write("Removing Claim ID #" + i + " From Claim List for Chunk:" + chunk, DebugLevel.Verbose);
				aclaims.remove(claim);
				if (aclaims.size() == 0) {
					chunkmap.remove(chunk);
					Debugger.Write("Removing empty chunk mapping entry for chunk " + chunk + " As it now contains no claims.", DebugLevel.Verbose);
				}
			}
		}
	}

	public int size() {
		return claims.size();
	}

}
