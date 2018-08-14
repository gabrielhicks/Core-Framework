/**
* Generated By NPCScript :: A scripting engine created for openrsc by Zilent
*/
package org.openrsc.server.npchandler.Merlins_Crystal;
import org.openrsc.server.Config;
import org.openrsc.server.event.DelayedQuestChat;
import org.openrsc.server.logging.Logger;
import org.openrsc.server.logging.model.eventLog;
import org.openrsc.server.model.Npc;
import org.openrsc.server.model.Player;
import org.openrsc.server.model.Quest;
import org.openrsc.server.model.Quests;
import org.openrsc.server.model.World;
import org.openrsc.server.npchandler.NpcHandler;
import org.openrsc.server.util.DataConversions;



public class Merlin implements NpcHandler {

	public void handleNpc(final Npc npc, final Player owner) throws Exception {
		npc.blockedBy(owner);
		owner.setBusy(true);
		Quest q = owner.getQuest(Quests.MERLINS_CRYSTAL);
		if(q != null) {
			if(q.finished()) {
				noQuestStarted(npc, owner);
			} else {
				switch(q.getStage()) {
					case 7:
						questStage7(npc, owner);
						break;
					default:
						noQuestStarted(npc, owner);
				}
			}
		} else {
			noQuestStarted(npc, owner);
		}
	}
	

	private void noQuestStarted(final Npc npc, final Player owner) {
		World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, new String[] {"Don't speak to me"}, true) {
			public void finished() {
				owner.setBusy(false);
				npc.unblock();	
			}
		});
	}
		
	private void questStage7(final Npc npc, final Player owner) {
		World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, new String[] {"Thank you for freeing me", "I will see to it that you become apart of our order"}, true) {
			public void finished() {
				owner.finishQuest(Quests.MERLINS_CRYSTAL);
				owner.sendMessage("@gre@You have completed the Merlin's Crystal quest!");
				owner.sendMessage("@gre@You have been awarded 6 quest points!");
				owner.setBusy(false);
				npc.unblock();	
				Logger.log(new eventLog(owner.getUsernameHash(), owner.getAccount(), owner.getIP(), DataConversions.getTimeStamp(), "<strong>" + owner.getUsername() + "</strong>" + " has completed the <span class=\"recent_quest\">Merlin's Crystal</span> quest!"));
			}
		});
	}
	
	
	
}