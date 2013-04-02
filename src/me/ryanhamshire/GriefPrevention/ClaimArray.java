package me.ryanhamshire.GriefPrevention;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimArray {
	
	private ArrayList<Claim> claims = new ArrayList<Claim>();
	ConcurrentHashMap<Long, Claim> claimmap = new ConcurrentHashMap<Long, Claim>();
	
	ConcurrentHashMap<String, ArrayList<Claim>> chunkmap = new ConcurrentHashMap<String, ArrayList<Claim>>();

	public int size() {
		return claims.size();
	}

	public Claim get(int i) {
		return claims.get(i);
	}
	
	public Claim getID(long i) {
		return claimmap.get(i);
	}

	public void add(int j, Claim newClaim) {
		claims.add(j, newClaim);
		claimmap.put(newClaim.getID(), newClaim);
		ArrayList<String> chunks = getChunks(newClaim);
		for(String chunk : chunks) {
			ArrayList<Claim> aclaims = chunkmap.get(chunk);
			if(aclaims == null) {
				aclaims = new ArrayList<Claim>();
				aclaims.add(newClaim);
				chunkmap.put(chunk, aclaims);
			}else {
				int k = 0;
				while(k < aclaims.size() && !aclaims.get(k).greaterThan(newClaim)) k++;
				if(k < aclaims.size())
					aclaims.add(k, newClaim);
				else
					aclaims.add(this.claims.size(), newClaim);
			}
		}
	}
	
	public void removeID(Long i) {
		Claim claim = claimmap.remove(i);
		ArrayList<String> chunks = getChunks(claim);
		claims.remove(claim);
		for(String chunk : chunks) {
			ArrayList<Claim> aclaims = chunkmap.get(chunk);
			if(aclaims != null) {
				aclaims.remove(claim);
				if(aclaims.size() == 0) {
					chunkmap.remove(chunk);
				}
			}
		}
	}
	
	public static ArrayList<String> getChunks(Claim claim) {
		String world = claim.getLesserBoundaryCorner().getWorld().getName();
		int lx = claim.getLesserBoundaryCorner().getBlockX();
		int lz = claim.getLesserBoundaryCorner().getBlockZ();
		int gx = claim.getGreaterBoundaryCorner().getBlockX();
		int gz = claim.getGreaterBoundaryCorner().getBlockZ();
		//Let's make sure the lowest value of X is in lx.
		if(gx < lx) {
			int tx = gx;
			gx = lx;
			lx = tx;
		}
		//Let's make sure the lowest value of Z is in lz.
		if(gz < lz) {
			int tz = gz;
			gz = lz;
			lz = tz;
		}
		ArrayList<String> chunks = new ArrayList<String>();
		for(int tx = lx; (tx >> 4) <= (gx >> 4); tx += 16) {
			for(int tz = lz; (tz >> 4) <= (gz >> 4); tz += 16) {
				int chunkX = tx >> 4;
			    int chunkZ = tz >> 4;
			    chunks.add(world + ";" + chunkX + "," + chunkZ);
			}
		}
		return chunks;
	}

}
