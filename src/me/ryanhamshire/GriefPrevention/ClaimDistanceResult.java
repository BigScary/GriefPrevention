package me.ryanhamshire.GriefPrevention;

public class ClaimDistanceResult {

	private Claim claim;
	private int Distance;
	public Claim getClaim(){ return claim;}
	public int getDistance(){ return Distance;}
	public ClaimDistanceResult(Claim c,int pDistance){
		claim=c;
		Distance=pDistance;
	}
}
