package com.openrsc.server.net.rsc.handlers;

import com.openrsc.server.model.MenuOptionListener;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.net.Packet;
import com.openrsc.server.net.rsc.PacketHandler;

public class MenuReplyHandler implements PacketHandler {

	public void handlePacket(Packet packet, final Player player) throws Exception {
		final MenuOptionListener menuHandler = player.getMenuHandler();
		final int option = packet.readByte();
		// NO game content code should be run in handleReply. This is to ensure Functions.sleep() is never called on the main game thread
		if (player.getMenu() != null) {
			player.getMenu().handleReply(player, option);
		} else if (menuHandler != null) {
			if (option == -1) {
				menuHandler.handleReply(option, null);
				return;
			}
			final String reply = option == 30 ? ""
				: menuHandler.getOption(option);
			player.resetMenuHandler();
			if (reply == null) {
				player.setSuspiciousPlayer(true, "menu reply with null reply");
			} else {
				menuHandler.handleReply(option, reply);
			}
		}
	}
}
