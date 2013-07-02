package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuySellClaimBlocks extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		// buyclaimblocks
		Player player = (sender instanceof Player) ? (Player) sender : null;
		if (player == null)
			return false;
		GriefPrevention inst = GriefPrevention.instance;
		if (command.getName().equalsIgnoreCase("buyclaimblocks")
				&& player != null) {
			// if economy is disabled, don't do anything
			if (GriefPrevention.economy == null) {
				GriefPrevention.sendMessage(player, TextMode.Err,
						Messages.BuySellNotConfigured);
				return true;
			}
				if (!player.hasPermission("griefprevention.buysellclaimblocks")) {
					GriefPrevention.sendMessage(player, TextMode.Err,
							Messages.NoPermissionForCommand);
					return true;
				}

				// if purchase disabled, send error message
				if (GriefPrevention.instance.config_economy_claimBlocksPurchaseCost == 0) {
					GriefPrevention.sendMessage(player, TextMode.Err,
							Messages.OnlySellBlocks);
					return true;
				}

				// if no parameter, just tell player cost per block and balance
				if (args.length != 1) {
					GriefPrevention
							.sendMessage(
									player,
									TextMode.Info,
									Messages.BlockPurchaseCost,
									String.valueOf(GriefPrevention.instance.config_economy_claimBlocksPurchaseCost),
									String.valueOf(GriefPrevention.economy
											.getBalance(player.getName())));
					return false;
				}

				else {
					// determine max purchasable blocks
					PlayerData playerData = inst.dataStore.getPlayerData(player
							.getName());
					int maxPurchasable = GriefPrevention.instance.config_claims_maxAccruedBlocks
							- playerData.accruedClaimBlocks;

					// if the player is at his max, tell him so
					if (maxPurchasable <= 0) {
						GriefPrevention.sendMessage(player, TextMode.Err,
								Messages.ClaimBlockLimit);
						return true;
					}

					// try to parse number of blocks
					int blockCount;
					try {
						blockCount = Integer.parseInt(args[0]);
					} catch (NumberFormatException numberFormatException) {
						return false; // causes usage to be displayed
					}

					if (blockCount <= 0) {
						return false;
					}

					// correct block count to max allowed
					if (blockCount > maxPurchasable) {
						blockCount = maxPurchasable;
					}

					// if the player can't afford his purchase, send error
					// message
					double balance = inst.economy.getBalance(player.getName());
					double totalCost = blockCount
							* GriefPrevention.instance.config_economy_claimBlocksPurchaseCost;
					if (totalCost > balance) {
						GriefPrevention.sendMessage(player, TextMode.Err,
								Messages.InsufficientFunds,
								String.valueOf(totalCost),
								String.valueOf(balance));
					}

					// otherwise carry out transaction
					else {
						// withdraw cost
						inst.economy
								.withdrawPlayer(player.getName(), totalCost);

						// add blocks
						playerData.accruedClaimBlocks += blockCount;
						inst.dataStore.savePlayerData(player.getName(),
								playerData);

						// inform player
						GriefPrevention.sendMessage(player, TextMode.Success,
								Messages.PurchaseConfirmation, String
										.valueOf(totalCost), String
										.valueOf(playerData
												.getRemainingClaimBlocks()));
					}

					return true;
				}
			}

			// sellclaimblocks <amount>
			else if (command.getName().equalsIgnoreCase("sellclaimblocks")
					&& player != null) {
				// if economy is disabled, don't do anything
				if (GriefPrevention.economy == null) {
					GriefPrevention.sendMessage(player, TextMode.Err,
							Messages.BuySellNotConfigured);
					return true;
				}

				if (!player.hasPermission("griefprevention.buysellclaimblocks")) {
					GriefPrevention.sendMessage(player, TextMode.Err,
							Messages.NoPermissionForCommand);
					return true;
				}

				// if disabled, error message
				if (GriefPrevention.instance.config_economy_claimBlocksSellValue == 0) {
					GriefPrevention.sendMessage(player, TextMode.Err,
							Messages.OnlyPurchaseBlocks);
					return true;
				}

				// load player data
				PlayerData playerData = inst.dataStore.getPlayerData(player
						.getName());
				int availableBlocks = playerData.getRemainingClaimBlocks();

				// if no amount provided, just tell player value per block sold,
				// and how many he can sell
				if (args.length != 1) {
					GriefPrevention
							.sendMessage(
									player,
									TextMode.Info,
									Messages.BlockSaleValue,
									String.valueOf(GriefPrevention.instance.config_economy_claimBlocksSellValue),
									String.valueOf(availableBlocks));
					return false;
				}

				// parse number of blocks
				int blockCount;
				try {
					blockCount = Integer.parseInt(args[0]);
				} catch (NumberFormatException numberFormatException) {
					return false; // causes usage to be displayed
				}

				if (blockCount <= 0) {
					return false;
				}

				// if he doesn't have enough blocks, tell him so
				if (blockCount > availableBlocks) {
					GriefPrevention.sendMessage(player, TextMode.Err,
							Messages.NotEnoughBlocksForSale);
				}

				// otherwise carry out the transaction
				else {
					// compute value and deposit it
					double totalValue = blockCount
							* GriefPrevention.instance.config_economy_claimBlocksSellValue;
					inst.economy.depositPlayer(player.getName(), totalValue);

					// subtract blocks
					playerData.accruedClaimBlocks -= blockCount;
					inst.dataStore.savePlayerData(player.getName(), playerData);

					// inform player
					GriefPrevention.sendMessage(player, TextMode.Success,
							Messages.BlockSaleConfirmation, String
									.valueOf(totalValue), String
									.valueOf(playerData
											.getRemainingClaimBlocks()));
				}

				return true;
			}
			return false;
		
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[] { "buyclaimblocks", "sellclaimblocks" };
	}

}
