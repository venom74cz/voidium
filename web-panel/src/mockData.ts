import type {
  AiSuggestResult,
  ConfigApplyResult,
  ConfigDiffChange,
  ConfigDiffResult,
  ConfigField,
  ConfigPreviewResult,
  ConfigSchema,
  ConfigValues,
  DashboardData,
  DimensionHeatmap,
  DiscordRole,
  PlayerAiConversation,
} from './types'

const MASKED_SECRET = '******'
const CONFIG_FILE_NAMES = [
  'general.json',
  'web.json',
  'announcements.json',
  'restart.json',
  'ranks.json',
  'playerlist.json',
  'tickets.json',
  'discord.json',
  'stats.json',
  'votes.json',
  'entitycleaner.json',
  'ai.json',
]

type SectionKey =
  | 'general'
  | 'web'
  | 'announcements'
  | 'restart'
  | 'ranks'
  | 'playerlist'
  | 'tickets'
  | 'discord'
  | 'stats'
  | 'vote'
  | 'entitycleaner'
  | 'ai'

interface DemoState {
  configValues: ConfigValues
  history: ConfigValues[]
  players: DashboardData['players']
  tickets: DashboardData['tickets']['items']
  votePlayers: DashboardData['voteQueue']['players']
  consoleFeed: DashboardData['consoleFeed']
  auditFeed: DashboardData['auditFeed']
  chatFeed: DashboardData['chatFeed']
  serverProperties: { properties: Record<string, string> }
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function field(
  key: string,
  label: string,
  type: ConfigField['type'],
  description: string,
  options?: string[],
): ConfigField {
  return {
    key,
    label,
    type,
    description,
    ...(options ? { options } : {}),
  }
}

export const mockConfigSchema: ConfigSchema = {
  general: {
    label: 'General',
    fields: [
      field('enableMod', 'Enable mod', 'boolean', 'Master power switch for the whole mod.'),
      field('enableRestarts', 'Automatic restarts', 'boolean', 'Controls scheduled restart automation.'),
      field('enableAnnouncements', 'Announcements', 'boolean', 'Enables automatic broadcast messages.'),
      field('enableSkinRestorer', 'Skin Restorer', 'boolean', 'Fetches real skins in offline mode.'),
      field('enableDiscord', 'Discord integration', 'boolean', 'Controls Discord bot and bridge features.'),
      field('enableWeb', 'Web UI', 'boolean', 'Controls the web control panel availability.'),
      field('enableStats', 'Statistics', 'boolean', 'Enables tracking and report generation.'),
      field('enableRanks', 'Ranks', 'boolean', 'Enables the automatic playtime rank system.'),
      field('enableVote', 'Vote manager', 'boolean', 'Enables the vote listener and reward processing.'),
      field('enablePlayerList', 'Custom player list', 'boolean', 'Enables TAB list enhancements.'),
      field('maintenanceMode', 'Maintenance mode', 'boolean', 'Blocks player login and shows dashboard banner.'),
      field('skinCacheHours', 'Skin cache hours', 'number', 'Lower values refresh skins more often.'),
      field('modPrefix', 'Mod prefix', 'text', 'Shown in Voidium messages. Supports & color codes.'),
    ],
  },
  web: {
    label: 'Web',
    restartRequired: true,
    fields: [
      field('port', 'Port', 'number', 'HTTP port used by the Voidium panel.'),
      field('language', 'Language', 'select', 'Current WebUI locale.', ['en', 'cz']),
      field('publicHostname', 'Public hostname', 'text', 'Address shown in generated access links.'),
      field('bindAddress', 'Bind address', 'text', 'Network interface used by the embedded web server.'),
      field('adminToken', 'Persistent admin token', 'secret', 'Static token for repeated admin access.'),
      field('sessionTtlMinutes', 'Session TTL', 'number', 'How long the session cookie remains valid.'),
    ],
  },
  announcements: {
    label: 'Announcements',
    fields: [
      field('prefix', 'Prefix', 'text', 'Added before each announcement line.'),
      field('announcementIntervalMinutes', 'Interval minutes', 'number', 'Set 0 to disable automatic broadcasts.'),
      field('announcements', 'Announcement lines', 'multiline-list', 'One line per announcement.'),
    ],
  },
  restart: {
    label: 'Restart',
    fields: [
      field('restartType', 'Restart type', 'select', 'Controls how restarts are scheduled.', ['FIXED_TIME', 'INTERVAL', 'DELAY']),
      field('fixedRestartTimes', 'Fixed restart times', 'multiline-list', 'One HH:MM time per line.'),
      field('intervalHours', 'Interval hours', 'number', 'Used when restart type is INTERVAL.'),
      field('delayMinutes', 'Delay minutes', 'number', 'Used when restart type is DELAY.'),
      field('warningMessage', 'Warning message', 'text', 'Supports %minutes% placeholder.'),
      field('restartingNowMessage', 'Restarting now', 'text', 'Shown when restart begins.'),
      field('kickMessage', 'Kick message', 'text', 'Shown when players are disconnected.'),
    ],
  },
  ranks: {
    label: 'Ranks',
    fields: [
      field('enableAutoRanks', 'Enable auto ranks', 'boolean', 'Turns on automatic rank checks.'),
      field('checkIntervalMinutes', 'Check interval', 'number', 'How often rank promotion checks run.'),
      field('countAfkTime', 'Count AFK time', 'boolean', 'Include AFK time in playtime-based rank progress.'),
      field('promotionMessage', 'Promotion message', 'text', 'Supports %rank% placeholder.'),
      field('tooltipPlayed', 'Tooltip played', 'text', 'Hover text for current playtime.'),
      field('tooltipRequired', 'Tooltip required', 'text', 'Hover text for required playtime.'),
      field('ranks', 'Rank definitions', 'rank-list', 'Structured rank editor with type, value, hours, and optional custom conditions JSON.'),
    ],
  },
  playerlist: {
    label: 'Player List',
    fields: [
      field('enableCustomPlayerList', 'Enable custom list', 'boolean', 'Turns on header/footer rendering.'),
      field('headerLine1', 'Header line 1', 'text', 'Supports placeholders like %online% and %max%.'),
      field('headerLine2', 'Header line 2', 'text', 'Supports placeholders like %tps%.'),
      field('headerLine3', 'Header line 3', 'text', 'Optional third header line.'),
      field('footerLine1', 'Footer line 1', 'text', 'Optional first footer line.'),
      field('footerLine2', 'Footer line 2', 'text', 'Optional second footer line.'),
      field('footerLine3', 'Footer line 3', 'text', 'Optional third footer line.'),
      field('enableCustomNames', 'Custom player names', 'boolean', 'Turns on formatted TAB player names.'),
      field('playerNameFormat', 'Player name format', 'text', 'Supports %rank_prefix%, %player_name%, %rank_suffix%.'),
      field('defaultPrefix', 'Default prefix', 'text', 'Used when no prefix is available.'),
      field('defaultSuffix', 'Default suffix', 'text', 'Used when no suffix is available.'),
      field('combineMultipleRanks', 'Combine multiple ranks', 'boolean', 'When enabled, multiple rank parts are merged.'),
      field('updateIntervalSeconds', 'Update interval', 'number', 'Minimum safe interval is 3 seconds.'),
    ],
  },
  tickets: {
    label: 'Tickets',
    fields: [
      field('enableTickets', 'Enable tickets', 'boolean', 'Turns the Discord ticket workflow on or off.'),
      field('ticketCategoryId', 'Ticket category ID', 'text', 'Discord category where ticket channels are created.'),
      field('supportRoleId', 'Support role ID', 'text', 'Discord role granted visibility to ticket channels.'),
      field('enableAutoAssign', 'Enable auto-assign', 'boolean', 'Assign new tickets to the support member with the fewest active tickets.'),
      field('assignedMessage', 'Assigned message', 'text', 'Shown when a support member is auto-assigned. Supports %assignee% placeholder.'),
      field('ticketChannelTopic', 'Ticket channel topic', 'text', 'Supports %user% and %reason% placeholders.'),
      field('maxTicketsPerUser', 'Max tickets per user', 'number', 'Maximum number of open tickets allowed per user.'),
      field('ticketCreatedMessage', 'Ticket created message', 'text', 'Discord message shown after a ticket is created.'),
      field('ticketWelcomeMessage', 'Ticket welcome message', 'text', 'Welcome text sent to a newly created ticket channel.'),
      field('ticketCloseMessage', 'Ticket close message', 'text', 'Message sent when the ticket is being closed.'),
      field('noPermissionMessage', 'No permission message', 'text', 'Shown when a user tries a ticket action without permission.'),
      field('ticketLimitReachedMessage', 'Ticket limit reached', 'text', 'Shown when the user already has too many open tickets.'),
      field('ticketAlreadyClosedMessage', 'Ticket already closed', 'text', 'Shown when a close action targets an already closed ticket.'),
      field('enableTranscript', 'Enable transcript', 'boolean', 'Upload a transcript file when the ticket closes.'),
      field('transcriptFormat', 'Transcript format', 'select', 'Select TXT or JSON transcript output.', ['TXT', 'JSON']),
      field('transcriptFilename', 'Transcript filename', 'text', 'Supports %user%, %date%, and %reason% placeholders.'),
      field('mcBotNotConnectedMessage', 'MC bot not connected', 'text', 'In-game message shown when the Discord bot is offline.'),
      field('mcGuildNotFoundMessage', 'MC guild not found', 'text', 'In-game message shown when the configured guild cannot be found.'),
      field('mcCategoryNotFoundMessage', 'MC category not found', 'text', 'In-game message shown when the ticket category is missing.'),
      field('mcTicketCreatedMessage', 'MC ticket created', 'text', 'In-game confirmation shown after ticket creation succeeds.'),
      field('mcDiscordNotFoundMessage', 'MC Discord not found', 'text', 'In-game message shown when the linked Discord user is missing.'),
    ],
  },
  discord: {
    label: 'Discord',
    fields: [
      field('enableDiscord', 'Enable Discord', 'boolean', 'Turns on Discord integration for the server.'),
      field('botToken', 'Bot token', 'secret', 'Discord bot token used to log in the bot account.'),
      field('guildId', 'Guild ID', 'text', 'Discord server ID used by the bot and whitelist flow.'),
      field('botActivityType', 'Bot activity type', 'select', 'Discord activity type shown on the bot profile.', ['PLAYING', 'WATCHING', 'LISTENING', 'COMPETING']),
      field('botActivityText', 'Bot activity text', 'text', 'Discord activity message shown under the bot name.'),
      field('enableWhitelist', 'Enable whitelist', 'boolean', 'Requires players to verify through Discord before joining.'),
      field('kickMessage', 'Whitelist kick message', 'text', 'Shown in Minecraft when an unverified player is rejected.'),
      field('verificationHintMessage', 'Verification hint', 'text', 'Hint shown in Minecraft with the verification code.'),
      field('linkSuccessMessage', 'Link success message', 'text', 'Discord response shown when linking succeeds.'),
      field('alreadyLinkedMessage', 'Already linked message', 'text', 'Discord response when the account limit is reached.'),
      field('maxAccountsPerDiscord', 'Max accounts per Discord', 'number', 'Maximum number of Minecraft accounts linked to one Discord account.'),
      field('chatChannelId', 'Chat channel ID', 'text', 'Discord channel ID for chat bridge output.'),
      field('consoleChannelId', 'Console channel ID', 'text', 'Discord channel ID for console output.'),
      field('linkChannelId', 'Link channel ID', 'text', 'Discord channel ID where linking commands are allowed.'),
      field('statusChannelId', 'Status channel ID', 'text', 'Discord channel ID for status updates. Falls back when blank.'),
      field('enableStatusMessages', 'Enable status messages', 'boolean', 'Send start, stop, and restart lifecycle messages to Discord.'),
      field('statusMessageStarting', 'Status starting', 'text', 'Message sent when the server starts booting.'),
      field('statusMessageStarted', 'Status started', 'text', 'Message sent when the server is fully online.'),
      field('statusMessageStopping', 'Status stopping', 'text', 'Message sent when the server begins stopping.'),
      field('statusMessageStopped', 'Status stopped', 'text', 'Message sent when the server is offline.'),
      field('linkedRoleId', 'Linked role ID', 'text', 'Role granted after a successful Minecraft to Discord link.'),
      field('renameOnLink', 'Rename on link', 'boolean', 'Rename Discord members after they link their Minecraft account.'),
      field('rolePrefixes', 'Role style map', 'discord-role-style-list', 'Structured Discord role style editor for prefix, suffix, color, and priority by role ID.'),
      field('useHexColors', 'Use hex colors', 'boolean', 'Use RGB hex colors for Discord role styles when supported.'),
      field('syncBansDiscordToMc', 'Sync bans Discord to MC', 'boolean', 'Mirror Discord moderation bans into Minecraft.'),
      field('syncBansMcToDiscord', 'Sync bans MC to Discord', 'boolean', 'Mirror Minecraft bans into Discord moderation.'),
      field('enableChatBridge', 'Enable chat bridge', 'boolean', 'Forward chat messages between Minecraft and Discord.'),
      field('minecraftToDiscordFormat', 'Minecraft to Discord format', 'text', 'Format used when Minecraft chat is relayed to Discord.'),
      field('discordToMinecraftFormat', 'Discord to Minecraft format', 'text', 'Format used when Discord chat is relayed to Minecraft.'),
      field('translateEmojis', 'Translate emojis', 'boolean', 'Translate Discord emojis into Minecraft-friendly text.'),
      field('chatWebhookUrl', 'Chat webhook URL', 'text', 'Optional Discord webhook URL for rich chat delivery.'),
      field('enableTopicUpdate', 'Enable topic update', 'boolean', 'Update channel topic with online count and uptime.'),
      field('channelTopicFormat', 'Channel topic format', 'text', 'Supports %online%, %max%, and %uptime% placeholders.'),
      field('uptimeFormat', 'Uptime format', 'text', 'Formatting used inside the Discord topic uptime value.'),
      field('invalidCodeMessage', 'Invalid code message', 'text', 'Discord response for invalid or expired verification codes.'),
      field('notLinkedMessage', 'Not linked message', 'text', 'Discord response when no linked account exists.'),
      field('alreadyLinkedSingleMessage', 'Already linked single', 'text', 'Discord response when one account is already linked.'),
      field('alreadyLinkedMultipleMessage', 'Already linked multiple', 'text', 'Discord response when multiple accounts are linked.'),
      field('unlinkSuccessMessage', 'Unlink success message', 'text', 'Discord response after unlinking completes.'),
      field('wrongGuildMessage', 'Wrong guild message', 'text', 'Discord response for commands executed in the wrong server.'),
      field('ticketCreatedMessage', 'Ticket created message', 'text', 'Discord response after a ticket channel is created.'),
      field('ticketClosingMessage', 'Ticket closing message', 'text', 'Discord response shown while a ticket is closing.'),
      field('textChannelOnlyMessage', 'Text channel only message', 'text', 'Discord response when a command is used outside a text channel.'),
    ],
  },
  stats: {
    label: 'Stats',
    fields: [
      field('enableStats', 'Enable stats', 'boolean', 'Turns scheduled statistics reports on or off.'),
      field('reportChannelId', 'Report channel ID', 'text', 'Discord channel ID used for daily stats reports.'),
      field('reportTime', 'Report time', 'text', 'Daily report time in HH:MM format.'),
      field('reportTitle', 'Report title', 'text', 'Supports %date% placeholder.'),
      field('reportPeakLabel', 'Peak label', 'text', 'Label for peak players in the report.'),
      field('reportAverageLabel', 'Average label', 'text', 'Label for average players in the report.'),
      field('reportFooter', 'Report footer', 'text', 'Footer text shown in the stats report.'),
    ],
  },
  vote: {
    label: 'Vote',
    fields: [
      field('enabled', 'Enable vote listener', 'boolean', 'Master switch for the Votifier listener.'),
      field('host', 'Host', 'text', 'Interface address used by the vote listener.'),
      field('port', 'Port', 'number', 'Listener port for vote events.'),
      field('rsaPrivateKeyPath', 'RSA private key path', 'text', 'Path to the private key relative to config/voidium.'),
      field('rsaPublicKeyPath', 'RSA public key path', 'text', 'Path for the public key file relative to config/voidium.'),
      field('sharedSecret', 'Shared secret', 'secret', 'NuVotifier shared secret used for token validation.'),
      field('announceVotes', 'Announce votes', 'boolean', 'Broadcast a message when a vote is paid out.'),
      field('announcementMessage', 'Announcement message', 'text', 'Supports %PLAYER% placeholder.'),
      field('announcementCooldown', 'Announcement cooldown', 'number', 'Cooldown in seconds between vote announcements.'),
      field('maxVoteAgeHours', 'Max vote age hours', 'number', 'Votes older than this are ignored.'),
      field('commands', 'Reward commands', 'multiline-list', 'One server command per line. Supports %PLAYER%.'),
      field('logging.voteLog', 'Vote log file', 'boolean', 'Write plain vote records to a log file.'),
      field('logging.voteLogFile', 'Vote log path', 'text', 'Path to the vote log file in storage.'),
      field('logging.archiveJson', 'Archive JSON', 'boolean', 'Append vote history as NDJSON.'),
      field('logging.archivePath', 'Archive path', 'text', 'Path to the vote history archive.'),
      field('logging.notifyOpsOnError', 'Notify ops on error', 'boolean', 'Warn operators when vote processing fails.'),
      field('logging.pendingQueueFile', 'Pending queue path', 'text', 'Storage file for pending offline votes.'),
      field('logging.pendingVoteMessage', 'Pending vote message', 'text', 'Shown when pending votes are paid out.'),
    ],
  },
  entitycleaner: {
    label: 'EntityCleaner',
    fields: [
      field('enabled', 'Enable cleaner', 'boolean', 'Turns automatic entity cleanup on or off.'),
      field('cleanupIntervalSeconds', 'Cleanup interval', 'number', 'Time between cleanups in seconds.'),
      field('warningTimes', 'Warning times', 'multiline-list', 'One number of seconds per line before cleanup.'),
      field('removeDroppedItems', 'Remove dropped items', 'boolean', 'Remove dropped item entities.'),
      field('removePassiveMobs', 'Remove passive mobs', 'boolean', 'Remove animals during cleanup.'),
      field('removeHostileMobs', 'Remove hostile mobs', 'boolean', 'Remove hostile mobs during cleanup.'),
      field('removeXpOrbs', 'Remove XP orbs', 'boolean', 'Remove XP orbs during cleanup.'),
      field('removeArrows', 'Remove arrows', 'boolean', 'Remove arrows stuck in blocks.'),
      field('removeNamedEntities', 'Remove named entities', 'boolean', 'When false, named entities are protected.'),
      field('removeTamedAnimals', 'Remove tamed animals', 'boolean', 'When false, tamed animals are protected.'),
      field('protectBosses', 'Protect bosses', 'boolean', 'When enabled, vanilla and detectable modded bosses are never removed.'),
      field('entityWhitelist', 'Entity whitelist', 'multiline-list', 'One entity ID per line that is never removed.'),
      field('itemWhitelist', 'Item whitelist', 'multiline-list', 'One item ID per line that is never removed.'),
      field('warningMessage', 'Warning message', 'text', 'Supports %seconds% placeholder.'),
      field('cleanupMessage', 'Cleanup message', 'text', 'Supports %items%, %mobs%, %xp%, %arrows%.'),
    ],
  },
  ai: {
    label: 'AI',
    fields: [
      field('enablePlayerChat', 'Enable player AI', 'boolean', 'Turns on /ai for players.'),
      field('playerAccessMode', 'Player access mode', 'select', 'Controls whether player AI is open, time-gated, Discord-role gated, or both.', ['ALL', 'PLAYTIME', 'DISCORD_ROLE', 'PLAYTIME_OR_DISCORD_ROLE', 'PLAYTIME_AND_DISCORD_ROLE']),
      field('playerAccessMinHours', 'Minimum played hours', 'number', 'Used for playtime-based AI access. Set 0 to disable hour gating.'),
      field('playerAccessDiscordRoleIds', 'Allowed Discord role IDs', 'multiline-list', 'One Discord role ID per line for AI access gating.'),
      field('disabledWorlds', 'Disabled worlds', 'multiline-list', 'Dimension IDs where /ai is blocked. One per line.'),
      field('disabledGameModes', 'Disabled game modes', 'multiline-list', 'Game modes where /ai is blocked. One per line.'),
      field('enableAdminAssistant', 'Enable admin assistant', 'boolean', 'Turns on WebUI admin AI.'),
      field('redactSensitiveValues', 'Redact secrets', 'boolean', 'Masks sensitive values before admin AI sees config context.'),
      field('playerPromptMaxLength', 'Player prompt max', 'number', 'Maximum length of a player /ai prompt.'),
      field('playerCooldownSeconds', 'Player cooldown', 'number', 'Cooldown between player AI requests.'),
      field('adminPromptMaxLength', 'Admin prompt max', 'number', 'Maximum length of an admin AI prompt.'),
      field('adminContextMaxChars', 'Admin context max', 'number', 'Maximum size of context passed to admin AI.'),
      field('playerApi.endpointUrl', 'Player endpoint URL', 'text', 'OpenAI-compatible endpoint for player chat.'),
      field('playerApi.apiKey', 'Player API key', 'secret', 'Stored server-side for player AI.'),
      field('playerApi.authHeaderName', 'Player auth header', 'text', 'Authorization header name used for player AI requests.'),
      field('playerApi.authHeaderPrefix', 'Player auth prefix', 'text', 'Prefix prepended before the player API key.'),
      field('playerApi.model', 'Player model', 'text', 'Model name used for player chat.'),
      field('playerApi.systemPrompt', 'Player system prompt', 'text', 'Instructions for player AI replies.'),
      field('playerApi.temperature', 'Player temperature', 'number', 'Sampling temperature for player chat.'),
      field('playerApi.maxTokens', 'Player max tokens', 'number', 'Max generated tokens for player AI.'),
      field('playerApi.timeoutSeconds', 'Player timeout', 'number', 'Request timeout in seconds for player AI.'),
      field('adminApi.endpointUrl', 'Admin endpoint URL', 'text', 'OpenAI-compatible endpoint for admin AI.'),
      field('adminApi.apiKey', 'Admin API key', 'secret', 'Stored server-side for admin AI.'),
      field('adminApi.authHeaderName', 'Admin auth header', 'text', 'Authorization header name used for admin AI requests.'),
      field('adminApi.authHeaderPrefix', 'Admin auth prefix', 'text', 'Prefix prepended before the admin API key.'),
      field('adminApi.model', 'Admin model', 'text', 'Model name used for admin AI.'),
      field('adminApi.systemPrompt', 'Admin system prompt', 'text', 'Instructions for admin AI replies.'),
      field('adminApi.temperature', 'Admin temperature', 'number', 'Sampling temperature for admin AI.'),
      field('adminApi.maxTokens', 'Admin max tokens', 'number', 'Max generated tokens for admin AI.'),
      field('adminApi.timeoutSeconds', 'Admin timeout', 'number', 'Request timeout in seconds for admin AI.'),
    ],
  },
}

type DemoLocale = 'en' | 'cz'

const DEMO_LOCALE_PRESETS: Record<DemoLocale, Partial<Record<SectionKey, Record<string, unknown>>>> = {
  en: {
    general: {
      modPrefix: '&8[&5VOIDIUM&8]&r ',
    },
    web: {
      language: 'en',
    },
    announcements: {
      prefix: '&8[&dVOIDIUM&8]&r ',
      announcements: [
        '&dJoin our Discord for support, synced perks, and live events.',
        '&bUse &f/voidium web &bto open the live admin panel safely.',
        '&aVote daily to keep your reward queue and bonus perks flowing.',
      ],
    },
    restart: {
      warningMessage: '&cServer restart in %minutes% minutes!',
      restartingNowMessage: '&cServer is restarting now!',
      kickMessage: '&cServer is restarting. Please reconnect in a few minutes.',
    },
    ranks: {
      promotionMessage: '&aCongratulations %player%! After &f%hours%h&a you unlocked &b%rank%&a.',
      tooltipPlayed: '&7Played: &b%hours%h',
      tooltipRequired: '&7Next tier at: &d%hours%h',
    },
    playerlist: {
      headerLine1: '&5&l✦ VOID-BOX ✦',
      headerLine2: '&7Online: &a%online%&7/&a%max%',
      headerLine3: '&7World TPS: &b%tps%',
      footerLine1: '&7Ping: &e%ping%ms',
      footerLine2: '&7Powered by &dVOIDIUM',
      footerLine3: '&7VOID-CRAFT.EU',
    },
    tickets: {
      assignedMessage: '&7Assigned to &b%assignee%&7.',
      ticketChannelTopic: 'Ticket for %user% | %reason%',
      ticketCreatedMessage: 'Ticket created in %channel%!',
      ticketWelcomeMessage: 'Hello %user%,\nSupport will be with you shortly.\nReason: %reason%',
      ticketCloseMessage: 'Ticket closed by %user%.',
      noPermissionMessage: 'You do not have permission to do this.',
      ticketLimitReachedMessage: 'You have reached the maximum number of open tickets.',
      ticketAlreadyClosedMessage: 'This ticket is already closed.',
      mcBotNotConnectedMessage: '&cDiscord bot is not connected.',
      mcGuildNotFoundMessage: '&cConfigured Discord server was not found.',
      mcCategoryNotFoundMessage: '&cTicket category is not configured!',
      mcTicketCreatedMessage: '&aTicket created on Discord!',
      mcDiscordNotFoundMessage: '&cYour Discord account was not found on the server.',
    },
    discord: {
      kickMessage: '&cYou are not whitelisted!\n&7To join, verify on our Discord first.\n&7Your code: &b%code%',
      verificationHintMessage: '&7Use &b/link &7on Discord and paste the code from Minecraft.',
      linkSuccessMessage: 'Successfully linked account **%player%**!',
      alreadyLinkedMessage: 'This Discord account is already linked to the maximum number of accounts (%max%).',
      minecraftToDiscordFormat: '**%player%** » %message%',
      discordToMinecraftFormat: '&9[Discord] &f%user% &8» &7%message%',
      statusMessageStarting: ':yellow_circle: **Server is starting...**',
      statusMessageStarted: ':green_circle: **Server is online!**',
      statusMessageStopping: ':orange_circle: **Server is stopping...**',
      statusMessageStopped: ':red_circle: **Server is offline.**',
      channelTopicFormat: 'Online: %online%/%max% | Uptime: %uptime% | VOIDIUM',
      uptimeFormat: '%days%d %hours%h %minutes%m',
      invalidCodeMessage: 'Invalid or expired code.',
      notLinkedMessage: 'You are not linked! Enter a valid code from the game.',
      alreadyLinkedSingleMessage: 'You are already linked! UUID: `%uuid%`',
      alreadyLinkedMultipleMessage: 'You are already linked to %count% accounts!',
      unlinkSuccessMessage: 'All linked accounts have been successfully unlinked.',
      wrongGuildMessage: 'This command can only be used on the official Discord server.',
      ticketCreatedMessage: 'Ticket created!',
      ticketClosingMessage: 'Closing ticket...',
      textChannelOnlyMessage: 'This command can only be used in a text channel.',
    },
    stats: {
      reportTitle: 'Daily Statistics - %date%',
      reportPeakLabel: 'Peak players',
      reportAverageLabel: 'Average players',
      reportFooter: 'Voidium Stats',
    },
    vote: {
      announcementMessage: '&b%PLAYER% &7voted for the server and received a reward!',
      'logging.pendingVoteMessage': '&aYour queued vote rewards have been delivered.',
    },
    entitycleaner: {
      warningMessage: '&e[EntityCleaner] &fClearing entities in &c%seconds% &fseconds!',
      cleanupMessage: '&a[EntityCleaner] &fRemoved &e%items% items&f, &e%mobs% mobs&f, &e%xp% XP orbs&f, &e%arrows% arrows&f.',
    },
  },
  cz: {
    general: {
      modPrefix: '&8[&5VOIDIUM CZ&8]&r ',
    },
    web: {
      language: 'cz',
    },
    announcements: {
      prefix: '&8[&dVOIDIUM CZ&8]&r ',
      announcements: [
        '&dPripoj se na Discord pro podporu, eventy a sync benefity.',
        '&bPouzij &f/voidium web &bpro bezpecny vstup do admin panelu.',
        '&aHlasuj kazdy den a drz svou reward queue aktivni.',
      ],
    },
    restart: {
      warningMessage: '&cServer restartuje za %minutes% minut!',
      restartingNowMessage: '&cServer se restartuje prave ted!',
      kickMessage: '&cServer se restartuje. Pripoj se prosim znovu za par minut.',
    },
    ranks: {
      promotionMessage: '&aGratulujeme %player%! Po &f%hours%h&a odemykas rank &b%rank%&a.',
      tooltipPlayed: '&7Nahrano: &b%hours%h',
      tooltipRequired: '&7Dalsi tier pri: &d%hours%h',
    },
    playerlist: {
      headerLine1: '&5&l✦ VOID-BOX CZ ✦',
      headerLine2: '&7Online: &a%online%&7/&a%max%',
      headerLine3: '&7TPS sveta: &b%tps%',
      footerLine1: '&7Ping: &e%ping%ms',
      footerLine2: '&7Pohani &dVOIDIUM',
      footerLine3: '&7VOID-CRAFT.EU',
    },
    tickets: {
      assignedMessage: '&7Prirazeno pro &b%assignee%&7.',
      ticketChannelTopic: 'Ticket pro %user% | %reason%',
      ticketCreatedMessage: 'Ticket vytvoren v %channel%!',
      ticketWelcomeMessage: 'Ahoj %user%,\npodpora se ti bude brzy venovat.\nDuvod: %reason%',
      ticketCloseMessage: 'Ticket uzavren uzivatelem %user%.',
      noPermissionMessage: 'Na tuto akci nemas opravneni.',
      ticketLimitReachedMessage: 'Dosahl jsi maximalniho poctu otevrenych ticketu.',
      ticketAlreadyClosedMessage: 'Tento ticket je uz uzavreny.',
      mcBotNotConnectedMessage: '&cDiscord bot neni pripojen.',
      mcGuildNotFoundMessage: '&cNakonfigurovany Discord server nebyl nalezen.',
      mcCategoryNotFoundMessage: '&cTicket kategorie neni nakonfigurovana!',
      mcTicketCreatedMessage: '&aTicket byl vytvoren na Discordu!',
      mcDiscordNotFoundMessage: '&cTvuj Discord ucet nebyl na serveru nalezen.',
    },
    discord: {
      kickMessage: '&cNejsi na whitelistu!\n&7Pro pripojeni se musis overit na nasem Discordu.\n&7Tvuj kod: &b%code%',
      verificationHintMessage: '&7Pouzij &b/link &7na Discordu a vloz kod ze hry.',
      linkSuccessMessage: 'Uspesne propojen ucet **%player%**!',
      alreadyLinkedMessage: 'Tento Discord ucet je uz propojen s maximem uctu (%max%).',
      minecraftToDiscordFormat: '**%player%** » %message%',
      discordToMinecraftFormat: '&9[Discord] &f%user% &8» &7%message%',
      statusMessageStarting: ':yellow_circle: **Server startuje...**',
      statusMessageStarted: ':green_circle: **Server je online!**',
      statusMessageStopping: ':orange_circle: **Server se vypina...**',
      statusMessageStopped: ':red_circle: **Server je offline.**',
      channelTopicFormat: 'Online: %online%/%max% | Uptime: %uptime% | VOIDIUM',
      uptimeFormat: '%days%d %hours%h %minutes%m',
      invalidCodeMessage: 'Neplatny nebo expirovany kod.',
      notLinkedMessage: 'Nejsi propojen. Zadej platny kod ze hry.',
      alreadyLinkedSingleMessage: 'Jsi uz propojen! UUID: `%uuid%`',
      alreadyLinkedMultipleMessage: 'Jsi uz propojen k %count% uctum!',
      unlinkSuccessMessage: 'Vsechny propojene ucty byly uspesne odpojeny.',
      wrongGuildMessage: 'Tento prikaz lze pouzit jen na oficialnim Discord serveru.',
      ticketCreatedMessage: 'Ticket vytvoren!',
      ticketClosingMessage: 'Zaviram ticket...',
      textChannelOnlyMessage: 'Tento prikaz lze pouzit jen v textovem kanalu.',
    },
    stats: {
      reportTitle: 'Denni statistiky - %date%',
      reportPeakLabel: 'Maximum hracu',
      reportAverageLabel: 'Prumer hracu',
      reportFooter: 'Voidium Stats',
    },
    vote: {
      announcementMessage: '&b%PLAYER% &7hlasoval pro server a ziskal odmenu!',
      'logging.pendingVoteMessage': '&aTvoje odlozene vote odmeny byly doruceny.',
    },
    entitycleaner: {
      warningMessage: '&e[EntityCleaner] &fCisteni entit za &c%seconds% &fsekund!',
      cleanupMessage: '&a[EntityCleaner] &fOdstraneno &e%items% itemu&f, &e%mobs% mobu&f, &e%xp% XP orbu&f, &e%arrows% sipu&f.',
    },
  },
}

function mergeDemoLocalePreset(section: string, locale: string, values: ConfigValues): { message: string; values?: ConfigValues } {
  const normalized = locale.toLowerCase() === 'cz' ? 'cz' : 'en'
  const key = section as SectionKey
  const preset = DEMO_LOCALE_PRESETS[normalized][key]
  const next = clone(values)

  if (!preset) {
    return { message: `No localized preset is defined for ${section} in demo mode.`, values: next }
  }

  next[key] = {
    ...(next[key] || {}),
    ...clone(preset),
  }

  return { message: `Applied ${normalized.toUpperCase()} preset for ${section}.`, values: next }
}

export const mockConfigDefaults: ConfigValues = {
  general: {
    enableMod: true,
    enableRestarts: true,
    enableAnnouncements: true,
    enableSkinRestorer: false,
    enableDiscord: true,
    enableWeb: true,
    enableStats: true,
    enableRanks: true,
    enableVote: true,
    enablePlayerList: true,
    maintenanceMode: false,
    skinCacheHours: 24,
    ...clone(DEMO_LOCALE_PRESETS.en.general || {}),
  },
  web: {
    port: 8888,
    ...clone(DEMO_LOCALE_PRESETS.en.web || {}),
    publicHostname: 'voidium.void-craft.eu',
    bindAddress: '0.0.0.0',
    adminToken: MASKED_SECRET,
    sessionTtlMinutes: 60,
  },
  announcements: {
    announcementIntervalMinutes: 20,
    ...clone(DEMO_LOCALE_PRESETS.en.announcements || {}),
  },
  restart: {
    restartType: 'FIXED_TIME',
    fixedRestartTimes: ['03:00:00'],
    intervalHours: 24,
    delayMinutes: 120,
    ...clone(DEMO_LOCALE_PRESETS.en.restart || {}),
  },
  ranks: {
    enableAutoRanks: true,
    checkIntervalMinutes: 60,
    countAfkTime: false,
    ...clone(DEMO_LOCALE_PRESETS.en.ranks || {}),
    ranks: JSON.stringify([
      { type: 'PREFIX', value: '§7[Settler] ', hours: 1, customConditions: [] },
      { type: 'PREFIX', value: '§b[Builder] ', hours: 12, customConditions: [] },
      { type: 'PREFIX', value: '§d[Voidwalker] ', hours: 48, customConditions: [] },
    ], null, 2),
  },
  playerlist: {
    enableCustomPlayerList: true,
    ...clone(DEMO_LOCALE_PRESETS.en.playerlist || {}),
    enableCustomNames: true,
    playerNameFormat: '%rank_prefix%%player_name%%rank_suffix%',
    defaultPrefix: '§7',
    defaultSuffix: '',
    combineMultipleRanks: true,
    updateIntervalSeconds: 5,
  },
  tickets: {
    enableTickets: true,
    ticketCategoryId: '118200000000000001',
    supportRoleId: '118200000000000021',
    enableAutoAssign: true,
    maxTicketsPerUser: 3,
    enableTranscript: true,
    transcriptFormat: 'TXT',
    transcriptFilename: '%user%-%date%',
    ...clone(DEMO_LOCALE_PRESETS.en.tickets || {}),
  },
  discord: {
    enableDiscord: true,
    botToken: MASKED_SECRET,
    guildId: '118200000000000000',
    botActivityType: 'WATCHING',
    botActivityText: 'VOID-BOX online status',
    enableWhitelist: false,
    maxAccountsPerDiscord: 1,
    chatChannelId: '118200000000000010',
    consoleChannelId: '118200000000000011',
    linkChannelId: '118200000000000012',
    statusChannelId: '118200000000000013',
    enableStatusMessages: true,
    linkedRoleId: '118200000000000030',
    renameOnLink: true,
    rolePrefixes: JSON.stringify({
      '118200000000000040': { prefix: '§c[Owner] ', suffix: '', color: '#ef4444', priority: 100 },
      '118200000000000041': { prefix: '§b[Creator] ', suffix: '', color: '#38bdf8', priority: 80 },
      '118200000000000042': { prefix: '§d[Support] ', suffix: '', color: '#e879f9', priority: 60 },
    }, null, 2),
    useHexColors: true,
    syncBansDiscordToMc: true,
    syncBansMcToDiscord: true,
    enableChatBridge: true,
    translateEmojis: true,
    chatWebhookUrl: '',
    enableTopicUpdate: true,
    ...clone(DEMO_LOCALE_PRESETS.en.discord || {}),
  },
  stats: {
    enableStats: true,
    reportChannelId: '118200000000000020',
    reportTime: '08:00:00',
    ...clone(DEMO_LOCALE_PRESETS.en.stats || {}),
  },
  vote: {
    enabled: true,
    host: '0.0.0.0',
    port: 8192,
    rsaPrivateKeyPath: 'votifier/private.key',
    rsaPublicKeyPath: 'votifier/public.key',
    sharedSecret: MASKED_SECRET,
    announceVotes: true,
    announcementCooldown: 5,
    maxVoteAgeHours: 24,
    commands: [
      'crate key give %PLAYER% vote 1',
      'money give %PLAYER% 500',
    ],
    'logging.voteLog': true,
    'logging.voteLogFile': 'vote.log',
    'logging.archiveJson': true,
    'logging.archivePath': 'votes.ndjson',
    'logging.notifyOpsOnError': true,
    'logging.pendingQueueFile': 'pending_votes.json',
    ...clone(DEMO_LOCALE_PRESETS.en.vote || {}),
  },
  entitycleaner: {
    enabled: true,
    cleanupIntervalSeconds: 300,
    warningTimes: ['60', '30', '10'],
    removeDroppedItems: true,
    removePassiveMobs: false,
    removeHostileMobs: true,
    removeXpOrbs: true,
    removeArrows: true,
    removeNamedEntities: false,
    removeTamedAnimals: false,
    protectBosses: true,
    entityWhitelist: ['minecraft:villager', 'minecraft:armor_stand'],
    itemWhitelist: ['minecraft:nether_star'],
    ...clone(DEMO_LOCALE_PRESETS.en.entitycleaner || {}),
  },
  ai: {
    enablePlayerChat: true,
    playerAccessMode: 'PLAYTIME_OR_DISCORD_ROLE',
    playerAccessMinHours: 6,
    playerAccessDiscordRoleIds: ['118200000000000050'],
    disabledWorlds: ['minecraft:the_end'],
    disabledGameModes: ['spectator'],
    enableAdminAssistant: true,
    redactSensitiveValues: true,
    playerPromptMaxLength: 256,
    playerCooldownSeconds: 30,
    adminPromptMaxLength: 2000,
    adminContextMaxChars: 50000,
    'playerApi.endpointUrl': 'https://api.openai.com/v1/chat/completions',
    'playerApi.apiKey': MASKED_SECRET,
    'playerApi.authHeaderName': 'Authorization',
    'playerApi.authHeaderPrefix': 'Bearer',
    'playerApi.model': 'gpt-4o-mini',
    'playerApi.systemPrompt': 'You are the friendly VOIDIUM helper for players.',
    'playerApi.temperature': 0.7,
    'playerApi.maxTokens': 180,
    'playerApi.timeoutSeconds': 30,
    'adminApi.endpointUrl': 'https://api.openai.com/v1/chat/completions',
    'adminApi.apiKey': MASKED_SECRET,
    'adminApi.authHeaderName': 'Authorization',
    'adminApi.authHeaderPrefix': 'Bearer',
    'adminApi.model': 'gpt-4.1',
    'adminApi.systemPrompt': 'You are the VOIDIUM admin assistant for config and incident analysis.',
    'adminApi.temperature': 0.4,
    'adminApi.maxTokens': 1000,
    'adminApi.timeoutSeconds': 60,
  },
}

export const mockConfigValues: ConfigValues = clone(mockConfigDefaults)

export const mockDiscordRoles: DiscordRole[] = [
  { id: '118200000000000040', name: 'Owner', color: '#EF4444', position: 100 },
  { id: '118200000000000041', name: 'Creator', color: '#38BDF8', position: 80 },
  { id: '118200000000000042', name: 'Support', color: '#E879F9', position: 60 },
  { id: '118200000000000050', name: 'AI Access', color: '#22C55E', position: 40 },
  { id: '118200000000000030', name: 'Linked', color: '#A855F7', position: 20 },
]

export const mockServerProperties = {
  properties: {
    'server-port': '25565',
    'view-distance': '10',
    'simulation-distance': '10',
    'max-players': '120',
    motd: 'VOID-BOX | Premium survival with VOIDIUM systems',
    'online-mode': 'true',
    'spawn-protection': '0',
  },
}

export const mockPlayerAiConversations: PlayerAiConversation[] = [
  {
    uuid: 'b08eb6cf-bd5f-4ba3-8cb1-5d7c2e2fa001',
    turns: 4,
    history: [
      { role: 'user', content: 'How do I unlock the Builder rank?' },
      { role: 'assistant', content: 'Reach 12 hours of playtime and keep progressing through the starter quests.' },
    ],
  },
  {
    uuid: 'b08eb6cf-bd5f-4ba3-8cb1-5d7c2e2fa002',
    turns: 3,
    history: [
      { role: 'user', content: 'Why is The End blocked for AI chat?' },
      { role: 'assistant', content: 'The End is listed in the disabled worlds array for player AI.' },
    ],
  },
]

const basePlayers: DashboardData['players'] = [
  {
    uuid: 'b08eb6cf-bd5f-4ba3-8cb1-5d7c2e2fa001',
    name: 'VOIDIVIDE',
    ping: 12,
    linked: true,
    discordId: '901234567890123456',
  },
  {
    uuid: 'b08eb6cf-bd5f-4ba3-8cb1-5d7c2e2fa002',
    name: 'CraftPilot',
    ping: 28,
    linked: true,
    discordId: '901234567890123457',
  },
  {
    uuid: 'b08eb6cf-bd5f-4ba3-8cb1-5d7c2e2fa003',
    name: 'NetherRider',
    ping: 41,
    linked: false,
    discordId: null,
  },
]

const baseTickets: DashboardData['tickets']['items'] = [
  {
    channelId: '118200000000000101',
    player: 'VOIDIVIDE',
    cachedMessages: 5,
    previewLines: ['Need help with linked perks.', 'Unsure if Discord sync finished.'],
  },
  {
    channelId: '118200000000000102',
    player: 'CraftPilot',
    cachedMessages: 3,
    previewLines: ['Vote reward did not apply yet.'],
  },
]

const baseVotePlayers: DashboardData['voteQueue']['players'] = [
  {
    player: 'VOIDIVIDE',
    count: 2,
    latestService: 'czech-craft',
    latestQueuedAt: '2026-04-07 18:42:00',
    latestVoteAt: '2026-04-07 18:40:00',
    online: true,
  },
  {
    player: 'NetherRider',
    count: 1,
    latestService: 'minecraft-server-list',
    latestQueuedAt: '2026-04-07 17:15:00',
    latestVoteAt: '2026-04-07 17:14:00',
    online: false,
  },
]

const baseConsoleFeed: DashboardData['consoleFeed'] = [
  { level: 'INFO', logger: 'Voidium', message: 'Web panel initialized on port 8888' },
  { level: 'INFO', logger: 'Discord', message: 'Status update pushed to configured channel.' },
  { level: 'WARN', logger: 'Vote', message: '1 offline vote reward remains queued for payout.' },
]

const baseAuditFeed: DashboardData['auditFeed'] = [
  { action: 'Panel booted', detail: 'Demo preview initialized from default config set.', timestamp: '18:30', source: 'Demo', success: true },
  { action: 'Discord sync', detail: 'Linked role prefixes loaded for 3 roles.', timestamp: '18:28', source: 'Discord', success: true },
]

const baseChatFeed: DashboardData['chatFeed'] = [
  { sender: 'VOIDIVIDE', message: 'Need support for linked cosmetics.' },
  { sender: 'CraftPilot', message: 'Vote queue looks healthy today.' },
]

const baseHistory: DashboardData['history'] = [
  { timestamp: 1712505600, tps: 20, players: 11 },
  { timestamp: 1712509200, tps: 19.98, players: 18 },
  { timestamp: 1712512800, tps: 19.96, players: 27 },
  { timestamp: 1712516400, tps: 19.97, players: 36 },
  { timestamp: 1712520000, tps: 19.95, players: 42 },
]

const demoState: DemoState = {
  configValues: clone(mockConfigValues),
  history: [],
  players: clone(basePlayers),
  tickets: clone(baseTickets),
  votePlayers: clone(baseVotePlayers),
  consoleFeed: clone(baseConsoleFeed),
  auditFeed: clone(baseAuditFeed),
  chatFeed: clone(baseChatFeed),
  serverProperties: clone(mockServerProperties),
}

function sectionValues(values: ConfigValues, key: SectionKey): Record<string, unknown> {
  return (values[key] || {}) as Record<string, unknown>
}

function boolValue(value: unknown): boolean {
  return value === true || value === 'true'
}

function numberValue(value: unknown, fallback = 0): number {
  const parsed = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function stringValue(value: unknown, fallback = ''): string {
  return typeof value === 'string' ? value : fallback
}

function listValue(value: unknown): string[] {
  return Array.isArray(value) ? value.map(item => String(item)) : []
}

function sameValue(left: unknown, right: unknown): boolean {
  return JSON.stringify(left) === JSON.stringify(right)
}

function countConfiguredRanks(raw: unknown): number {
  try {
    const parsed = JSON.parse(String(raw || '[]'))
    return Array.isArray(parsed) ? parsed.length : 0
  } catch {
    return 0
  }
}

function appendConsole(level: string, logger: string, message: string) {
  demoState.consoleFeed = [{ level, logger, message }, ...demoState.consoleFeed].slice(0, 12)
}

function appendAudit(action: string, detail: string, source = 'Web', success = true) {
  const timestamp = new Date().toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
  demoState.auditFeed = [{ action, detail, timestamp, source, success }, ...demoState.auditFeed].slice(0, 12)
}

function voteTotal(): number {
  return demoState.votePlayers.reduce((sum, entry) => sum + numberValue(entry.count), 0)
}

function buildModules(values: ConfigValues): DashboardData['modules'] {
  const general = sectionValues(values, 'general')
  const discord = sectionValues(values, 'discord')
  const ai = sectionValues(values, 'ai')
  const vote = sectionValues(values, 'vote')
  const stats = sectionValues(values, 'stats')
  const tickets = sectionValues(values, 'tickets')
  const cleaner = sectionValues(values, 'entitycleaner')
  return [
    { name: 'Web', enabled: boolValue(general.enableWeb), detail: 'Web panel online' },
    { name: 'Discord', enabled: boolValue(general.enableDiscord), detail: boolValue(discord.enableChatBridge) ? 'Bridge and linking active' : 'Integration configured' },
    { name: 'Stats', enabled: boolValue(general.enableStats) && boolValue(stats.enableStats), detail: `Daily report ${stringValue(stats.reportTime, '08:00:00')}` },
    { name: 'Ranks', enabled: boolValue(general.enableRanks), detail: `${countConfiguredRanks(sectionValues(values, 'ranks').ranks)} configured tier(s)` },
    { name: 'Vote', enabled: boolValue(general.enableVote) && boolValue(vote.enabled), detail: `${voteTotal()} pending reward(s)` },
    { name: 'Player List', enabled: boolValue(general.enablePlayerList), detail: 'Custom TAB formatting active' },
    { name: 'Tickets', enabled: boolValue(tickets.enableTickets), detail: `${demoState.tickets.length} open ticket(s)` },
    { name: 'Announcements', enabled: boolValue(general.enableAnnouncements), detail: `${listValue(sectionValues(values, 'announcements').announcements).length} scheduled line(s)` },
    { name: 'Restarts', enabled: boolValue(general.enableRestarts), detail: `Mode ${stringValue(sectionValues(values, 'restart').restartType, 'FIXED_TIME')}` },
    { name: 'Entity Cleaner', enabled: boolValue(cleaner.enabled), detail: `Every ${numberValue(cleaner.cleanupIntervalSeconds, 0)}s` },
    { name: 'AI', enabled: boolValue(ai.enableAdminAssistant) || boolValue(ai.enablePlayerChat), detail: 'Player and admin assistants configured' },
  ]
}

function buildAlerts(values: ConfigValues): string[] {
  const alerts: string[] = []
  if (boolValue(sectionValues(values, 'general').maintenanceMode)) {
    alerts.push('Maintenance mode is active. Public login should remain blocked.')
  }
  if (voteTotal() > 0) {
    alerts.push(`${voteTotal()} vote reward(s) are still queued for delivery.`)
  }
  if (demoState.tickets.length > 0) {
    alerts.push(`${demoState.tickets.length} support ticket(s) currently need review.`)
  }
  if (!alerts.length) {
    alerts.push('All major VOIDIUM systems look stable.')
  }
  return alerts
}

function buildPublicAccessUrl(values: ConfigValues): string {
  const web = sectionValues(values, 'web')
  const host = stringValue(web.publicHostname, 'voidium.void-craft.eu')
  const port = numberValue(web.port, 8888)
  const suffix = port === 80 || port === 443 ? '' : `:${port}`
  return `https://${host}${suffix}`
}

function buildDashboard(values = demoState.configValues): DashboardData {
  const general = sectionValues(values, 'general')
  const ai = sectionValues(values, 'ai')
  return {
    serverName: 'VOID-BOX | VOIDIUM Control Surface',
    baseUrl: 'https://void-craft.eu',
    publicAccessUrl: buildPublicAccessUrl(values),
    version: '2.6.1',
    latestVersion: '2.6.1',
    updateAvailable: false,
    updateUrl: 'https://github.com/venom74cz/voidium/releases/latest',
    serverIconUrl: null,
    onlinePlayers: demoState.players.length,
    maxPlayers: Number(demoState.serverProperties.properties['max-players'] || 120),
    tps: 19.97,
    mspt: 24.1,
    uptime: '4d 12h 30m',
    memoryUsedMb: 4096,
    memoryMaxMb: 8192,
    memoryUsagePercent: 50,
    nextRestart: '03:00:00',
    maintenanceMode: boolValue(general.maintenanceMode),
    timers: [
      { id: 'restart', title: 'Next restart', subtitle: 'Scheduled maintenance cycle', remainingSeconds: 8100, totalSeconds: 86400, tone: 'danger' },
      { id: 'cleanup', title: 'Entity cleanup', subtitle: 'Drops + hostile mobs', remainingSeconds: 180, totalSeconds: 300, tone: 'accent' },
      { id: 'stats', title: 'Stats report', subtitle: 'Daily Discord summary', remainingSeconds: 46800, totalSeconds: 86400, tone: 'mint' },
    ],
    tickets: { open: demoState.tickets.length, items: clone(demoState.tickets) },
    voteQueue: { total: voteTotal(), players: clone(demoState.votePlayers) },
    players: clone(demoState.players),
    modules: buildModules(values),
    alerts: buildAlerts(values),
    history: clone(baseHistory),
    ai: {
      enabled: boolValue(ai.enablePlayerChat) || boolValue(ai.enableAdminAssistant),
      redactsSensitiveValues: boolValue(ai.redactSensitiveValues),
      configFiles: [...CONFIG_FILE_NAMES],
      history: [
        { role: 'system', content: 'VOIDIUM admin assistant ready in demo mode.' },
        { role: 'assistant', content: 'Default config set has been loaded for localhost preview.' },
      ],
    },
    chatFeed: clone(demoState.chatFeed),
    consoleFeed: clone(demoState.consoleFeed),
    auditFeed: clone(demoState.auditFeed),
    systemInfo: {
      osName: 'Windows',
      osArch: 'amd64',
      availableProcessors: 8,
      cpuLoad: 34,
      processCpuLoad: 14,
      systemRamTotalMb: 16384,
      systemRamUsedMb: 7340,
      systemRamPercent: 45,
      diskTotalGb: 1024,
      diskUsedGb: 362,
      diskPercent: 35,
    },
  }
}

export const mockDashboard: DashboardData = buildDashboard()

function validateConfig(values: ConfigValues): string[] {
  const errors: string[] = []
  const web = sectionValues(values, 'web')
  const restart = sectionValues(values, 'restart')
  const stats = sectionValues(values, 'stats')
  const ai = sectionValues(values, 'ai')

  const webPort = numberValue(web.port, -1)
  if (webPort < 1 || webPort > 65535) {
    errors.push('Web port must be between 1 and 65535.')
  }
  if (stringValue(restart.restartType) === 'FIXED_TIME' && !listValue(restart.fixedRestartTimes).length) {
    errors.push('At least one fixed restart time is required for FIXED_TIME mode.')
  }
  if (stringValue(stats.reportTime) && !/^\d{2}:\d{2}(:\d{2})?$/.test(stringValue(stats.reportTime))) {
    errors.push('Stats report time must use HH:MM or HH:MM:SS format.')
  }
  if (!stringValue(ai['playerApi.endpointUrl']).startsWith('http')) {
    errors.push('Player AI endpoint must start with http or https.')
  }
  if (!stringValue(ai['adminApi.endpointUrl']).startsWith('http')) {
    errors.push('Admin AI endpoint must start with http or https.')
  }

  return errors
}

function buildPreviewForSection(section: string, values: Record<string, unknown>): string {
  switch (section) {
    case 'general':
      return [
        `Maintenance mode: ${boolValue(values.maintenanceMode) ? 'ON' : 'OFF'}`,
        `Core modules enabled: ${['enableMod', 'enableWeb', 'enableDiscord', 'enableStats', 'enableRanks', 'enableVote'].filter(key => boolValue(values[key])).length}/6`,
        `Prefix preview: ${stringValue(values.modPrefix)}`,
      ].join('\n')
    case 'web':
      return [
        `Bind: ${stringValue(values.bindAddress)}:${numberValue(values.port, 8888)}`,
        `Public access: https://${stringValue(values.publicHostname)}`,
        `Language: ${stringValue(values.language).toUpperCase()}`,
      ].join('\n')
    case 'announcements':
      return [
        `Broadcast interval: ${numberValue(values.announcementIntervalMinutes)} minute(s)`,
        `Lines configured: ${listValue(values.announcements).length}`,
        `Prefix: ${stringValue(values.prefix)}`,
      ].join('\n')
    case 'restart':
      return [
        `Mode: ${stringValue(values.restartType)}`,
        `Fixed times: ${listValue(values.fixedRestartTimes).join(', ') || 'none'}`,
        `Kick message: ${stringValue(values.kickMessage)}`,
      ].join('\n')
    case 'ranks':
      return [
        `Auto ranks: ${boolValue(values.enableAutoRanks) ? 'enabled' : 'disabled'}`,
        `Count AFK time: ${boolValue(values.countAfkTime) ? 'yes' : 'no'}`,
        `Structured tiers: ${countConfiguredRanks(values.ranks)}`,
        `Promotion copy: ${stringValue(values.promotionMessage)}`,
      ].join('\n')
    case 'playerlist':
      return [
        `Custom TAB list: ${boolValue(values.enableCustomPlayerList) ? 'enabled' : 'disabled'}`,
        `Header line 1: ${stringValue(values.headerLine1)}`,
        `Name format: ${stringValue(values.playerNameFormat)}`,
      ].join('\n')
    case 'tickets':
      return [
        `Tickets: ${boolValue(values.enableTickets) ? 'enabled' : 'disabled'}`,
        `Category: ${stringValue(values.ticketCategoryId) || 'not set'}`,
        `Auto assign: ${boolValue(values.enableAutoAssign) ? 'enabled' : 'disabled'}`,
      ].join('\n')
    case 'discord':
      return [
        `Guild: ${stringValue(values.guildId) || 'not set'}`,
        `Chat bridge: ${boolValue(values.enableChatBridge) ? 'enabled' : 'disabled'}`,
        `Status updates: ${boolValue(values.enableStatusMessages) ? 'enabled' : 'disabled'}`,
      ].join('\n')
    case 'stats':
      return [
        `Stats reports: ${boolValue(values.enableStats) ? 'enabled' : 'disabled'}`,
        `Report time: ${stringValue(values.reportTime)}`,
        `Report title: ${stringValue(values.reportTitle)}`,
      ].join('\n')
    case 'vote':
      return [
        `Vote listener: ${boolValue(values.enabled) ? 'enabled' : 'disabled'}`,
        `Endpoint: ${stringValue(values.host)}:${numberValue(values.port, 8192)}`,
        `Reward commands: ${listValue(values.commands).length}`,
      ].join('\n')
    case 'entitycleaner':
      return [
        `Cleaner: ${boolValue(values.enabled) ? 'enabled' : 'disabled'}`,
        `Interval: ${numberValue(values.cleanupIntervalSeconds)} second(s)`,
        `Warnings: ${listValue(values.warningTimes).join(', ') || 'none'}`,
      ].join('\n')
    case 'ai':
      return [
        `Player AI: ${boolValue(values.enablePlayerChat) ? 'enabled' : 'disabled'}`,
        `Admin AI: ${boolValue(values.enableAdminAssistant) ? 'enabled' : 'disabled'}`,
        `Models: ${stringValue(values['playerApi.model'])} / ${stringValue(values['adminApi.model'])}`,
      ].join('\n')
    default:
      return JSON.stringify(values, null, 2)
  }
}

function buildRecommendedAction(changes: ConfigDiffChange[]) {
  const requiresWebRestart = changes.some(change => change.section === 'web' && (change.field === 'port' || change.field === 'bindAddress'))
  if (!requiresWebRestart) return undefined
  return { label: 'Restart embedded web server to apply bind changes.' }
}

function allSectionKeys(): SectionKey[] {
  return Object.keys(mockConfigSchema) as SectionKey[]
}

function buildDiff(values: ConfigValues): ConfigDiffResult {
  const changes: ConfigDiffChange[] = []

  allSectionKeys().forEach(section => {
    const currentSection = sectionValues(demoState.configValues, section)
    const nextSection = sectionValues(values, section)
    const fields = mockConfigSchema[section].fields || []
    fields.forEach(fieldDef => {
      const current = currentSection[fieldDef.key]
      const proposed = nextSection[fieldDef.key]
      if (!sameValue(current, proposed)) {
        changes.push({
          section,
          field: fieldDef.key,
          current,
          proposed,
          impactLabel: section === 'web' && (fieldDef.key === 'port' || fieldDef.key === 'bindAddress')
            ? 'Restart required'
            : mockConfigSchema[section].restartRequired
              ? 'Config reload recommended'
              : 'Live-safe',
        })
      }
    })
  })

  return {
    summary: changes.length ? `${changes.length} change(s) detected.` : 'No changes detected.',
    changes,
    requiresWebRestart: changes.some(change => change.section === 'web' && (change.field === 'port' || change.field === 'bindAddress')),
    recommendedAction: buildRecommendedAction(changes),
  }
}

function updateDashboardSideEffects(values: ConfigValues) {
  const general = sectionValues(values, 'general')
  if (boolValue(general.maintenanceMode)) {
    appendConsole('WARN', 'Voidium', 'Maintenance mode has been enabled from the web panel.')
  }
}

export function demoConfigSchema(): ConfigSchema {
  return clone(mockConfigSchema)
}

export function demoConfigSchemaExport(): Record<string, unknown> {
  const properties = Object.fromEntries(allSectionKeys().map(section => [section, {
    type: 'object',
    title: mockConfigSchema[section].label,
    properties: Object.fromEntries(mockConfigSchema[section].fields.map(fieldDef => [fieldDef.key, {
      type: fieldDef.type === 'boolean'
        ? 'boolean'
        : fieldDef.type === 'number'
          ? 'number'
          : fieldDef.type === 'multiline-list' || fieldDef.type === 'rank-list' || fieldDef.type === 'discord-role-style-list' || fieldDef.type === 'json'
            ? 'array'
            : 'string',
      title: fieldDef.label,
      description: fieldDef.description,
      ...(fieldDef.options ? { enum: fieldDef.options } : {}),
    }]))
  }]))
  return {
    $schema: 'http://json-schema.org/draft-07/schema#',
    title: 'Voidium Configuration',
    type: 'object',
    properties,
  }
}

export function demoConfigValues(): ConfigValues {
  return clone(demoState.configValues)
}

export function demoConfigDefaults(section: string, values: ConfigValues): { message: string; values?: ConfigValues } {
  if (!(section in mockConfigDefaults)) {
    return { message: `Unknown section: ${section}` }
  }
  const next = clone(values)
  next[section] = clone(mockConfigDefaults[section])
  return { message: `Restored demo defaults for ${section}.`, values: next }
}

export function demoConfigLocale(section: string, locale: string, values: ConfigValues): { message: string; values?: ConfigValues } {
  return mergeDemoLocalePreset(section, locale, values)
}

export function demoConfigPreview(values: ConfigValues): ConfigPreviewResult {
  const previews = Object.fromEntries(allSectionKeys().map(section => [section, buildPreviewForSection(section, sectionValues(values, section))]))
  return {
    previews,
    validationErrors: validateConfig(values),
  }
}

export function demoConfigDiff(values: ConfigValues): ConfigDiffResult {
  return buildDiff(values)
}

export function demoConfigApply(values: ConfigValues, source = 'manual-web'): ConfigApplyResult {
  const validationErrors = validateConfig(values)
  const diff = buildDiff(values)
  if (validationErrors.length) {
    return {
      message: 'Demo config validation failed.',
      applied: false,
      validationErrors,
      recommendedAction: diff.recommendedAction,
    }
  }

  demoState.history.push(clone(demoState.configValues))
  demoState.configValues = clone(values)
  updateDashboardSideEffects(values)
  appendAudit('Config applied', `${source} updated ${diff.changes.length} field(s).`, 'Config', true)
  appendConsole('INFO', 'ConfigStudio', `Applied ${diff.changes.length} config change(s) in demo mode.`)
  return {
    message: 'Demo config applied to the VOIDIUM preview state.',
    applied: true,
    recommendedAction: diff.recommendedAction,
  }
}

export function demoConfigRollback(): { message: string; rolledBack?: boolean } {
  const previous = demoState.history.pop()
  if (!previous) {
    return { message: 'No previous demo snapshot is available.', rolledBack: false }
  }
  demoState.configValues = clone(previous)
  appendAudit('Config rollback', 'Reverted to the previous demo snapshot.', 'Config', true)
  appendConsole('INFO', 'ConfigStudio', 'Rolled back the latest demo config snapshot.')
  return { message: 'Rolled back to the previous demo snapshot.', rolledBack: true }
}

export function demoConfigReload(): { message: string } {
  appendAudit('Config reload', 'Reload requested inside demo mode.', 'Config', true)
  appendConsole('INFO', 'Voidium', 'Reload requested in demo mode.')
  return { message: 'Reload requested in demo mode.' }
}

const heatmap: DimensionHeatmap = {
  overworld: { items: 44, mobs: 16, xpOrbs: 28, arrows: 9, total: 97 },
  the_nether: { items: 18, mobs: 43, xpOrbs: 12, arrows: 4, total: 77 },
  the_end: { items: 7, mobs: 3, xpOrbs: 2, arrows: 1, total: 13 },
}

function updateVotePlayers(next: DemoState['votePlayers']) {
  demoState.votePlayers = next.filter(item => numberValue(item.count) > 0)
}

function removeVotesForPlayer(player: string) {
  updateVotePlayers(demoState.votePlayers.map(item => item.player === player ? { ...item, count: 0 } : item))
}

function payoutVotesForPlayer(player: string): number {
  let delivered = 0
  updateVotePlayers(demoState.votePlayers.map(item => {
    if (item.player !== player || !item.online) return item
    delivered = numberValue(item.count)
    return { ...item, count: 0 }
  }))
  return delivered
}

function isAllowedDemoCommand(command: string): boolean {
  const root = command.trim().replace(/^\//, '').split(/\s+/, 1)[0].toLowerCase()
  return ['say', 'me', 'msg', 'tell', 'tellraw', 'title', 'tp', 'teleport', 'spreadplayers', 'time', 'weather', 'gamerule', 'difficulty', 'list', 'playsound', 'give', 'clear', 'effect', 'xp', 'experience', 'kick', 'ban', 'pardon', 'banlist', 'voidium'].includes(root)
}

export function demoAction(action: string, extra: Record<string, string> = {}): { message: string } {
  switch (action) {
    case 'announce': {
      const message = extra.message?.trim()
      if (!message) return { message: 'Announcement message is required.' }
      appendAudit('Broadcast', message, 'Operations', true)
      appendConsole('INFO', 'Announcement', `Broadcast from panel: ${message}`)
      return { message: 'Announcement sent in demo mode.' }
    }
    case 'restart':
      appendAudit('Restart scheduled', 'Restart scheduled in 5 minutes from quick actions.', 'Operations', true)
      appendConsole('WARN', 'Restart', 'Manual restart scheduled in 5 minutes.')
      return { message: 'Restart scheduled in 5 minutes.' }
    case 'reload':
      return demoConfigReload()
    case 'entitycleaner_preview':
      return { message: 'Preview: 62 items, 19 mobs, 14 XP orbs, 6 arrows would be removed.' }
    case 'entitycleaner_all':
    case 'entitycleaner_items':
    case 'entitycleaner_mobs':
    case 'entitycleaner_xp':
    case 'entitycleaner_arrows':
      appendAudit('EntityCleaner run', `Executed ${action}.`, 'Operations', true)
      appendConsole('INFO', 'EntityCleaner', `${action} executed in demo mode.`)
      return { message: `${action} executed in demo mode.` }
    case 'maintenance_on':
      demoState.configValues.general.maintenanceMode = true
      appendAudit('Maintenance enabled', 'Maintenance mode enabled from the panel.', 'Operations', true)
      return { message: 'Maintenance mode enabled.' }
    case 'maintenance_off':
      demoState.configValues.general.maintenanceMode = false
      appendAudit('Maintenance disabled', 'Maintenance mode disabled from the panel.', 'Operations', true)
      return { message: 'Maintenance mode disabled.' }
    case 'player_kick': {
      const player = extra.player?.trim()
      if (!player) return { message: 'Player is required.' }
      demoState.players = demoState.players.filter(entry => entry.name !== player)
      appendAudit('Player kicked', `${player} was removed from the live roster.`, 'Players', true)
      appendConsole('INFO', 'PlayerControl', `Kick requested for ${player}.`)
      return { message: `Kick requested for ${player}.` }
    }
    case 'player_ban': {
      const player = extra.player?.trim()
      if (!player) return { message: 'Player is required.' }
      demoState.players = demoState.players.filter(entry => entry.name !== player)
      removeVotesForPlayer(player)
      appendAudit('Player banned', `${player} was banned from the demo roster.`, 'Players', true)
      appendConsole('WARN', 'PlayerControl', `Ban requested for ${player}.`)
      return { message: `Ban requested for ${player}.` }
    }
    case 'player_unlink': {
      const uuid = extra.uuid?.trim()
      if (!uuid) return { message: 'Player UUID is required.' }
      demoState.players = demoState.players.map(player => player.uuid === uuid ? { ...player, linked: false, discordId: null } : player)
      appendAudit('Discord unlink', `Removed Discord link for ${extra.player || uuid}.`, 'Players', true)
      return { message: `Discord link removed for ${extra.player || uuid}.` }
    }
    case 'ticket_note': {
      const channelId = extra.channelId?.trim()
      const message = extra.message?.trim()
      if (!channelId || !message) return { message: 'Ticket channel and message are required.' }
      demoState.tickets = demoState.tickets.map(ticket => ticket.channelId === channelId ? {
        ...ticket,
        cachedMessages: ticket.cachedMessages + 1,
        previewLines: [...ticket.previewLines, message].slice(-4),
      } : ticket)
      appendAudit('Ticket note', `Added web note to ${channelId}.`, 'Tickets', true)
      return { message: 'Ticket note sent.' }
    }
    case 'ticket_close': {
      const channelId = extra.channelId?.trim()
      if (!channelId) return { message: 'Ticket channel is required.' }
      demoState.tickets = demoState.tickets.filter(ticket => ticket.channelId !== channelId)
      appendAudit('Ticket closed', `Closed ticket ${channelId} from the panel.`, 'Tickets', true)
      return { message: 'Ticket close requested.' }
    }
    case 'vote_payout_player': {
      const player = extra.player?.trim()
      if (!player) return { message: 'Player is required.' }
      const delivered = payoutVotesForPlayer(player)
      if (!delivered) {
        return { message: 'Player must be online for manual payout.' }
      }
      appendAudit('Vote payout', `Delivered ${delivered} queued vote(s) to ${player}.`, 'Votes', true)
      return { message: `Queued payout for ${player}: ${delivered} vote(s).` }
    }
    case 'vote_clear_player': {
      const player = extra.player?.trim()
      if (!player) return { message: 'Player is required.' }
      removeVotesForPlayer(player)
      appendAudit('Vote queue cleared', `Removed pending votes for ${player}.`, 'Votes', true)
      return { message: `Removed pending vote(s) for ${player}.` }
    }
    case 'vote_payout_all_online': {
      const onlinePlayers = new Set(demoState.players.map(player => player.name))
      let delivered = 0
      updateVotePlayers(demoState.votePlayers.map(item => {
        if (!onlinePlayers.has(item.player) || !item.online) return item
        delivered += numberValue(item.count)
        return { ...item, count: 0 }
      }))
      appendAudit('Vote payout all', `Delivered ${delivered} queued vote(s) to online players.`, 'Votes', true)
      return { message: `Delivered ${delivered} queued vote(s) to online players.` }
    }
    case 'vote_clear_all':
      updateVotePlayers([])
      appendAudit('Vote queue cleared', 'Removed all pending votes from the queue.', 'Votes', true)
      return { message: 'Cleared all pending vote(s).' }
    default:
      return { message: `Demo action ${action} completed.` }
  }
}

export function demoActionJson(
  action: string,
  extra: Record<string, string> = {},
): { message: string; dimensions?: DimensionHeatmap; result?: Record<string, number>; lines?: string[]; player?: string } {
  if (action === 'entitycleaner_preview_dimensions') {
    return {
      message: 'Dimension heatmap loaded.',
      dimensions: clone(heatmap),
    }
  }
  if (action === 'ticket_transcript') {
    const ticket = demoState.tickets.find(item => item.channelId === extra.channelId)
    return {
      message: ticket ? 'Transcript ready.' : 'No transcript lines cached.',
      lines: ticket ? clone(ticket.previewLines) : [],
      player: ticket?.player,
    }
  }
  return demoAction(action, extra)
}

export function demoAiAdmin(payload: { message: string }) {
  return {
    answer: payload.message.trim()
      ? 'Demo mode is active. The admin assistant is not calling a live model, but the loaded VOIDIUM default config set is available for UI review.'
      : 'Provide a prompt to the admin assistant.',
  }
}

export function demoAiSuggest(): AiSuggestResult {
  return {
    summary: 'Demo mode does not generate staged AI config changes.',
    diffPreview: 'The panel is showing the default VOIDIUM config dataset. Connect the live backend to generate real AI suggestions.',
    staged: false,
    warnings: ['AI suggestion staging is disabled in localhost demo mode.'],
  }
}

export function demoAiPlayers() {
  return { conversations: clone(mockPlayerAiConversations) }
}

export function demoDiscordRoleList() {
  return { roles: clone(mockDiscordRoles) }
}

export function demoConsoleExecute(command: string) {
  const trimmed = command.trim()
  if (!trimmed) {
    return { message: 'Command is required.' }
  }
  if (!isAllowedDemoCommand(trimmed)) {
    return { message: 'This command family is blocked from the web panel.' }
  }
  appendConsole('INFO', 'Console', `Executed: ${trimmed}`)
  appendAudit('Console command', trimmed, 'Console', true)
  return { message: `Command queued in demo mode: ${trimmed}` }
}

export function demoServerProperties() {
  return clone(demoState.serverProperties)
}

export function demoServerPropertiesSave(properties: Record<string, string>) {
  demoState.serverProperties.properties = {
    ...demoState.serverProperties.properties,
    ...properties,
  }
  appendAudit('server.properties updated', `${Object.keys(properties).length} key(s) changed.`, 'Config', true)
  return {
    message: 'server.properties updated in demo mode.',
    changedKeys: Object.keys(properties),
  }
}

export function demoDashboard() {
  return buildDashboard()
}
