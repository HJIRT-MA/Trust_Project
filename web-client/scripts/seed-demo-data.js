const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log('🌱 Seeding highly realistic fake data for Investor Demo...');

const seedSql = `
INSERT INTO blockchain_proofs (event_id, event_type, tx_hash, block_number, timestamp, status, user_id, payload) VALUES
('RAG-1001', 'rag-interactions', '0x1b2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d', 120543, EXTRACT(EPOCH FROM NOW() - INTERVAL '2 hours') * 1000, 'CONFIRMED', 'admin', '{"question":"What is AI Act?","answer":"Regulation on AI"}'),
('AUDIT-2002', 'audit-results', '0x2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d', 120550, EXTRACT(EPOCH FROM NOW() - INTERVAL '1 hour') * 1000, 'CONFIRMED', 'analyst', '{"findings":[]}'),
('GUARD-3003', 'hallucination-checks', '0x3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e', 120600, EXTRACT(EPOCH FROM NOW() - INTERVAL '30 minutes') * 1000, 'CONFIRMED', 'system', '{"score":99,"status":"PASS"}'),
('RAG-1002', 'rag-interactions', '0x4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f', 120610, EXTRACT(EPOCH FROM NOW() - INTERVAL '15 minutes') * 1000, 'CONFIRMED', 'admin', '{"question":"Explain smart contract audit","answer":"An audit reviews the code for vulnerabilities."}'),
('AUDIT-2003', 'audit-results', 'skipped', null, EXTRACT(EPOCH FROM NOW() - INTERVAL '5 minutes') * 1000, 'FAILED', 'admin', '{"findings":[{"severity":"CRITICAL"}]}');
`;

const sqlFilePath = path.join(__dirname, 'temp_seed.sql');
fs.writeFileSync(sqlFilePath, seedSql);

try {
  console.log('Injecting data into PostgreSQL container...');
  // Using docker exec to run the sql file inside the container
  execSync(`docker exec -i trustai-postgres psql -U trustai_user -d trustaidb < "${sqlFilePath}"`, { stdio: 'inherit' });
  console.log('✅ Demo data successfully injected!');
  console.log('Go to http://localhost:4200/viewer-dashboard to see the results!');
} catch (error) {
  console.error('❌ Failed to inject data. Make sure trustai-postgres container is running.');
} finally {
  fs.unlinkSync(sqlFilePath);
}
