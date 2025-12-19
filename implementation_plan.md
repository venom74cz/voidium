# Implementation Plan - Ticket Reply & Modern Chat Addon

## User Review Required
> [!IMPORTANT]
> **Complex UI State**: The client now needs to manage distinct "Channels" (Main, Ticket, Groups). This requires a robust client-side state manager (`ChatChannelManager`).
> **Private Groups**: These will be stored on the server to handle routing, but are "session-based" or lightweight unless database storage is requested.
> **Unified HUD**: The unified stream will just be a visual aggregation; the actual data is separated into channels.

## Proposed Changes

### 1. Ticket System Update (Server-Side Support)

While the UI handles the reply, the underlying logic stays similar but exposed via packets.

#### [MODIFY] [TicketManager.java](file:///home/voidivide/Dokumenty/GitHub/voidium/src/main/java/cz/voidium/discord/TicketManager.java)
-   Add `replyToTicket(ServerPlayer player, String message)` method.
-   **New**: `getOpenTicket(ServerPlayer player)` returns the ID/Name of the ticket channel if exists. Used to sync client state.
-   **New**: `getTicketHistory(String channelId, int limit)` returns the last N messages from the Discord channel to send to the client.

### Server-Side Emoji Sync
#### [MODIFY] [EmojiSyncService.java](file:///home/voidivide/Dokumenty/GitHub/voidium/src/main/java/cz/voidium/server/chat/EmojiSyncService.java)
- Implement `fetchStandardEmojis()` method that downloads a robust `emojis.json` from `github.com/iamcal/emoji-data` or similar.
- Parse this JSON to map thousands of shortcodes (e.g., `:rocket:`) to Twemoji URLs.
- Merge this with Discord guild emojis.
- This avoids hardcoding and ensures the client has access to the full standard library.

### Client-Side
#### [MODIFY] [ModernChatScreen.java](file:///home/voidivide/Dokumenty/GitHub/voidium/src/main/java/cz/voidium/client/ui/ModernChatScreen.java)
- Ensure the autocomplete list can handle 3000+ items efficiently (it already uses a simple loop, should be fine for now).

### 2. Modern Chat Client Addon (Multi-Channel Architecture)

#### Features & Architecture

*   **Channel System (Tabs)**:
    *   **Main**: The global/server chat.
    *   **Ticket**: Automatically appears if the user has an open ticket. Messages sent here go directly to the ticket.
    *   **Private Groups**: Created via `+` button. User invites other players.
*   **Persistent History (Sync)**:
    *   **No Empty Chat**: When joining, the chat is **populated** with recent context.
    *   **Mechanism**:
        *   **Main Chat**: Server buffers the last 100 messages in memory. Sends to client on join.
        *   **Ticket Chat**: Server fetches recent messages from Discord via JDA history. Sends to client on join/open.
*   **Focus Mode UI (Press 'T')**:
    *   **Interactive Tabs**: Left/Top sidebar with clickable tabs for switching channels.
    *   **Creation (+)**: Clickable button to open "Create Group" modal.
    *   **Content Area**: Shows messages *only* for the selected channel.
    *   **Input**: Sending a message targets the *selected* channel.
*   **Unified HUD (Passive Mode)**:
    *   Shows **ALL** messages from **ALL** channels in one stream.
    *   **Clickable Source**: Clicking `[Ticket]` or `[Group]` prefix instantly opens Focus Mode and switches to that tab.
*   **Permissions & Discord Sync**:
    *   **PacketSyncPermissions**: On join, sync relevant Discord roles/permissions.
    *   **Respect**: If a user is muted or lacks permission to Send Message in Discord, prevent sending in Main chat (if configured). 
    *   Ticket Tab is only visible if the user has permission to see that ticket (naturally handled by "do they have a ticket").
*   **Inline Media**: (As previously defined) Images/GIFs work in all channels.
*   **Mentions**: (As previously defined) Works across channels.

#### New Packets (`cz.voidium.network`)
*   `PacketSyncChatState`: Server -> Client. Sends list of active channels (e.g., "You have Ticket #5 open", "You are in Group 'Base Builders'").
*   `PacketSyncChatHistory`: Server -> Client. Sends a list of historical messages for a specific channel (Main or Ticket).
*   `PacketClientChat`: Client -> Server. Sends message + **Target Channel ID**.
*   `PacketCreateGroup`: Client -> Server. Request to create a new group.

#### [NEW] `cz.voidium.client.chat.ChatChannelManager`
-   Client-side singleton.
-   Maintains list of `ChatChannel` objects (Name, History, ID).
-   Handles "Unread" indicators for tabs.

#### [NEW] `cz.voidium.client.ui.ModernChatScreen`
-   **Sidebar**: Renders the tabs vertically on the left or horizontally on top.
-   **Blur & Scale**: Applies the focus effects.
-   **Input**: Context-aware (knows which channel is active).
-   **Interaction**: Fully handles mouse clicks for tabs, buttons, and links.

#### [NEW] `cz.voidium.server.chat.ChatHistoryManager`
-   Server-side.
-   Stores a rolling buffer (CircularFifoQueue) of global chat messages.
-   Handles fetching Discord history for tickets.

#### [NEW] `cz.voidium.server.chat.PrivateGroupManager`
-   Server-side.
-   Manages ad-hoc group chats between players.
-   Routes messages to members.

## Verification Plan

### Automated Tests
-   Verify packet serialization for multi-channel chat.

### Manual Verification
1.  **Ticket Integration**:
    *   Create ticket via command.
    *   Observe "Ticket" tab appear in Chat UI.
    *   Select Tab -> Type message -> Verify it goes to Discord ticket.
    *   Receive reply in Discord -> Verify it shows in Ticket Tab (Focus) and with `[Ticket]` prefix in HUD.
2.  **Private Groups**:
    *   Click `+` in Chat UI.
    *   Invite another player.
    *   Type in new Group Tab.
    *   Verify only invited player sees it.
3.  **Unified HUD**:
    *   Receive messages in Main, Ticket, and Group.
    *   Verify all appear in the corner HUD with correct prefixes.
4.  **History Sync**:
    *   Send 5 messages in Main Chat.
    *   Disconnect and Reconnect.
    *   Verify those 5 messages **reappear** in the chat history.
