// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";

contract ProofVault is Ownable {
    
    struct Proof {
        bytes32 hash;
        string metadata;
        uint256 timestamp;
        address storedBy;
    }

    mapping(bytes32 => Proof) public proofs;
    
    event ProofStored(bytes32 indexed hash, string metadata, uint256 timestamp, address storedBy);

    constructor() Ownable(msg.sender) {}

    /**
     * @dev Stores a hash and associated metadata immutably on the blockchain.
     * @param _hash The SHA-256 hash of the audit report/findings.
     * @param _metadata Additional JSON or text metadata about the audit (contract name, ID, etc.).
     */
    function storeHash(bytes32 _hash, string memory _metadata) external onlyOwner {
        require(proofs[_hash].timestamp == 0, "Proof already exists");

        proofs[_hash] = Proof({
            hash: _hash,
            metadata: _metadata,
            timestamp: block.timestamp,
            storedBy: msg.sender
        });

        emit ProofStored(_hash, _metadata, block.timestamp, msg.sender);
    }

    /**
     * @dev Retrieves a proof by its hash.
     */
    function getProof(bytes32 _hash) external view returns (bytes32, string memory, uint256, address) {
        Proof memory p = proofs[_hash];
        require(p.timestamp != 0, "Proof not found");
        return (p.hash, p.metadata, p.timestamp, p.storedBy);
    }
}
