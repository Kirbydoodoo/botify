package net.robinfriedli.botify.tasks;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.robinfriedli.botify.entities.Playlist;
import net.robinfriedli.botify.entities.PlaylistItem;

public class UpdatePlaylistItemIndicesTask implements PersistTask<Void> {

    private final Collection<Playlist> playlistsToUpdate;

    public UpdatePlaylistItemIndicesTask(Collection<Playlist> playlistsToUpdate) {
        this.playlistsToUpdate = playlistsToUpdate;
    }

    @Override
    public Void perform() {
        for (Playlist playlist : playlistsToUpdate) {
            List<PlaylistItem> items = playlist.getItems();
            List<PlaylistItem> itemsOrdered = items.stream()
                .filter(item -> item.getIndex() != null)
                .sorted(Comparator.comparing(PlaylistItem::getIndex))
                .collect(Collectors.toList());
            List<PlaylistItem> addedItems = items.stream()
                .filter(item -> item.getIndex() == null)
                .sorted(Comparator.comparing(PlaylistItem::getCreatedTimestamp))
                .collect(Collectors.toList());
            itemsOrdered.addAll(addedItems);

            for (int i = 0; i < itemsOrdered.size(); i++) {
                PlaylistItem item = itemsOrdered.get(i);
                if (item.getIndex() == null || item.getIndex() != i) {
                    item.setIndex(i);
                }
            }
        }

        return null;
    }
}
