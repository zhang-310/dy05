const fs = require("fs");
const path = require("path");
function walk(dir) { let results = []; for (const e of fs.readdirSync(dir, {withFileTypes:true})) { if(e.name==="node_modules"||e.name===".git")continue; const f=path.join(dir,e.name); if(e.isDirectory())results=results.concat(walk(f)); else if(e.name.endsWith(".tsx")||e.name.endsWith(".ts"))results.push(f); } return results; }
const files=walk("src"); const casts=[];
for(const file of files){const content=fs.readFileSync(file,"utf8"); content.split("\n").forEach((line,i)=>{const m=line.match(/as unknown as|as any/g); if(m){casts.push(file.replace(/\\/g,"/").replace("src/","").replace(/.*\//,"")+":"+(i+1)+" ("+m.length+"): "+line.trim().slice(0,120));}});}
casts.forEach(c=>console.log(c)); console.log("Total casts:",casts.length);
