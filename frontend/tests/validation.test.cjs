const {test}=require("node:test");
const assert=require("node:assert/strict");
const fs=require("node:fs");
const path=require("node:path");
const vm=require("node:vm");
const ts=require("typescript");
function load(file){
 const filename=path.join(__dirname,"../src",file);
 const javascript=ts.transpileModule(fs.readFileSync(filename,"utf8"),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
 const context={exports:{},URL,TextEncoder};vm.runInNewContext(javascript,context);return context.exports;
}
const {validateArticleText,validateArticleUrl}=load("features/submission/validation.ts");
const {validateRegistration}=load("features/registration/validation.ts");
test("blank and whitespace-only text remain invalid",()=>{for(const value of [""," ","\t\n","\u2003"])assert.ok(validateArticleText(value,20000));});
test("the text limit counts Unicode code points",()=>{assert.equal(validateArticleText("😀".repeat(20000),20000),"");assert.ok(validateArticleText("😀".repeat(20001),20000));});
test("both HTTP and HTTPS links remain valid",()=>{assert.equal(validateArticleUrl("https://news.org/article"),"");assert.equal(validateArticleUrl("http://news.org/article"),"");});
test("unsupported schemes, credentials and malformed URLs remain invalid",()=>{for(const value of ["ftp://news.org/a","javascript:alert(1)","https://user:pass@news.org/a","not a URL","https://news.org/a b"])assert.ok(validateArticleUrl(value));});
test("registration keeps the eight-character password boundary",()=>{const fields={name:"Test Reader",email:"reader@example.com",password:"Abcd1234"};assert.equal(Object.keys(validateRegistration(fields)).length,0);assert.ok(validateRegistration({...fields,password:"Abcd123"}).password);});
test("registration still validates email and password byte length",()=>{const fields={name:"Test Reader",email:"invalid",password:"Abcd1234"};assert.ok(validateRegistration(fields).email);assert.ok(validateRegistration({...fields,email:"reader@example.com",password:"A1"+"😀".repeat(18)}).password);});
