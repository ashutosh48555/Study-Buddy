// Multi-Source Music Service with Fallback System
// Combines multiple free music sources to avoid API limits

import JamendoMusicService from './jamendo-music-service.js';
import FreesoundService from './freesound-service.js';
import ArchiveOrgService from './archive-org-service.js';
import YouTubeService from './youtube-service.js';
import config from './config.js';

class MultiSourceMusicService {
  constructor(userConfig = {}) {
    // Merge user config with default config
    this.config = { ...config, ...userConfig };
    
    this.sources = {
      jamendo: new JamendoMusicService(this.config.jamendo),
      freesound: new FreesoundService(this.config.freesound),
      archive: new ArchiveOrgService(this.config.archive),
      youtube: new YouTubeService(this.config.youtube)
    };
    
    this.currentSource = 'jamendo';
    this.fallbackOrder = ['jamendo', 'freesound', 'archive', 'youtube'];
    this.rateLimits = {
      jamendo: { limit: 10000, used: 0, resetTime: Date.now() + 24 * 60 * 60 * 1000 },
      freesound: { limit: 2000, used: 0, resetTime: Date.now() + 24 * 60 * 60 * 1000 },
      archive: { limit: Infinity, used: 0, resetTime: Date.now() + 24 * 60 * 60 * 1000 },
      youtube: { limit: 100, used: 0, resetTime: Date.now() + 24 * 60 * 60 * 1000 }
    };
    
    this.cache = new Map();
    this.currentPlaylist = [];
    this.currentIndex = 0;
  }

  // Check if source is available (not rate limited)
  isSourceAvailable(sourceName) {
    const rateLimit = this.rateLimits[sourceName];
    
    // Reset counter if 24 hours have passed
    if (Date.now() > rateLimit.resetTime) {
      rateLimit.used = 0;
      rateLimit.resetTime = Date.now() + 24 * 60 * 60 * 1000;
    }
    
    return rateLimit.used < rateLimit.limit;
  }

  // Get the next available source
  getNextAvailableSource() {
    for (const source of this.fallbackOrder) {
      if (this.isSourceAvailable(source)) {
        return source;
      }
    }
    return null;
  }

  // Increment usage counter for a source
  incrementUsage(sourceName) {
    this.rateLimits[sourceName].used++;
  }

  // Get lofi tracks from available sources
  async getLofiTracks(limit = 20, offset = 0) {
    const cacheKey = `lofi_tracks_${limit}_${offset}`;
    
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey);
    }

    // Try each source in order
    for (const sourceName of this.fallbackOrder) {
      if (!this.isSourceAvailable(sourceName)) {
        console.log(`Source ${sourceName} is rate limited, trying next...`);
        continue;
      }

      try {
        const service = this.sources[sourceName];
        const tracks = await service.getLofiTracks(limit, offset);
        
        if (tracks && tracks.length > 0) {
          this.incrementUsage(sourceName);
          this.cache.set(cacheKey, tracks);
          console.log(`Successfully fetched ${tracks.length} tracks from ${sourceName}`);
          return tracks;
        }
      } catch (error) {
        console.error(`Error fetching from ${sourceName}:`, error);
        continue;
      }
    }

    console.error('All sources failed or are rate limited');
    return [];
  }

  // Create a mixed playlist from multiple sources
  async createMixedPlaylist(tracksPerSource = 10) {
    const allTracks = [];
    
    for (const sourceName of this.fallbackOrder) {
      if (!this.isSourceAvailable(sourceName)) continue;
      
      try {
        const service = this.sources[sourceName];
        const tracks = await service.getLofiTracks(tracksPerSource);
        
        if (tracks && tracks.length > 0) {
          // Add source identifier to each track
          const sourceTracks = tracks.map(track => ({
            ...track,
            source: sourceName
          }));
          
          allTracks.push(...sourceTracks);
          this.incrementUsage(sourceName);
        }
      } catch (error) {
        console.error(`Error fetching from ${sourceName}:`, error);
      }
    }

    // Shuffle the mixed playlist
    for (let i = allTracks.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [allTracks[i], allTracks[j]] = [allTracks[j], allTracks[i]];
    }

    this.currentPlaylist = allTracks;
    this.currentIndex = 0;
    return allTracks;
  }

  // Get next track with automatic source switching
  async getNextTrack() {
    if (this.currentPlaylist.length === 0) {
      await this.createMixedPlaylist();
    }

    if (this.currentPlaylist.length === 0) {
      console.error('No tracks available from any source');
      return null;
    }

    const track = this.currentPlaylist[this.currentIndex];
    this.currentIndex = (this.currentIndex + 1) % this.currentPlaylist.length;
    
    // Load more tracks when we're near the end
    if (this.currentIndex >= this.currentPlaylist.length - 5) {
      this.loadMoreTracks();
    }
    
    return track;
  }

  // Load more tracks from available sources
  async loadMoreTracks() {
    const newTracks = await this.getLofiTracks(20, this.currentPlaylist.length);
    if (newTracks.length > 0) {
      this.currentPlaylist = [...this.currentPlaylist, ...newTracks];
    }
  }

  // Get rate limit status for all sources
  getRateLimitStatus() {
    const status = {};
    
    for (const [sourceName, rateLimit] of Object.entries(this.rateLimits)) {
      status[sourceName] = {
        used: rateLimit.used,
        limit: rateLimit.limit,
        remaining: rateLimit.limit - rateLimit.used,
        resetTime: new Date(rateLimit.resetTime).toLocaleString(),
        available: this.isSourceAvailable(sourceName)
      };
    }
    
    return status;
  }
}

// Freesound.org API Service
class FreesoundService {
  constructor() {
    this.baseUrl = 'https://freesound.org/apiv2';
    this.token = 'YOUR_FREESOUND_TOKEN'; // Get from https://freesound.org/
  }

  async getLofiTracks(limit = 20, offset = 0) {
    try {
      const params = new URLSearchParams({
        token: this.token,
        query: 'lofi OR chillhop OR ambient',
        filter: 'duration:[30.0 TO 600.0] type:wav',
        sort: 'score',
        page_size: limit,
        page: Math.floor(offset / limit) + 1,
        fields: 'id,name,description,duration,download,previews,images,license,username'
      });

      const response = await fetch(`${this.baseUrl}/search/text/?${params}`);
      const data = await response.json();

      return data.results?.map(track => ({
        id: track.id,
        name: track.name,
        artist: track.username,
        album: 'Freesound',
        duration: track.duration,
        image: track.images?.waveform_m,
        streamUrl: track.previews?.['preview-hq-mp3'],
        downloadUrl: track.download,
        license: track.license,
        tags: ['lofi', 'freesound']
      })) || [];
    } catch (error) {
      console.error('Error fetching from Freesound:', error);
      return [];
    }
  }
}

// Archive.org API Service
class ArchiveOrgService {
  constructor() {
    this.baseUrl = 'https://archive.org/advancedsearch.php';
  }

  async getLofiTracks(limit = 20, offset = 0) {
    try {
      const params = new URLSearchParams({
        q: 'collection:opensource_audio AND (lofi OR chillhop OR ambient)',
        fl: 'identifier,title,creator,date,description,downloads,format',
        sort: 'downloads desc',
        rows: limit,
        start: offset,
        output: 'json'
      });

      const response = await fetch(`${this.baseUrl}?${params}`);
      const data = await response.json();

      const tracks = [];
      for (const doc of data.response?.docs || []) {
        const detailResponse = await fetch(`https://archive.org/details/${doc.identifier}&output=json`);
        const details = await detailResponse.json();
        
        const mp3Files = details.files?.filter(file => file.format === 'VBR MP3') || [];
        
        for (const file of mp3Files.slice(0, 2)) { // Limit to 2 files per item
          tracks.push({
            id: `${doc.identifier}_${file.name}`,
            name: file.title || file.name,
            artist: doc.creator || 'Unknown',
            album: doc.title,
            duration: file.length ? parseFloat(file.length) : 0,
            image: `https://archive.org/services/img/${doc.identifier}`,
            streamUrl: `https://archive.org/download/${doc.identifier}/${file.name}`,
            downloadUrl: `https://archive.org/download/${doc.identifier}/${file.name}`,
            license: 'Public Domain',
            tags: ['lofi', 'archive']
          });
        }
      }

      return tracks;
    } catch (error) {
      console.error('Error fetching from Archive.org:', error);
      return [];
    }
  }
}

// YouTube API Service (with multiple API keys)
class YouTubeService {
  constructor() {
    this.apiKeys = [
      'YOUR_YOUTUBE_API_KEY_1',
      'YOUR_YOUTUBE_API_KEY_2',
      'YOUR_YOUTUBE_API_KEY_3'
    ];
    this.currentKeyIndex = 0;
    this.baseUrl = 'https://www.googleapis.com/youtube/v3';
  }

  getCurrentApiKey() {
    return this.apiKeys[this.currentKeyIndex];
  }

  rotateApiKey() {
    this.currentKeyIndex = (this.currentKeyIndex + 1) % this.apiKeys.length;
  }

  async getLofiTracks(limit = 20, offset = 0) {
    try {
      const params = new URLSearchParams({
        key: this.getCurrentApiKey(),
        part: 'snippet',
        q: 'lofi hip hop beats to relax study',
        type: 'video',
        maxResults: limit,
        order: 'relevance',
        videoDuration: 'medium'
      });

      const response = await fetch(`${this.baseUrl}/search?${params}`);
      const data = await response.json();

      if (data.error) {
        console.log('YouTube API key limit reached, rotating...');
        this.rotateApiKey();
        throw new Error('API key limit reached');
      }

      return data.items?.map(item => ({
        id: item.id.videoId,
        name: item.snippet.title,
        artist: item.snippet.channelTitle,
        album: 'YouTube',
        duration: 0, // Would need additional API call
        image: item.snippet.thumbnails.medium.url,
        streamUrl: `https://www.youtube.com/watch?v=${item.id.videoId}`,
        downloadUrl: null,
        license: 'YouTube',
        tags: ['lofi', 'youtube']
      })) || [];
    } catch (error) {
      console.error('Error fetching from YouTube:', error);
      return [];
    }
  }
}

export default MultiSourceMusicService;
