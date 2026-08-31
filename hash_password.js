const bcrypt = require('bcryptjs');

const rawPassword = process.argv[2];

if (!rawPassword) {
    console.error("❌ Error: Please provide a password to hash.");
    console.log("Usage: node hash_password.js \"your_password_here\"");
    process.exit(1);
}

// Spring Security BCryptPasswordEncoder default strength is 10
const saltRounds = 10;
const salt = bcrypt.genSaltSync(saltRounds);
const hash = bcrypt.hashSync(rawPassword, salt);

console.log(`\n=================================================`);
console.log(`🔑 Raw Password : ${rawPassword}`);
console.log(`🔒 BCrypt Hash  : ${hash}`);
console.log(`=================================================\n`);

console.log(`✅ Example MySQL UPDATE Statement:`);
console.log(`UPDATE users SET password_hash = '${hash}' WHERE email = 'student@example.com';\n`);
