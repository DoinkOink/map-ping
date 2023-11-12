package com.doinkoink.messages;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@EqualsAndHashCode(callSuper = true)
public class MapPin extends PartyMemberMessage {
	private final WorldPoint mapPoint;
	private final String member;
}
