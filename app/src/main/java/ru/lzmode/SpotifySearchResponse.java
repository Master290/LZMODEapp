package ru.lzmode;

import java.util.List;

public class SpotifySearchResponse {
    private Tracks tracks;

    public Tracks getTracks() {
        return tracks;
    }

    public static class Tracks {
        private List<Item> items;

        public List<Item> getItems() {
            return items;
        }
    }

    public static class Item {
        private Album album;

        public Album getAlbum() {
            return album;
        }
    }

    public static class Album {
        private List<Image> images;

        public List<Image> getImages() {
            return images;
        }
    }

    public static class Image {
        private String url;

        public String getUrl() {
            return url;
        }
    }
}
