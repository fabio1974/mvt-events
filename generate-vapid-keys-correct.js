const crypto = require("crypto");

// Gerar par de chaves VAPID
const { publicKey, privateKey } = crypto.generateKeyPairSync("ec", {
  namedCurve: "prime256v1", // P-256
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
function toBase64Url(buffer) {
  return buffer
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

// Extrair apenas a chave pública raw (65 bytes) do SPKI format
// SPKI format: [header 26 bytes] + [public key 65 bytes]
const publicKeyRaw = publicKey.slice(26); // Pula o header ASN.1

// Extrair apenas a chave privada raw (32 bytes) do PKCS#8 format
// Para EC P-256, a chave privada está no offset 36 (32 bytes)
const privateKeyRaw = privateKey.slice(36, 68); // 32 bytes da chave privada

console.log("=".repeat(80));
console.log("VAPID KEYS - FORMATO CORRETO PARA WEB PUSH");
console.log("=".repeat(80));
console.log("");
console.log("Public Key (base64url):");
console.log(toBase64Url(publicKeyRaw));
console.log("");
console.log("Private Key (base64url):");
console.log(toBase64Url(privateKeyRaw));
console.log("");
console.log("=".repeat(80));
console.log("ADICIONE NO application.properties:");
console.log("=".repeat(80));
console.log("");
console.log(
  `webpush.vapid.public-key=\${VAPID_PUBLIC_KEY:${toBase64Url(publicKeyRaw)}}`
);
console.log(
  `webpush.vapid.private-key=\${VAPID_PRIVATE_KEY:${toBase64Url(
    privateKeyRaw
  )}}`
);
console.log(
  `webpush.vapid.subject=\${VAPID_SUBJECT:mailto:admin@mvt-events.com}`
);
console.log("");
console.log("=".repeat(80));
console.log("IMPORTANTE:");
console.log(
  "- A chave pública deve ter ~88 caracteres (65 bytes em base64url)"
);
console.log(
  "- A chave privada deve ter ~43 caracteres (32 bytes em base64url)"
);
console.log("=".repeat(80));
