// Freesound.org Direct API Service
// Uses client-side API calls without backend server

class FreesoundService {
  constructor(config = {}) {
    this.baseUrl = 'https://freesound.org/apiv2';
    this.token = config.token || 'YOUR_FREESOUND_TOKEN';
    this.corsProxy = 'https://cors-anywhere.herokuapp.com/'; // For CORS handling
  }

  // Get lofi tracks from Freesound
  async getLofiTracks(limit = 20, offset = 0) {
    try {
      const params = new URLSearchParams({
        token: this.token,
        query: 'lofi OR chillhop OR ambient OR chill OR downtempo',
        filter: 'duration:[30.0 TO 600.0] type:wav OR type:mp3',
        sort: 'score',
        page_size: Math.min(limit, 150), // Freesound max is 150
        page: Math.floor(offset / limit) + 1,
        fields: 'id,name,description,duration,download,previews,images,license,username,tags'
      });

      const url = `${this.corsProxy}${this.baseUrl}/search/text/?${params}`;
      const response = await fetch(url);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();

      return data.results?.map(track => ({
        id: `freesound_${track.id}`,
        name: track.name,
        artist: track.username,
        album: 'Freesound',
        duration: Math.floor(track.duration),
        image: track.images?.waveform_m || track.images?.spectral_m,
        streamUrl: track.previews?.['preview-hq-mp3'] || track.previews?.['preview-lq-mp3'],
        downloadUrl: track.download,
        license: track.license,
        tags: track.tags || ['freesound', 'creative-commons'],
        source: 'freesound'
      })) || [];
    } catch (error) {
      console.error('Freesound API error:', error);
      return [];
    }
  }

  // Search specific tracks
  async searchTracks(query, limit = 20) {
    try {
      const params = new URLSearchParams({
        token: this.token,
        query: `${query} AND (lofi OR chillhop OR ambient)`,
        filter: 'duration:[30.0 TO 600.0]',
        sort: 'score',
        page_size: Math.min(limit, 150),
        fields: 'id,name,description,duration,download,previews,images,license,username,tags'
      });

      const url = `${this.corsProxy}${this.baseUrl}/search/text/?${params}`;
      const response = await fetch(url);
      const data = await response.json();

      return data.results?.map(track => ({
        id: `freesound_${track.id}`,
        name: track.name,
        artist: track.username,
        album: 'Freesound',
        duration: Math.floor(track.duration),
        image: track.images?.waveform_m,
        streamUrl: track.previews?.['preview-hq-mp3'],
        downloadUrl: track.download,
        license: track.license,
        tags: track.tags || ['freesound'],
        source: 'freesound'
      })) || [];
    } catch (error) {
      console.error('Freesound search error:', error);
      return [];
    }
  }

  // Get track details by ID
  async getTrackById(freesoundId) {
    try {
      const params = new URLSearchParams({
        token: this.token,
        fields: 'id,name,description,duration,download,previews,images,license,username,tags'
      });

      const url = `${this.corsProxy}${this.baseUrl}/sounds/${freesoundId}/?${params}`;
      const response = await fetch(url);
      const track = await response.json();

      return {
        id: `freesound_${track.id}`,
        name: track.name,
        artist: track.username,
        album: 'Freesound',
        duration: Math.floor(track.duration),
        image: track.images?.waveform_m,
        streamUrl: track.previews?.['preview-hq-mp3'],
        downloadUrl: track.download,
        license: track.license,
        tags: track.tags || ['freesound'],
        source: 'freesound'
      };
    } catch (error) {
      console.error('Freesound track fetch error:', error);
      return null;
    }
  }
}

export default FreesoundService;
