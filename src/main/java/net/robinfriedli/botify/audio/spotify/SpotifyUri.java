package net.robinfriedli.botify.audio.spotify;

import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.wrapper.spotify.exceptions.detailed.NotFoundException;
import com.wrapper.spotify.model_objects.specification.Track;
import net.robinfriedli.botify.audio.Playable;
import net.robinfriedli.botify.audio.PlayableFactory;
import net.robinfriedli.botify.concurrent.Invoker;
import net.robinfriedli.botify.exceptions.InvalidCommandException;

public class SpotifyUri {

    private static final Pattern URI_REGEX = Pattern.compile("spotify:(track|album|playlist):([a-zA-Z0-9])([a-zA-Z0-9])*");

    private final String id;
    private final Type type;

    public SpotifyUri(String uri) {
        if (Type.TRACK.getPattern().matcher(uri).matches()) {
            type = Type.TRACK;
        } else if (Type.ALBUM.getPattern().matcher(uri).matches()) {
            type = Type.ALBUM;
        } else if (Type.PLAYLIST.getPattern().matcher(uri).matches()) {
            type = Type.PLAYLIST;
        } else {
            throw new InvalidCommandException("Unsupported URI! Supported: spotify:track, spotify:album, spotify:playlist");
        }
        id = parseId(uri);
    }

    public static SpotifyUri parse(String uri) {
        return new SpotifyUri(uri);
    }

    public static boolean isSpotifyUri(String s) {
        return URI_REGEX.matcher(s).matches();
    }

    private static String parseId(String s) {
        String[] split = s.split(":");
        return split[split.length - 1];
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public List<Playable> loadPlayables(PlayableFactory playableFactory,
                                        SpotifyService spotifyService,
                                        boolean redirect,
                                        boolean mayInterrupt) throws Exception {
        return type.loadPlayables(playableFactory, spotifyService, this, redirect, mayInterrupt);
    }

    public enum Type {

        TRACK(Pattern.compile("spotify:track:([a-zA-Z0-9])([a-zA-Z0-9])*")) {
            @Override
            public List<Playable> loadPlayables(PlayableFactory playableFactory,
                                                SpotifyService spotifyService,
                                                SpotifyUri uri,
                                                boolean redirect,
                                                boolean mayInterrupt) throws Exception {
                Invoker invoker = new Invoker();
                Track track;
                try {
                    track = invoker.runWithCredentials(spotifyService.getSpotifyApi(), () -> spotifyService.getTrack(uri.getId()));
                } catch (NotFoundException e) {
                    throw new InvalidCommandException("Invalid id " + uri.getId());
                }
                return Lists.newArrayList(playableFactory.createPlayable(redirect, track));
            }
        },
        ALBUM(Pattern.compile("spotify:album:([a-zA-Z0-9])([a-zA-Z0-9])*")) {
            @Override
            public List<Playable> loadPlayables(PlayableFactory playableFactory,
                                                SpotifyService spotifyService,
                                                SpotifyUri uri,
                                                boolean redirect,
                                                boolean mayInterrupt) throws Exception {
                Invoker invoker = new Invoker();
                List<Track> tracks;
                try {
                    tracks = invoker.runWithCredentials(spotifyService.getSpotifyApi(), () -> spotifyService.getAlbumTracks(uri.getId()));
                } catch (NotFoundException e) {
                    throw new InvalidCommandException("Invalid id " + uri.getId());
                }
                return playableFactory.createPlayables(redirect, tracks, mayInterrupt);
            }
        },
        PLAYLIST(Pattern.compile("spotify:playlist:([a-zA-Z0-9])([a-zA-Z0-9])*")) {
            @Override
            public List<Playable> loadPlayables(PlayableFactory playableFactory,
                                                SpotifyService spotifyService,
                                                SpotifyUri uri,
                                                boolean redirect,
                                                boolean mayInterrupt) throws Exception {
                Invoker invoker = new Invoker();
                List<Track> tracks;
                try {
                    tracks = invoker.runWithCredentials(spotifyService.getSpotifyApi(), () -> spotifyService.getPlaylistTracks(uri.getId()));
                } catch (NotFoundException e) {
                    throw new InvalidCommandException("Invalid id " + uri.getId());
                }
                return playableFactory.createPlayables(redirect, tracks, mayInterrupt);
            }
        };

        private final Pattern pattern;

        Type(Pattern pattern) {
            this.pattern = pattern;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public abstract List<Playable> loadPlayables(PlayableFactory playableFactory,
                                                     SpotifyService spotifyService,
                                                     SpotifyUri uri,
                                                     boolean redirect,
                                                     boolean mayInterrupt) throws Exception;

    }

}
