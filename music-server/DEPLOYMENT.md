# Music Server Deployment Guide

## Prerequisites

1. **AWS S3 Bucket Setup**
2. **Railway Account**
3. **Node.js 18+**

## Step 1: AWS S3 Setup

### Create S3 Bucket
```bash
# Create bucket (replace with your bucket name)
aws s3 mb s3://your-music-bucket-name

# Enable public access for music files
aws s3api put-bucket-policy --bucket your-music-bucket-name --policy '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::your-music-bucket-name/*"
    }
  ]
}'
```

### Get AWS Credentials
1. Go to AWS Console → IAM → Users
2. Create new user with S3 permissions
3. Generate Access Keys
4. Note down:
   - AWS_ACCESS_KEY_ID
   - AWS_SECRET_ACCESS_KEY

## Step 2: Railway Deployment

### Install Railway CLI
```bash
npm install -g @railway/cli
```

### Deploy to Railway
```bash
# Navigate to server directory
cd music-server

# Install dependencies
npm install

# Login to Railway
railway login

# Create new project
railway init

# Add environment variables
railway variables set AWS_ACCESS_KEY_ID=your_access_key
railway variables set AWS_SECRET_ACCESS_KEY=your_secret_key  
railway variables set AWS_REGION=us-east-1
railway variables set S3_BUCKET_NAME=your-music-bucket-name
railway variables set NODE_ENV=production

# Deploy
railway up

# Get your app URL
railway status
```

## Step 3: Test Your Server

### Local Testing
```bash
# Run locally
npm run dev

# Test endpoints
curl http://localhost:3000/health
curl http://localhost:3000/api/songs
```

### Production Testing
```bash
# Replace with your Railway URL
curl https://your-app.railway.app/health
curl https://your-app.railway.app/api/songs
```

## Step 4: Upload Music Files

### Using curl
```bash
curl -X POST https://your-app.railway.app/api/upload \
  -F "music=@/path/to/your/song.mp3" \
  -F "title=Song Title" \
  -F "artist=Artist Name" \
  -F "duration=3:45" \
  -F "genre=lofi"
```

### Using web interface (optional)
Create a simple HTML form to upload songs.

## Step 5: Update Android App

1. Update `MusicApiService.kt` baseUrl with your Railway URL
2. Remove local JSON files to reduce APK size
3. Test streaming from your server

## Cost Estimation

### Railway (Free Tier)
- 500 execution hours/month
- $0.000463 per GB-hour after free tier

### AWS S3
- 5GB free storage
- $0.023 per GB after free tier
- Data transfer: $0.09 per GB

### Total Monthly Cost (estimated)
- **Small library (50 songs)**: ~$2-5/month
- **Medium library (200 songs)**: ~$5-10/month

## Security Notes

1. **Never commit .env files**
2. **Use environment variables for secrets**
3. **Enable CORS properly**
4. **Add rate limiting**
5. **Consider authentication for upload endpoints**

## Troubleshooting

### Common Issues

1. **S3 Access Denied**
   - Check bucket policy
   - Verify AWS credentials

2. **Railway Deployment Failed**
   - Check logs: `railway logs`
   - Verify environment variables

3. **Android App Can't Connect**
   - Check Railway URL in MusicApiService
   - Verify network permissions

### Logs
```bash
# Railway logs
railway logs

# Local logs
npm run dev
```

## Next Steps

1. **Add Database**: Replace in-memory storage with PostgreSQL
2. **Add Authentication**: Secure upload endpoints
3. **Add CDN**: CloudFront for faster delivery
4. **Add Analytics**: Track popular songs
5. **Add Playlists**: User-created playlists
