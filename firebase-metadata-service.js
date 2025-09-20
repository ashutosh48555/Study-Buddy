// Firebase Metadata Storage Service
// Stores track metadata from multiple sources in Firestore

import { initializeApp } from 'firebase/app';
import { 
  getFirestore, 
  collection, 
  doc, 
  setDoc, 
  getDoc, 
  getDocs, 
  query, 
  where, 
  orderBy, 
  limit, 
  startAfter 
} from 'firebase/firestore';

import MultiSourceMusicService from './multi-source-music-service.js';

class FirebaseMetadataService {
  constructor(firebaseConfig) {
    this.app = initializeApp(firebaseConfig);
    this.db = getFirestore(this.app);
    this.musicService = new MultiSourceMusicService();
    this.tracksCollection = 'lofi_tracks';
    this.playlistsCollection = 'lofi_playlists';
  }

  // Save track metadata to Firestore
  async saveTrackMetadata(track) {
    try {
      const trackDoc = doc(this.db, this.tracksCollection, track.id);
      
      const metadata = {
        id: track.id,
        name: track.name,
        artist: track.artist,
        album: track.album,
        duration: track.duration,
        image: track.image,
        streamUrl: track.streamUrl,
        downloadUrl: track.downloadUrl,
        source: track.source,
        license: track.license,
        tags: track.tags || [],
        quality: this.assessQuality(track),
        lastUpdated: new Date(),
        playCount: 0,
        rating: 0,
        isAvailable: true
      };

      await setDoc(trackDoc, metadata);
      console.log(`Saved metadata for: ${track.name}`);
      return metadata;
    } catch (error) {
      console.error('Error saving track metadata:', error);
      throw error;
    }
  }

  // Bulk save tracks from multiple sources
  async populateTracksFromSources(tracksPerSource = 20) {
    try {
      console.log('Fetching tracks from multiple sources...');
      const tracks = await this.musicService.createMixedPlaylist(tracksPerSource);
      
      const savedTracks = [];
      for (const track of tracks) {
        try {
          const metadata = await this.saveTrackMetadata(track);
          savedTracks.push(metadata);
        } catch (error) {
          console.error(`Failed to save track ${track.name}:`, error);
        }
      }

      console.log(`Successfully saved ${savedTracks.length} tracks to Firestore`);
      return savedTracks;
    } catch (error) {
      console.error('Error populating tracks:', error);
      throw error;
    }
  }

  // Get tracks from Firestore with pagination
  async getTracks(limitCount = 20, lastTrackId = null) {
    try {
      let q = query(
        collection(this.db, this.tracksCollection),
        where('isAvailable', '==', true),
        orderBy('lastUpdated', 'desc'),
        limit(limitCount)
      );

      if (lastTrackId) {
        const lastDoc = await getDoc(doc(this.db, this.tracksCollection, lastTrackId));
        if (lastDoc.exists()) {
          q = query(q, startAfter(lastDoc));
        }
      }

      const querySnapshot = await getDocs(q);
      const tracks = [];
      
      querySnapshot.forEach((doc) => {
        tracks.push(doc.data());
      });

      return tracks;
    } catch (error) {
      console.error('Error fetching tracks:', error);
      throw error;
    }
  }

  // Search tracks by name, artist, or tags
  async searchTracks(searchTerm, limitCount = 20) {
    try {
      const queries = [
        query(
          collection(this.db, this.tracksCollection),
          where('name', '>=', searchTerm),
          where('name', '<=', searchTerm + '\uf8ff'),
          limit(limitCount)
        ),
        query(
          collection(this.db, this.tracksCollection),
          where('artist', '>=', searchTerm),
          where('artist', '<=', searchTerm + '\uf8ff'),
          limit(limitCount)
        ),
        query(
          collection(this.db, this.tracksCollection),
          where('tags', 'array-contains', searchTerm.toLowerCase()),
          limit(limitCount)
        )
      ];

      const results = [];
      for (const q of queries) {
        const querySnapshot = await getDocs(q);
        querySnapshot.forEach((doc) => {
          const track = doc.data();
          if (!results.find(r => r.id === track.id)) {
            results.push(track);
          }
        });
      }

      return results.slice(0, limitCount);
    } catch (error) {
      console.error('Error searching tracks:', error);
      throw error;
    }
  }

  // Get tracks by source
  async getTracksBySource(source, limitCount = 20) {
    try {
      const q = query(
        collection(this.db, this.tracksCollection),
        where('source', '==', source),
        where('isAvailable', '==', true),
        orderBy('lastUpdated', 'desc'),
        limit(limitCount)
      );

      const querySnapshot = await getDocs(q);
      const tracks = [];
      
      querySnapshot.forEach((doc) => {
        tracks.push(doc.data());
      });

      return tracks;
    } catch (error) {
      console.error('Error fetching tracks by source:', error);
      throw error;
    }
  }

  // Update track availability (if stream URL becomes invalid)
  async updateTrackAvailability(trackId, isAvailable) {
    try {
      const trackDoc = doc(this.db, this.tracksCollection, trackId);
      await setDoc(trackDoc, { 
        isAvailable: isAvailable,
        lastUpdated: new Date()
      }, { merge: true });
      
      console.log(`Updated availability for track ${trackId}: ${isAvailable}`);
    } catch (error) {
      console.error('Error updating track availability:', error);
      throw error;
    }
  }

  // Increment play count
  async incrementPlayCount(trackId) {
    try {
      const trackDoc = doc(this.db, this.tracksCollection, trackId);
      const trackData = await getDoc(trackDoc);
      
      if (trackData.exists()) {
        const currentCount = trackData.data().playCount || 0;
        await setDoc(trackDoc, { 
          playCount: currentCount + 1,
          lastUpdated: new Date()
        }, { merge: true });
      }
    } catch (error) {
      console.error('Error incrementing play count:', error);
    }
  }

  // Get popular tracks
  async getPopularTracks(limitCount = 20) {
    try {
      const q = query(
        collection(this.db, this.tracksCollection),
        where('isAvailable', '==', true),
        orderBy('playCount', 'desc'),
        limit(limitCount)
      );

      const querySnapshot = await getDocs(q);
      const tracks = [];
      
      querySnapshot.forEach((doc) => {
        tracks.push(doc.data());
      });

      return tracks;
    } catch (error) {
      console.error('Error fetching popular tracks:', error);
      throw error;
    }
  }

  // Create curated playlist
  async createPlaylist(name, description, trackIds) {
    try {
      const playlistId = `playlist_${Date.now()}`;
      const playlistDoc = doc(this.db, this.playlistsCollection, playlistId);
      
      const playlist = {
        id: playlistId,
        name: name,
        description: description,
        trackIds: trackIds,
        trackCount: trackIds.length,
        createdAt: new Date(),
        updatedAt: new Date(),
        isPublic: true
      };

      await setDoc(playlistDoc, playlist);
      console.log(`Created playlist: ${name}`);
      return playlist;
    } catch (error) {
      console.error('Error creating playlist:', error);
      throw error;
    }
  }

  // Get playlist tracks
  async getPlaylistTracks(playlistId) {
    try {
      const playlistDoc = await getDoc(doc(this.db, this.playlistsCollection, playlistId));
      
      if (!playlistDoc.exists()) {
        throw new Error('Playlist not found');
      }

      const playlist = playlistDoc.data();
      const tracks = [];
      
      for (const trackId of playlist.trackIds) {
        const trackDoc = await getDoc(doc(this.db, this.tracksCollection, trackId));
        if (trackDoc.exists()) {
          tracks.push(trackDoc.data());
        }
      }

      return tracks;
    } catch (error) {
      console.error('Error fetching playlist tracks:', error);
      throw error;
    }
  }

  // Assess track quality based on source and metadata
  assessQuality(track) {
    const sourceQuality = {
      'jamendo': 'high',
      'freesound': 'medium',
      'archive': 'medium',
      'youtube': 'variable'
    };

    const baseQuality = sourceQuality[track.source] || 'unknown';
    
    // Adjust based on duration (longer tracks might be better quality)
    if (track.duration > 300) { // 5 minutes
      return baseQuality === 'high' ? 'high' : 'medium';
    }
    
    return baseQuality;
  }

  // Refresh track metadata from source
  async refreshTrackFromSource(trackId) {
    try {
      const trackDoc = await getDoc(doc(this.db, this.tracksCollection, trackId));
      
      if (!trackDoc.exists()) {
        throw new Error('Track not found');
      }

      const trackData = trackDoc.data();
      const source = trackData.source;
      
      // Try to fetch updated info from the original source
      const service = this.musicService.sources[source];
      if (service) {
        // This would need source-specific implementation
        console.log(`Refreshing track ${trackId} from ${source}`);
        // Update availability and metadata if needed
      }
    } catch (error) {
      console.error('Error refreshing track:', error);
      throw error;
    }
  }
}

// Firebase configuration template
const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "your-project.firebaseapp.com",
  projectId: "your-project-id",
  storageBucket: "your-project.appspot.com",
  messagingSenderId: "123456789",
  appId: "your-app-id"
};

export default FirebaseMetadataService;
export { firebaseConfig };
