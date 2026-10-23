import { readFileSync } from "node:fs";
import { Script } from "node:vm";
import assert from "node:assert/strict";

const html = readFileSync(new URL("./index.html", import.meta.url), "utf8");
const servedHtml = readFileSync(new URL("../../backend/src/main/resources/static/index.html", import.meta.url), "utf8");
assert.equal(servedHtml, html, "Browser source and Spring Boot served page must stay in sync");
assert.match(html, /<html lang="en">/);
assert.match(html, /name="viewport"/);
assert.match(html, /prefers-reduced-motion/);
assert.match(html, /aria-live="polite"/);
assert.match(html, /name="description"/);
assert.match(html, /reset-password\.html/);
assert.ok(html.includes("/api/auth/login"));
assert.ok(html.includes("/api/auth/register"));
assert.ok(html.includes("/api/games"));
assert.ok(html.includes("/api/games/sessions"));

const ids = new Set([...html.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]));
const duplicates = [...html.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]).filter((id, index, all) => all.indexOf(id) !== index);
assert.deepEqual(duplicates, [], "HTML IDs must be unique");

const scripts = [...html.matchAll(/<script>([\s\S]*?)<\/script>/gi)];
assert.equal(scripts.length, 1, "Expected one first-party inline client script");
new Script(scripts[0][1], { filename: "client/web/index.html:inline-script" });

const referencedIds = new Set([...scripts[0][1].matchAll(/\$\(["']([A-Za-z][A-Za-z0-9_-]*)["']\)/g)].map(match => match[1]));
const missing = [...referencedIds].filter(id => !ids.has(id));
assert.deepEqual(missing, [], "Every literal DOM lookup must reference an existing HTML id");

for (const gameId of ["skyline-crash", "neon-reels", "reef-ranger"]) {
  assert.ok(html.includes(gameId), `Missing catalogue integration for ${gameId}`);
}
assert.match(html, /credentials:"same-origin"/);
assert.match(html, /Authorization/);
assert.doesNotMatch(html, /localStorage\.setItem\(["'](?:token|accessToken)/i);
console.log(`Client smoke checks passed: ${ids.size} unique IDs, ${referencedIds.size} DOM references, inline JavaScript syntax valid.`);
