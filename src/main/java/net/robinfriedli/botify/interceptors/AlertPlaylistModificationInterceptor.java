package net.robinfriedli.botify.interceptors;

import java.util.List;

import org.slf4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.robinfriedli.botify.command.CommandContext;
import net.robinfriedli.botify.discord.MessageService;
import net.robinfriedli.botify.entities.Playlist;
import net.robinfriedli.botify.entities.PlaylistItem;
import net.robinfriedli.stringlist.StringListImpl;
import org.hibernate.Interceptor;

public class AlertPlaylistModificationInterceptor extends CollectingInterceptor {

    private final MessageChannel channel;
    private final MessageService messageService;

    public AlertPlaylistModificationInterceptor(Interceptor next, Logger logger, CommandContext commandContext) {
        super(next, logger);
        channel = commandContext.getChannel();
        messageService = new MessageService();
    }

    @Override
    public void afterCommit() {
        List<Playlist> createdPlaylists = getCreatedEntities(Playlist.class);
        List<Playlist> deletedPlaylists = getDeletedEntities(Playlist.class);
        List<PlaylistItem> addedItems = getCreatedEntities(PlaylistItem.class);
        List<PlaylistItem> removedItems = getDeletedEntities(PlaylistItem.class);

        if (!createdPlaylists.isEmpty()) {
            String s = createdPlaylists.size() > 1 ? "Created playlists: " : "Created playlist: ";
            messageService.sendSuccess(s + StringListImpl.create(createdPlaylists, Playlist::getName).toSeparatedString(", "), channel);
        }

        if (!deletedPlaylists.isEmpty()) {
            String s = deletedPlaylists.size() > 1 ? "Deleted playlists: " : "Deleted playlist: ";
            messageService.sendSuccess(s + StringListImpl.create(deletedPlaylists, Playlist::getName).toSeparatedString(", "), channel);
        }

        if (!addedItems.isEmpty()) {
            if (addedItems.size() == 1) {
                PlaylistItem item = addedItems.get(0);
                if (!createdPlaylists.contains(item.getPlaylist())) {
                    messageService.sendSuccess("Added " + item.display() + " to " + item.getPlaylist().getName(), channel);
                }
            } else {
                Multimap<Playlist, PlaylistItem> playlistWithItems = HashMultimap.create();
                for (PlaylistItem playlistItem : addedItems) {
                    playlistWithItems.put(playlistItem.getPlaylist(), playlistItem);
                }
                for (Playlist playlist : playlistWithItems.keySet()) {
                    if (!createdPlaylists.contains(playlist)) {
                        messageService.sendSuccess("Added " + playlistWithItems.get(playlist).size() + " items to playlist " + playlist.getName(), channel);
                    }
                }
            }
        }

        if (!removedItems.isEmpty()) {
            if (removedItems.size() == 1) {
                PlaylistItem item = removedItems.get(0);
                if (!deletedPlaylists.contains(item.getPlaylist())) {
                    messageService.sendSuccess("Removed " + item.display() + " from " + item.getPlaylist().getName(), channel);
                }
            } else {
                Multimap<Playlist, PlaylistItem> playlistWithItems = HashMultimap.create();
                for (PlaylistItem playlistItem : removedItems) {
                    playlistWithItems.put(playlistItem.getPlaylist(), playlistItem);
                }
                for (Playlist playlist : playlistWithItems.keySet()) {
                    if (!deletedPlaylists.contains(playlist)) {
                        messageService.sendSuccess("Removed " + playlistWithItems.get(playlist).size() + " items from playlist " + playlist.getName(), channel);
                    }
                }
            }
        }
    }
}
