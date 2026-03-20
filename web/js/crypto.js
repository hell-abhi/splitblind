// Crypto
async function genKey(){const k=await crypto.subtle.generateKey({name:'AES-GCM',length:256},true,['encrypt','decrypt']);return bufB64(new Uint8Array(await crypto.subtle.exportKey('raw',k)))}
async function impKey(b){return crypto.subtle.importKey('raw',b64Buf(b),{name:'AES-GCM'},false,['encrypt','decrypt'])}
async function enc(kb,data){const k=await impKey(kb),iv=crypto.getRandomValues(new Uint8Array(12)),ct=await crypto.subtle.encrypt({name:'AES-GCM',iv},k,new TextEncoder().encode(JSON.stringify(data)));return{d:bufB64(new Uint8Array(ct)),n:bufB64(iv)}}
async function dec(kb,e){const k=await impKey(kb),pt=await crypto.subtle.decrypt({name:'AES-GCM',iv:b64Buf(e.n)},k,b64Buf(e.d));return JSON.parse(new TextDecoder().decode(pt))}
function bufB64(b){let s='';b.forEach(c=>s+=String.fromCharCode(c));return btoa(s)}
function b64Buf(s){const d=atob(s),b=new Uint8Array(d.length);for(let i=0;i<d.length;i++)b[i]=d.charCodeAt(i);return b}
