// Jamendo API Service for Lofi Music Streaming
// No API key required for basic usage, 10,000 requests/day limit

class JamendoMusicService {
  constructor(config = {}) {
    this.baseUrl = 'https://api.jamendo.com/v3.0';
    this.clientId = config.clientId || 'YOUR_CLIENT_ID'; // Get from https://developer.jamendo.com/
    this.cache = new Map();
    this.currentPlaylist = [];
    this.currentIndex = 0;
  }

  // Get lofi/chillhop tracks
  async getLofiTracks(limit = 20, offset = 0) {
    const cacheKey = `lofi_tracks_${limit}_${offset}`;
    
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey);
    }

    try {
      const params = new URLSearchParams({
        client_id: this.clientId,
        format: 'json',
        limit: limit,
        offset: offset,
        order: 'popularity_total',
        tags: 'lofi,chillhop,ambient,downtempo,chillout', // Multiple tags for better results
        include: 'musicinfo',
        audioformat: 'mp3'
      });

      const response = await fetch(`${this.baseUrl}/tracks/?${params}`);
      const data = await response.json();

      if (data.headers.status === 'success') {
        const tracks = data.results.map(track => ({
          id: track.id,
          name: track.name,
          artist: track.artist_name,
          album: track.album_name,
          duration: track.duration,
          image: track.album_image || track.artist_image,
          streamUrl: track.audio,
          downloadUrl: track.audiodownload,
          license: track.license_ccurl,
          tags: track.musicinfo?.tags || []
        }));

        this.cache.set(cacheKey, tracks);
        return tracks;
      }
    } catch (error) {
      console.error('Error fetching tracks from Jamendo:', error);
      return [];
    }
  }

  // Search for specific lofi tracks
  async searchLofiTracks(query, limit = 10) {
    try {
      const params = new URLSearchParams({
        client_id: this.clientId,
        format: 'json',
        limit: limit,
        namesearch: query,
        tags: 'lofi,chillhop,ambient',
        include: 'musicinfo',
        audioformat: 'mp3'
      });

      const response = await fetch(`${this.baseUrl}/tracks/?${params}`);
      const data = await response.json();

      if (data.headers.status === 'success') {
        return data.results.map(track => ({
          id: track.id,
          name: track.name,
          artist: track.artist_name,
          album: track.album_name,
          duration: track.duration,
          image: track.album_image || track.artist_image,
          streamUrl: track.audio,
          downloadUrl: track.audiodownload,
          license: track.license_ccurl,
          tags: track.musicinfo?.tags || []
        }));
      }
    } catch (error) {
      console.error('Error searching tracks:', error);
      return [];
    }
  }

  // Get curated lofi playlists
  async getLofiPlaylists(limit = 10) {
    try {
      const params = new URLSearchParams({
        client_id: this.clientId,
        format: 'json',
        limit: limit,
        order: 'popularity_total',
        namesearch: 'lofi,chillhop,ambient'
      });

      const response = await fetch(`${this.baseUrl}/playlists/?${params}`);
      const data = await response.json();

      if (data.headers.status === 'success') {
        return data.results.map(playlist => ({
          id: playlist.id,
          name: playlist.name,
          user: playlist.user_name,
          tracks: playlist.tracks,
          createdAt: playlist.creationdate
        }));
      }
    } catch (error) {
      console.error('Error fetching playlists:', error);
      return [];
    }
  }

  // Get tracks from a specific playlist
  async getPlaylistTracks(playlistId) {
    try {
      const params = new URLSearchParams({
        client_id: this.clientId,
        format: 'json',
        id: playlistId,
        include: 'tracks'
      });

      const response = await fetch(`${this.baseUrl}/playlists/tracks/?${params}`);
      const data = await response.json();

      if (data.headers.status === 'success') {
        return data.results[0].tracks.map(track => ({
          id: track.id,
          name: track.name,
          artist: track.artist_name,
          album: track.album_name,
          duration: track.duration,
          image: track.album_image || track.artist_image,
          streamUrl: track.audio,
          downloadUrl: track.audiodownload,
          license: track.license_ccurl
        }));
      }
    } catch (error) {
      console.error('Error fetching playlist tracks:', error);
      return [];
    }
  }

  // Create a continuous lofi stream
  async createLofiStream(initialTracks = 50) {
    try {
      const tracks = await this.getLofiTracks(initialTracks);
      this.currentPlaylist = tracks;
      this.currentIndex = 0;
      return this.currentPlaylist;
    } catch (error) {
      console.error('Error creating lofi stream:', error);
      return [];
    }
  }

  // Get next track in stream
  getNextTrack() {
    if (this.currentPlaylist.length === 0) return null;
    
    const track = this.currentPlaylist[this.currentIndex];
    this.currentIndex = (this.currentIndex + 1) % this.currentPlaylist.length;
    
    // Load more tracks when we're near the end
    if (this.currentIndex >= this.currentPlaylist.length - 5) {
      this.loadMoreTracks();
    }
    
    return track;
  }

  // Load more tracks to extend the stream
  async loadMoreTracks() {
    try {
      const newTracks = await this.getLofiTracks(20, this.currentPlaylist.length);
      this.currentPlaylist = [...this.currentPlaylist, ...newTracks];
    } catch (error) {
      console.error('Error loading more tracks:', error);
    }
  }

  // Get current track info
  getCurrentTrack() {
    if (this.currentPlaylist.length === 0) return null;
    return this.currentPlaylist[this.currentIndex];
  }

  // Shuffle the current playlist
  shufflePlaylist() {
    for (let i = this.currentPlaylist.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [this.currentPlaylist[i], this.currentPlaylist[j]] = [this.currentPlaylist[j], this.currentPlaylist[i]];
    }
    this.currentIndex = 0;
  }
}

// Usage example
async function initializeLofiPlayer() {
  const musicService = new JamendoMusicService();
  
  // Create initial stream
  const tracks = await musicService.createLofiStream(30);
  console.log(`Loaded ${tracks.length} lofi tracks`);
  
  // Get next track to play
  const nextTrack = musicService.getNextTrack();
  if (nextTrack) {
    console.log(`Now playing: ${nextTrack.name} by ${nextTrack.artist}`);
    console.log(`Stream URL: ${nextTrack.streamUrl}`);
  }
  
  return musicService;
}

export default JamendoMusicService;
