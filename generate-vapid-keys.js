const crypto = require("crypto");

// Gerar chaves VAPID usando Node.js crypto
function generateVapidKeys() {
  // Gerar par de chaves EC P-256
  const { publicKey, privateKey } = crypto.generateKeyPairSync("ec", {
    namedCurve: "prime256v1",
    publicKeyEncoding: {
      type: "spki",
      format: "der",
    },
    privateKeyEncoding: {
      type: "pkcs8",
      format: "der",
    },
  });

  // Converter para base64url
  const publicKeyBase64 = publicKey
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");

  const privateKeyBase64 = privateKey
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");

  return {
    publicKey: publicKeyBase64,
    privateKey: privateKeyBase64,
  };
}

// Gerar e exibir as chaves
const keys = generateVapidKeys();

console.log("🔑 VAPID Keys Generated:");
console.log("");
console.log("Public Key:", keys.publicKey);
console.log("Private Key:", keys.privateKey);
console.log("");
console.log("📋 Add to application.properties:");
console.log(`webpush.vapid.public-key=${keys.publicKey}`);
console.log(`webpush.vapid.private-key=${keys.privateKey}`);
console.log("webpush.vapid.subject=mailto:admin@mvt-events.com");
