const fs = require('fs');
const path = require('path');
const FormData = require('form-data');
const fetch = require('node-fetch');

// Configuration
const SERVER_URL = 'https://your-app.railway.app'; // Replace with your Railway URL
const MUSIC_FOLDER = './music-files'; // Folder containing your music files

// Supported audio formats
const SUPPORTED_FORMATS = ['.mp3', '.wav', '.flac', '.m4a', '.ogg'];

/**
 * Upload a single song to the server
 */
async function uploadSong(filePath, metadata) {
    try {
        const form = new FormData();
        form.append('music', fs.createReadStream(filePath));
        form.append('title', metadata.title);
        form.append('artist', metadata.artist);
        form.append('duration', metadata.duration || '0:00');
        form.append('genre', metadata.genre || 'lofi');

        const response = await fetch(`${SERVER_URL}/api/upload`, {
            method: 'POST',
            body: form
        });

        const result = await response.json();
        
        if (result.success) {
            console.log(`✅ Uploaded: ${metadata.title} by ${metadata.artist}`);
            return true;
        } else {
            console.error(`❌ Failed to upload ${metadata.title}: ${result.message}`);
            return false;
        }
    } catch (error) {
        console.error(`❌ Error uploading ${metadata.title}:`, error.message);
        return false;
    }
}

/**
 * Extract metadata from filename
 * Expected format: "Artist - Title.mp3"
 */
function extractMetadata(filename) {
    const nameWithoutExt = path.parse(filename).name;
    
    // Try to parse "Artist - Title" format
    const parts = nameWithoutExt.split(' - ');
    
    if (parts.length >= 2) {
        return {
            artist: parts[0].trim(),
            title: parts.slice(1).join(' - ').trim(),
            duration: '0:00',
            genre: 'lofi'
        };
    } else {
        // Fallback: use filename as title
        return {
            artist: 'Unknown Artist',
            title: nameWithoutExt,
            duration: '0:00',
            genre: 'lofi'
        };
    }
}

/**
 * Bulk upload songs from a directory
 */
async function bulkUpload() {
    if (!fs.existsSync(MUSIC_FOLDER)) {
        console.error(`❌ Music folder not found: ${MUSIC_FOLDER}`);
        console.log('Please create the folder and add your music files.');
        return;
    }

    const files = fs.readdirSync(MUSIC_FOLDER);
    const musicFiles = files.filter(file => {
        const ext = path.extname(file).toLowerCase();
        return SUPPORTED_FORMATS.includes(ext);
    });

    if (musicFiles.length === 0) {
        console.log('❌ No music files found in the folder.');
        console.log('Supported formats:', SUPPORTED_FORMATS.join(', '));
        return;
    }

    console.log(`🎵 Found ${musicFiles.length} music files to upload...`);
    console.log('─'.repeat(50));

    let successCount = 0;
    let failureCount = 0;

    for (const file of musicFiles) {
        const filePath = path.join(MUSIC_FOLDER, file);
        const metadata = extractMetadata(file);
        
        console.log(`📤 Uploading: ${file}`);
        
        const success = await uploadSong(filePath, metadata);
        
        if (success) {
            successCount++;
        } else {
            failureCount++;
        }
        
        // Add delay between uploads to avoid rate limiting
        await new Promise(resolve => setTimeout(resolve, 1000));
    }

    console.log('─'.repeat(50));
    console.log(`✅ Successfully uploaded: ${successCount} songs`);
    console.log(`❌ Failed uploads: ${failureCount} songs`);
}

/**
 * Upload a single song with custom metadata
 */
async function uploadSingle(filePath, title, artist, duration = '0:00', genre = 'lofi') {
    if (!fs.existsSync(filePath)) {
        console.error(`❌ File not found: ${filePath}`);
        return;
    }

    const metadata = { title, artist, duration, genre };
    console.log(`📤 Uploading: ${title} by ${artist}`);
    
    const success = await uploadSong(filePath, metadata);
    
    if (success) {
        console.log('✅ Upload completed successfully!');
    } else {
        console.log('❌ Upload failed!');
    }
}

// Command line interface
if (require.main === module) {
    const args = process.argv.slice(2);
    
    if (args.length === 0) {
        // Bulk upload
        bulkUpload();
    } else if (args.length >= 3) {
        // Single upload: node upload-songs.js "path/to/song.mp3" "Title" "Artist" "duration" "genre"
        const [filePath, title, artist, duration, genre] = args;
        uploadSingle(filePath, title, artist, duration, genre);
    } else {
        console.log('Usage:');
        console.log('  Bulk upload: node upload-songs.js');
        console.log('  Single upload: node upload-songs.js "path/to/song.mp3" "Title" "Artist" "3:45" "lofi"');
    }
}

module.exports = { uploadSong, bulkUpload, uploadSingle };
