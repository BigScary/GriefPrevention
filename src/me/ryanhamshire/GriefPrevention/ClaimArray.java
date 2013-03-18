package me.ryanhamshire.GriefPrevention;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimArray {
	
	ArrayList<Claim> claims = new ArrayList<Claim>();
	ConcurrentHashMap<Long, Claim> claimmap = new ConcurrentHashMap<Long, Claim>();

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
	}
	
	public void removeID(Long i) {
		claims.remove(claimmap.remove(i));
	}

}
