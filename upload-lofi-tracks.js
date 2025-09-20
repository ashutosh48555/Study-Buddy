const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin
// Replace with your service account key path
const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: 'your-project-id.appspot.com' // Replace with your project ID
});

const bucket = admin.storage().bucket();

async function uploadLofiTracks() {
  const tracksDir = './lofi-tracks'; // Local directory with your tracks
  
  // Check if directory exists
  if (!fs.existsSync(tracksDir)) {
    console.error(`Directory ${tracksDir} does not exist. Please create it and add your MP3 files.`);
    return;
  }

  const files = fs.readdirSync(tracksDir);
  const mp3Files = files.filter(file => file.toLowerCase().endsWith('.mp3'));
  
  if (mp3Files.length === 0) {
    console.log('No MP3 files found in the lofi-tracks directory.');
    return;
  }

  console.log(`Found ${mp3Files.length} MP3 files to upload:`);
  mp3Files.forEach(file => console.log(`  - ${file}`));
  console.log('');

  const uploadedUrls = [];
  
  for (const file of mp3Files) {
    try {
      const filePath = path.join(tracksDir, file);
      const destination = `lofi-tracks/${file}`;
      
      console.log(`Uploading: ${file}...`);
      
      const [uploadedFile] = await bucket.upload(filePath, {
        destination: destination,
        metadata: {
          contentType: 'audio/mpeg',
          cacheControl: 'public, max-age=31536000', // 1 year cache
        },
        public: true
      });
      
      // Make the file public
      await uploadedFile.makePublic();
      
      // Get public URL
      const publicUrl = `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodeURIComponent(destination)}?alt=media`;
      
      console.log(`✓ Uploaded: ${file}`);
      console.log(`  URL: ${publicUrl}`);
      console.log('');
      
      uploadedUrls.push({
        filename: file,
        url: publicUrl
      });
      
    } catch (error) {
      console.error(`✗ Failed to upload ${file}:`, error.message);
    }
  }

  // Save URLs to a file for reference
  const urlsFile = 'uploaded-tracks-urls.json';
  fs.writeFileSync(urlsFile, JSON.stringify(uploadedUrls, null, 2));
  console.log(`\nAll URLs saved to ${urlsFile}`);
  
  console.log('\nUpload Summary:');
  console.log(`Total files: ${mp3Files.length}`);
  console.log(`Successfully uploaded: ${uploadedUrls.length}`);
  console.log(`Failed: ${mp3Files.length - uploadedUrls.length}`);
}

// Run the upload
uploadLofiTracks().catch(console.error);
