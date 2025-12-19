Implementation Plan - Ticket Reply & Modern Chat Addon
User Review Required
IMPORTANT

Complex UI State: The client now needs to manage distinct "Channels" (Main, Ticket, Groups). This requires a robust client-side state manager (ChatChannelManager). Private Groups: These will be stored on the server to handle routing, but are "session-based" or lightweight unless database storage is requested. Unified HUD: The unified stream will just be a visual aggregation; the actual data is separated into channels.

Proposed Changes
1. Ticket System Update (Server-Side Support)
While the UI handles the reply, the underlying logic stays similar but exposed via packets.

[MODIFY] 
TicketManager.java
Add replyToTicket(ServerPlayer player, String message) method.
New: getOpenTicket(ServerPlayer player) returns the ID/Name of the ticket channel if exists. Used to sync client state.
2. Modern Chat Client Addon (Multi-Channel Architecture)
Features & Architecture
Channel System (Tabs):
Main: The global/server chat.
Ticket: Automatically appears if the user has an open ticket. Messages sent here go directly to the ticket.
Private Groups: Created via + button. User invites other players.
Focus Mode UI (Press 'T'):
Sidebar/Tabs: List of active channels (Main, [Ticket #123], [Group: Builders]).
Creation (+): Button to create a new private group and invite players via a dropdown.
Content Area: Shows messages only for the selected channel.
Input: Sending a message targets the selected channel.
Unified HUD (Passive Mode):
Shows ALL messages from ALL channels in one stream.
Source Indicators:
Main: No prefix (or optional).
Ticket: [Ticket] prefix (clickable -> opens Focus Mode on that tab).
Group: [Group] prefix.
Permissions & Discord Sync:
PacketSyncPermissions: On join, sync relevant Discord roles/permissions.
Respect: If a user is muted or lacks permission to Send Message in Discord, prevent sending in Main chat (if configured).
Ticket Tab is only visible if the user has permission to see that ticket (naturally handled by "do they have a ticket").
Inline Media: (As previously defined) Images/GIFs work in all channels.
Mentions: (As previously defined) Works across channels.
New Packets (cz.voidium.network)
PacketSyncChatState: Server -> Client. Sends list of active channels (e.g., "You have Ticket #5 open", "You are in Group 'Base Builders'").
PacketClientChat: Client -> Server. Sends message + Target Channel ID.
PacketCreateGroup: Client -> Server. Request to create a new group.
[NEW] cz.voidium.client.chat.ChatChannelManager
Client-side singleton.
Maintains list of ChatChannel objects (Name, History, ID).
Handles "Unread" indicators for tabs.
[NEW] cz.voidium.client.ui.ModernChatScreen
Sidebar: Renders the tabs vertically on the left or horizontally on top.
Blur & Scale: Applies the focus effects.
Input: Context-aware (knows which channel is active).
[NEW] cz.voidium.server.chat.PrivateGroupManager
Server-side.
Manages ad-hoc group chats between players.
Routes messages to members.
Verification Plan
Automated Tests
Verify packet serialization for multi-channel chat.
Manual Verification
Ticket Integration:
Create ticket via command.
Observe "Ticket" tab appear in Chat UI.
Select Tab -> Type message -> Verify it goes to Discord ticket.
Receive reply in Discord -> Verify it shows in Ticket Tab (Focus) and with [Ticket] prefix in HUD.
Private Groups:
Click + in Chat UI.
Invite another player.
Type in new Group Tab.
Verify only invited player sees it.
Unified HUD:
Receive messages in Main, Ticket, and Group.
Verify all appear in the corner HUD with correct prefixes.