const assert = require("assert");
const { ethers } = require("hardhat");

// No chai/hardhat-toolbox was installed in this project, so these tests use
// Node's built-in `assert` module instead of adding a new dependency.
describe("ProofVault", function () {
  let proofVault;
  let owner;
  let other;

  beforeEach(async function () {
    [owner, other] = await ethers.getSigners();
    const ProofVault = await ethers.getContractFactory("ProofVault");
    proofVault = await ProofVault.deploy();
    await proofVault.waitForDeployment();
  });

  it("stores a hash and lets anyone read it back", async function () {
    const hash = ethers.keccak256(ethers.toUtf8Bytes("audit-report-1"));
    const metadata = "contract:VulnerableBank;event:audit-results";

    await proofVault.storeHash(hash, metadata);

    const [storedHash, storedMetadata, timestamp, storedBy] = await proofVault.getProof(hash);
    assert.strictEqual(storedHash, hash);
    assert.strictEqual(storedMetadata, metadata);
    assert.strictEqual(storedBy, owner.address);
    assert.ok(timestamp > 0n, "timestamp should be set to the block time");
  });

  it("emits a ProofStored event when a hash is stored", async function () {
    const hash = ethers.keccak256(ethers.toUtf8Bytes("audit-report-2"));
    const metadata = "meta";

    const tx = await proofVault.storeHash(hash, metadata);
    const receipt = await tx.wait();

    const event = receipt.logs
      .map((log) => {
        try {
          return proofVault.interface.parseLog(log);
        } catch {
          return null;
        }
      })
      .find((parsed) => parsed && parsed.name === "ProofStored");

    assert.ok(event, "ProofStored event should have been emitted");
    assert.strictEqual(event.args.hash, hash);
    assert.strictEqual(event.args.metadata, metadata);
    assert.strictEqual(event.args.storedBy, owner.address);
  });

  it("rejects storing the same hash twice (immutability guarantee)", async function () {
    const hash = ethers.keccak256(ethers.toUtf8Bytes("duplicate"));
    await proofVault.storeHash(hash, "first");

    await assert.rejects(proofVault.storeHash(hash, "second"), (err) => {
      assert.match(err.message, /Proof already exists/);
      return true;
    });
  });

  it("rejects storeHash calls from a non-owner account", async function () {
    const hash = ethers.keccak256(ethers.toUtf8Bytes("unauthorized"));

    await assert.rejects(proofVault.connect(other).storeHash(hash, "meta"));

    // And confirms the write truly never happened.
    await assert.rejects(proofVault.getProof(hash));
  });

  it("reverts when reading a proof that was never stored", async function () {
    const hash = ethers.keccak256(ethers.toUtf8Bytes("never-stored"));

    await assert.rejects(proofVault.getProof(hash), (err) => {
      assert.match(err.message, /Proof not found/);
      return true;
    });
  });
});
