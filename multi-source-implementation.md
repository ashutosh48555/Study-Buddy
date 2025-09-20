# Multi-Source Music System Implementation

## Setup Steps

### 1. Get API Keys/Tokens

#### Jamendo API (Primary Source)
- Visit: https://developer.jamendo.com/
- Create account and get client_id
- Rate limit: 10,000 requests/day
- **Best for**: High-quality lofi tracks

#### Freesound API (Secondary Source)
- Visit: https://freesound.org/apiv2/apply/
- Create account and get API token
- Rate limit: 2,000 requests/day
- **Best for**: Ambient sounds, creative commons

#### Archive.org (Fallback Source)
- No API key required
- Unlimited requests
- **Best for**: Public domain music

#### YouTube API (Optional)
- Visit: https://console.developers.google.com/
- Create project, enable YouTube Data API v3
- Get multiple API keys for rotation
- Rate limit: 100 requests/day per key
- **Best for**: Largest music library

### 2. Install Dependencies

```bash
npm install
```

### 3. Configure API Keys

Update the service files with your actual API keys:

```javascript
// In jamendo-music-service.js
this.clientId = 'YOUR_JAMENDO_CLIENT_ID';

// In multi-source-music-service.js
this.token = 'YOUR_FREESOUND_TOKEN';

// YouTube API keys array
this.apiKeys = [
  'YOUR_YOUTUBE_API_KEY_1',
  'YOUR_YOUTUBE_API_KEY_2',
  'YOUR_YOUTUBE_API_KEY_3'
];
```

### 4. Test the System

```bash
npm run test-sources
```

## Integration with Android App

### Option A: Node.js Backend Service
Deploy the music service as a backend API that your Android app calls.

### Option B: Direct Integration
Convert the JavaScript service to work with your Android app's web components.

### Option C: Hybrid Approach
Use the service to pre-populate a cloud storage bucket with track metadata and URLs.

## Cloud Storage Strategy

Since we're using multiple sources, we'll create a **metadata storage system** instead of hosting actual audio files:

### Firebase Firestore Structure:
```
lofi_tracks/
├── {track_id}/
│   ├── id: string
│   ├── name: string
│   ├── artist: string
│   ├── album: string
│   ├── duration: number
│   ├── image: string
│   ├── streamUrl: string
│   ├── source: string
│   ├── license: string
│   ├── tags: array
│   ├── quality: string
│   └── lastUpdated: timestamp
```

### Benefits:
- ✅ No audio file storage costs
- ✅ Fast metadata queries
- ✅ Easy to update stream URLs
- ✅ Can cache popular tracks
- ✅ Support for multiple sources

## Next Steps

1. **Set up API keys** for each service
2. **Choose integration method** (Backend API vs Direct)
3. **Configure Firebase** for metadata storage
4. **Test the multi-source system**
5. **Deploy and monitor** rate limits

Which integration approach would you prefer for your Android app?
