package net.robinfriedli.botify.command.commands;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.validator.routines.UrlValidator;

import com.google.common.collect.Lists;
import com.wrapper.spotify.model_objects.specification.AlbumSimplified;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.Track;
import net.robinfriedli.botify.Botify;
import net.robinfriedli.botify.audio.AudioManager;
import net.robinfriedli.botify.audio.AudioPlayback;
import net.robinfriedli.botify.audio.Playable;
import net.robinfriedli.botify.audio.PlayableFactory;
import net.robinfriedli.botify.audio.spotify.SpotifyService;
import net.robinfriedli.botify.audio.spotify.SpotifyUri;
import net.robinfriedli.botify.audio.youtube.YouTubePlaylist;
import net.robinfriedli.botify.audio.youtube.YouTubeService;
import net.robinfriedli.botify.audio.youtube.YouTubeVideo;
import net.robinfriedli.botify.command.AbstractCommand;
import net.robinfriedli.botify.command.CommandContext;
import net.robinfriedli.botify.command.CommandManager;
import net.robinfriedli.botify.entities.Playlist;
import net.robinfriedli.botify.entities.xml.CommandContribution;
import net.robinfriedli.botify.exceptions.InvalidCommandException;
import net.robinfriedli.botify.exceptions.NoResultsFoundException;
import net.robinfriedli.botify.exceptions.NoSpotifyResultsFoundException;
import net.robinfriedli.botify.util.SearchEngine;
import net.robinfriedli.stringlist.StringListImpl;

public abstract class AbstractQueueLoadingCommand extends AbstractCommand {

    private final boolean mayInterrupt;
    int loadedAmount;
    Playlist loadedLocalList;
    YouTubePlaylist loadedYouTubePlaylist;
    PlaylistSimplified loadedSpotifyPlaylist;
    Playable loadedTrack;
    AlbumSimplified loadedAlbum;

    public AbstractQueueLoadingCommand(CommandContribution commandContribution,
                                       CommandContext context,
                                       CommandManager commandManager,
                                       String commandString,
                                       boolean requiresInput,
                                       String identifier,
                                       String description,
                                       Category category,
                                       boolean mayInterrupt) {
        super(commandContribution, context, commandManager, commandString, requiresInput, identifier, description, category);
        this.mayInterrupt = mayInterrupt;
    }

    @Override
    public void doRun() throws Exception {
        AudioManager audioManager = Botify.get().getAudioManager();
        AudioPlayback playback = audioManager.getPlaybackForGuild(getContext().getGuild());
        playback.setCommunicationChannel(getContext().getChannel());

        if (UrlValidator.getInstance().isValid(getCommandBody())) {
            loadUrlItems(audioManager, playback);
        } else if (SpotifyUri.isSpotifyUri(getCommandBody())) {
            loadSpotifyUri(audioManager);
        } else if (argumentSet("list")) {
            if (argumentSet("spotify")) {
                loadSpotifyList(audioManager);
            } else if (argumentSet("youtube")) {
                loadYouTubeList(audioManager);
            } else {
                loadLocalList(audioManager);
            }
        } else {
            if (argumentSet("youtube")) {
                loadYouTubeVideo(audioManager);
            } else if (argumentSet("album")) {
                loadSpotifyAlbum(audioManager);
            } else if (UrlValidator.getInstance().isValid(getCommandBody())) {
                loadUrlItems(audioManager, playback);
            } else {
                loadTrack(audioManager);
            }
        }
    }

    protected abstract void handleResults(List<Playable> playables);

    private void loadUrlItems(AudioManager audioManager, AudioPlayback playback) {
        PlayableFactory playableFactory = audioManager.createPlayableFactory(playback.getGuild());
        List<Playable> playables = playableFactory.createPlayables(getCommandBody(), getContext().getSpotifyApi(), !argumentSet("preview"), mayInterrupt);
        if (playables.isEmpty()) {
            throw new NoResultsFoundException("Result is empty!");
        }
        handleResults(playables);
        loadedAmount = playables.size();
    }

    private void loadSpotifyUri(AudioManager audioManager) throws Exception {
        SpotifyUri spotifyUri = SpotifyUri.parse(getCommandBody());
        SpotifyService spotifyService = getContext().getSpotifyService();
        PlayableFactory playableFactory = audioManager.createPlayableFactory(getContext().getGuild());
        List<Playable> playables = spotifyUri.loadPlayables(playableFactory, spotifyService, !argumentSet("preview"), mayInterrupt);
        handleResults(playables);
        loadedAmount = playables.size();
    }

    private void loadLocalList(AudioManager audioManager) throws Exception {
        Playlist playlist = SearchEngine.searchLocalList(getContext().getSession(), getCommandBody(), isPartitioned(), getContext().getGuild().getId());
        if (playlist == null) {
            throw new NoResultsFoundException("No local playlist found for " + getCommandBody());
        }

        List<Object> items = runWithCredentials(() -> playlist.getTracks(getContext().getSpotifyApi()));

        if (items.isEmpty()) {
            throw new NoResultsFoundException("Playlist is empty");
        }

        PlayableFactory playableFactory = audioManager.createPlayableFactory(getContext().getGuild());
        List<Playable> playables = playableFactory.createPlayables(!argumentSet("preview"), items, mayInterrupt);
        handleResults(playables);
        loadedLocalList = playlist;
    }

    private void loadYouTubeList(AudioManager audioManager) {
        YouTubeService youTubeService = audioManager.getYouTubeService();
        PlayableFactory playableFactory = audioManager.createPlayableFactory(getContext().getGuild());

        if (argumentSet("limit")) {
            int limit = getArgumentValue("limit", Integer.class);
            if (!(limit > 0 && limit <= 10)) {
                throw new InvalidCommandException("Limit must be between 1 and 10");
            }

            List<YouTubePlaylist> playlists = youTubeService.searchSeveralPlaylists(limit, getCommandBody());
            if (playlists.size() == 1) {
                YouTubePlaylist playlist = playlists.get(0);
                List<Playable> playables = playableFactory.createPlayables(playlist, mayInterrupt);
                handleResults(playables);
                loadedYouTubePlaylist = playlist;
            } else if (playlists.isEmpty()) {
                throw new NoResultsFoundException("No playlist found for " + getCommandBody());
            } else {
                askQuestion(playlists, YouTubePlaylist::getTitle, YouTubePlaylist::getChannelTitle);
            }
        } else {
            YouTubePlaylist youTubePlaylist = youTubeService.searchPlaylist(getCommandBody());
            List<Playable> playables = playableFactory.createPlayables(youTubePlaylist, mayInterrupt);
            handleResults(playables);
            loadedYouTubePlaylist = youTubePlaylist;
        }
    }

    private void loadSpotifyList(AudioManager audioManager) throws Exception {
        Callable<Void> callable = () -> {
            List<PlaylistSimplified> found;
            if (argumentSet("own")) {
                found = getSpotifyService().searchOwnPlaylist(getCommandBody());
            } else {
                found = getSpotifyService().searchPlaylist(getCommandBody());
            }

            if (found.size() == 1) {
                PlaylistSimplified playlist = found.get(0);
                List<Track> playlistTracks = getSpotifyService().getPlaylistTracks(playlist);
                PlayableFactory playableFactory = audioManager.createPlayableFactory(getContext().getGuild());
                List<Playable> playables = playableFactory.createPlayables(!argumentSet("preview"), playlistTracks, mayInterrupt);
                handleResults(playables);
                loadedSpotifyPlaylist = playlist;
            } else if (found.isEmpty()) {
                throw new NoSpotifyResultsFoundException("No playlist found for " + getCommandBody());
            } else {
                askQuestion(found, PlaylistSimplified::getName, p -> p.getOwner().getDisplayName());
            }

            return null;
        };

        if (argumentSet("own")) {
            runWithLogin(callable);
        } else {
            runWithCredentials(callable);
        }
    }

    private void loadSpotifyAlbum(AudioManager audioManager) throws Exception {
        Callable<List<AlbumSimplified>> albumLoadCallable = () -> getSpotifyService().searchAlbum(getCommandBody(), argumentSet("own"));
        List<AlbumSimplified> albums;
        if (argumentSet("own")) {
            albums = runWithLogin(albumLoadCallable);
        } else {
            albums = runWithCredentials(albumLoadCallable);
        }

        if (albums.size() == 1) {
            AlbumSimplified album = albums.get(0);
            List<Track> tracks = runWithCredentials(() -> getSpotifyService().getAlbumTracks(album.getId()));
            PlayableFactory playableFactory = audioManager.createPlayableFactory(getContext().getGuild());
            List<Playable> playables = playableFactory.createPlayables(!argumentSet("preview"), tracks, mayInterrupt);
            handleResults(playables);
            loadedAlbum = album;
        } else if (albums.isEmpty()) {
            throw new NoSpotifyResultsFoundException("No albums found for " + getCommandBody());
        } else {
            askQuestion(albums, AlbumSimplified::getName, album -> StringListImpl.create(album.getArtists(), ArtistSimplified::getName).toSeparatedString(", "));
        }
    }

    private void loadTrack(AudioManager audioManager) throws Exception {
        Callable<List<Track>> loadTrackCallable = () -> getSpotifyService().searchTrack(getCommandBody(), argumentSet("own"));
        List<Track> found;
        if (argumentSet("own")) {
            found = runWithLogin(loadTrackCallable);
        } else {
            found = runWithCredentials(loadTrackCallable);
        }

        if (found.size() == 1) {
            PlayableFactory playableFactory = audioManager.createPlayableFactory(getContext().getGuild());
            Playable track = playableFactory.createPlayable(!argumentSet("preview"), found.get(0));
            handleResults(Lists.newArrayList(track));
            loadedTrack = track;
        } else if (found.isEmpty()) {
            throw new NoSpotifyResultsFoundException("No Spotify track found for " + getCommandBody());
        } else {
            askQuestion(found, track -> {
                String artistString = StringListImpl.create(track.getArtists(), ArtistSimplified::getName).toSeparatedString(", ");
                return String.format("%s by %s", track.getName(), artistString);
            }, track -> track.getAlbum().getName());
        }
    }

    private void loadYouTubeVideo(AudioManager audioManager) {
        YouTubeService youTubeService = audioManager.getYouTubeService();
        if (argumentSet("limit")) {
            int limit = getArgumentValue("limit", Integer.class);
            if (!(limit > 0 && limit <= 10)) {
                throw new InvalidCommandException("Limit must be between 1 and 10");
            }

            List<YouTubeVideo> youTubeVideos = youTubeService.searchSeveralVideos(limit, getCommandBody());
            if (youTubeVideos.size() == 1) {
                Playable playable = youTubeVideos.get(0);
                handleResults(Lists.newArrayList(playable));
                loadedTrack = playable;
            } else if (youTubeVideos.isEmpty()) {
                throw new NoResultsFoundException("No YouTube video found for " + getCommandBody());
            } else {
                askQuestion(youTubeVideos, youTubeVideo -> {
                    try {
                        return youTubeVideo.getTitle();
                    } catch (InterruptedException e) {
                        // Unreachable since only HollowYouTubeVideos might get interrupted
                        throw new RuntimeException(e);
                    }
                });
            }
        } else {
            YouTubeVideo youTubeVideo = youTubeService.searchVideo(getCommandBody());
            audioManager.getQueue(getContext().getGuild()).add(youTubeVideo);
            handleResults(Lists.newArrayList(youTubeVideo));
            loadedTrack = youTubeVideo;
        }
    }

}
