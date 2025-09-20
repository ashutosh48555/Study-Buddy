// Test Script for Multi-Source Music System
// Run this to verify all sources are working

import MultiSourceMusicService from './multi-source-music-service.js';
import FirebaseMetadataService from './firebase-metadata-service.js';

// Test configuration
const testConfig = {
  jamendo: {
    clientId: 'YOUR_JAMENDO_CLIENT_ID' // Replace with actual
  },
  freesound: {
    token: 'YOUR_FREESOUND_TOKEN' // Replace with actual
  },
  youtube: {
    apiKeys: [
      'YOUR_YOUTUBE_API_KEY_1', // Replace with actual
      'YOUR_YOUTUBE_API_KEY_2',
      'YOUR_YOUTUBE_API_KEY_3'
    ]
  },
  firebase: {
    apiKey: "YOUR_API_KEY",
    authDomain: "your-project.firebaseapp.com",
    projectId: "your-project-id",
    storageBucket: "your-project.appspot.com",
    messagingSenderId: "123456789",
    appId: "your-app-id"
  }
};

class MultiSourceTester {
  constructor() {
    this.musicService = new MultiSourceMusicService();
    this.metadataService = new FirebaseMetadataService(testConfig.firebase);
    this.testResults = {
      jamendo: { status: 'pending', tracks: 0, error: null },
      freesound: { status: 'pending', tracks: 0, error: null },
      archive: { status: 'pending', tracks: 0, error: null },
      youtube: { status: 'pending', tracks: 0, error: null }
    };
  }

  // Test individual source
  async testSource(sourceName) {
    console.log(`\n🔍 Testing ${sourceName} source...`);
    
    try {
      const service = this.musicService.sources[sourceName];
      const tracks = await service.getLofiTracks(5); // Get 5 tracks for testing
      
      if (tracks && tracks.length > 0) {
        this.testResults[sourceName] = {
          status: 'success',
          tracks: tracks.length,
          error: null
        };
        
        console.log(`✅ ${sourceName}: Found ${tracks.length} tracks`);
        console.log(`   Sample track: "${tracks[0].name}" by ${tracks[0].artist}`);
        
        // Test stream URL
        if (tracks[0].streamUrl) {
          console.log(`   Stream URL: ${tracks[0].streamUrl}`);
        }
        
        return tracks;
      } else {
        throw new Error('No tracks returned');
      }
    } catch (error) {
      this.testResults[sourceName] = {
        status: 'failed',
        tracks: 0,
        error: error.message
      };
      
      console.log(`❌ ${sourceName}: ${error.message}`);
      return [];
    }
  }

  // Test all sources
  async testAllSources() {
    console.log('🎵 Testing Multi-Source Music System\n');
    console.log('=' * 50);
    
    const allTracks = [];
    
    // Test each source individually
    for (const sourceName of ['jamendo', 'freesound', 'archive', 'youtube']) {
      const tracks = await this.testSource(sourceName);
      allTracks.push(...tracks);
      
      // Add delay to avoid rate limiting
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
    
    return allTracks;
  }

  // Test mixed playlist creation
  async testMixedPlaylist() {
    console.log('\n🎵 Testing Mixed Playlist Creation...');
    
    try {
      const tracks = await this.musicService.createMixedPlaylist(5);
      
      if (tracks.length > 0) {
        console.log(`✅ Mixed playlist created with ${tracks.length} tracks`);
        
        // Show source distribution
        const sourceCount = {};
        tracks.forEach(track => {
          sourceCount[track.source] = (sourceCount[track.source] || 0) + 1;
        });
        
        console.log('   Source distribution:');
        Object.entries(sourceCount).forEach(([source, count]) => {
          console.log(`     ${source}: ${count} tracks`);
        });
        
        return tracks;
      } else {
        throw new Error('No tracks in mixed playlist');
      }
    } catch (error) {
      console.log(`❌ Mixed playlist failed: ${error.message}`);
      return [];
    }
  }

  // Test rate limit handling
  async testRateLimits() {
    console.log('\n⚡ Testing Rate Limit Handling...');
    
    try {
      // Get rate limit status
      const status = this.musicService.getRateLimitStatus();
      
      console.log('Current rate limit status:');
      Object.entries(status).forEach(([source, info]) => {
        console.log(`  ${source}: ${info.used}/${info.limit} used (${info.remaining} remaining)`);
        console.log(`    Available: ${info.available ? '✅' : '❌'}`);
        console.log(`    Resets: ${info.resetTime}`);
      });
      
      return status;
    } catch (error) {
      console.log(`❌ Rate limit test failed: ${error.message}`);
      return null;
    }
  }

  // Test Firebase metadata storage
  async testFirebaseIntegration() {
    console.log('\n🔥 Testing Firebase Integration...');
    
    try {
      // Get a few tracks
      const tracks = await this.musicService.getLofiTracks(3);
      
      if (tracks.length === 0) {
        console.log('⚠️  No tracks available for Firebase test');
        return;
      }
      
      // Save tracks to Firebase
      console.log('Saving tracks to Firebase...');
      const savedTracks = [];
      
      for (const track of tracks) {
        try {
          const metadata = await this.metadataService.saveTrackMetadata(track);
          savedTracks.push(metadata);
          console.log(`✅ Saved: ${track.name}`);
        } catch (error) {
          console.log(`❌ Failed to save ${track.name}: ${error.message}`);
        }
      }
      
      // Test retrieval
      console.log('\nTesting retrieval from Firebase...');
      const retrievedTracks = await this.metadataService.getTracks(10);
      console.log(`✅ Retrieved ${retrievedTracks.length} tracks from Firebase`);
      
      return { saved: savedTracks.length, retrieved: retrievedTracks.length };
    } catch (error) {
      console.log(`❌ Firebase integration failed: ${error.message}`);
      return null;
    }
  }

  // Run full test suite
  async runFullTest() {
    console.log('🚀 Starting Full Multi-Source Test Suite\n');
    
    try {
      // Test all sources
      const sourceTracks = await this.testAllSources();
      
      // Test mixed playlist
      const mixedTracks = await this.testMixedPlaylist();
      
      // Test rate limits
      const rateLimitStatus = await this.testRateLimits();
      
      // Test Firebase integration
      const firebaseResult = await this.testFirebaseIntegration();
      
      // Generate report
      this.generateReport(sourceTracks, mixedTracks, rateLimitStatus, firebaseResult);
      
    } catch (error) {
      console.log(`❌ Test suite failed: ${error.message}`);
    }
  }

  // Generate test report
  generateReport(sourceTracks, mixedTracks, rateLimitStatus, firebaseResult) {
    console.log('\n📊 TEST REPORT');
    console.log('=' * 50);
    
    // Source results
    console.log('\n🔍 Source Test Results:');
    Object.entries(this.testResults).forEach(([source, result]) => {
      const status = result.status === 'success' ? '✅' : '❌';
      console.log(`  ${status} ${source}: ${result.tracks} tracks ${result.error ? `(${result.error})` : ''}`);
    });
    
    // Mixed playlist
    console.log(`\n🎵 Mixed Playlist: ${mixedTracks.length} tracks`);
    
    // Rate limits
    console.log('\n⚡ Rate Limit Status:');
    if (rateLimitStatus) {
      Object.entries(rateLimitStatus).forEach(([source, info]) => {
        const available = info.available ? '✅' : '❌';
        console.log(`  ${available} ${source}: ${info.used}/${info.limit} used`);
      });
    }
    
    // Firebase
    console.log('\n🔥 Firebase Integration:');
    if (firebaseResult) {
      console.log(`  ✅ Saved: ${firebaseResult.saved} tracks`);
      console.log(`  ✅ Retrieved: ${firebaseResult.retrieved} tracks`);
    } else {
      console.log('  ❌ Firebase test failed');
    }
    
    // Overall status
    const successfulSources = Object.values(this.testResults).filter(r => r.status === 'success').length;
    const totalSources = Object.keys(this.testResults).length;
    
    console.log(`\n🎯 Overall Status: ${successfulSources}/${totalSources} sources working`);
    
    if (successfulSources > 0) {
      console.log('✅ Multi-source system is operational!');
    } else {
      console.log('❌ All sources failed - check API keys and configuration');
    }
  }
}

// Run the test
async function runTest() {
  const tester = new MultiSourceTester();
  await tester.runFullTest();
}

// Export for use in other files
export default MultiSourceTester;

// Run if this file is executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  runTest().catch(console.error);
}
