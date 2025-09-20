// Archive.org Direct API Service
// Uses client-side API calls without backend server

class ArchiveOrgService {
  constructor(config = {}) {
    this.baseUrl = 'https://archive.org/advancedsearch.php';
    this.corsProxy = 'https://cors-anywhere.herokuapp.com/'; // For CORS handling
  }

  // Get lofi tracks from Archive.org
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

      const url = `${this.corsProxy}${this.baseUrl}?${params}`;
      const response = await fetch(url);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();

      const tracks = [];
      for (const doc of data.response?.docs || []) {
        const detailResponse = await fetch(`https://archive.org/details/${doc.identifier}output=json`);
        const details = await detailResponse.json();
        const mp3Files = details.files?.filter(file => file.format === 'VBR MP3') || [];

        for (const file of mp3Files.slice(0, 1)) { // Limit one file per item
          tracks.push({
            id: `archive_${file.name}`,
            name: file.title || file.name,
            artist: doc.creator || 'Unknown',
            album: doc.title,
            duration: file.length ? parseFloat(file.length) : 0,
            image: `https://archive.org/services/img/${doc.identifier}`,
            streamUrl: `https://archive.org/download/${doc.identifier}/${file.name}`,
            license: 'Public Domain',
            tags: ['archive', 'public-domain'],
            source: 'archive'
          });
        }
      }

      return tracks;
    } catch (error) {
      console.error('Archive.org API error:', error);
      return [];
    }
  }
}

export default ArchiveOrgService;
