// Simple Usage Example for Multi-Source Music Player
// This shows how to use the music player without any server setup

import MultiSourceMusicService from './multi-source-music-service.js';

// Initialize the music service
const musicPlayer = new MultiSourceMusicService();

// Example: Get lofi tracks
async function getLofiTracks() {
  try {
    console.log('🎵 Fetching lofi tracks...');
    const tracks = await musicPlayer.getLofiTracks(10);
    
    console.log(`Found ${tracks.length} tracks:`);
    tracks.forEach((track, index) => {
      console.log(`${index + 1}. "${track.name}" by ${track.artist} (${track.source})`);
      console.log(`   Stream URL: ${track.streamUrl}`);
    });
    
    return tracks;
  } catch (error) {
    console.error('Error fetching tracks:', error);
    return [];
  }
}

// Example: Create a mixed playlist
async function createMixedPlaylist() {
  try {
    console.log('🎵 Creating mixed playlist...');
    const playlist = await musicPlayer.createMixedPlaylist(5); // 5 tracks per source
    
    console.log(`Created playlist with ${playlist.length} tracks:`);
    
    // Group by source
    const bySource = {};
    playlist.forEach(track => {
      if (!bySource[track.source]) bySource[track.source] = [];
      bySource[track.source].push(track);
    });
    
    Object.entries(bySource).forEach(([source, tracks]) => {
      console.log(`\n${source.toUpperCase()}: ${tracks.length} tracks`);
      tracks.forEach(track => {
        console.log(`  - "${track.name}" by ${track.artist}`);
      });
    });
    
    return playlist;
  } catch (error) {
    console.error('Error creating playlist:', error);
    return [];
  }
}

// Example: Play next track
async function playNextTrack() {
  try {
    const track = await musicPlayer.getNextTrack();
    if (track) {
      console.log(`🎵 Now playing: "${track.name}" by ${track.artist}`);
      console.log(`   Source: ${track.source}`);
      console.log(`   Stream URL: ${track.streamUrl}`);
      
      // Here you would integrate with your audio player
      // For example: audioPlayer.play(track.streamUrl);
      
      return track;
    } else {
      console.log('No more tracks available');
      return null;
    }
  } catch (error) {
    console.error('Error playing next track:', error);
    return null;
  }
}

// Example: Check rate limits
function checkRateLimits() {
  const status = musicPlayer.getRateLimitStatus();
  console.log('\n⚡ Rate Limit Status:');
  
  Object.entries(status).forEach(([source, info]) => {
    const percentage = ((info.used / info.limit) * 100).toFixed(1);
    const available = info.available ? '✅' : '❌';
    
    console.log(`${available} ${source.toUpperCase()}:`);
    console.log(`   Used: ${info.used}/${info.limit} (${percentage}%)`);
    console.log(`   Remaining: ${info.remaining}`);
    console.log(`   Resets: ${info.resetTime}`);
  });
}

// Example: Search for specific tracks
async function searchTracks(query) {
  try {
    console.log(`🔍 Searching for: "${query}"`);
    
    // Search in each source
    const results = {};
    for (const [sourceName, service] of Object.entries(musicPlayer.sources)) {
      if (service.searchTracks) {
        const tracks = await service.searchTracks(query, 3);
        if (tracks.length > 0) {
          results[sourceName] = tracks;
        }
      }
    }
    
    console.log('Search results:');
    Object.entries(results).forEach(([source, tracks]) => {
      console.log(`\n${source.toUpperCase()}: ${tracks.length} results`);
      tracks.forEach(track => {
        console.log(`  - "${track.name}" by ${track.artist}`);
      });
    });
    
    return results;
  } catch (error) {
    console.error('Error searching tracks:', error);
    return {};
  }
}

// HTML Integration Example
function createPlayerHTML() {
  return `
    <div id="lofi-player">
      <h2>Lofi Music Player</h2>
      <div id="current-track">
        <div id="track-info">No track selected</div>
        <audio id="audio-player" controls style="width: 100%;"></audio>
      </div>
      <div id="controls">
        <button onclick="loadTracks()">Load Tracks</button>
        <button onclick="playNext()">Next Track</button>
        <button onclick="checkLimits()">Check Limits</button>
        <input type="text" id="search-input" placeholder="Search tracks...">
        <button onclick="searchMusic()">Search</button>
      </div>
      <div id="playlist"></div>
    </div>
  `;
}

// JavaScript functions for HTML integration
window.loadTracks = async function() {
  const tracks = await getLofiTracks();
  displayPlaylist(tracks);
};

window.playNext = async function() {
  const track = await playNextTrack();
  if (track) {
    document.getElementById('track-info').textContent = `${track.name} by ${track.artist}`;
    document.getElementById('audio-player').src = track.streamUrl;
  }
};

window.checkLimits = function() {
  checkRateLimits();
};

window.searchMusic = async function() {
  const query = document.getElementById('search-input').value;
  if (query) {
    const results = await searchTracks(query);
    displaySearchResults(results);
  }
};

function displayPlaylist(tracks) {
  const playlistDiv = document.getElementById('playlist');
  playlistDiv.innerHTML = '<h3>Current Playlist:</h3>';
  
  tracks.forEach((track, index) => {
    const trackDiv = document.createElement('div');
    trackDiv.innerHTML = `
      <p>${index + 1}. "${track.name}" by ${track.artist} (${track.source})</p>
      <button onclick="playTrack('${track.streamUrl}', '${track.name}', '${track.artist}')">Play</button>
    `;
    playlistDiv.appendChild(trackDiv);
  });
}

window.playTrack = function(url, name, artist) {
  document.getElementById('track-info').textContent = `${name} by ${artist}`;
  document.getElementById('audio-player').src = url;
};

function displaySearchResults(results) {
  const playlistDiv = document.getElementById('playlist');
  playlistDiv.innerHTML = '<h3>Search Results:</h3>';
  
  Object.entries(results).forEach(([source, tracks]) => {
    const sourceDiv = document.createElement('div');
    sourceDiv.innerHTML = `<h4>${source.toUpperCase()}</h4>`;
    
    tracks.forEach(track => {
      const trackDiv = document.createElement('div');
      trackDiv.innerHTML = `
        <p>"${track.name}" by ${track.artist}</p>
        <button onclick="playTrack('${track.streamUrl}', '${track.name}', '${track.artist}')">Play</button>
      `;
      sourceDiv.appendChild(trackDiv);
    });
    
    playlistDiv.appendChild(sourceDiv);
  });
}

// Export functions for use in other files
export {
  getLofiTracks,
  createMixedPlaylist,
  playNextTrack,
  checkRateLimits,
  searchTracks,
  createPlayerHTML
};

// Auto-run example if this file is executed directly
if (typeof window === 'undefined') {
  // Node.js environment
  console.log('🚀 Running Multi-Source Music Player Example');
  
  (async () => {
    await getLofiTracks();
    await createMixedPlaylist();
    await playNextTrack();
    checkRateLimits();
    await searchTracks('chill');
  })();
}
