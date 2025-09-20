# Lofi Tracks Cloud Storage Setup Guide

## Option 1: Firebase Storage (Recommended)

Firebase Storage automatically includes CDN (via Google Cloud CDN) and is easy to set up.

### Setup Steps:

1. **Install Firebase CLI**
   ```bash
   npm install -g firebase-tools
   ```

2. **Initialize Firebase in your project**
   ```bash
   firebase login
   firebase init storage
   ```

3. **Configure Firebase Storage Rules**
   Create/update `storage.rules`:
   ```javascript
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /lofi-tracks/{allPaths=**} {
         allow read: if true; // Public read access
         allow write: if request.auth != null; // Authenticated write
       }
     }
   }
   ```

4. **Deploy the rules**
   ```bash
   firebase deploy --only storage
   ```

### Upload Script for Lofi Tracks:

```javascript
// upload-lofi-tracks.js
const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin
const serviceAccount = require('./path/to/serviceAccountKey.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: 'your-project-id.appspot.com'
});

const bucket = admin.storage().bucket();

async function uploadLofiTracks() {
  const tracksDir = './lofi-tracks'; // Local directory with your tracks
  const files = fs.readdirSync(tracksDir);
  
  for (const file of files) {
    if (file.endsWith('.mp3')) {
      const filePath = path.join(tracksDir, file);
      const destination = `lofi-tracks/${file}`;
      
      await bucket.upload(filePath, {
        destination: destination,
        metadata: {
          contentType: 'audio/mpeg',
          cacheControl: 'public, max-age=31536000', // 1 year cache
        },
        public: true
      });
      
      console.log(`Uploaded: ${file}`);
      
      // Get public URL
      const publicUrl = `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodeURIComponent(destination)}?alt=media`;
      console.log(`Public URL: ${publicUrl}`);
    }
  }
}

uploadLofiTracks().catch(console.error);
```

## Option 2: Amazon S3 + CloudFront

### Setup Steps:

1. **Install AWS CLI**
   ```bash
   pip install awscli
   aws configure
   ```

2. **Create S3 Bucket**
   ```bash
   aws s3 mb s3://your-lofi-tracks-bucket
   ```

3. **Configure bucket for public read**
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Sid": "PublicReadGetObject",
         "Effect": "Allow",
         "Principal": "*",
         "Action": "s3:GetObject",
         "Resource": "arn:aws:s3:::your-lofi-tracks-bucket/lofi-tracks/*"
       }
     ]
   }
   ```

4. **Create CloudFront Distribution**
   - Origin: your-lofi-tracks-bucket.s3.amazonaws.com
   - Cache behaviors: Optimize for audio content
   - TTL: 1 year for static audio files

5. **Upload with correct MIME types**
   ```bash
   aws s3 cp ./lofi-tracks/ s3://your-lofi-tracks-bucket/lofi-tracks/ \
     --recursive \
     --content-type "audio/mpeg" \
     --cache-control "public, max-age=31536000"
   ```

## File Naming Convention

Use consistent naming for your lofi tracks:
- Format: `lofi-track-{number}-{title}.mp3`
- Examples:
  - `lofi-track-001-midnight-rain.mp3`
  - `lofi-track-002-coffee-shop-vibes.mp3`
  - `lofi-track-003-lazy-sunday.mp3`

## MIME Type Configuration

All files should be uploaded with:
- Content-Type: `audio/mpeg`
- Cache-Control: `public, max-age=31536000`

## Public URL Formats

### Firebase Storage:
```
https://firebasestorage.googleapis.com/v0/b/your-project-id.appspot.com/o/lofi-tracks%2Ffilename.mp3?alt=media
```

### S3 + CloudFront:
```
https://your-cloudfront-domain.cloudfront.net/lofi-tracks/filename.mp3
```

## Next Steps

1. Choose your preferred storage solution (Firebase or AWS)
2. Gather your lofi track files
3. Run the setup commands above
4. Upload your tracks using the provided scripts
5. Test the public URLs to ensure CDN is working

## Security Considerations

- For Firebase: Use security rules to control write access
- For S3: Use IAM policies to limit upload permissions
- Consider implementing token-based access for premium tracks if needed
