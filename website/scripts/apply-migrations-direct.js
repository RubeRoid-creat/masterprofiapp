const { Client } = require('pg');
const fs = require('fs');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });

async function applyMigrations() {
  const client = new Client({
    connectionString: process.env.DATABASE_URL
  });

  try {
    console.log('[INFO] Connecting to database...');
    await client.connect();
    console.log('[SUCCESS] Connected to database');

    const migrationFile = path.join(__dirname, '../prisma/migrations/001_init.sql');
    
    console.log('[INFO] Reading migration file...');
    const sql = fs.readFileSync(migrationFile, 'utf-8');
    
    console.log('[INFO] Applying migration...');
    await client.query(sql);
    
    console.log('[SUCCESS] Migration applied successfully!');
  } catch (error) {
    console.error('[ERROR] Migration failed:', error.message);
    process.exit(1);
  } finally {
    await client.end();
  }
}

applyMigrations();

