package com.openrsc.server.plugins.skills;

import com.openrsc.server.event.custom.BatchEvent;
import com.openrsc.server.external.EntityHandler;
import com.openrsc.server.external.ItemId;
import com.openrsc.server.external.ObjectMiningDef;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.World;
import com.openrsc.server.plugins.listeners.action.ObjectActionListener;
import com.openrsc.server.plugins.listeners.executive.ObjectActionExecutiveListener;
import com.openrsc.server.util.rsc.Formulae;

import static com.openrsc.server.plugins.Functions.*;

public class GemMining implements ObjectActionListener,
	ObjectActionExecutiveListener {

	private static final int GEM_ROCK = 588;

	private static final int UNCUT_OPAL = 891;
	private static final int UNCUT_JADE = 890;
	private static final int UNCUT_RED_TOPAZ = 889;
	private static final int UNCUT_SAPPHIRE = 160;
	private static final int UNCUT_EMERALD = 159;
	private static final int UNCUT_RUBY = 158;
	private static final int UNCUT_DIAMOND = 157;

	private static final int[] gemWeightsWithoutDragonstone = {64, 32, 16, 8, 3, 3, 2};
	private static final int[] gemWeightsWithDragonstone = {60, 30, 15, 9, 5, 5, 4};
	private static final int[] gemIds = {
		UNCUT_OPAL,
		UNCUT_JADE,
		UNCUT_RED_TOPAZ,
		UNCUT_SAPPHIRE,
		UNCUT_EMERALD,
		UNCUT_RUBY,
		UNCUT_DIAMOND
	};

	private void handleGemRockMining(final GameObject obj, Player p, int click) {
		if (p.isBusy()) {
			return;
		}
		if (!p.withinRange(obj, 1)) {
			return;
		}
		final ObjectMiningDef def = EntityHandler.getObjectMiningDef(obj.getID());
		final int axeId = getAxe(p);
		int retrytimes = -1;
		final int mineLvl = p.getSkills().getLevel(14);
		int reqlvl = 1;
		switch (axeId) {
			case 156:
				retrytimes = 1;
				break;
			case 1258:
				retrytimes = 2;
				break;
			case 1259:
				retrytimes = 3;
				reqlvl = 6;
				break;
			case 1260:
				retrytimes = 5;
				reqlvl = 21;
				break;
			case 1261:
				retrytimes = 8;
				reqlvl = 31;
				break;
			case 1262:
				retrytimes = 12;
				reqlvl = 41;
				break;
		}

		if (p.click == 1) {
			p.playSound("prospect");
			p.setBusyTimer(1800);
			p.message("You examine the rock for ores...");
			sleep(1800);
			if (obj.getID() == GEM_ROCK) {
				p.message("You fail to find anything interesting");
				return;
			}
			//should not get into the else, just a fail-safe
			else {
				p.message("There is currently no ore available in this rock");
				return;
			}
		}

		if (axeId < 0 || reqlvl > mineLvl) {
			message(p, "You need a pickaxe to mine this rock",
				"You do not have a pickaxe which you have the mining level to use");
			return;
		}

		if (p.getFatigue() >= p.MAX_FATIGUE) {
			p.message("You are too tired to mine this rock");
			return;
		}

		p.playSound("mine");
		showBubble(p, new Item(1258));
		p.message("You have a swing at the rock!");
		p.setBatchEvent(new BatchEvent(p, 1800, 1000 + retrytimes) {
			@Override
			public void action() {
				if (getGem(p, 40, owner.getSkills().getLevel(14), axeId) && mineLvl >= 40) { // always 40 required mining.
					Item gem = new Item(getGemFormula(p.getInventory().wielding(ItemId.CHARGED_DRAGONSTONE_AMULET.id())), 1);
					owner.message(minedString(gem.getID()));
					owner.incExp(14, 200, true); // always 50XP
					owner.getInventory().add(gem);
					interrupt();
					GameObject object = owner.getViewArea().getGameObject(obj.getID(), obj.getX(), obj.getY());
					if (object != null && object.getID() == obj.getID()) {
						GameObject newObject = new GameObject(obj.getLocation(), 98, obj.getDirection(), obj.getType());
						World.getWorld().replaceGameObject(obj, newObject);
						World.getWorld().delayedSpawnObject(object.getLoc(), 120 * 1000); // 2 minutes respawn time
					}
				} else {
					owner.message("You only succeed in scratching the rock");
					if (getRepeatFor() > 1) {
						GameObject checkObj = owner.getViewArea().getGameObject(obj.getID(), obj.getX(), obj.getY());
						if (checkObj == null) {
							interrupt();
						}
					}
				}
				if (!isCompleted()) {
					showBubble(owner, new Item(1258));
					owner.message("You have a swing at the rock!");
				}
			}
		});
	}

	@Override
	public boolean blockObjectAction(GameObject obj, String command, Player p) {
		return obj.getID() == GEM_ROCK && (command.equals("mine") || command.equals("prospect"));
	}

	@Override
	public void onObjectAction(GameObject obj, String command, Player p) {
		if (obj.getID() == GEM_ROCK && (command.equals("mine") || command.equals("prospect"))) {
			handleGemRockMining(obj, p, p.click);
		}
	}

	private int getAxe(Player p) {
		int lvl = p.getSkills().getLevel(14);
		for (int i = 0; i < Formulae.miningAxeIDs.length; i++) {
			if (p.getInventory().countId(Formulae.miningAxeIDs[i]) > 0) {
				if (lvl >= Formulae.miningAxeLvls[i]) {
					return Formulae.miningAxeIDs[i];
				}
			}
		}
		return -1;
	}

	public static int calcAxeBonus(int axeId) {
		int bonus = 0;
		/*switch (axeId) {
			case 156:
				bonus = 0;
				break;
			case 1258:
				bonus = 1;
				break;
			case 1259:
				bonus = 2;
				break;
			case 1260:
				bonus = 4;
				break;
			case 1261:
				bonus = 8;
				break;
			case 1262:
				bonus = 16;
				break;
		}*/
		return bonus;
	}

	private static boolean getGem(Player p, int req, int miningLevel, int axeId) {
		return Formulae.calcGatheringSuccessful(req, miningLevel, calcAxeBonus(axeId));
	}

	/**
	 * Returns a gem ID
	 */
	private int getGemFormula(boolean dragonstoneAmmy) {
		return dragonstoneAmmy ?
			Formulae.weightedRandomChoice(gemIds, gemWeightsWithDragonstone) :
			Formulae.weightedRandomChoice(gemIds, gemWeightsWithoutDragonstone);
	}

	private String minedString(int gemID) {
		if (gemID == UNCUT_OPAL) {
			return "You just mined an Opal!";
		} else if (gemID == UNCUT_JADE) {
			return "You just mined a piece of Jade!";
		} else if (gemID == UNCUT_RED_TOPAZ) {
			return "You just mined a Red Topaz!";
		} else if (gemID == UNCUT_SAPPHIRE) {
			return "You just found a sapphire!";
		} else if (gemID == UNCUT_EMERALD) {
			return "You just found an emerald!";
		} else if (gemID == UNCUT_RUBY) {
			return "You just found a ruby!";
		} else if (gemID == UNCUT_DIAMOND) {
			return "You just found a diamond!";
		}
		return null;
	}
}
