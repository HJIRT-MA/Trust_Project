// SPDX-License-Identifier: MIT
pragma solidity ^0.4.24; // Older version to allow overflow

contract VulnerableBank {
    mapping(address => uint256) public balances;
    address public owner;

    constructor() public {
        owner = msg.sender;
    }

    // SWC-101: Integer Overflow
    function deposit() public payable {
        balances[msg.sender] += msg.value;
    }

    // SWC-107: Reentrancy
    function withdrawAll() public {
        uint256 bal = balances[msg.sender];
        require(bal > 0, "Insufficient balance");
        
        // Vulnerable to Reentrancy
        (bool sent, ) = msg.sender.call.value(bal)("");
        require(sent, "Failed to send Ether");
        
        balances[msg.sender] = 0;
    }

    // SWC-106: Unprotected SELFDESTRUCT
    function kill() public {
        // No onlyOwner modifier!
        selfdestruct(msg.sender);
    }
}
