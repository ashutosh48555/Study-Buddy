// YouTube Direct API Service
// Uses client-side API calls without backend server

class YouTubeService {
  constructor(config = {}) {
    this.apiKeys = config.apiKeys || [
      'YOUR_YOUTUBE_API_KEY_1',
      'YOUR_YOUTUBE_API_KEY_2',
      'YOUR_YOUTUBE_API_KEY_3'
    ];
    this.currentKeyIndex = 0;
    this.baseUrl = 'https://www.googleapis.com/youtube/v3';
    this.corsProxy = 'https://cors-anywhere.herokuapp.com/'; // For CORS handling
  }

  getCurrentApiKey() {
    return this.apiKeys[this.currentKeyIndex];
  }

  rotateApiKey() {
    this.currentKeyIndex = (this.currentKeyIndex + 1) % this.apiKeys.length;
  }

  // Get lofi tracks from YouTube
  async getLofiTracks(limit = 20, offset = 0) {
    try {
      const params = new URLSearchParams({
        key: this.getCurrentApiKey(),
        part: 'snippet',
        q: 'lofi hip hop beats to relax study',
        type: 'video',
        maxResults: Math.min(limit, 50), // YouTube max is 50
        order: 'relevance',
        videoDuration: 'medium',
        videoLicense: 'any'
      });

      const url = `${this.corsProxy}${this.baseUrl}/search?${params}`;
      const response = await fetch(url);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();

      if (data.error) {
        console.log('YouTube API key limit reached, rotating...');
        this.rotateApiKey();
        throw new Error(`API Error: ${data.error.message}`);
      }

      return data.items?.map(item => ({
        id: `youtube_${item.id.videoId}`,
        name: item.snippet.title,
        artist: item.snippet.channelTitle,
        album: 'YouTube',
        duration: 0, // Would need additional API call to get duration
        image: item.snippet.thumbnails.medium.url,
        streamUrl: `https://www.youtube.com/watch?v=${item.id.videoId}`,
        downloadUrl: null,
        license: 'YouTube',
        tags: ['youtube', 'lofi'],
        source: 'youtube'
      })) || [];
    } catch (error) {
      console.error('YouTube API error:', error);
      return [];
    }
  }

  // Search YouTube tracks
  async searchTracks(query, limit = 20) {
    try {
      const params = new URLSearchParams({
        key: this.getCurrentApiKey(),
        part: 'snippet',
        q: `${query} lofi hip hop`,
        type: 'video',
        maxResults: Math.min(limit, 50),
        order: 'relevance',
        videoDuration: 'medium'
      });

      const url = `${this.corsProxy}${this.baseUrl}/search?${params}`;
      const response = await fetch(url);
      const data = await response.json();

      if (data.error) {
        this.rotateApiKey();
        throw new Error(`API Error: ${data.error.message}`);
      }

      return data.items?.map(item => ({
        id: `youtube_${item.id.videoId}`,
        name: item.snippet.title,
        artist: item.snippet.channelTitle,
        album: 'YouTube',
        duration: 0,
        image: item.snippet.thumbnails.medium.url,
        streamUrl: `https://www.youtube.com/watch?v=${item.id.videoId}`,
        downloadUrl: null,
        license: 'YouTube',
        tags: ['youtube', 'lofi'],
        source: 'youtube'
      })) || [];
    } catch (error) {
      console.error('YouTube search error:', error);
      return [];
    }
  }
}

export default YouTubeService;
