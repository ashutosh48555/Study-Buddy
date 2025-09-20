const express = require('express');
const multer = require('multer');
const AWS = require('aws-sdk');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Security and performance middleware
app.use(helmet());
app.use(compression());
app.use(cors());
app.use(express.json());

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use('/api/', limiter);

// AWS S3 Configuration
const s3 = new AWS.S3({
  accessKeyId: process.env.AWS_ACCESS_KEY_ID,
  secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
  region: process.env.AWS_REGION
});

const BUCKET_NAME = process.env.S3_BUCKET_NAME;

// Configure multer for memory storage (lightweight)
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 10 * 1024 * 1024, // 10MB limit
  },
  fileFilter: (req, file, cb) => {
    // Accept only audio files
    if (file.mimetype.startsWith('audio/')) {
      cb(null, true);
    } else {
      cb(new Error('Only audio files are allowed!'), false);
    }
  }
});

// In-memory store for songs metadata (use database in production)
let songsDatabase = [
  {
    id: 1,
    title: "Chill Vibes",
    artist: "StudyBeats",
    duration: "3:45",
    url: "https://your-s3-bucket.s3.amazonaws.com/chill-vibes.mp3",
    coverArt: "https://your-s3-bucket.s3.amazonaws.com/covers/chill-vibes.jpg",
    genre: "lofi",
    createdAt: new Date().toISOString()
  },
  {
    id: 2,
    title: "Study Focus",
    artist: "LofiGirl",
    duration: "4:12",
    url: "https://your-s3-bucket.s3.amazonaws.com/study-focus.mp3",
    coverArt: "https://your-s3-bucket.s3.amazonaws.com/covers/study-focus.jpg",
    genre: "lofi",
    createdAt: new Date().toISOString()
  }
];

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// Get all songs (lightweight response)
app.get('/api/songs', (req, res) => {
  try {
    const lightweightSongs = songsDatabase.map(song => ({
      id: song.id,
      title: song.title,
      artist: song.artist,
      duration: song.duration,
      url: song.url,
      coverArt: song.coverArt,
      genre: song.genre
    }));
    
    res.json({
      success: true,
      count: lightweightSongs.length,
      songs: lightweightSongs
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch songs',
      error: error.message
    });
  }
});

// Get specific song by ID
app.get('/api/songs/:id', (req, res) => {
  try {
    const songId = parseInt(req.params.id);
    const song = songsDatabase.find(s => s.id === songId);
    
    if (!song) {
      return res.status(404).json({
        success: false,
        message: 'Song not found'
      });
    }
    
    res.json({
      success: true,
      song: song
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch song',
      error: error.message
    });
  }
});

// Upload song to S3
app.post('/api/upload', upload.single('music'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'No file uploaded'
      });
    }

    const { title, artist, duration, genre } = req.body;
    
    if (!title || !artist) {
      return res.status(400).json({
        success: false,
        message: 'Title and artist are required'
      });
    }

    const fileName = `music/${Date.now()}-${req.file.originalname}`;
    
    const uploadParams = {
      Bucket: BUCKET_NAME,
      Key: fileName,
      Body: req.file.buffer,
      ContentType: req.file.mimetype,
      ACL: 'public-read'
    };

    const result = await s3.upload(uploadParams).promise();
    
    const newSong = {
      id: songsDatabase.length + 1,
      title,
      artist,
      duration: duration || "0:00",
      url: result.Location,
      coverArt: `https://picsum.photos/200/200?random=${Date.now()}`,
      genre: genre || "lofi",
      createdAt: new Date().toISOString()
    };
    
    songsDatabase.push(newSong);
    
    res.json({
      success: true,
      message: 'Song uploaded successfully',
      song: newSong
    });
    
  } catch (error) {
    console.error('Upload error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to upload song',
      error: error.message
    });
  }
});

// Delete song
app.delete('/api/songs/:id', async (req, res) => {
  try {
    const songId = parseInt(req.params.id);
    const songIndex = songsDatabase.findIndex(s => s.id === songId);
    
    if (songIndex === -1) {
      return res.status(404).json({
        success: false,
        message: 'Song not found'
      });
    }
    
    const song = songsDatabase[songIndex];
    
    // Delete from S3
    const urlParts = song.url.split('/');
    const key = urlParts.slice(-2).join('/'); // Get 'music/filename'
    
    await s3.deleteObject({
      Bucket: BUCKET_NAME,
      Key: key
    }).promise();
    
    // Remove from database
    songsDatabase.splice(songIndex, 1);
    
    res.json({
      success: true,
      message: 'Song deleted successfully'
    });
    
  } catch (error) {
    console.error('Delete error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to delete song',
      error: error.message
    });
  }
});

// Search songs
app.get('/api/search', (req, res) => {
  try {
    const { q, genre } = req.query;
    
    let filteredSongs = songsDatabase;
    
    if (q) {
      filteredSongs = filteredSongs.filter(song => 
        song.title.toLowerCase().includes(q.toLowerCase()) ||
        song.artist.toLowerCase().includes(q.toLowerCase())
      );
    }
    
    if (genre) {
      filteredSongs = filteredSongs.filter(song => 
        song.genre.toLowerCase() === genre.toLowerCase()
      );
    }
    
    res.json({
      success: true,
      count: filteredSongs.length,
      songs: filteredSongs
    });
    
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Search failed',
      error: error.message
    });
  }
});

// Error handling middleware
app.use((error, req, res, next) => {
  console.error(error);
  res.status(500).json({
    success: false,
    message: 'Internal server error',
    error: process.env.NODE_ENV === 'development' ? error.message : 'Something went wrong'
  });
});

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({
    success: false,
    message: 'Endpoint not found'
  });
});

app.listen(PORT, () => {
  console.log(`🎵 Music Server running on port ${PORT}`);
  console.log(`🌐 Health check: http://localhost:${PORT}/health`);
});
