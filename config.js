// Configuration for Multi-Source Music Player
// Add your API keys here

const config = {
  jamendo: {
    clientId: 'YOUR_JAMENDO_CLIENT_ID' // Get from https://developer.jamendo.com/
  },
  
  freesound: {
    token: 'YOUR_FREESOUND_TOKEN' // Get from https://freesound.org/apiv2/apply/
  },
  
  youtube: {
    apiKeys: [
      'YOUR_YOUTUBE_API_KEY_1', // Primary key
      'YOUR_YOUTUBE_API_KEY_2', // Backup key 1
      'YOUR_YOUTUBE_API_KEY_3'  // Backup key 2
    ]
  },
  
  // Archive.org doesn't need API keys
  archive: {},
  
  // Rate limits for each service
  rateLimits: {
    jamendo: 10000,   // 10,000 requests/day
    freesound: 2000,  // 2,000 requests/day
    archive: Infinity, // No limit
    youtube: 100      // 100 requests/day per key
  },
  
  // Fallback order (try sources in this order)
  fallbackOrder: ['jamendo', 'freesound', 'archive', 'youtube'],
  
  // CORS proxy for handling cross-origin requests
  corsProxy: 'https://cors-anywhere.herokuapp.com/',
  
  // Cache settings
  cache: {
    enabled: true,
    maxSize: 1000,
    ttl: 3600000 // 1 hour in milliseconds
  }
};

export default config;
