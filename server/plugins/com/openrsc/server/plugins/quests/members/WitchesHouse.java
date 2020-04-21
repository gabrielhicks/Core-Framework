package com.openrsc.server.plugins.quests.members;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.Quests;
import com.openrsc.server.constants.Skills;
import com.openrsc.server.model.Point;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.Functions;
import com.openrsc.server.plugins.QuestInterface;
import com.openrsc.server.plugins.triggers.*;
import com.openrsc.server.util.rsc.DataConversions;

import java.util.Optional;

import static com.openrsc.server.plugins.Functions.*;

public class WitchesHouse implements QuestInterface, TalkNpcTrigger,
	OpBoundTrigger,
	OpLocTrigger,
	DropObjTrigger,
	UseNpcTrigger,
	KillNpcTrigger,
	TakeObjTrigger,
	AttackNpcTrigger {

	/**
	 * INFORMATION Rat appears on coords: 356, 494 Dropping cheese in the whole
	 * room and rat appears on the same coord Rat is never removed untill you
	 * use magnet room inbounds : MIN X: 356 MAX X: 357 MIN Y: 494 MAX Y: 496
	 */

	private static final int WITCHES_HOUSE_CUPBOARD_OPEN = 259;
	private static final int WITCHES_HOUSE_CUPBOARD_CLOSED = 258;

	@Override
	public int getQuestId() {
		return Quests.WITCHS_HOUSE;
	}

	@Override
	public String getQuestName() {
		return "Witch's house (members)";
	}

	@Override
	public boolean isMembers() {
		return true;
	}

	@Override
	public void handleReward(Player p) {
		p.message("Well done you have completed the Witches house quest");
		p.message("@gre@You haved gained 4 quest points!");
		incQuestReward(p, p.getWorld().getServer().getConstants().getQuests().questData.get(Quests.WITCHS_HOUSE), true);
		p.getCache().remove("witch_gone");
		p.getCache().remove("shapeshifter");
		p.getCache().remove("found_magnet");
	}

	@Override
	public boolean blockTalkNpc(Player p, Npc n) {
		return n.getID() == NpcId.BOY.id();
	}

	@Override
	public void onTalkNpc(Player p, Npc n) {
		if (n.getID() == NpcId.BOY.id()) {
			switch (p.getQuestStage(this)) {
				case 0:
					say(p, n, "Hello young man");
					Functions.mes(p, "The boy sobs");
					int first = multi(p, n, "What's the matter?",
						"Well if you're not going to answer, I'll go");
					if (first == 0) {
						npcsay(p, n,
							"I've kicked my ball over that wall, into that garden",
							"The old lady who lives there is scary",
							"She's locked the ball in her wooden shed",
							"Can you get my ball back for me please");
						int second = multi(p, n, "Ok, I'll see what I can do",
							"Get it back yourself");
						if (second == 0) {
							npcsay(p, n, "Thankyou");
							p.updateQuestStage(getQuestId(), 1);
						} else if (second == 1) {
							// NOTHING
						}
					} else if (first == 1) {
						Functions.mes(p, "The boy sniffs slightly");
					}
					break;
				case 1:
				case 2:
				case 3:
					if (p.getCarriedItems().hasCatalogID(ItemId.BALL.id(), Optional.of(false))) {
						say(p, n, "Hi I have got your ball back",
							"It was harder than I thought it would be");
						npcsay(p, n, "Thankyou very much");
						p.getCarriedItems().remove(new Item(ItemId.BALL.id()));
						if (p.getQuestStage(Quests.WITCHS_HOUSE) == 3) {
							p.sendQuestComplete(Quests.WITCHS_HOUSE);
						}
					} else {
						npcsay(p, n, "Have you got my ball back yet?");
						say(p, n, "Not yet");
						npcsay(p, n, "Well it's in the shed in that garden");
					}
					break;
				case -1:
					npcsay(p, n, "Thankyou for getting my ball back");
					break;
			}
		}
	}

	@Override
	public boolean blockOpBound(GameObject obj, Integer click,
								Player player) {
		return obj.getID() == 69 || (obj.getID() == 70 && obj.getX() == 358) || (obj.getID() == 71 && obj.getY() == 495)
				|| (obj.getID() == 73 && obj.getX() == 351) || (obj.getID() == 72 && obj.getX() == 356);
	}

	@Override
	public void onOpBound(GameObject obj, Integer click, Player p) {
		if (obj.getID() == 69) {
			p.message("The door is locked");
		}
		else if (obj.getID() == 70 && obj.getX() == 358) {
			doDoor(obj, p);
		}
		else if (obj.getID() == 71 && obj.getY() == 495) {
			if (p.getCache().hasKey("witch_spawned") && p.getQuestStage(getQuestId()) == 2) {
				Npc witch = p.getWorld().getNpcById(NpcId.NORA_T_HAG.id());
				if (witch != null) {
					witch.teleport(355, 494);

					npcsay(p, witch, "Oi what are you doing in my garden?");
					npcsay(p, witch, "Get out you pesky intruder");
					Functions.mes(p, "Nora begins to cast a spell");

					p.teleport(347, 616, true);
					Functions.delnpc(witch, false);

					p.getCache().remove("witch_spawned");
					p.updateQuestStage(this, 1);
					p.getCache().remove("found_magnet");
				}
				return;
			}
			if (p.getQuestStage(this) == 2 || p.getQuestStage(getQuestId()) == -1 || p.getX() == 355) {
				doDoor(obj, p);
			} else {
				p.message("The door won't open");
			}
		}

		else if (obj.getID() == 73 && obj.getX() == 351) {
			Npc witch =p. getWorld().getNpcById(NpcId.NORA_T_HAG.id());
			if (p.getQuestStage(this) == 3 || p.getQuestStage(getQuestId()) == -1) {
				doDoor(obj, p);
				return;
			}
			if (!p.getCache().hasKey("witch_spawned")) {
				Functions.mes(p, "As you reach out to open the door you hear footsteps inside the house", "The footsteps approach the back door");
				addnpc(p.getWorld(), NpcId.NORA_T_HAG.id(), 356, 494, 60000);
				p.getCache().store("witch_spawned", true);
			} else {
				Functions.mes(p, "The shed door is locked");
				if (witch == null) {
					return;
				}
				witch.teleport(355, 494);
				npcsay(p, witch, "Oi what are you doing in my garden?");
				npcsay(p, witch, "Get out you pesky intruder");
				Functions.mes(p, "Nora begins to cast a spell");

				p.teleport(347, 616, true);
				Functions.delnpc(witch, false);
				p.updateQuestStage(this, 1);
				p.getCache().remove("found_magnet");
			}
		}
		else if (obj.getID() == 72 && obj.getX() == 356) {
			if (p.getX() <= 355) {
				doDoor(obj, p);
				if (p.getCache().hasKey("witch_spawned")) {
					Npc witch = p.getWorld().getNpcById(NpcId.NORA_T_HAG.id());
					witch.setBusy(true);
					delay(p.getWorld().getServer().getConfig().GAME_TICK * 3);
					p.message("Through a crack in the door, you see a witch enter the garden");
					witch.teleport(353, 492);
					delay(p.getWorld().getServer().getConfig().GAME_TICK * 4);
					witch.teleport(351, 491);
					p.message("The witch disappears into the shed");
					npcsay(p, witch, "How are you tonight my pretty?",
						"Would you like some food?",
						"Just wait there while I get some");
					witch.teleport(353, 492);
					witch.setLocation(Point.location(353, 492), true);
					Functions.mes(p,
						"The witch passes  back through the garden again",
						"Leaving the shed door unlocked");
					delay(p.getWorld().getServer().getConfig().GAME_TICK * 4);

					Functions.delnpc(witch, false);
					p.getCache().remove("witch_spawned");

					p.updateQuestStage(this, 3);
				}
			} else {
				p.teleport(355, 492, false);
			}
		}
	}

	@Override
	public boolean blockOpLoc(GameObject obj, String command,
							  Player player) {
		return obj.getID() == 255 || (obj.getID() == 256 && obj.getX() == 363)
				|| ((obj.getID() == WITCHES_HOUSE_CUPBOARD_OPEN || obj.getID() == WITCHES_HOUSE_CUPBOARD_CLOSED) && obj.getY() == 3328);
	}

	@Override
	public void onOpLoc(GameObject obj, String command, Player p) {
		if (obj.getID() == 255) {
			if (!p.getCarriedItems().hasCatalogID(ItemId.FRONT_DOOR_KEY.id(), Optional.of(false))) {
				p.message("You find a key under the mat");
				give(p, ItemId.FRONT_DOOR_KEY.id(), 1);
			} else {
				p.message("You find nothing interesting");
			}
		}
		else if (obj.getID() == 256 && obj.getX() == 363) {
			boolean shouldShock = false;
			if (wearingMetalArmour(p)) {
				p.message("As your metal armour touches the gate you feel a shock");
				shouldShock = true;
			} else if (!wearingInsulatingGloves(p)) {
				p.message("As your bare hands touch the gate you feel a shock");
				shouldShock = true;
			}
			if (shouldShock) {
				int damage;
				if (p.getSkills().getLevel(Skills.HITS) < 20) {
					damage = DataConversions.getRandom().nextInt(9) + 1;
				} else {
					damage = DataConversions.getRandom().nextInt(14) + 1;
				}
				p.damage(damage);
			} else {
				doGate(p, obj);
			}
		}
		else if ((obj.getID() == WITCHES_HOUSE_CUPBOARD_OPEN || obj.getID() == WITCHES_HOUSE_CUPBOARD_CLOSED) && obj.getY() == 3328) {
			if (command.equalsIgnoreCase("open")) {
				openCupboard(obj, p, WITCHES_HOUSE_CUPBOARD_OPEN);
			} else if (command.equalsIgnoreCase("close")) {
				closeCupboard(obj, p, WITCHES_HOUSE_CUPBOARD_CLOSED);
			} else {
				if (!p.getCarriedItems().hasCatalogID(ItemId.MAGNET.id(), Optional.of(false))) {
					p.message("You find a magnet in the cupboard");
					give(p, ItemId.MAGNET.id(), 1);
					if (p.getQuestStage(this) > 0) {
						p.getCache().store("found_magnet", true);
					}
				} else {
					p.message("You search the cupboard, but find nothing");
				}
			}
		}

	}

	@Override
	public boolean blockDropObj(Player p, Item i, Boolean fromInventory) {
		return i.getCatalogId() == ItemId.CHEESE.id() && p.getLocation().inBounds(356, 357, 494, 496);
	}

	// room inbounds : MIN X: 356 MAX X: 357 MIN Y: 494 MAX Y: 496
	@Override
	public void onDropObj(Player p, Item i, Boolean fromInventory) {
		if (i.getCatalogId() == ItemId.CHEESE.id() && p.getLocation().inBounds(356, 357, 494, 496)) {
			if (p.getQuestStage(this) == -1) {
				say(p, null, "I would rather eat it to be honest");
				return;
			}
			Functions.mes(p, "A rat appears from a hole and eats the cheese");
			addnpc(p.getWorld(), NpcId.RAT_WITCHES_HOUSE.id(), 356, 494, 60000);
		}
	}

	@Override
	public boolean blockUseNpc(Player player, Npc npc, Item item) {
		return item.getCatalogId() == ItemId.MAGNET.id() && npc.getID() == NpcId.RAT_WITCHES_HOUSE.id();
	}

	@Override
	public void onUseNpc(Player p, Npc npc, Item item) {
		if (item.getCatalogId() == ItemId.MAGNET.id() && npc.getID() == NpcId.RAT_WITCHES_HOUSE.id()) {
			if (p.getQuestStage(this) == -1) {
				return;
			}
			if (!p.getCache().hasKey("found_magnet")) {
				p.message("You need to get the magnet yourself to do this quest");
			} else {
				p.message("You put the magnet on the rat");
				Npc rat = p.getWorld().getNpcById(NpcId.RAT_WITCHES_HOUSE.id());
				Functions.delnpc(rat, false);
				Functions.mes(p, "The rat runs back into his hole",
					"You hear a click and whirr");
				p.getCarriedItems().remove(new Item(ItemId.MAGNET.id()));
				p.updateQuestStage(getQuestId(), 2);
			}
		}

	}

	@Override
	public boolean blockKillNpc(Player p, Npc n) {
		return DataConversions.inArray(new int[] {NpcId.SHAPESHIFTER_HUMAN.id(), NpcId.SHAPESHIFTER_SPIDER.id(),
				NpcId.SHAPESHIFTER_BEAR.id(), NpcId.SHAPESHIFTER_WOLF.id()}, n.getID());
	}

	@Override
	public void onKillNpc(Player p, Npc n) {
		n.resetCombatEvent();
		if (n.getID() >= NpcId.SHAPESHIFTER_WOLF.id()) {
			n.killedBy(p);
			p.message("You finally kill the shapeshifter once and for all");
			if (!p.getCache().hasKey("shapeshifter")) {
				p.getCache().store("shapeshifter", true);
			}
			return;
		}
		n.killedBy(p);
		Npc nextShape = addnpc(p.getWorld(), n.getID() + 1, n.getX(), n.getY(), 300000);

		p.message("The shapeshifer turns into a "
			+ npcMessage(nextShape.getID()) + "!");
		nextShape.startCombat(p);
	}

	private String npcMessage(int id) {
		if (id == NpcId.SHAPESHIFTER_SPIDER.id()) {
			return "spider";
		} else if (id == NpcId.SHAPESHIFTER_BEAR.id()) {
			return "bear";
		} else if (id == NpcId.SHAPESHIFTER_WOLF.id()) {
			return "wolf";
		}
		return "";
	}

	@Override
	public void onAttackNpc(Player p, Npc affectedmob) {
		if (affectedmob.getID() == NpcId.SHAPESHIFTER_HUMAN.id() && p.getQuestStage(getQuestId()) == -1) {
			p.message("I have already done that quest");
		}
	}

	@Override
	public boolean blockAttackNpc(Player p, Npc n) {
		if (n.getID() == NpcId.SHAPESHIFTER_HUMAN.id() && p.getQuestStage(getQuestId()) == -1) {
			return true;
		}
		return false;
	}

	@Override
	public boolean blockTakeObj(Player p, GroundItem i) {
		if (i.getID() == ItemId.BALL.id() && i.getX() == 351 && i.getY() == 491) {
			if (p.getQuestStage(getQuestId()) == -1) {
				return true;
			}
			if (!p.getCache().hasKey("shapeshifter")) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onTakeObj(Player p, GroundItem i) {
		if (!p.getCache().hasKey("shapeshifter")) {
			Npc shapeshifter = ifnearvisnpc(p, NpcId.SHAPESHIFTER_HUMAN.id(), 20);
			if (shapeshifter != null) {
				shapeshifter.startCombat(p);
				if (DataConversions.random(0, 3) == 0) weakenPlayer(p);
			}
		} else if (p.getQuestStage(getQuestId()) == -1) {
			say(p, null, "I'd better not take it, its not mine");
		}

	}

	private void weakenPlayer(Player player) {
		player.message("The shapeshifter glares at you");
		//delay of about 2 ticks
		player.message("You feel slightly weakened");
		int[] stats = {Skills.ATTACK, Skills.DEFENSE, Skills.STRENGTH};
		for(int affectedStat : stats) {
			/* How much to lower the stat */
			int lowerBy = (int) Math.ceil(((player.getSkills().getMaxStat(affectedStat) - 4) / 15.0));
			/* New current level */
			final int newStat = Math.max(0, player.getSkills().getLevel(affectedStat) - lowerBy);
			player.getSkills().setLevel(affectedStat, newStat);
		}
	}

	private boolean wearingInsulatingGloves(Player p) {
		return p.getCarriedItems().getEquipment().hasEquipped(ItemId.LEATHER_GLOVES.id()) || p.getCarriedItems().getEquipment().hasEquipped(ItemId.ICE_GLOVES.id());
	}

	//considerations: sq/kite shields, med/large helms, plate-bodies/plate-tops/chains, legs/skirts
	public static final int[] METAL_ARMOURS = {
		//plate bodies
		ItemId.BRONZE_PLATE_MAIL_BODY.id(), ItemId.IRON_PLATE_MAIL_BODY.id(), ItemId.STEEL_PLATE_MAIL_BODY.id(), ItemId.MITHRIL_PLATE_MAIL_BODY.id(), ItemId.ADAMANTITE_PLATE_MAIL_BODY.id(), ItemId.BLACK_PLATE_MAIL_BODY.id(), ItemId.RUNE_PLATE_MAIL_BODY.id(),
		//plate tops
		ItemId.BRONZE_PLATE_MAIL_TOP.id(), ItemId.IRON_PLATE_MAIL_TOP.id(), ItemId.STEEL_PLATE_MAIL_TOP.id(), ItemId.MITHRIL_PLATE_MAIL_TOP.id(), ItemId.ADAMANTITE_PLATE_MAIL_TOP.id(), ItemId.BLACK_PLATE_MAIL_TOP.id(), ItemId.RUNE_PLATE_MAIL_TOP.id(),
		//chains
		ItemId.BRONZE_CHAIN_MAIL_BODY.id(), ItemId.IRON_CHAIN_MAIL_BODY.id(), ItemId.STEEL_CHAIN_MAIL_BODY.id(), ItemId.MITHRIL_CHAIN_MAIL_BODY.id(), ItemId.ADAMANTITE_CHAIN_MAIL_BODY.id(), ItemId.BLACK_CHAIN_MAIL_BODY.id(), ItemId.RUNE_CHAIN_MAIL_BODY.id(),
		//legs
		ItemId.BRONZE_PLATE_MAIL_LEGS.id(), ItemId.IRON_PLATE_MAIL_LEGS.id(), ItemId.STEEL_PLATE_MAIL_LEGS.id(), ItemId.MITHRIL_PLATE_MAIL_LEGS.id(), ItemId.ADAMANTITE_PLATE_MAIL_LEGS.id(), ItemId.BLACK_PLATE_MAIL_LEGS.id(), ItemId.RUNE_PLATE_MAIL_LEGS.id(),
		//skirts
		ItemId.BRONZE_PLATED_SKIRT.id(), ItemId.IRON_PLATED_SKIRT.id(), ItemId.STEEL_PLATED_SKIRT.id(), ItemId.MITHRIL_PLATED_SKIRT.id(), ItemId.ADAMANTITE_PLATED_SKIRT.id(), ItemId.BLACK_PLATED_SKIRT.id(), ItemId.RUNE_SKIRT.id(),
		//medium helmets
		ItemId.MEDIUM_BRONZE_HELMET.id(), ItemId.MEDIUM_IRON_HELMET.id(), ItemId.MEDIUM_STEEL_HELMET.id(), ItemId.MEDIUM_MITHRIL_HELMET.id(), ItemId.MEDIUM_ADAMANTITE_HELMET.id(), ItemId.MEDIUM_BLACK_HELMET.id(), ItemId.MEDIUM_RUNE_HELMET.id(), ItemId.DRAGON_MEDIUM_HELMET.id(),
		//large helmets
		ItemId.LARGE_BRONZE_HELMET.id(), ItemId.LARGE_IRON_HELMET.id(), ItemId.LARGE_STEEL_HELMET.id(), ItemId.LARGE_MITHRIL_HELMET.id(), ItemId.LARGE_ADAMANTITE_HELMET.id(), ItemId.LARGE_BLACK_HELMET.id(), ItemId.LARGE_RUNE_HELMET.id(),
		//square shields
		ItemId.BRONZE_SQUARE_SHIELD.id(), ItemId.IRON_SQUARE_SHIELD.id(), ItemId.STEEL_SQUARE_SHIELD.id(), ItemId.MITHRIL_SQUARE_SHIELD.id(), ItemId.ADAMANTITE_SQUARE_SHIELD.id(), ItemId.BLACK_SQUARE_SHIELD.id(), ItemId.RUNE_SQUARE_SHIELD.id(), ItemId.DRAGON_SQUARE_SHIELD.id(),
		//kite shields
		ItemId.BRONZE_KITE_SHIELD.id(), ItemId.IRON_KITE_SHIELD.id(), ItemId.STEEL_KITE_SHIELD.id(), ItemId.MITHRIL_KITE_SHIELD.id(), ItemId.ADAMANTITE_KITE_SHIELD.id(), ItemId.BLACK_KITE_SHIELD.id(), ItemId.RUNE_KITE_SHIELD.id()
	};

	private boolean wearingMetalArmour(Player p) {
		if (wearingInsulatingGloves(p)) {
			return false;
		}
		boolean isWearingMetal = false;

		for (int itemId : METAL_ARMOURS) {
			isWearingMetal |= p.getCarriedItems().getEquipment().hasEquipped(itemId);
			if (isWearingMetal) break;
		}

		return isWearingMetal;
	}
}
