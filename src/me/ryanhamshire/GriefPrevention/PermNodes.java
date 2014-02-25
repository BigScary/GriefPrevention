package me.ryanhamshire.GriefPrevention;

public class PermNodes {

	/**
	 * Administrator Claims Permission.
	 */
	public static final String AdminClaimsPermission = "griefprevention.admin.claims";
	
	public static final String AdminEavesDropPermission = "griefprevention.admin.eavesdrop";
	public static final String AdminToolPermission = "griefprevention.admin.tool";
	public static final String AdminLockPermission = "griefprevention.admin.lock";
    public static final String IgnoreClaimsLimitPermission = "griefprevention.claims.ignorelimit";
	public static final String GiveClaimsPermission = "griefprevention.claims.give";
	public static final String CreateClaimsPermission = "griefprevention.claims.chestcreate";
    public static final String CreateClaimsShovelPermission = "griefprevention.claims.toolcreate";
	public static final String DeleteClaimsPermission = "griefprevention.claims.delete";
	public static final String IgnoreClaimsPermission = "griefprevention.claims.ignore";
	public static final String InvestigateAreaPermission = "griefprevention.claims.investigatearea";
	public static final String LockClaimsPermission = "griefprevention.claims.lock";
	public static final String TransferClaimsPermission = "griefprevention.claims.transfer";
	public static final String LavaPermission = "griefprevention.placement.lava";
	public static final String WaterPermission = "griefprevention.placement.water";
    public static final String AllHorsesPermission = "griefprevention.admin.horses";
	/**
	 * "standard" eavesdropping, which is whispers/tells.
	 */
	public static final String EavesDropPermission = "griefprevention.eavesdrop.standard";
    public static final String EavesDropMute = "griefprevention.eavesdrop.mute";

	public static final String LoginSpamPermission = "griefprevention.spam.login";
	public static final String NoPvPImmunityPermission = "griefprevention.pvp.noimmunity";
	public static final String ReloadPermission = "griefprevention.admin.reload";
	public static final String SpamPermission = "griefprevention.spam.chat";

    public static final String NotIgnorablePermission= "griefprevention.spam.notignorable";
	public static final String commandpermission = "griefprevention.commands.%s";
	
	public static String getCommandPermission(String CommandName){
		return String.format(commandpermission, CommandName.toLowerCase());
	}
	
	
	
}
