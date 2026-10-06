// Evaluates Parliva data files inside an isolated vm context (no require/process) and dumps globals.
const vm = require('vm'); const fs = require('fs');
const dir = process.argv[2]; const files = process.argv.slice(3);
const window = {}; const ctx = vm.createContext({ window, console: { log() {} } });
for (const f of files) {
  let src = fs.readFileSync(dir + '/' + f, 'utf8');
  // expose top-level const quiz arrays too
  src += '\n;try{window.CORE_WORD_QUIZ=CORE_WORD_QUIZ}catch(e){}';
  try { vm.runInContext(src, ctx, { timeout: 2000 }); } catch (e) { console.error(f, e.message); }
}
fs.writeFileSync(process.argv[2] + '/../../../../dump-' + dir.split('/').pop() + '.json', JSON.stringify(window, null, 1));
console.error(Object.keys(window).map(k => k + ':' + (Array.isArray(window[k]) ? window[k].length : typeof window[k])).join('\n'));
