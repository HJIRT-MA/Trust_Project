const hre = require("hardhat");

async function main() {
  console.log("Deploying ProofVault...");

  const ProofVault = await hre.ethers.getContractFactory("ProofVault");
  const proofVault = await ProofVault.deploy();

  await proofVault.waitForDeployment();

  const address = await proofVault.getAddress();
  console.log(`ProofVault deployed to: ${address}`);
  console.log("Please update your Spring Boot application.properties with this contract address.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
