# Multi-Source Lofi Music Player

A client-side music player that streams lofi tracks from multiple free sources without requiring a backend server.

## Features

✅ **No Server Required** - Runs entirely in the browser using API keys  
✅ **Multiple Sources** - Jamendo, Freesound, Archive.org, YouTube  
✅ **Rate Limit Handling** - Automatic fallback when APIs hit limits  
✅ **Mixed Playlists** - Combines tracks from multiple sources  
✅ **Search Functionality** - Search across all sources  
✅ **Continuous Playback** - Auto-loads more tracks  

## Quick Start

1. **Get API Keys**:
   - [Jamendo](https://developer.jamendo.com/) - Get client_id (10,000 requests/day)
   - [Freesound](https://freesound.org/apiv2/apply/) - Get API token (2,000 requests/day)
   - [YouTube](https://console.developers.google.com/) - Get API keys (100 requests/day each)
   - Archive.org - No API key needed

2. **Configure**:
   ```javascript
   // Update config.js with your API keys
   const config = {
     jamendo: { clientId: 'YOUR_JAMENDO_CLIENT_ID' },
     freesound: { token: 'YOUR_FREESOUND_TOKEN' },
     youtube: { apiKeys: ['YOUR_YOUTUBE_API_KEY'] }
   };
   ```

3. **Use**:
   ```javascript
   import MultiSourceMusicService from './multi-source-music-service.js';
   
   const player = new MultiSourceMusicService();
   const tracks = await player.getLofiTracks(20);
   ```

## API Sources

### Jamendo (Primary)
- **Rate Limit**: 10,000 requests/day
- **Content**: High-quality Creative Commons music
- **Best For**: Curated lofi tracks

### Freesound (Secondary)
- **Rate Limit**: 2,000 requests/day
- **Content**: User-generated Creative Commons sounds
- **Best For**: Ambient sounds, short clips

### Archive.org (Fallback)
- **Rate Limit**: Unlimited
- **Content**: Public domain music
- **Best For**: Reliable fallback source

### YouTube (Optional)
- **Rate Limit**: 100 requests/day per API key
- **Content**: Largest music library
- **Best For**: Popular lofi channels

## Files Overview

- `multi-source-music-service.js` - Main service combining all sources
- `jamendo-music-service.js` - Jamendo API integration
- `freesound-service.js` - Freesound API integration
- `archive-org-service.js` - Archive.org API integration
- `youtube-service.js` - YouTube API integration
- `config.js` - Configuration and API keys
- `simple-usage-example.js` - Usage examples
- `demo.html` - Working demo page

## Usage Examples

### Basic Usage
```javascript
const player = new MultiSourceMusicService();

// Get tracks from all sources
const tracks = await player.getLofiTracks(20);

// Create mixed playlist
const playlist = await player.createMixedPlaylist(5);

// Play next track
const nextTrack = await player.getNextTrack();
```

### Search Across Sources
```javascript
const results = await player.searchTracks('chill');
```

### Check Rate Limits
```javascript
const status = player.getRateLimitStatus();
console.log(status);
```

### HTML Integration
```html
<script type="module">
  import { getLofiTracks, createPlayerHTML } from './simple-usage-example.js';
  
  document.body.innerHTML = createPlayerHTML();
  const tracks = await getLofiTracks();
</script>
```

## Android Integration

For Android apps, you can:

1. **WebView Integration**: Use the HTML demo in a WebView
2. **Native API Calls**: Convert JS services to Java/Kotlin
3. **Hybrid Approach**: Use JS in WebView, control from native code

## Rate Limit Management

The system automatically:
- Tracks usage for each source
- Rotates to available sources when limits are reached
- Resets counters after 24 hours
- Provides fallback order: Jamendo → Freesound → Archive → YouTube

## CORS Handling

For client-side usage, the services use a CORS proxy. You can:
- Use the default: `https://cors-anywhere.herokuapp.com/`
- Deploy your own CORS proxy
- Use a browser extension for development

## Demo

Open `demo.html` in your browser to see the player in action (uses mock data).

## Development

1. Install dependencies:
   ```bash
   npm install
   ```

2. Run tests:
   ```bash
   npm test
   ```

3. Start development:
   ```bash
   npm start
   ```

## API Key Setup

### Jamendo
1. Go to https://developer.jamendo.com/
2. Create account
3. Get client_id
4. Update `config.js`

### Freesound
1. Go to https://freesound.org/apiv2/apply/
2. Create account and apply for API access
3. Get API token
4. Update `config.js`

### YouTube
1. Go to https://console.developers.google.com/
2. Create new project
3. Enable YouTube Data API v3
4. Create API keys
5. Update `config.js`

## License

MIT License - Feel free to use in your projects!

## Contributing

Pull requests welcome! Please ensure:
- Code follows existing style
- Tests pass
- Documentation is updated
