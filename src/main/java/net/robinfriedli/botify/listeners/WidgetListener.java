package net.robinfriedli.botify.listeners;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wrapper.spotify.SpotifyApi;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.robinfriedli.botify.Botify;
import net.robinfriedli.botify.command.AbstractWidget;
import net.robinfriedli.botify.command.CommandContext;
import net.robinfriedli.botify.command.CommandManager;
import net.robinfriedli.botify.concurrent.CommandExecutionThread;
import net.robinfriedli.botify.concurrent.ThreadExecutionQueue;
import net.robinfriedli.botify.discord.CommandExecutionQueueManager;
import net.robinfriedli.botify.discord.CompletablePlaceholderMessage;
import net.robinfriedli.botify.discord.GuildContext;
import net.robinfriedli.botify.discord.MessageService;
import net.robinfriedli.botify.exceptions.CommandRuntimeException;
import net.robinfriedli.botify.exceptions.UserException;
import net.robinfriedli.botify.exceptions.WidgetExceptionHandler;
import net.robinfriedli.botify.util.StaticSessionProvider;

public class WidgetListener extends ListenerAdapter {

    private final CommandExecutionQueueManager executionQueueManager;
    private final CommandManager commandManager;
    private final Logger logger;

    public WidgetListener(CommandExecutionQueueManager executionQueueManager, CommandManager commandManager) {
        this.executionQueueManager = executionQueueManager;
        this.commandManager = commandManager;
        logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (!event.getUser().isBot()) {

            String messageId = event.getMessageId();

            Optional<AbstractWidget> activeWidget = commandManager.getActiveWidget(messageId);
            activeWidget.ifPresent(abstractWidget -> handleWidgetExecution(event, messageId, abstractWidget));
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        commandManager.getActiveWidget(event.getMessageId()).ifPresent(widget -> {
            widget.setMessageDeleted(true);
            widget.destroy();
        });
    }

    private void handleWidgetExecution(GuildMessageReactionAddEvent event, String messageId, AbstractWidget activeWidget) {
        TextChannel channel = event.getChannel();
        CompletablePlaceholderMessage message = new CompletablePlaceholderMessage();
        channel.getMessageById(messageId).queue(message::complete, message::completeExceptionally);

        Guild guild = event.getGuild();
        Botify botify = Botify.get();
        SpotifyApi spotifyApi = botify.getSpotifyApiBuilder().build();
        GuildContext guildContext = botify.getGuildManager().getContextForGuild(guild);
        CommandContext commandContext = new CommandContext("", message, StaticSessionProvider.getSessionFactory(), spotifyApi, guildContext);
        ThreadExecutionQueue queue = executionQueueManager.getForGuild(guild);

        CommandExecutionThread widgetExecutionThread = new CommandExecutionThread(commandContext, queue, () -> {
            try {
                activeWidget.handleReaction(event);
            } catch (UserException e) {
                new MessageService().sendError(e.getMessage(), channel);
            } catch (Exception e) {
                throw new CommandRuntimeException(e);
            }
        });
        widgetExecutionThread.setName("Widget execution thread " + messageId);
        widgetExecutionThread.setUncaughtExceptionHandler(new WidgetExceptionHandler(channel, logger));
        queue.add(widgetExecutionThread);
    }

}
