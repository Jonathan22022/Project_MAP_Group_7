const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');

const app = express();
app.use(cors());
app.use(bodyParser.json());

// Route utama
app.get('/', (req, res) => {
  res.json({ message: 'Server berjalan!' });
});

// Route untuk registrasi
app.post('/register', (req, res) => {
  const { username, email, password, phone, nimNik } = req.body;

  if (!username || !email || !password) {
    return res.status(400).json({ success: false, message: 'Data tidak lengkap!' });
  }

  console.log('📩 Data diterima:', req.body);
  res.json({ success: true, message: 'Registrasi berhasil!' });
});

// Jalankan server
app.listen(3000, '0.0.0.0', () => {
  console.log('✅ Server berjalan di http://10.0.2.2:3000');
});

