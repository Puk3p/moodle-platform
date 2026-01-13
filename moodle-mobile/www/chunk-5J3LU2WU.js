import{c as me}from"./chunk-TNQHSINF.js";import{$ as z,Ab as le,Db as fe,Eb as ue,Ma as re,Pb as ce,W as vt,X as ne,db as gt,eb as ie,fb as oe,ja as ae,kc as yt,lc as de,mb as se,nc as tt,qc as S}from"./chunk-N63NGGH2.js";import{a as te,b as ee}from"./chunk-FDERIQAA.js";function Ct(t,n){(n==null||n>t.length)&&(n=t.length);for(var e=0,a=Array(n);e<n;e++)a[e]=t[e];return a}function na(t){if(Array.isArray(t))return t}function aa(t){if(Array.isArray(t))return Ct(t)}function ra(t,n){if(!(t instanceof n))throw new TypeError("Cannot call a class as a function")}function he(t,n){for(var e=0;e<n.length;e++){var a=n[e];a.enumerable=a.enumerable||!1,a.configurable=!0,"value"in a&&(a.writable=!0),Object.defineProperty(t,Ve(a.key),a)}}function ia(t,n,e){return n&&he(t.prototype,n),e&&he(t,e),Object.defineProperty(t,"prototype",{writable:!1}),t}function at(t,n){var e=typeof Symbol<"u"&&t[Symbol.iterator]||t["@@iterator"];if(!e){if(Array.isArray(t)||(e=Wt(t))||n&&t&&typeof t.length=="number"){e&&(t=e);var a=0,r=function(){};return{s:r,n:function(){return a>=t.length?{done:!0}:{done:!1,value:t[a++]}},e:function(l){throw l},f:r}}throw new TypeError(`Invalid attempt to iterate non-iterable instance.
In order to be iterable, non-array objects must have a [Symbol.iterator]() method.`)}var i,o=!0,s=!1;return{s:function(){e=e.call(t)},n:function(){var l=e.next();return o=l.done,l},e:function(l){s=!0,i=l},f:function(){try{o||e.return==null||e.return()}finally{if(s)throw i}}}}function h(t,n,e){return(n=Ve(n))in t?Object.defineProperty(t,n,{value:e,enumerable:!0,configurable:!0,writable:!0}):t[n]=e,t}function oa(t){if(typeof Symbol<"u"&&t[Symbol.iterator]!=null||t["@@iterator"]!=null)return Array.from(t)}function sa(t,n){var e=t==null?null:typeof Symbol<"u"&&t[Symbol.iterator]||t["@@iterator"];if(e!=null){var a,r,i,o,s=[],l=!0,u=!1;try{if(i=(e=e.call(t)).next,n===0){if(Object(e)!==e)return;l=!1}else for(;!(l=(a=i.call(e)).done)&&(s.push(a.value),s.length!==n);l=!0);}catch(d){u=!0,r=d}finally{try{if(!l&&e.return!=null&&(o=e.return(),Object(o)!==o))return}finally{if(u)throw r}}return s}}function la(){throw new TypeError(`Invalid attempt to destructure non-iterable instance.
In order to be iterable, non-array objects must have a [Symbol.iterator]() method.`)}function fa(){throw new TypeError(`Invalid attempt to spread non-iterable instance.
In order to be iterable, non-array objects must have a [Symbol.iterator]() method.`)}function pe(t,n){var e=Object.keys(t);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(t);n&&(a=a.filter(function(r){return Object.getOwnPropertyDescriptor(t,r).enumerable})),e.push.apply(e,a)}return e}function f(t){for(var n=1;n<arguments.length;n++){var e=arguments[n]!=null?arguments[n]:{};n%2?pe(Object(e),!0).forEach(function(a){h(t,a,e[a])}):Object.getOwnPropertyDescriptors?Object.defineProperties(t,Object.getOwnPropertyDescriptors(e)):pe(Object(e)).forEach(function(a){Object.defineProperty(t,a,Object.getOwnPropertyDescriptor(e,a))})}return t}function ft(t,n){return na(t)||sa(t,n)||Wt(t,n)||la()}function F(t){return aa(t)||oa(t)||Wt(t)||fa()}function ua(t,n){if(typeof t!="object"||!t)return t;var e=t[Symbol.toPrimitive];if(e!==void 0){var a=e.call(t,n||"default");if(typeof a!="object")return a;throw new TypeError("@@toPrimitive must return a primitive value.")}return(n==="string"?String:Number)(t)}function Ve(t){var n=ua(t,"string");return typeof n=="symbol"?n:n+""}function ot(t){"@babel/helpers - typeof";return ot=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(n){return typeof n}:function(n){return n&&typeof Symbol=="function"&&n.constructor===Symbol&&n!==Symbol.prototype?"symbol":typeof n},ot(t)}function Wt(t,n){if(t){if(typeof t=="string")return Ct(t,n);var e={}.toString.call(t).slice(8,-1);return e==="Object"&&t.constructor&&(e=t.constructor.name),e==="Map"||e==="Set"?Array.from(t):e==="Arguments"||/^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(e)?Ct(t,n):void 0}}var ve=function(){},Ut={},Ge={},Xe=null,Ke={mark:ve,measure:ve};try{typeof window<"u"&&(Ut=window),typeof document<"u"&&(Ge=document),typeof MutationObserver<"u"&&(Xe=MutationObserver),typeof performance<"u"&&(Ke=performance)}catch{}var ca=Ut.navigator||{},ge=ca.userAgent,ye=ge===void 0?"":ge,M=Ut,g=Ge,be=Xe,et=Ke,Eo=!!M.document,j=!!g.documentElement&&!!g.head&&typeof g.addEventListener=="function"&&typeof g.createElement=="function",Je=~ye.indexOf("MSIE")||~ye.indexOf("Trident/"),bt,da=/fa(k|kd|s|r|l|t|d|dr|dl|dt|b|slr|slpr|wsb|tl|ns|nds|es|jr|jfr|jdr|usb|ufsb|udsb|cr|ss|sr|sl|st|sds|sdr|sdl|sdt)?[\-\ ]/,ma=/Font ?Awesome ?([567 ]*)(Solid|Regular|Light|Thin|Duotone|Brands|Free|Pro|Sharp Duotone|Sharp|Kit|Notdog Duo|Notdog|Chisel|Etch|Thumbprint|Jelly Fill|Jelly Duo|Jelly|Utility|Utility Fill|Utility Duo|Slab Press|Slab|Whiteboard)?.*/i,qe={classic:{fa:"solid",fas:"solid","fa-solid":"solid",far:"regular","fa-regular":"regular",fal:"light","fa-light":"light",fat:"thin","fa-thin":"thin",fab:"brands","fa-brands":"brands"},duotone:{fa:"solid",fad:"solid","fa-solid":"solid","fa-duotone":"solid",fadr:"regular","fa-regular":"regular",fadl:"light","fa-light":"light",fadt:"thin","fa-thin":"thin"},sharp:{fa:"solid",fass:"solid","fa-solid":"solid",fasr:"regular","fa-regular":"regular",fasl:"light","fa-light":"light",fast:"thin","fa-thin":"thin"},"sharp-duotone":{fa:"solid",fasds:"solid","fa-solid":"solid",fasdr:"regular","fa-regular":"regular",fasdl:"light","fa-light":"light",fasdt:"thin","fa-thin":"thin"},slab:{"fa-regular":"regular",faslr:"regular"},"slab-press":{"fa-regular":"regular",faslpr:"regular"},thumbprint:{"fa-light":"light",fatl:"light"},whiteboard:{"fa-semibold":"semibold",fawsb:"semibold"},notdog:{"fa-solid":"solid",fans:"solid"},"notdog-duo":{"fa-solid":"solid",fands:"solid"},etch:{"fa-solid":"solid",faes:"solid"},jelly:{"fa-regular":"regular",fajr:"regular"},"jelly-fill":{"fa-regular":"regular",fajfr:"regular"},"jelly-duo":{"fa-regular":"regular",fajdr:"regular"},chisel:{"fa-regular":"regular",facr:"regular"},utility:{"fa-semibold":"semibold",fausb:"semibold"},"utility-duo":{"fa-semibold":"semibold",faudsb:"semibold"},"utility-fill":{"fa-semibold":"semibold",faufsb:"semibold"}},ha={GROUP:"duotone-group",SWAP_OPACITY:"swap-opacity",PRIMARY:"primary",SECONDARY:"secondary"},Qe=["fa-classic","fa-duotone","fa-sharp","fa-sharp-duotone","fa-thumbprint","fa-whiteboard","fa-notdog","fa-notdog-duo","fa-chisel","fa-etch","fa-jelly","fa-jelly-fill","fa-jelly-duo","fa-slab","fa-slab-press","fa-utility","fa-utility-duo","fa-utility-fill"],w="classic",q="duotone",Ze="sharp",tn="sharp-duotone",en="chisel",nn="etch",an="jelly",rn="jelly-duo",on="jelly-fill",sn="notdog",ln="notdog-duo",fn="slab",un="slab-press",cn="thumbprint",dn="utility",mn="utility-duo",hn="utility-fill",pn="whiteboard",pa="Classic",va="Duotone",ga="Sharp",ya="Sharp Duotone",ba="Chisel",xa="Etch",wa="Jelly",ka="Jelly Duo",Sa="Jelly Fill",Aa="Notdog",Ia="Notdog Duo",Ca="Slab",Pa="Slab Press",Fa="Thumbprint",Ea="Utility",Oa="Utility Duo",Na="Utility Fill",Ta="Whiteboard",vn=[w,q,Ze,tn,en,nn,an,rn,on,sn,ln,fn,un,cn,dn,mn,hn,pn],Oo=(bt={},h(h(h(h(h(h(h(h(h(h(bt,w,pa),q,va),Ze,ga),tn,ya),en,ba),nn,xa),an,wa),rn,ka),on,Sa),sn,Aa),h(h(h(h(h(h(h(h(bt,ln,Ia),fn,Ca),un,Pa),cn,Fa),dn,Ea),mn,Oa),hn,Na),pn,Ta)),ja={classic:{900:"fas",400:"far",normal:"far",300:"fal",100:"fat"},duotone:{900:"fad",400:"fadr",300:"fadl",100:"fadt"},sharp:{900:"fass",400:"fasr",300:"fasl",100:"fast"},"sharp-duotone":{900:"fasds",400:"fasdr",300:"fasdl",100:"fasdt"},slab:{400:"faslr"},"slab-press":{400:"faslpr"},whiteboard:{600:"fawsb"},thumbprint:{300:"fatl"},notdog:{900:"fans"},"notdog-duo":{900:"fands"},etch:{900:"faes"},chisel:{400:"facr"},jelly:{400:"fajr"},"jelly-fill":{400:"fajfr"},"jelly-duo":{400:"fajdr"},utility:{600:"fausb"},"utility-duo":{600:"faudsb"},"utility-fill":{600:"faufsb"}},Da={"Font Awesome 7 Free":{900:"fas",400:"far"},"Font Awesome 7 Pro":{900:"fas",400:"far",normal:"far",300:"fal",100:"fat"},"Font Awesome 7 Brands":{400:"fab",normal:"fab"},"Font Awesome 7 Duotone":{900:"fad",400:"fadr",normal:"fadr",300:"fadl",100:"fadt"},"Font Awesome 7 Sharp":{900:"fass",400:"fasr",normal:"fasr",300:"fasl",100:"fast"},"Font Awesome 7 Sharp Duotone":{900:"fasds",400:"fasdr",normal:"fasdr",300:"fasdl",100:"fasdt"},"Font Awesome 7 Jelly":{400:"fajr",normal:"fajr"},"Font Awesome 7 Jelly Fill":{400:"fajfr",normal:"fajfr"},"Font Awesome 7 Jelly Duo":{400:"fajdr",normal:"fajdr"},"Font Awesome 7 Slab":{400:"faslr",normal:"faslr"},"Font Awesome 7 Slab Press":{400:"faslpr",normal:"faslpr"},"Font Awesome 7 Thumbprint":{300:"fatl",normal:"fatl"},"Font Awesome 7 Notdog":{900:"fans",normal:"fans"},"Font Awesome 7 Notdog Duo":{900:"fands",normal:"fands"},"Font Awesome 7 Etch":{900:"faes",normal:"faes"},"Font Awesome 7 Chisel":{400:"facr",normal:"facr"},"Font Awesome 7 Whiteboard":{600:"fawsb",normal:"fawsb"},"Font Awesome 7 Utility":{600:"fausb",normal:"fausb"},"Font Awesome 7 Utility Duo":{600:"faudsb",normal:"faudsb"},"Font Awesome 7 Utility Fill":{600:"faufsb",normal:"faufsb"}},Ma=new Map([["classic",{defaultShortPrefixId:"fas",defaultStyleId:"solid",styleIds:["solid","regular","light","thin","brands"],futureStyleIds:[],defaultFontWeight:900}],["duotone",{defaultShortPrefixId:"fad",defaultStyleId:"solid",styleIds:["solid","regular","light","thin"],futureStyleIds:[],defaultFontWeight:900}],["sharp",{defaultShortPrefixId:"fass",defaultStyleId:"solid",styleIds:["solid","regular","light","thin"],futureStyleIds:[],defaultFontWeight:900}],["sharp-duotone",{defaultShortPrefixId:"fasds",defaultStyleId:"solid",styleIds:["solid","regular","light","thin"],futureStyleIds:[],defaultFontWeight:900}],["chisel",{defaultShortPrefixId:"facr",defaultStyleId:"regular",styleIds:["regular"],futureStyleIds:[],defaultFontWeight:400}],["etch",{defaultShortPrefixId:"faes",defaultStyleId:"solid",styleIds:["solid"],futureStyleIds:[],defaultFontWeight:900}],["jelly",{defaultShortPrefixId:"fajr",defaultStyleId:"regular",styleIds:["regular"],futureStyleIds:[],defaultFontWeight:400}],["jelly-duo",{defaultShortPrefixId:"fajdr",defaultStyleId:"regular",styleIds:["regular"],futureStyleIds:[],defaultFontWeight:400}],["jelly-fill",{defaultShortPrefixId:"fajfr",defaultStyleId:"regular",styleIds:["regular"],futureStyleIds:[],defaultFontWeight:400}],["notdog",{defaultShortPrefixId:"fans",defaultStyleId:"solid",styleIds:["solid"],futureStyleIds:[],defaultFontWeight:900}],["notdog-duo",{defaultShortPrefixId:"fands",defaultStyleId:"solid",styleIds:["solid"],futureStyleIds:[],defaultFontWeight:900}],["slab",{defaultShortPrefixId:"faslr",defaultStyleId:"regular",styleIds:["regular"],futureStyleIds:[],defaultFontWeight:400}],["slab-press",{defaultShortPrefixId:"faslpr",defaultStyleId:"regular",styleIds:["regular"],futureStyleIds:[],defaultFontWeight:400}],["thumbprint",{defaultShortPrefixId:"fatl",defaultStyleId:"light",styleIds:["light"],futureStyleIds:[],defaultFontWeight:300}],["utility",{defaultShortPrefixId:"fausb",defaultStyleId:"semibold",styleIds:["semibold"],futureStyleIds:[],defaultFontWeight:600}],["utility-duo",{defaultShortPrefixId:"faudsb",defaultStyleId:"semibold",styleIds:["semibold"],futureStyleIds:[],defaultFontWeight:600}],["utility-fill",{defaultShortPrefixId:"faufsb",defaultStyleId:"semibold",styleIds:["semibold"],futureStyleIds:[],defaultFontWeight:600}],["whiteboard",{defaultShortPrefixId:"fawsb",defaultStyleId:"semibold",styleIds:["semibold"],futureStyleIds:[],defaultFontWeight:600}]]),_a={chisel:{regular:"facr"},classic:{brands:"fab",light:"fal",regular:"far",solid:"fas",thin:"fat"},duotone:{light:"fadl",regular:"fadr",solid:"fad",thin:"fadt"},etch:{solid:"faes"},jelly:{regular:"fajr"},"jelly-duo":{regular:"fajdr"},"jelly-fill":{regular:"fajfr"},notdog:{solid:"fans"},"notdog-duo":{solid:"fands"},sharp:{light:"fasl",regular:"fasr",solid:"fass",thin:"fast"},"sharp-duotone":{light:"fasdl",regular:"fasdr",solid:"fasds",thin:"fasdt"},slab:{regular:"faslr"},"slab-press":{regular:"faslpr"},thumbprint:{light:"fatl"},utility:{semibold:"fausb"},"utility-duo":{semibold:"faudsb"},"utility-fill":{semibold:"faufsb"},whiteboard:{semibold:"fawsb"}},gn=["fak","fa-kit","fakd","fa-kit-duotone"],xe={kit:{fak:"kit","fa-kit":"kit"},"kit-duotone":{fakd:"kit-duotone","fa-kit-duotone":"kit-duotone"}},$a=["kit"],za="kit",La="kit-duotone",Ra="Kit",Wa="Kit Duotone",No=h(h({},za,Ra),La,Wa),Ua={kit:{"fa-kit":"fak"},"kit-duotone":{"fa-kit-duotone":"fakd"}},Ha={"Font Awesome Kit":{400:"fak",normal:"fak"},"Font Awesome Kit Duotone":{400:"fakd",normal:"fakd"}},Ya={kit:{fak:"fa-kit"},"kit-duotone":{fakd:"fa-kit-duotone"}},we={kit:{kit:"fak"},"kit-duotone":{"kit-duotone":"fakd"}},xt,nt={GROUP:"duotone-group",SWAP_OPACITY:"swap-opacity",PRIMARY:"primary",SECONDARY:"secondary"},Ba=["fa-classic","fa-duotone","fa-sharp","fa-sharp-duotone","fa-thumbprint","fa-whiteboard","fa-notdog","fa-notdog-duo","fa-chisel","fa-etch","fa-jelly","fa-jelly-fill","fa-jelly-duo","fa-slab","fa-slab-press","fa-utility","fa-utility-duo","fa-utility-fill"],Va="classic",Ga="duotone",Xa="sharp",Ka="sharp-duotone",Ja="chisel",qa="etch",Qa="jelly",Za="jelly-duo",tr="jelly-fill",er="notdog",nr="notdog-duo",ar="slab",rr="slab-press",ir="thumbprint",or="utility",sr="utility-duo",lr="utility-fill",fr="whiteboard",ur="Classic",cr="Duotone",dr="Sharp",mr="Sharp Duotone",hr="Chisel",pr="Etch",vr="Jelly",gr="Jelly Duo",yr="Jelly Fill",br="Notdog",xr="Notdog Duo",wr="Slab",kr="Slab Press",Sr="Thumbprint",Ar="Utility",Ir="Utility Duo",Cr="Utility Fill",Pr="Whiteboard",To=(xt={},h(h(h(h(h(h(h(h(h(h(xt,Va,ur),Ga,cr),Xa,dr),Ka,mr),Ja,hr),qa,pr),Qa,vr),Za,gr),tr,yr),er,br),h(h(h(h(h(h(h(h(xt,nr,xr),ar,wr),rr,kr),ir,Sr),or,Ar),sr,Ir),lr,Cr),fr,Pr)),Fr="kit",Er="kit-duotone",Or="Kit",Nr="Kit Duotone",jo=h(h({},Fr,Or),Er,Nr),Tr={classic:{"fa-brands":"fab","fa-duotone":"fad","fa-light":"fal","fa-regular":"far","fa-solid":"fas","fa-thin":"fat"},duotone:{"fa-regular":"fadr","fa-light":"fadl","fa-thin":"fadt"},sharp:{"fa-solid":"fass","fa-regular":"fasr","fa-light":"fasl","fa-thin":"fast"},"sharp-duotone":{"fa-solid":"fasds","fa-regular":"fasdr","fa-light":"fasdl","fa-thin":"fasdt"},slab:{"fa-regular":"faslr"},"slab-press":{"fa-regular":"faslpr"},whiteboard:{"fa-semibold":"fawsb"},thumbprint:{"fa-light":"fatl"},notdog:{"fa-solid":"fans"},"notdog-duo":{"fa-solid":"fands"},etch:{"fa-solid":"faes"},jelly:{"fa-regular":"fajr"},"jelly-fill":{"fa-regular":"fajfr"},"jelly-duo":{"fa-regular":"fajdr"},chisel:{"fa-regular":"facr"},utility:{"fa-semibold":"fausb"},"utility-duo":{"fa-semibold":"faudsb"},"utility-fill":{"fa-semibold":"faufsb"}},jr={classic:["fas","far","fal","fat","fad"],duotone:["fadr","fadl","fadt"],sharp:["fass","fasr","fasl","fast"],"sharp-duotone":["fasds","fasdr","fasdl","fasdt"],slab:["faslr"],"slab-press":["faslpr"],whiteboard:["fawsb"],thumbprint:["fatl"],notdog:["fans"],"notdog-duo":["fands"],etch:["faes"],jelly:["fajr"],"jelly-fill":["fajfr"],"jelly-duo":["fajdr"],chisel:["facr"],utility:["fausb"],"utility-duo":["faudsb"],"utility-fill":["faufsb"]},Pt={classic:{fab:"fa-brands",fad:"fa-duotone",fal:"fa-light",far:"fa-regular",fas:"fa-solid",fat:"fa-thin"},duotone:{fadr:"fa-regular",fadl:"fa-light",fadt:"fa-thin"},sharp:{fass:"fa-solid",fasr:"fa-regular",fasl:"fa-light",fast:"fa-thin"},"sharp-duotone":{fasds:"fa-solid",fasdr:"fa-regular",fasdl:"fa-light",fasdt:"fa-thin"},slab:{faslr:"fa-regular"},"slab-press":{faslpr:"fa-regular"},whiteboard:{fawsb:"fa-semibold"},thumbprint:{fatl:"fa-light"},notdog:{fans:"fa-solid"},"notdog-duo":{fands:"fa-solid"},etch:{faes:"fa-solid"},jelly:{fajr:"fa-regular"},"jelly-fill":{fajfr:"fa-regular"},"jelly-duo":{fajdr:"fa-regular"},chisel:{facr:"fa-regular"},utility:{fausb:"fa-semibold"},"utility-duo":{faudsb:"fa-semibold"},"utility-fill":{faufsb:"fa-semibold"}},Dr=["fa-solid","fa-regular","fa-light","fa-thin","fa-duotone","fa-brands","fa-semibold"],yn=["fa","fas","far","fal","fat","fad","fadr","fadl","fadt","fab","fass","fasr","fasl","fast","fasds","fasdr","fasdl","fasdt","faslr","faslpr","fawsb","fatl","fans","fands","faes","fajr","fajfr","fajdr","facr","fausb","faudsb","faufsb"].concat(Ba,Dr),Mr=["solid","regular","light","thin","duotone","brands","semibold"],bn=[1,2,3,4,5,6,7,8,9,10],_r=bn.concat([11,12,13,14,15,16,17,18,19,20]),$r=["aw","fw","pull-left","pull-right"],zr=[].concat(F(Object.keys(jr)),Mr,$r,["2xs","xs","sm","lg","xl","2xl","beat","border","fade","beat-fade","bounce","flip-both","flip-horizontal","flip-vertical","flip","inverse","layers","layers-bottom-left","layers-bottom-right","layers-counter","layers-text","layers-top-left","layers-top-right","li","pull-end","pull-start","pulse","rotate-180","rotate-270","rotate-90","rotate-by","shake","spin-pulse","spin-reverse","spin","stack-1x","stack-2x","stack","ul","width-auto","width-fixed",nt.GROUP,nt.SWAP_OPACITY,nt.PRIMARY,nt.SECONDARY]).concat(bn.map(function(t){return"".concat(t,"x")})).concat(_r.map(function(t){return"w-".concat(t)})),Lr={"Font Awesome 5 Free":{900:"fas",400:"far"},"Font Awesome 5 Pro":{900:"fas",400:"far",normal:"far",300:"fal"},"Font Awesome 5 Brands":{400:"fab",normal:"fab"},"Font Awesome 5 Duotone":{900:"fad"}},N="___FONT_AWESOME___",Ft=16,xn="fa",wn="svg-inline--fa",R="data-fa-i2svg",Et="data-fa-pseudo-element",Rr="data-fa-pseudo-element-pending",Ht="data-prefix",Yt="data-icon",ke="fontawesome-i2svg",Wr="async",Ur=["HTML","HEAD","STYLE","SCRIPT"],kn=["::before","::after",":before",":after"],Sn=(function(){try{return!0}catch{return!1}})();function Q(t){return new Proxy(t,{get:function(e,a){return a in e?e[a]:e[w]}})}var An=f({},qe);An[w]=f(f(f(f({},{"fa-duotone":"duotone"}),qe[w]),xe.kit),xe["kit-duotone"]);var Hr=Q(An),Ot=f({},_a);Ot[w]=f(f(f(f({},{duotone:"fad"}),Ot[w]),we.kit),we["kit-duotone"]);var Se=Q(Ot),Nt=f({},Pt);Nt[w]=f(f({},Nt[w]),Ya.kit);var Bt=Q(Nt),Tt=f({},Tr);Tt[w]=f(f({},Tt[w]),Ua.kit);var Do=Q(Tt),Yr=da,In="fa-layers-text",Br=ma,Vr=f({},ja),Mo=Q(Vr),Gr=["class","data-prefix","data-icon","data-fa-transform","data-fa-mask"],wt=ha,Xr=[].concat(F($a),F(zr)),X=M.FontAwesomeConfig||{};function Kr(t){var n=g.querySelector("script["+t+"]");if(n)return n.getAttribute(t)}function Jr(t){return t===""?!0:t==="false"?!1:t==="true"?!0:t}g&&typeof g.querySelector=="function"&&(Ae=[["data-family-prefix","familyPrefix"],["data-css-prefix","cssPrefix"],["data-family-default","familyDefault"],["data-style-default","styleDefault"],["data-replacement-class","replacementClass"],["data-auto-replace-svg","autoReplaceSvg"],["data-auto-add-css","autoAddCss"],["data-search-pseudo-elements","searchPseudoElements"],["data-search-pseudo-elements-warnings","searchPseudoElementsWarnings"],["data-search-pseudo-elements-full-scan","searchPseudoElementsFullScan"],["data-observe-mutations","observeMutations"],["data-mutate-approach","mutateApproach"],["data-keep-original-source","keepOriginalSource"],["data-measure-performance","measurePerformance"],["data-show-missing-icons","showMissingIcons"]],Ae.forEach(function(t){var n=ft(t,2),e=n[0],a=n[1],r=Jr(Kr(e));r!=null&&(X[a]=r)}));var Ae,Cn={styleDefault:"solid",familyDefault:w,cssPrefix:xn,replacementClass:wn,autoReplaceSvg:!0,autoAddCss:!0,searchPseudoElements:!1,searchPseudoElementsWarnings:!0,searchPseudoElementsFullScan:!1,observeMutations:!0,mutateApproach:"async",keepOriginalSource:!0,measurePerformance:!1,showMissingIcons:!0};X.familyPrefix&&(X.cssPrefix=X.familyPrefix);var B=f(f({},Cn),X);B.autoReplaceSvg||(B.observeMutations=!1);var m={};Object.keys(Cn).forEach(function(t){Object.defineProperty(m,t,{enumerable:!0,set:function(e){B[t]=e,K.forEach(function(a){return a(m)})},get:function(){return B[t]}})});Object.defineProperty(m,"familyPrefix",{enumerable:!0,set:function(n){B.cssPrefix=n,K.forEach(function(e){return e(m)})},get:function(){return B.cssPrefix}});M.FontAwesomeConfig=m;var K=[];function qr(t){return K.push(t),function(){K.splice(K.indexOf(t),1)}}var D=Ft,E={size:16,x:0,y:0,rotate:0,flipX:!1,flipY:!1};function Qr(t){if(!(!t||!j)){var n=g.createElement("style");n.setAttribute("type","text/css"),n.innerHTML=t;for(var e=g.head.childNodes,a=null,r=e.length-1;r>-1;r--){var i=e[r],o=(i.tagName||"").toUpperCase();["STYLE","LINK"].indexOf(o)>-1&&(a=i)}return g.head.insertBefore(n,a),t}}var Zr="0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";function Ie(){for(var t=12,n="";t-- >0;)n+=Zr[Math.random()*62|0];return n}function V(t){for(var n=[],e=(t||[]).length>>>0;e--;)n[e]=t[e];return n}function Vt(t){return t.classList?V(t.classList):(t.getAttribute("class")||"").split(" ").filter(function(n){return n})}function Pn(t){return"".concat(t).replace(/&/g,"&amp;").replace(/"/g,"&quot;").replace(/'/g,"&#39;").replace(/</g,"&lt;").replace(/>/g,"&gt;")}function ti(t){return Object.keys(t||{}).reduce(function(n,e){return n+"".concat(e,'="').concat(Pn(t[e]),'" ')},"").trim()}function ut(t){return Object.keys(t||{}).reduce(function(n,e){return n+"".concat(e,": ").concat(t[e].trim(),";")},"")}function Gt(t){return t.size!==E.size||t.x!==E.x||t.y!==E.y||t.rotate!==E.rotate||t.flipX||t.flipY}function ei(t){var n=t.transform,e=t.containerWidth,a=t.iconWidth,r={transform:"translate(".concat(e/2," 256)")},i="translate(".concat(n.x*32,", ").concat(n.y*32,") "),o="scale(".concat(n.size/16*(n.flipX?-1:1),", ").concat(n.size/16*(n.flipY?-1:1),") "),s="rotate(".concat(n.rotate," 0 0)"),l={transform:"".concat(i," ").concat(o," ").concat(s)},u={transform:"translate(".concat(a/2*-1," -256)")};return{outer:r,inner:l,path:u}}function ni(t){var n=t.transform,e=t.width,a=e===void 0?Ft:e,r=t.height,i=r===void 0?Ft:r,o=t.startCentered,s=o===void 0?!1:o,l="";return s&&Je?l+="translate(".concat(n.x/D-a/2,"em, ").concat(n.y/D-i/2,"em) "):s?l+="translate(calc(-50% + ".concat(n.x/D,"em), calc(-50% + ").concat(n.y/D,"em)) "):l+="translate(".concat(n.x/D,"em, ").concat(n.y/D,"em) "),l+="scale(".concat(n.size/D*(n.flipX?-1:1),", ").concat(n.size/D*(n.flipY?-1:1),") "),l+="rotate(".concat(n.rotate,"deg) "),l}var ai=`:root, :host {
  --fa-font-solid: normal 900 1em/1 "Font Awesome 7 Free";
  --fa-font-regular: normal 400 1em/1 "Font Awesome 7 Free";
  --fa-font-light: normal 300 1em/1 "Font Awesome 7 Pro";
  --fa-font-thin: normal 100 1em/1 "Font Awesome 7 Pro";
  --fa-font-duotone: normal 900 1em/1 "Font Awesome 7 Duotone";
  --fa-font-duotone-regular: normal 400 1em/1 "Font Awesome 7 Duotone";
  --fa-font-duotone-light: normal 300 1em/1 "Font Awesome 7 Duotone";
  --fa-font-duotone-thin: normal 100 1em/1 "Font Awesome 7 Duotone";
  --fa-font-brands: normal 400 1em/1 "Font Awesome 7 Brands";
  --fa-font-sharp-solid: normal 900 1em/1 "Font Awesome 7 Sharp";
  --fa-font-sharp-regular: normal 400 1em/1 "Font Awesome 7 Sharp";
  --fa-font-sharp-light: normal 300 1em/1 "Font Awesome 7 Sharp";
  --fa-font-sharp-thin: normal 100 1em/1 "Font Awesome 7 Sharp";
  --fa-font-sharp-duotone-solid: normal 900 1em/1 "Font Awesome 7 Sharp Duotone";
  --fa-font-sharp-duotone-regular: normal 400 1em/1 "Font Awesome 7 Sharp Duotone";
  --fa-font-sharp-duotone-light: normal 300 1em/1 "Font Awesome 7 Sharp Duotone";
  --fa-font-sharp-duotone-thin: normal 100 1em/1 "Font Awesome 7 Sharp Duotone";
  --fa-font-slab-regular: normal 400 1em/1 "Font Awesome 7 Slab";
  --fa-font-slab-press-regular: normal 400 1em/1 "Font Awesome 7 Slab Press";
  --fa-font-whiteboard-semibold: normal 600 1em/1 "Font Awesome 7 Whiteboard";
  --fa-font-thumbprint-light: normal 300 1em/1 "Font Awesome 7 Thumbprint";
  --fa-font-notdog-solid: normal 900 1em/1 "Font Awesome 7 Notdog";
  --fa-font-notdog-duo-solid: normal 900 1em/1 "Font Awesome 7 Notdog Duo";
  --fa-font-etch-solid: normal 900 1em/1 "Font Awesome 7 Etch";
  --fa-font-jelly-regular: normal 400 1em/1 "Font Awesome 7 Jelly";
  --fa-font-jelly-fill-regular: normal 400 1em/1 "Font Awesome 7 Jelly Fill";
  --fa-font-jelly-duo-regular: normal 400 1em/1 "Font Awesome 7 Jelly Duo";
  --fa-font-chisel-regular: normal 400 1em/1 "Font Awesome 7 Chisel";
  --fa-font-utility-semibold: normal 600 1em/1 "Font Awesome 7 Utility";
  --fa-font-utility-duo-semibold: normal 600 1em/1 "Font Awesome 7 Utility Duo";
  --fa-font-utility-fill-semibold: normal 600 1em/1 "Font Awesome 7 Utility Fill";
}

.svg-inline--fa {
  box-sizing: content-box;
  display: var(--fa-display, inline-block);
  height: 1em;
  overflow: visible;
  vertical-align: -0.125em;
  width: var(--fa-width, 1.25em);
}
.svg-inline--fa.fa-2xs {
  vertical-align: 0.1em;
}
.svg-inline--fa.fa-xs {
  vertical-align: 0em;
}
.svg-inline--fa.fa-sm {
  vertical-align: -0.0714285714em;
}
.svg-inline--fa.fa-lg {
  vertical-align: -0.2em;
}
.svg-inline--fa.fa-xl {
  vertical-align: -0.25em;
}
.svg-inline--fa.fa-2xl {
  vertical-align: -0.3125em;
}
.svg-inline--fa.fa-pull-left,
.svg-inline--fa .fa-pull-start {
  float: inline-start;
  margin-inline-end: var(--fa-pull-margin, 0.3em);
}
.svg-inline--fa.fa-pull-right,
.svg-inline--fa .fa-pull-end {
  float: inline-end;
  margin-inline-start: var(--fa-pull-margin, 0.3em);
}
.svg-inline--fa.fa-li {
  width: var(--fa-li-width, 2em);
  inset-inline-start: calc(-1 * var(--fa-li-width, 2em));
  inset-block-start: 0.25em; /* syncing vertical alignment with Web Font rendering */
}

.fa-layers-counter, .fa-layers-text {
  display: inline-block;
  position: absolute;
  text-align: center;
}

.fa-layers {
  display: inline-block;
  height: 1em;
  position: relative;
  text-align: center;
  vertical-align: -0.125em;
  width: var(--fa-width, 1.25em);
}
.fa-layers .svg-inline--fa {
  inset: 0;
  margin: auto;
  position: absolute;
  transform-origin: center center;
}

.fa-layers-text {
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  transform-origin: center center;
}

.fa-layers-counter {
  background-color: var(--fa-counter-background-color, #ff253a);
  border-radius: var(--fa-counter-border-radius, 1em);
  box-sizing: border-box;
  color: var(--fa-inverse, #fff);
  line-height: var(--fa-counter-line-height, 1);
  max-width: var(--fa-counter-max-width, 5em);
  min-width: var(--fa-counter-min-width, 1.5em);
  overflow: hidden;
  padding: var(--fa-counter-padding, 0.25em 0.5em);
  right: var(--fa-right, 0);
  text-overflow: ellipsis;
  top: var(--fa-top, 0);
  transform: scale(var(--fa-counter-scale, 0.25));
  transform-origin: top right;
}

.fa-layers-bottom-right {
  bottom: var(--fa-bottom, 0);
  right: var(--fa-right, 0);
  top: auto;
  transform: scale(var(--fa-layers-scale, 0.25));
  transform-origin: bottom right;
}

.fa-layers-bottom-left {
  bottom: var(--fa-bottom, 0);
  left: var(--fa-left, 0);
  right: auto;
  top: auto;
  transform: scale(var(--fa-layers-scale, 0.25));
  transform-origin: bottom left;
}

.fa-layers-top-right {
  top: var(--fa-top, 0);
  right: var(--fa-right, 0);
  transform: scale(var(--fa-layers-scale, 0.25));
  transform-origin: top right;
}

.fa-layers-top-left {
  left: var(--fa-left, 0);
  right: auto;
  top: var(--fa-top, 0);
  transform: scale(var(--fa-layers-scale, 0.25));
  transform-origin: top left;
}

.fa-1x {
  font-size: 1em;
}

.fa-2x {
  font-size: 2em;
}

.fa-3x {
  font-size: 3em;
}

.fa-4x {
  font-size: 4em;
}

.fa-5x {
  font-size: 5em;
}

.fa-6x {
  font-size: 6em;
}

.fa-7x {
  font-size: 7em;
}

.fa-8x {
  font-size: 8em;
}

.fa-9x {
  font-size: 9em;
}

.fa-10x {
  font-size: 10em;
}

.fa-2xs {
  font-size: calc(10 / 16 * 1em); /* converts a 10px size into an em-based value that's relative to the scale's 16px base */
  line-height: calc(1 / 10 * 1em); /* sets the line-height of the icon back to that of it's parent */
  vertical-align: calc((6 / 10 - 0.375) * 1em); /* vertically centers the icon taking into account the surrounding text's descender */
}

.fa-xs {
  font-size: calc(12 / 16 * 1em); /* converts a 12px size into an em-based value that's relative to the scale's 16px base */
  line-height: calc(1 / 12 * 1em); /* sets the line-height of the icon back to that of it's parent */
  vertical-align: calc((6 / 12 - 0.375) * 1em); /* vertically centers the icon taking into account the surrounding text's descender */
}

.fa-sm {
  font-size: calc(14 / 16 * 1em); /* converts a 14px size into an em-based value that's relative to the scale's 16px base */
  line-height: calc(1 / 14 * 1em); /* sets the line-height of the icon back to that of it's parent */
  vertical-align: calc((6 / 14 - 0.375) * 1em); /* vertically centers the icon taking into account the surrounding text's descender */
}

.fa-lg {
  font-size: calc(20 / 16 * 1em); /* converts a 20px size into an em-based value that's relative to the scale's 16px base */
  line-height: calc(1 / 20 * 1em); /* sets the line-height of the icon back to that of it's parent */
  vertical-align: calc((6 / 20 - 0.375) * 1em); /* vertically centers the icon taking into account the surrounding text's descender */
}

.fa-xl {
  font-size: calc(24 / 16 * 1em); /* converts a 24px size into an em-based value that's relative to the scale's 16px base */
  line-height: calc(1 / 24 * 1em); /* sets the line-height of the icon back to that of it's parent */
  vertical-align: calc((6 / 24 - 0.375) * 1em); /* vertically centers the icon taking into account the surrounding text's descender */
}

.fa-2xl {
  font-size: calc(32 / 16 * 1em); /* converts a 32px size into an em-based value that's relative to the scale's 16px base */
  line-height: calc(1 / 32 * 1em); /* sets the line-height of the icon back to that of it's parent */
  vertical-align: calc((6 / 32 - 0.375) * 1em); /* vertically centers the icon taking into account the surrounding text's descender */
}

.fa-width-auto {
  --fa-width: auto;
}

.fa-fw,
.fa-width-fixed {
  --fa-width: 1.25em;
}

.fa-ul {
  list-style-type: none;
  margin-inline-start: var(--fa-li-margin, 2.5em);
  padding-inline-start: 0;
}
.fa-ul > li {
  position: relative;
}

.fa-li {
  inset-inline-start: calc(-1 * var(--fa-li-width, 2em));
  position: absolute;
  text-align: center;
  width: var(--fa-li-width, 2em);
  line-height: inherit;
}

/* Heads Up: Bordered Icons will not be supported in the future!
  - This feature will be deprecated in the next major release of Font Awesome (v8)!
  - You may continue to use it in this version *v7), but it will not be supported in Font Awesome v8.
*/
/* Notes:
* --@{v.$css-prefix}-border-width = 1/16 by default (to render as ~1px based on a 16px default font-size)
* --@{v.$css-prefix}-border-padding =
  ** 3/16 for vertical padding (to give ~2px of vertical whitespace around an icon considering it's vertical alignment)
  ** 4/16 for horizontal padding (to give ~4px of horizontal whitespace around an icon)
*/
.fa-border {
  border-color: var(--fa-border-color, #eee);
  border-radius: var(--fa-border-radius, 0.1em);
  border-style: var(--fa-border-style, solid);
  border-width: var(--fa-border-width, 0.0625em);
  box-sizing: var(--fa-border-box-sizing, content-box);
  padding: var(--fa-border-padding, 0.1875em 0.25em);
}

.fa-pull-left,
.fa-pull-start {
  float: inline-start;
  margin-inline-end: var(--fa-pull-margin, 0.3em);
}

.fa-pull-right,
.fa-pull-end {
  float: inline-end;
  margin-inline-start: var(--fa-pull-margin, 0.3em);
}

.fa-beat {
  animation-name: fa-beat;
  animation-delay: var(--fa-animation-delay, 0s);
  animation-direction: var(--fa-animation-direction, normal);
  animation-duration: var(--fa-animation-duration, 1s);
  animation-iteration-count: var(--fa-animation-iteration-count, infinite);
  animation-timing-function: var(--fa-animation-timing, ease-in-out);
}

.fa-bounce {
  animation-name: fa-bounce;
  animation-delay: var(--fa-animation-delay, 0s);
  animation-direction: var(--fa-animation-direction, normal);
  animation-duration: var(--fa-animation-duration, 1s);
  animation-iteration-count: var(--fa-animation-iteration-count, infinite);
  animation-timing-function: var(--fa-animation-timing, cubic-bezier(0.28, 0.84, 0.42, 1));
}

.fa-fade {
  animation-name: fa-fade;
  animation-delay: var(--fa-animation-delay, 0s);
  animation-direction: var(--fa-animation-direction, normal);
  animation-duration: var(--fa-animation-duration, 1s);
  animation-iteration-count: var(--fa-animation-iteration-count, infinite);
  animation-timing-function: var(--fa-animation-timing, cubic-bezier(0.4, 0, 0.6, 1));
}

.fa-beat-fade {
  animation-name: fa-beat-fade;
  animation-delay: var(--fa-animation-delay, 0s);
  animation-direction: var(--fa-animation-direction, normal);
  animation-duration: var(--fa-animation-duration, 1s);
  animation-iteration-count: var(--fa-animation-iteration-count, infinite);
  animation-timing-function: var(--fa-animation-timing, cubic-bezier(0.4, 0, 0.6, 1));
}

.fa-flip {
  animation-name: fa-flip;
  animation-delay: var(--fa-animation-delay, 0s);
  animation-direction: var(--fa-animation-direction, normal);
  animation-duration: var(--fa-animation-duration, 1s);
  animation-iteration-count: var(--fa-animation-iteration-count, infinite);
  animation-timing-function: var(--fa-animation-timing, ease-in-out);
}

.fa-shake {
  animation-name: fa-shake;
  animation-delay: var(--fa-animation-delay, 0s);
  animation-direction: var(--fa-animation-direction, normal);
  animation-duration: var(--fa-animation-duration, 1s);
  animation-iteration-count: var(--fa-animation-iteration-count, infinite);
  animation-timing-function: var(--fa-animation-timing, linear);
}

.fa-spin {
  animation-name: fa-spin;
  animation-delay: var(--fa-animation-delay, 0s);
  animation-direction: var(--fa-animation-direction, normal);
  animation-duration: var(--fa-animation-duration, 2s);
  animation-iteration-count: var(--fa-animation-iteration-count, infinite);
  animation-timing-function: var(--fa-animation-timing, linear);
}

.fa-spin-reverse {
  --fa-animation-direction: reverse;
}

.fa-pulse,
.fa-spin-pulse {
  animation-name: fa-spin;
  animation-direction: var(--fa-animation-direction, normal);
  animation-duration: var(--fa-animation-duration, 1s);
  animation-iteration-count: var(--fa-animation-iteration-count, infinite);
  animation-timing-function: var(--fa-animation-timing, steps(8));
}

@media (prefers-reduced-motion: reduce) {
  .fa-beat,
  .fa-bounce,
  .fa-fade,
  .fa-beat-fade,
  .fa-flip,
  .fa-pulse,
  .fa-shake,
  .fa-spin,
  .fa-spin-pulse {
    animation: none !important;
    transition: none !important;
  }
}
@keyframes fa-beat {
  0%, 90% {
    transform: scale(1);
  }
  45% {
    transform: scale(var(--fa-beat-scale, 1.25));
  }
}
@keyframes fa-bounce {
  0% {
    transform: scale(1, 1) translateY(0);
  }
  10% {
    transform: scale(var(--fa-bounce-start-scale-x, 1.1), var(--fa-bounce-start-scale-y, 0.9)) translateY(0);
  }
  30% {
    transform: scale(var(--fa-bounce-jump-scale-x, 0.9), var(--fa-bounce-jump-scale-y, 1.1)) translateY(var(--fa-bounce-height, -0.5em));
  }
  50% {
    transform: scale(var(--fa-bounce-land-scale-x, 1.05), var(--fa-bounce-land-scale-y, 0.95)) translateY(0);
  }
  57% {
    transform: scale(1, 1) translateY(var(--fa-bounce-rebound, -0.125em));
  }
  64% {
    transform: scale(1, 1) translateY(0);
  }
  100% {
    transform: scale(1, 1) translateY(0);
  }
}
@keyframes fa-fade {
  50% {
    opacity: var(--fa-fade-opacity, 0.4);
  }
}
@keyframes fa-beat-fade {
  0%, 100% {
    opacity: var(--fa-beat-fade-opacity, 0.4);
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(var(--fa-beat-fade-scale, 1.125));
  }
}
@keyframes fa-flip {
  50% {
    transform: rotate3d(var(--fa-flip-x, 0), var(--fa-flip-y, 1), var(--fa-flip-z, 0), var(--fa-flip-angle, -180deg));
  }
}
@keyframes fa-shake {
  0% {
    transform: rotate(-15deg);
  }
  4% {
    transform: rotate(15deg);
  }
  8%, 24% {
    transform: rotate(-18deg);
  }
  12%, 28% {
    transform: rotate(18deg);
  }
  16% {
    transform: rotate(-22deg);
  }
  20% {
    transform: rotate(22deg);
  }
  32% {
    transform: rotate(-12deg);
  }
  36% {
    transform: rotate(12deg);
  }
  40%, 100% {
    transform: rotate(0deg);
  }
}
@keyframes fa-spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
.fa-rotate-90 {
  transform: rotate(90deg);
}

.fa-rotate-180 {
  transform: rotate(180deg);
}

.fa-rotate-270 {
  transform: rotate(270deg);
}

.fa-flip-horizontal {
  transform: scale(-1, 1);
}

.fa-flip-vertical {
  transform: scale(1, -1);
}

.fa-flip-both,
.fa-flip-horizontal.fa-flip-vertical {
  transform: scale(-1, -1);
}

.fa-rotate-by {
  transform: rotate(var(--fa-rotate-angle, 0));
}

.svg-inline--fa .fa-primary {
  fill: var(--fa-primary-color, currentColor);
  opacity: var(--fa-primary-opacity, 1);
}

.svg-inline--fa .fa-secondary {
  fill: var(--fa-secondary-color, currentColor);
  opacity: var(--fa-secondary-opacity, 0.4);
}

.svg-inline--fa.fa-swap-opacity .fa-primary {
  opacity: var(--fa-secondary-opacity, 0.4);
}

.svg-inline--fa.fa-swap-opacity .fa-secondary {
  opacity: var(--fa-primary-opacity, 1);
}

.svg-inline--fa mask .fa-primary,
.svg-inline--fa mask .fa-secondary {
  fill: black;
}

.svg-inline--fa.fa-inverse {
  fill: var(--fa-inverse, #fff);
}

.fa-stack {
  display: inline-block;
  height: 2em;
  line-height: 2em;
  position: relative;
  vertical-align: middle;
  width: 2.5em;
}

.fa-inverse {
  color: var(--fa-inverse, #fff);
}

.svg-inline--fa.fa-stack-1x {
  --fa-width: 1.25em;
  height: 1em;
  width: var(--fa-width);
}
.svg-inline--fa.fa-stack-2x {
  --fa-width: 2.5em;
  height: 2em;
  width: var(--fa-width);
}

.fa-stack-1x,
.fa-stack-2x {
  inset: 0;
  margin: auto;
  position: absolute;
  z-index: var(--fa-stack-z-index, auto);
}`;function Fn(){var t=xn,n=wn,e=m.cssPrefix,a=m.replacementClass,r=ai;if(e!==t||a!==n){var i=new RegExp("\\.".concat(t,"\\-"),"g"),o=new RegExp("\\--".concat(t,"\\-"),"g"),s=new RegExp("\\.".concat(n),"g");r=r.replace(i,".".concat(e,"-")).replace(o,"--".concat(e,"-")).replace(s,".".concat(a))}return r}var Ce=!1;function kt(){m.autoAddCss&&!Ce&&(Qr(Fn()),Ce=!0)}var ri={mixout:function(){return{dom:{css:Fn,insertCss:kt}}},hooks:function(){return{beforeDOMElementCreation:function(){kt()},beforeI2svg:function(){kt()}}}},T=M||{};T[N]||(T[N]={});T[N].styles||(T[N].styles={});T[N].hooks||(T[N].hooks={});T[N].shims||(T[N].shims=[]);var P=T[N],En=[],On=function(){g.removeEventListener("DOMContentLoaded",On),st=1,En.map(function(n){return n()})},st=!1;j&&(st=(g.documentElement.doScroll?/^loaded|^c/:/^loaded|^i|^c/).test(g.readyState),st||g.addEventListener("DOMContentLoaded",On));function ii(t){j&&(st?setTimeout(t,0):En.push(t))}function Z(t){var n=t.tag,e=t.attributes,a=e===void 0?{}:e,r=t.children,i=r===void 0?[]:r;return typeof t=="string"?Pn(t):"<".concat(n," ").concat(ti(a),">").concat(i.map(Z).join(""),"</").concat(n,">")}function Pe(t,n,e){if(t&&t[n]&&t[n][e])return{prefix:n,iconName:e,icon:t[n][e]}}var oi=function(n,e){return function(a,r,i,o){return n.call(e,a,r,i,o)}},St=function(n,e,a,r){var i=Object.keys(n),o=i.length,s=r!==void 0?oi(e,r):e,l,u,d;for(a===void 0?(l=1,d=n[i[0]]):(l=0,d=a);l<o;l++)u=i[l],d=s(d,n[u],u,n);return d};function Nn(t){return F(t).length!==1?null:t.codePointAt(0).toString(16)}function Fe(t){return Object.keys(t).reduce(function(n,e){var a=t[e],r=!!a.icon;return r?n[a.iconName]=a.icon:n[e]=a,n},{})}function jt(t,n){var e=arguments.length>2&&arguments[2]!==void 0?arguments[2]:{},a=e.skipHooks,r=a===void 0?!1:a,i=Fe(n);typeof P.hooks.addPack=="function"&&!r?P.hooks.addPack(t,Fe(n)):P.styles[t]=f(f({},P.styles[t]||{}),i),t==="fas"&&jt("fa",n)}var J=P.styles,si=P.shims,Tn=Object.keys(Bt),li=Tn.reduce(function(t,n){return t[n]=Object.keys(Bt[n]),t},{}),Xt=null,jn={},Dn={},Mn={},_n={},$n={};function fi(t){return~Xr.indexOf(t)}function ui(t,n){var e=n.split("-"),a=e[0],r=e.slice(1).join("-");return a===t&&r!==""&&!fi(r)?r:null}var zn=function(){var n=function(i){return St(J,function(o,s,l){return o[l]=St(s,i,{}),o},{})};jn=n(function(r,i,o){if(i[3]&&(r[i[3]]=o),i[2]){var s=i[2].filter(function(l){return typeof l=="number"});s.forEach(function(l){r[l.toString(16)]=o})}return r}),Dn=n(function(r,i,o){if(r[o]=o,i[2]){var s=i[2].filter(function(l){return typeof l=="string"});s.forEach(function(l){r[l]=o})}return r}),$n=n(function(r,i,o){var s=i[2];return r[o]=o,s.forEach(function(l){r[l]=o}),r});var e="far"in J||m.autoFetchSvg,a=St(si,function(r,i){var o=i[0],s=i[1],l=i[2];return s==="far"&&!e&&(s="fas"),typeof o=="string"&&(r.names[o]={prefix:s,iconName:l}),typeof o=="number"&&(r.unicodes[o.toString(16)]={prefix:s,iconName:l}),r},{names:{},unicodes:{}});Mn=a.names,_n=a.unicodes,Xt=ct(m.styleDefault,{family:m.familyDefault})};qr(function(t){Xt=ct(t.styleDefault,{family:m.familyDefault})});zn();function Kt(t,n){return(jn[t]||{})[n]}function ci(t,n){return(Dn[t]||{})[n]}function L(t,n){return($n[t]||{})[n]}function Ln(t){return Mn[t]||{prefix:null,iconName:null}}function di(t){var n=_n[t],e=Kt("fas",t);return n||(e?{prefix:"fas",iconName:e}:null)||{prefix:null,iconName:null}}function _(){return Xt}var Rn=function(){return{prefix:null,iconName:null,rest:[]}};function mi(t){var n=w,e=Tn.reduce(function(a,r){return a[r]="".concat(m.cssPrefix,"-").concat(r),a},{});return vn.forEach(function(a){(t.includes(e[a])||t.some(function(r){return li[a].includes(r)}))&&(n=a)}),n}function ct(t){var n=arguments.length>1&&arguments[1]!==void 0?arguments[1]:{},e=n.family,a=e===void 0?w:e,r=Hr[a][t];if(a===q&&!t)return"fad";var i=Se[a][t]||Se[a][r],o=t in P.styles?t:null,s=i||o||null;return s}function hi(t){var n=[],e=null;return t.forEach(function(a){var r=ui(m.cssPrefix,a);r?e=r:a&&n.push(a)}),{iconName:e,rest:n}}function Ee(t){return t.sort().filter(function(n,e,a){return a.indexOf(n)===e})}var Oe=yn.concat(gn);function dt(t){var n=arguments.length>1&&arguments[1]!==void 0?arguments[1]:{},e=n.skipLookups,a=e===void 0?!1:e,r=null,i=Ee(t.filter(function(p){return Oe.includes(p)})),o=Ee(t.filter(function(p){return!Oe.includes(p)})),s=i.filter(function(p){return r=p,!Qe.includes(p)}),l=ft(s,1),u=l[0],d=u===void 0?null:u,c=mi(i),v=f(f({},hi(o)),{},{prefix:ct(d,{family:c})});return f(f(f({},v),yi({values:t,family:c,styles:J,config:m,canonical:v,givenPrefix:r})),pi(a,r,v))}function pi(t,n,e){var a=e.prefix,r=e.iconName;if(t||!a||!r)return{prefix:a,iconName:r};var i=n==="fa"?Ln(r):{},o=L(a,r);return r=i.iconName||o||r,a=i.prefix||a,a==="far"&&!J.far&&J.fas&&!m.autoFetchSvg&&(a="fas"),{prefix:a,iconName:r}}var vi=vn.filter(function(t){return t!==w||t!==q}),gi=Object.keys(Pt).filter(function(t){return t!==w}).map(function(t){return Object.keys(Pt[t])}).flat();function yi(t){var n=t.values,e=t.family,a=t.canonical,r=t.givenPrefix,i=r===void 0?"":r,o=t.styles,s=o===void 0?{}:o,l=t.config,u=l===void 0?{}:l,d=e===q,c=n.includes("fa-duotone")||n.includes("fad"),v=u.familyDefault==="duotone",p=a.prefix==="fad"||a.prefix==="fa-duotone";if(!d&&(c||v||p)&&(a.prefix="fad"),(n.includes("fa-brands")||n.includes("fab"))&&(a.prefix="fab"),!a.prefix&&vi.includes(e)){var b=Object.keys(s).find(function(k){return gi.includes(k)});if(b||u.autoFetchSvg){var y=Ma.get(e).defaultShortPrefixId;a.prefix=y,a.iconName=L(a.prefix,a.iconName)||a.iconName}}return(a.prefix==="fa"||i==="fa")&&(a.prefix=_()||"fas"),a}var bi=(function(){function t(){ra(this,t),this.definitions={}}return ia(t,[{key:"add",value:function(){for(var e=this,a=arguments.length,r=new Array(a),i=0;i<a;i++)r[i]=arguments[i];var o=r.reduce(this._pullDefinitions,{});Object.keys(o).forEach(function(s){e.definitions[s]=f(f({},e.definitions[s]||{}),o[s]),jt(s,o[s]);var l=Bt[w][s];l&&jt(l,o[s]),zn()})}},{key:"reset",value:function(){this.definitions={}}},{key:"_pullDefinitions",value:function(e,a){var r=a.prefix&&a.iconName&&a.icon?{0:a}:a;return Object.keys(r).map(function(i){var o=r[i],s=o.prefix,l=o.iconName,u=o.icon,d=u[2];e[s]||(e[s]={}),d.length>0&&d.forEach(function(c){typeof c=="string"&&(e[s][c]=u)}),e[s][l]=u}),e}}])})(),Ne=[],H={},Y={},xi=Object.keys(Y);function wi(t,n){var e=n.mixoutsTo;return Ne=t,H={},Object.keys(Y).forEach(function(a){xi.indexOf(a)===-1&&delete Y[a]}),Ne.forEach(function(a){var r=a.mixout?a.mixout():{};if(Object.keys(r).forEach(function(o){typeof r[o]=="function"&&(e[o]=r[o]),ot(r[o])==="object"&&Object.keys(r[o]).forEach(function(s){e[o]||(e[o]={}),e[o][s]=r[o][s]})}),a.hooks){var i=a.hooks();Object.keys(i).forEach(function(o){H[o]||(H[o]=[]),H[o].push(i[o])})}a.provides&&a.provides(Y)}),e}function Dt(t,n){for(var e=arguments.length,a=new Array(e>2?e-2:0),r=2;r<e;r++)a[r-2]=arguments[r];var i=H[t]||[];return i.forEach(function(o){n=o.apply(null,[n].concat(a))}),n}function W(t){for(var n=arguments.length,e=new Array(n>1?n-1:0),a=1;a<n;a++)e[a-1]=arguments[a];var r=H[t]||[];r.forEach(function(i){i.apply(null,e)})}function $(){var t=arguments[0],n=Array.prototype.slice.call(arguments,1);return Y[t]?Y[t].apply(null,n):void 0}function Mt(t){t.prefix==="fa"&&(t.prefix="fas");var n=t.iconName,e=t.prefix||_();if(n)return n=L(e,n)||n,Pe(Wn.definitions,e,n)||Pe(P.styles,e,n)}var Wn=new bi,ki=function(){m.autoReplaceSvg=!1,m.observeMutations=!1,W("noAuto")},Si={i2svg:function(){var n=arguments.length>0&&arguments[0]!==void 0?arguments[0]:{};return j?(W("beforeI2svg",n),$("pseudoElements2svg",n),$("i2svg",n)):Promise.reject(new Error("Operation requires a DOM of some kind."))},watch:function(){var n=arguments.length>0&&arguments[0]!==void 0?arguments[0]:{},e=n.autoReplaceSvgRoot;m.autoReplaceSvg===!1&&(m.autoReplaceSvg=!0),m.observeMutations=!0,ii(function(){Ii({autoReplaceSvgRoot:e}),W("watch",n)})}},Ai={icon:function(n){if(n===null)return null;if(ot(n)==="object"&&n.prefix&&n.iconName)return{prefix:n.prefix,iconName:L(n.prefix,n.iconName)||n.iconName};if(Array.isArray(n)&&n.length===2){var e=n[1].indexOf("fa-")===0?n[1].slice(3):n[1],a=ct(n[0]);return{prefix:a,iconName:L(a,e)||e}}if(typeof n=="string"&&(n.indexOf("".concat(m.cssPrefix,"-"))>-1||n.match(Yr))){var r=dt(n.split(" "),{skipLookups:!0});return{prefix:r.prefix||_(),iconName:L(r.prefix,r.iconName)||r.iconName}}if(typeof n=="string"){var i=_();return{prefix:i,iconName:L(i,n)||n}}}},I={noAuto:ki,config:m,dom:Si,parse:Ai,library:Wn,findIconDefinition:Mt,toHtml:Z},Ii=function(){var n=arguments.length>0&&arguments[0]!==void 0?arguments[0]:{},e=n.autoReplaceSvgRoot,a=e===void 0?g:e;(Object.keys(P.styles).length>0||m.autoFetchSvg)&&j&&m.autoReplaceSvg&&I.dom.i2svg({node:a})};function mt(t,n){return Object.defineProperty(t,"abstract",{get:n}),Object.defineProperty(t,"html",{get:function(){return t.abstract.map(function(a){return Z(a)})}}),Object.defineProperty(t,"node",{get:function(){if(j){var a=g.createElement("div");return a.innerHTML=t.html,a.children}}}),t}function Ci(t){var n=t.children,e=t.main,a=t.mask,r=t.attributes,i=t.styles,o=t.transform;if(Gt(o)&&e.found&&!a.found){var s=e.width,l=e.height,u={x:s/l/2,y:.5};r.style=ut(f(f({},i),{},{"transform-origin":"".concat(u.x+o.x/16,"em ").concat(u.y+o.y/16,"em")}))}return[{tag:"svg",attributes:r,children:n}]}function Pi(t){var n=t.prefix,e=t.iconName,a=t.children,r=t.attributes,i=t.symbol,o=i===!0?"".concat(n,"-").concat(m.cssPrefix,"-").concat(e):i;return[{tag:"svg",attributes:{style:"display: none;"},children:[{tag:"symbol",attributes:f(f({},r),{},{id:o}),children:a}]}]}function Fi(t){var n=["aria-label","aria-labelledby","title","role"];return n.some(function(e){return e in t})}function Jt(t){var n=t.icons,e=n.main,a=n.mask,r=t.prefix,i=t.iconName,o=t.transform,s=t.symbol,l=t.maskId,u=t.extra,d=t.watchable,c=d===void 0?!1:d,v=a.found?a:e,p=v.width,b=v.height,y=[m.replacementClass,i?"".concat(m.cssPrefix,"-").concat(i):""].filter(function(O){return u.classes.indexOf(O)===-1}).filter(function(O){return O!==""||!!O}).concat(u.classes).join(" "),k={children:[],attributes:f(f({},u.attributes),{},{"data-prefix":r,"data-icon":i,class:y,role:u.attributes.role||"img",viewBox:"0 0 ".concat(p," ").concat(b)})};!Fi(u.attributes)&&!u.attributes["aria-hidden"]&&(k.attributes["aria-hidden"]="true"),c&&(k.attributes[R]="");var x=f(f({},k),{},{prefix:r,iconName:i,main:e,mask:a,maskId:l,transform:o,symbol:s,styles:f({},u.styles)}),A=a.found&&e.found?$("generateAbstractMask",x)||{children:[],attributes:{}}:$("generateAbstractIcon",x)||{children:[],attributes:{}},C=A.children,U=A.attributes;return x.children=C,x.attributes=U,s?Pi(x):Ci(x)}function Te(t){var n=t.content,e=t.width,a=t.height,r=t.transform,i=t.extra,o=t.watchable,s=o===void 0?!1:o,l=f(f({},i.attributes),{},{class:i.classes.join(" ")});s&&(l[R]="");var u=f({},i.styles);Gt(r)&&(u.transform=ni({transform:r,startCentered:!0,width:e,height:a}),u["-webkit-transform"]=u.transform);var d=ut(u);d.length>0&&(l.style=d);var c=[];return c.push({tag:"span",attributes:l,children:[n]}),c}function Ei(t){var n=t.content,e=t.extra,a=f(f({},e.attributes),{},{class:e.classes.join(" ")}),r=ut(e.styles);r.length>0&&(a.style=r);var i=[];return i.push({tag:"span",attributes:a,children:[n]}),i}var At=P.styles;function _t(t){var n=t[0],e=t[1],a=t.slice(4),r=ft(a,1),i=r[0],o=null;return Array.isArray(i)?o={tag:"g",attributes:{class:"".concat(m.cssPrefix,"-").concat(wt.GROUP)},children:[{tag:"path",attributes:{class:"".concat(m.cssPrefix,"-").concat(wt.SECONDARY),fill:"currentColor",d:i[0]}},{tag:"path",attributes:{class:"".concat(m.cssPrefix,"-").concat(wt.PRIMARY),fill:"currentColor",d:i[1]}}]}:o={tag:"path",attributes:{fill:"currentColor",d:i}},{found:!0,width:n,height:e,icon:o}}var Oi={found:!1,width:512,height:512};function Ni(t,n){!Sn&&!m.showMissingIcons&&t&&console.error('Icon with name "'.concat(t,'" and prefix "').concat(n,'" is missing.'))}function $t(t,n){var e=n;return n==="fa"&&m.styleDefault!==null&&(n=_()),new Promise(function(a,r){if(e==="fa"){var i=Ln(t)||{};t=i.iconName||t,n=i.prefix||n}if(t&&n&&At[n]&&At[n][t]){var o=At[n][t];return a(_t(o))}Ni(t,n),a(f(f({},Oi),{},{icon:m.showMissingIcons&&t?$("missingIconAbstract")||{}:{}}))})}var je=function(){},zt=m.measurePerformance&&et&&et.mark&&et.measure?et:{mark:je,measure:je},G='FA "7.1.0"',Ti=function(n){return zt.mark("".concat(G," ").concat(n," begins")),function(){return Un(n)}},Un=function(n){zt.mark("".concat(G," ").concat(n," ends")),zt.measure("".concat(G," ").concat(n),"".concat(G," ").concat(n," begins"),"".concat(G," ").concat(n," ends"))},qt={begin:Ti,end:Un},rt=function(){};function De(t){var n=t.getAttribute?t.getAttribute(R):null;return typeof n=="string"}function ji(t){var n=t.getAttribute?t.getAttribute(Ht):null,e=t.getAttribute?t.getAttribute(Yt):null;return n&&e}function Di(t){return t&&t.classList&&t.classList.contains&&t.classList.contains(m.replacementClass)}function Mi(){if(m.autoReplaceSvg===!0)return it.replace;var t=it[m.autoReplaceSvg];return t||it.replace}function _i(t){return g.createElementNS("http://www.w3.org/2000/svg",t)}function $i(t){return g.createElement(t)}function Hn(t){var n=arguments.length>1&&arguments[1]!==void 0?arguments[1]:{},e=n.ceFn,a=e===void 0?t.tag==="svg"?_i:$i:e;if(typeof t=="string")return g.createTextNode(t);var r=a(t.tag);Object.keys(t.attributes||[]).forEach(function(o){r.setAttribute(o,t.attributes[o])});var i=t.children||[];return i.forEach(function(o){r.appendChild(Hn(o,{ceFn:a}))}),r}function zi(t){var n=" ".concat(t.outerHTML," ");return n="".concat(n,"Font Awesome fontawesome.com "),n}var it={replace:function(n){var e=n[0];if(e.parentNode)if(n[1].forEach(function(r){e.parentNode.insertBefore(Hn(r),e)}),e.getAttribute(R)===null&&m.keepOriginalSource){var a=g.createComment(zi(e));e.parentNode.replaceChild(a,e)}else e.remove()},nest:function(n){var e=n[0],a=n[1];if(~Vt(e).indexOf(m.replacementClass))return it.replace(n);var r=new RegExp("".concat(m.cssPrefix,"-.*"));if(delete a[0].attributes.id,a[0].attributes.class){var i=a[0].attributes.class.split(" ").reduce(function(s,l){return l===m.replacementClass||l.match(r)?s.toSvg.push(l):s.toNode.push(l),s},{toNode:[],toSvg:[]});a[0].attributes.class=i.toSvg.join(" "),i.toNode.length===0?e.removeAttribute("class"):e.setAttribute("class",i.toNode.join(" "))}var o=a.map(function(s){return Z(s)}).join(`
`);e.setAttribute(R,""),e.innerHTML=o}};function Me(t){t()}function Yn(t,n){var e=typeof n=="function"?n:rt;if(t.length===0)e();else{var a=Me;m.mutateApproach===Wr&&(a=M.requestAnimationFrame||Me),a(function(){var r=Mi(),i=qt.begin("mutate");t.map(r),i(),e()})}}var Qt=!1;function Bn(){Qt=!0}function Lt(){Qt=!1}var lt=null;function _e(t){if(be&&m.observeMutations){var n=t.treeCallback,e=n===void 0?rt:n,a=t.nodeCallback,r=a===void 0?rt:a,i=t.pseudoElementsCallback,o=i===void 0?rt:i,s=t.observeMutationsRoot,l=s===void 0?g:s;lt=new be(function(u){if(!Qt){var d=_();V(u).forEach(function(c){if(c.type==="childList"&&c.addedNodes.length>0&&!De(c.addedNodes[0])&&(m.searchPseudoElements&&o(c.target),e(c.target)),c.type==="attributes"&&c.target.parentNode&&m.searchPseudoElements&&o([c.target],!0),c.type==="attributes"&&De(c.target)&&~Gr.indexOf(c.attributeName))if(c.attributeName==="class"&&ji(c.target)){var v=dt(Vt(c.target)),p=v.prefix,b=v.iconName;c.target.setAttribute(Ht,p||d),b&&c.target.setAttribute(Yt,b)}else Di(c.target)&&r(c.target)})}}),j&&lt.observe(l,{childList:!0,attributes:!0,characterData:!0,subtree:!0})}}function Li(){lt&&lt.disconnect()}function Ri(t){var n=t.getAttribute("style"),e=[];return n&&(e=n.split(";").reduce(function(a,r){var i=r.split(":"),o=i[0],s=i.slice(1);return o&&s.length>0&&(a[o]=s.join(":").trim()),a},{})),e}function Wi(t){var n=t.getAttribute("data-prefix"),e=t.getAttribute("data-icon"),a=t.innerText!==void 0?t.innerText.trim():"",r=dt(Vt(t));return r.prefix||(r.prefix=_()),n&&e&&(r.prefix=n,r.iconName=e),r.iconName&&r.prefix||(r.prefix&&a.length>0&&(r.iconName=ci(r.prefix,t.innerText)||Kt(r.prefix,Nn(t.innerText))),!r.iconName&&m.autoFetchSvg&&t.firstChild&&t.firstChild.nodeType===Node.TEXT_NODE&&(r.iconName=t.firstChild.data)),r}function Ui(t){var n=V(t.attributes).reduce(function(e,a){return e.name!=="class"&&e.name!=="style"&&(e[a.name]=a.value),e},{});return n}function Hi(){return{iconName:null,prefix:null,transform:E,symbol:!1,mask:{iconName:null,prefix:null,rest:[]},maskId:null,extra:{classes:[],styles:{},attributes:{}}}}function $e(t){var n=arguments.length>1&&arguments[1]!==void 0?arguments[1]:{styleParser:!0},e=Wi(t),a=e.iconName,r=e.prefix,i=e.rest,o=Ui(t),s=Dt("parseNodeAttributes",{},t),l=n.styleParser?Ri(t):[];return f({iconName:a,prefix:r,transform:E,mask:{iconName:null,prefix:null,rest:[]},maskId:null,symbol:!1,extra:{classes:i,styles:l,attributes:o}},s)}var Yi=P.styles;function Vn(t){var n=m.autoReplaceSvg==="nest"?$e(t,{styleParser:!1}):$e(t);return~n.extra.classes.indexOf(In)?$("generateLayersText",t,n):$("generateSvgReplacementMutation",t,n)}function Bi(){return[].concat(F(gn),F(yn))}function ze(t){var n=arguments.length>1&&arguments[1]!==void 0?arguments[1]:null;if(!j)return Promise.resolve();var e=g.documentElement.classList,a=function(c){return e.add("".concat(ke,"-").concat(c))},r=function(c){return e.remove("".concat(ke,"-").concat(c))},i=m.autoFetchSvg?Bi():Qe.concat(Object.keys(Yi));i.includes("fa")||i.push("fa");var o=[".".concat(In,":not([").concat(R,"])")].concat(i.map(function(d){return".".concat(d,":not([").concat(R,"])")})).join(", ");if(o.length===0)return Promise.resolve();var s=[];try{s=V(t.querySelectorAll(o))}catch{}if(s.length>0)a("pending"),r("complete");else return Promise.resolve();var l=qt.begin("onTree"),u=s.reduce(function(d,c){try{var v=Vn(c);v&&d.push(v)}catch(p){Sn||p.name==="MissingIcon"&&console.error(p)}return d},[]);return new Promise(function(d,c){Promise.all(u).then(function(v){Yn(v,function(){a("active"),a("complete"),r("pending"),typeof n=="function"&&n(),l(),d()})}).catch(function(v){l(),c(v)})})}function Vi(t){var n=arguments.length>1&&arguments[1]!==void 0?arguments[1]:null;Vn(t).then(function(e){e&&Yn([e],n)})}function Gi(t){return function(n){var e=arguments.length>1&&arguments[1]!==void 0?arguments[1]:{},a=(n||{}).icon?n:Mt(n||{}),r=e.mask;return r&&(r=(r||{}).icon?r:Mt(r||{})),t(a,f(f({},e),{},{mask:r}))}}var Xi=function(n){var e=arguments.length>1&&arguments[1]!==void 0?arguments[1]:{},a=e.transform,r=a===void 0?E:a,i=e.symbol,o=i===void 0?!1:i,s=e.mask,l=s===void 0?null:s,u=e.maskId,d=u===void 0?null:u,c=e.classes,v=c===void 0?[]:c,p=e.attributes,b=p===void 0?{}:p,y=e.styles,k=y===void 0?{}:y;if(n){var x=n.prefix,A=n.iconName,C=n.icon;return mt(f({type:"icon"},n),function(){return W("beforeDOMElementCreation",{iconDefinition:n,params:e}),Jt({icons:{main:_t(C),mask:l?_t(l.icon):{found:!1,width:null,height:null,icon:{}}},prefix:x,iconName:A,transform:f(f({},E),r),symbol:o,maskId:d,extra:{attributes:b,styles:k,classes:v}})})}},Ki={mixout:function(){return{icon:Gi(Xi)}},hooks:function(){return{mutationObserverCallbacks:function(e){return e.treeCallback=ze,e.nodeCallback=Vi,e}}},provides:function(n){n.i2svg=function(e){var a=e.node,r=a===void 0?g:a,i=e.callback,o=i===void 0?function(){}:i;return ze(r,o)},n.generateSvgReplacementMutation=function(e,a){var r=a.iconName,i=a.prefix,o=a.transform,s=a.symbol,l=a.mask,u=a.maskId,d=a.extra;return new Promise(function(c,v){Promise.all([$t(r,i),l.iconName?$t(l.iconName,l.prefix):Promise.resolve({found:!1,width:512,height:512,icon:{}})]).then(function(p){var b=ft(p,2),y=b[0],k=b[1];c([e,Jt({icons:{main:y,mask:k},prefix:i,iconName:r,transform:o,symbol:s,maskId:u,extra:d,watchable:!0})])}).catch(v)})},n.generateAbstractIcon=function(e){var a=e.children,r=e.attributes,i=e.main,o=e.transform,s=e.styles,l=ut(s);l.length>0&&(r.style=l);var u;return Gt(o)&&(u=$("generateAbstractTransformGrouping",{main:i,transform:o,containerWidth:i.width,iconWidth:i.width})),a.push(u||i.icon),{children:a,attributes:r}}}},Ji={mixout:function(){return{layer:function(e){var a=arguments.length>1&&arguments[1]!==void 0?arguments[1]:{},r=a.classes,i=r===void 0?[]:r;return mt({type:"layer"},function(){W("beforeDOMElementCreation",{assembler:e,params:a});var o=[];return e(function(s){Array.isArray(s)?s.map(function(l){o=o.concat(l.abstract)}):o=o.concat(s.abstract)}),[{tag:"span",attributes:{class:["".concat(m.cssPrefix,"-layers")].concat(F(i)).join(" ")},children:o}]})}}}},qi={mixout:function(){return{counter:function(e){var a=arguments.length>1&&arguments[1]!==void 0?arguments[1]:{},r=a.title,i=r===void 0?null:r,o=a.classes,s=o===void 0?[]:o,l=a.attributes,u=l===void 0?{}:l,d=a.styles,c=d===void 0?{}:d;return mt({type:"counter",content:e},function(){return W("beforeDOMElementCreation",{content:e,params:a}),Ei({content:e.toString(),title:i,extra:{attributes:u,styles:c,classes:["".concat(m.cssPrefix,"-layers-counter")].concat(F(s))}})})}}}},Qi={mixout:function(){return{text:function(e){var a=arguments.length>1&&arguments[1]!==void 0?arguments[1]:{},r=a.transform,i=r===void 0?E:r,o=a.classes,s=o===void 0?[]:o,l=a.attributes,u=l===void 0?{}:l,d=a.styles,c=d===void 0?{}:d;return mt({type:"text",content:e},function(){return W("beforeDOMElementCreation",{content:e,params:a}),Te({content:e,transform:f(f({},E),i),extra:{attributes:u,styles:c,classes:["".concat(m.cssPrefix,"-layers-text")].concat(F(s))}})})}}},provides:function(n){n.generateLayersText=function(e,a){var r=a.transform,i=a.extra,o=null,s=null;if(Je){var l=parseInt(getComputedStyle(e).fontSize,10),u=e.getBoundingClientRect();o=u.width/l,s=u.height/l}return Promise.resolve([e,Te({content:e.innerHTML,width:o,height:s,transform:r,extra:i,watchable:!0})])}}},Gn=new RegExp('"',"ug"),Le=[1105920,1112319],Re=f(f(f(f({},{FontAwesome:{normal:"fas",400:"fas"}}),Da),Lr),Ha),Rt=Object.keys(Re).reduce(function(t,n){return t[n.toLowerCase()]=Re[n],t},{}),Zi=Object.keys(Rt).reduce(function(t,n){var e=Rt[n];return t[n]=e[900]||F(Object.entries(e))[0][1],t},{});function to(t){var n=t.replace(Gn,"");return Nn(F(n)[0]||"")}function eo(t){var n=t.getPropertyValue("font-feature-settings").includes("ss01"),e=t.getPropertyValue("content"),a=e.replace(Gn,""),r=a.codePointAt(0),i=r>=Le[0]&&r<=Le[1],o=a.length===2?a[0]===a[1]:!1;return i||o||n}function no(t,n){var e=t.replace(/^['"]|['"]$/g,"").toLowerCase(),a=parseInt(n),r=isNaN(a)?"normal":a;return(Rt[e]||{})[r]||Zi[e]}function We(t,n){var e="".concat(Rr).concat(n.replace(":","-"));return new Promise(function(a,r){if(t.getAttribute(e)!==null)return a();var i=V(t.children),o=i.filter(function(ht){return ht.getAttribute(Et)===n})[0],s=M.getComputedStyle(t,n),l=s.getPropertyValue("font-family"),u=l.match(Br),d=s.getPropertyValue("font-weight"),c=s.getPropertyValue("content");if(o&&!u)return t.removeChild(o),a();if(u&&c!=="none"&&c!==""){var v=s.getPropertyValue("content"),p=no(l,d),b=to(v),y=u[0].startsWith("FontAwesome"),k=eo(s),x=Kt(p,b),A=x;if(y){var C=di(b);C.iconName&&C.prefix&&(x=C.iconName,p=C.prefix)}if(x&&!k&&(!o||o.getAttribute(Ht)!==p||o.getAttribute(Yt)!==A)){t.setAttribute(e,A),o&&t.removeChild(o);var U=Hi(),O=U.extra;O.attributes[Et]=n,$t(x,p).then(function(ht){var ta=Jt(f(f({},U),{},{icons:{main:ht,mask:Rn()},prefix:p,iconName:A,extra:O,watchable:!0})),pt=g.createElementNS("http://www.w3.org/2000/svg","svg");n==="::before"?t.insertBefore(pt,t.firstChild):t.appendChild(pt),pt.outerHTML=ta.map(function(ea){return Z(ea)}).join(`
`),t.removeAttribute(e),a()}).catch(r)}else a()}else a()})}function ao(t){return Promise.all([We(t,"::before"),We(t,"::after")])}function ro(t){return t.parentNode!==document.head&&!~Ur.indexOf(t.tagName.toUpperCase())&&!t.getAttribute(Et)&&(!t.parentNode||t.parentNode.tagName!=="svg")}var io=function(n){return!!n&&kn.some(function(e){return n.includes(e)})},oo=function(n){if(!n)return[];var e=new Set,a=n.split(/,(?![^()]*\))/).map(function(l){return l.trim()});a=a.flatMap(function(l){return l.includes("(")?l:l.split(",").map(function(u){return u.trim()})});var r=at(a),i;try{for(r.s();!(i=r.n()).done;){var o=i.value;if(io(o)){var s=kn.reduce(function(l,u){return l.replace(u,"")},o);s!==""&&s!=="*"&&e.add(s)}}}catch(l){r.e(l)}finally{r.f()}return e};function Ue(t){var n=arguments.length>1&&arguments[1]!==void 0?arguments[1]:!1;if(j){var e;if(n)e=t;else if(m.searchPseudoElementsFullScan)e=t.querySelectorAll("*");else{var a=new Set,r=at(document.styleSheets),i;try{for(r.s();!(i=r.n()).done;){var o=i.value;try{var s=at(o.cssRules),l;try{for(s.s();!(l=s.n()).done;){var u=l.value,d=oo(u.selectorText),c=at(d),v;try{for(c.s();!(v=c.n()).done;){var p=v.value;a.add(p)}}catch(y){c.e(y)}finally{c.f()}}}catch(y){s.e(y)}finally{s.f()}}catch(y){m.searchPseudoElementsWarnings&&console.warn("Font Awesome: cannot parse stylesheet: ".concat(o.href," (").concat(y.message,`)
If it declares any Font Awesome CSS pseudo-elements, they will not be rendered as SVG icons. Add crossorigin="anonymous" to the <link>, enable searchPseudoElementsFullScan for slower but more thorough DOM parsing, or suppress this warning by setting searchPseudoElementsWarnings to false.`))}}}catch(y){r.e(y)}finally{r.f()}if(!a.size)return;var b=Array.from(a).join(", ");try{e=t.querySelectorAll(b)}catch{}}return new Promise(function(y,k){var x=V(e).filter(ro).map(ao),A=qt.begin("searchPseudoElements");Bn(),Promise.all(x).then(function(){A(),Lt(),y()}).catch(function(){A(),Lt(),k()})})}}var so={hooks:function(){return{mutationObserverCallbacks:function(e){return e.pseudoElementsCallback=Ue,e}}},provides:function(n){n.pseudoElements2svg=function(e){var a=e.node,r=a===void 0?g:a;m.searchPseudoElements&&Ue(r)}}},He=!1,lo={mixout:function(){return{dom:{unwatch:function(){Bn(),He=!0}}}},hooks:function(){return{bootstrap:function(){_e(Dt("mutationObserverCallbacks",{}))},noAuto:function(){Li()},watch:function(e){var a=e.observeMutationsRoot;He?Lt():_e(Dt("mutationObserverCallbacks",{observeMutationsRoot:a}))}}}},Ye=function(n){var e={size:16,x:0,y:0,flipX:!1,flipY:!1,rotate:0};return n.toLowerCase().split(" ").reduce(function(a,r){var i=r.toLowerCase().split("-"),o=i[0],s=i.slice(1).join("-");if(o&&s==="h")return a.flipX=!0,a;if(o&&s==="v")return a.flipY=!0,a;if(s=parseFloat(s),isNaN(s))return a;switch(o){case"grow":a.size=a.size+s;break;case"shrink":a.size=a.size-s;break;case"left":a.x=a.x-s;break;case"right":a.x=a.x+s;break;case"up":a.y=a.y-s;break;case"down":a.y=a.y+s;break;case"rotate":a.rotate=a.rotate+s;break}return a},e)},fo={mixout:function(){return{parse:{transform:function(e){return Ye(e)}}}},hooks:function(){return{parseNodeAttributes:function(e,a){var r=a.getAttribute("data-fa-transform");return r&&(e.transform=Ye(r)),e}}},provides:function(n){n.generateAbstractTransformGrouping=function(e){var a=e.main,r=e.transform,i=e.containerWidth,o=e.iconWidth,s={transform:"translate(".concat(i/2," 256)")},l="translate(".concat(r.x*32,", ").concat(r.y*32,") "),u="scale(".concat(r.size/16*(r.flipX?-1:1),", ").concat(r.size/16*(r.flipY?-1:1),") "),d="rotate(".concat(r.rotate," 0 0)"),c={transform:"".concat(l," ").concat(u," ").concat(d)},v={transform:"translate(".concat(o/2*-1," -256)")},p={outer:s,inner:c,path:v};return{tag:"g",attributes:f({},p.outer),children:[{tag:"g",attributes:f({},p.inner),children:[{tag:a.icon.tag,children:a.icon.children,attributes:f(f({},a.icon.attributes),p.path)}]}]}}}},It={x:0,y:0,width:"100%",height:"100%"};function Be(t){var n=arguments.length>1&&arguments[1]!==void 0?arguments[1]:!0;return t.attributes&&(t.attributes.fill||n)&&(t.attributes.fill="black"),t}function uo(t){return t.tag==="g"?t.children:[t]}var co={hooks:function(){return{parseNodeAttributes:function(e,a){var r=a.getAttribute("data-fa-mask"),i=r?dt(r.split(" ").map(function(o){return o.trim()})):Rn();return i.prefix||(i.prefix=_()),e.mask=i,e.maskId=a.getAttribute("data-fa-mask-id"),e}}},provides:function(n){n.generateAbstractMask=function(e){var a=e.children,r=e.attributes,i=e.main,o=e.mask,s=e.maskId,l=e.transform,u=i.width,d=i.icon,c=o.width,v=o.icon,p=ei({transform:l,containerWidth:c,iconWidth:u}),b={tag:"rect",attributes:f(f({},It),{},{fill:"white"})},y=d.children?{children:d.children.map(Be)}:{},k={tag:"g",attributes:f({},p.inner),children:[Be(f({tag:d.tag,attributes:f(f({},d.attributes),p.path)},y))]},x={tag:"g",attributes:f({},p.outer),children:[k]},A="mask-".concat(s||Ie()),C="clip-".concat(s||Ie()),U={tag:"mask",attributes:f(f({},It),{},{id:A,maskUnits:"userSpaceOnUse",maskContentUnits:"userSpaceOnUse"}),children:[b,x]},O={tag:"defs",children:[{tag:"clipPath",attributes:{id:C},children:uo(v)},U]};return a.push(O,{tag:"rect",attributes:f({fill:"currentColor","clip-path":"url(#".concat(C,")"),mask:"url(#".concat(A,")")},It)}),{children:a,attributes:r}}}},mo={provides:function(n){var e=!1;M.matchMedia&&(e=M.matchMedia("(prefers-reduced-motion: reduce)").matches),n.missingIconAbstract=function(){var a=[],r={fill:"currentColor"},i={attributeType:"XML",repeatCount:"indefinite",dur:"2s"};a.push({tag:"path",attributes:f(f({},r),{},{d:"M156.5,447.7l-12.6,29.5c-18.7-9.5-35.9-21.2-51.5-34.9l22.7-22.7C127.6,430.5,141.5,440,156.5,447.7z M40.6,272H8.5 c1.4,21.2,5.4,41.7,11.7,61.1L50,321.2C45.1,305.5,41.8,289,40.6,272z M40.6,240c1.4-18.8,5.2-37,11.1-54.1l-29.5-12.6 C14.7,194.3,10,216.7,8.5,240H40.6z M64.3,156.5c7.8-14.9,17.2-28.8,28.1-41.5L69.7,92.3c-13.7,15.6-25.5,32.8-34.9,51.5 L64.3,156.5z M397,419.6c-13.9,12-29.4,22.3-46.1,30.4l11.9,29.8c20.7-9.9,39.8-22.6,56.9-37.6L397,419.6z M115,92.4 c13.9-12,29.4-22.3,46.1-30.4l-11.9-29.8c-20.7,9.9-39.8,22.6-56.8,37.6L115,92.4z M447.7,355.5c-7.8,14.9-17.2,28.8-28.1,41.5 l22.7,22.7c13.7-15.6,25.5-32.9,34.9-51.5L447.7,355.5z M471.4,272c-1.4,18.8-5.2,37-11.1,54.1l29.5,12.6 c7.5-21.1,12.2-43.5,13.6-66.8H471.4z M321.2,462c-15.7,5-32.2,8.2-49.2,9.4v32.1c21.2-1.4,41.7-5.4,61.1-11.7L321.2,462z M240,471.4c-18.8-1.4-37-5.2-54.1-11.1l-12.6,29.5c21.1,7.5,43.5,12.2,66.8,13.6V471.4z M462,190.8c5,15.7,8.2,32.2,9.4,49.2h32.1 c-1.4-21.2-5.4-41.7-11.7-61.1L462,190.8z M92.4,397c-12-13.9-22.3-29.4-30.4-46.1l-29.8,11.9c9.9,20.7,22.6,39.8,37.6,56.9 L92.4,397z M272,40.6c18.8,1.4,36.9,5.2,54.1,11.1l12.6-29.5C317.7,14.7,295.3,10,272,8.5V40.6z M190.8,50 c15.7-5,32.2-8.2,49.2-9.4V8.5c-21.2,1.4-41.7,5.4-61.1,11.7L190.8,50z M442.3,92.3L419.6,115c12,13.9,22.3,29.4,30.5,46.1 l29.8-11.9C470,128.5,457.3,109.4,442.3,92.3z M397,92.4l22.7-22.7c-15.6-13.7-32.8-25.5-51.5-34.9l-12.6,29.5 C370.4,72.1,384.4,81.5,397,92.4z"})});var o=f(f({},i),{},{attributeName:"opacity"}),s={tag:"circle",attributes:f(f({},r),{},{cx:"256",cy:"364",r:"28"}),children:[]};return e||s.children.push({tag:"animate",attributes:f(f({},i),{},{attributeName:"r",values:"28;14;28;28;14;28;"})},{tag:"animate",attributes:f(f({},o),{},{values:"1;0;1;1;0;1;"})}),a.push(s),a.push({tag:"path",attributes:f(f({},r),{},{opacity:"1",d:"M263.7,312h-16c-6.6,0-12-5.4-12-12c0-71,77.4-63.9,77.4-107.8c0-20-17.8-40.2-57.4-40.2c-29.1,0-44.3,9.6-59.2,28.7 c-3.9,5-11.1,6-16.2,2.4l-13.1-9.2c-5.6-3.9-6.9-11.8-2.6-17.2c21.2-27.2,46.4-44.7,91.2-44.7c52.3,0,97.4,29.8,97.4,80.2 c0,67.6-77.4,63.5-77.4,107.8C275.7,306.6,270.3,312,263.7,312z"}),children:e?[]:[{tag:"animate",attributes:f(f({},o),{},{values:"1;0;0;0;0;1;"})}]}),e||a.push({tag:"path",attributes:f(f({},r),{},{opacity:"0",d:"M232.5,134.5l7,168c0.3,6.4,5.6,11.5,12,11.5h9c6.4,0,11.7-5.1,12-11.5l7-168c0.3-6.8-5.2-12.5-12-12.5h-23 C237.7,122,232.2,127.7,232.5,134.5z"}),children:[{tag:"animate",attributes:f(f({},o),{},{values:"0;0;1;1;0;0;"})}]}),{tag:"g",attributes:{class:"missing"},children:a}}}},ho={hooks:function(){return{parseNodeAttributes:function(e,a){var r=a.getAttribute("data-fa-symbol"),i=r===null?!1:r===""?!0:r;return e.symbol=i,e}}}},po=[ri,Ki,Ji,qi,Qi,so,lo,fo,co,mo,ho];wi(po,{mixoutsTo:I});var _o=I.noAuto,Xn=I.config,$o=I.library,Kn=I.dom,Jn=I.parse,zo=I.findIconDefinition,Lo=I.toHtml,qn=I.icon,Ro=I.layer,vo=I.text,go=I.counter;var yo=["*"],bo=(()=>{class t{defaultPrefix="fas";fallbackIcon=null;fixedWidth;set autoAddCss(e){Xn.autoAddCss=e,this._autoAddCss=e}get autoAddCss(){return this._autoAddCss}_autoAddCss=!0;static \u0275fac=function(a){return new(a||t)};static \u0275prov=vt({token:t,factory:t.\u0275fac,providedIn:"root"})}return t})(),xo=(()=>{class t{definitions={};addIcons(...e){for(let a of e){a.prefix in this.definitions||(this.definitions[a.prefix]={}),this.definitions[a.prefix][a.iconName]=a;for(let r of a.icon[2])typeof r=="string"&&(this.definitions[a.prefix][r]=a)}}addIconPacks(...e){for(let a of e){let r=Object.keys(a).map(i=>a[i]);this.addIcons(...r)}}getIconDefinition(e,a){return e in this.definitions&&a in this.definitions[e]?this.definitions[e][a]:null}static \u0275fac=function(a){return new(a||t)};static \u0275prov=vt({token:t,factory:t.\u0275fac,providedIn:"root"})}return t})(),wo=t=>{throw new Error(`Could not find icon with iconName=${t.iconName} and prefix=${t.prefix} in the icon library.`)},ko=()=>{throw new Error("Property `icon` is required for `fa-icon`/`fa-duotone-icon` components.")},Zn=t=>t!=null&&(t===90||t===180||t===270||t==="90"||t==="180"||t==="270"),So=t=>{let n=Zn(t.rotate),e={[`fa-${t.animation}`]:t.animation!=null&&!t.animation.startsWith("spin"),"fa-spin":t.animation==="spin"||t.animation==="spin-reverse","fa-spin-pulse":t.animation==="spin-pulse"||t.animation==="spin-pulse-reverse","fa-spin-reverse":t.animation==="spin-reverse"||t.animation==="spin-pulse-reverse","fa-pulse":t.animation==="spin-pulse"||t.animation==="spin-pulse-reverse","fa-fw":t.fixedWidth,"fa-border":t.border,"fa-inverse":t.inverse,"fa-layers-counter":t.counter,"fa-flip-horizontal":t.flip==="horizontal"||t.flip==="both","fa-flip-vertical":t.flip==="vertical"||t.flip==="both",[`fa-${t.size}`]:t.size!==null,[`fa-rotate-${t.rotate}`]:n,"fa-rotate-by":t.rotate!=null&&!n,[`fa-pull-${t.pull}`]:t.pull!==null,[`fa-stack-${t.stackItemSize}`]:t.stackItemSize!=null};return Object.keys(e).map(a=>e[a]?a:null).filter(a=>a!=null)},Zt=new WeakSet,Qn="fa-auto-css";function Ao(t,n){if(!n.autoAddCss||Zt.has(t))return;if(t.getElementById(Qn)!=null){n.autoAddCss=!1,Zt.add(t);return}let e=t.createElement("style");e.setAttribute("type","text/css"),e.setAttribute("id",Qn),e.innerHTML=Kn.css();let a=t.head.childNodes,r=null;for(let i=a.length-1;i>-1;i--){let o=a[i],s=o.nodeName.toUpperCase();["STYLE","LINK"].indexOf(s)>-1&&(r=o)}t.head.insertBefore(e,r),n.autoAddCss=!1,Zt.add(t)}var Io=t=>t.prefix!==void 0&&t.iconName!==void 0,Co=(t,n)=>Io(t)?t:Array.isArray(t)&&t.length===2?{prefix:t[0],iconName:t[1]}:{prefix:n,iconName:t},Po=(()=>{class t{stackItemSize=tt("1x");size=tt();_effect=de(()=>{if(this.size())throw new Error('fa-icon is not allowed to customize size when used inside fa-stack. Set size on the enclosing fa-stack instead: <fa-stack size="4x">...</fa-stack>.')});static \u0275fac=function(a){return new(a||t)};static \u0275dir=oe({type:t,selectors:[["fa-icon","stackItemSize",""],["fa-duotone-icon","stackItemSize",""]],inputs:{stackItemSize:[1,"stackItemSize"],size:[1,"size"]}})}return t})(),Fo=(()=>{class t{size=tt();classes=yt(()=>{let e=this.size(),a=e?{[`fa-${e}`]:!0}:{};return ee(te({},a),{"fa-stack":!0})});static \u0275fac=function(a){return new(a||t)};static \u0275cmp=gt({type:t,selectors:[["fa-stack"]],hostVars:2,hostBindings:function(a,r){a&2&&ce(r.classes())},inputs:{size:[1,"size"]},ngContentSelectors:yo,decls:1,vars:0,template:function(a,r){a&1&&(fe(),ue(0))},encapsulation:2,changeDetection:0})}return t})(),Zo=(()=>{class t{icon=S();title=S();animation=S();mask=S();flip=S();size=S();pull=S();border=S();inverse=S();symbol=S();rotate=S();fixedWidth=S();transform=S();a11yRole=S();renderedIconHTML=yt(()=>{let e=this.icon()??this.config.fallbackIcon;if(!e)return ko(),"";let a=this.findIconDefinition(e);if(!a)return"";let r=this.buildParams();Ao(this.document,this.config);let i=qn(a,r);return this.sanitizer.bypassSecurityTrustHtml(i.html.join(`
`))});document=z(ae);sanitizer=z(me);config=z(bo);iconLibrary=z(xo);stackItem=z(Po,{optional:!0});stack=z(Fo,{optional:!0});constructor(){this.stack!=null&&this.stackItem==null&&console.error('FontAwesome: fa-icon and fa-duotone-icon elements must specify stackItemSize attribute when wrapped into fa-stack. Example: <fa-icon stackItemSize="2x" />.')}findIconDefinition(e){let a=Co(e,this.config.defaultPrefix);if("icon"in a)return a;let r=this.iconLibrary.getIconDefinition(a.prefix,a.iconName);return r??(wo(a),null)}buildParams(){let e=this.fixedWidth(),a={flip:this.flip(),animation:this.animation(),border:this.border(),inverse:this.inverse(),size:this.size(),pull:this.pull(),rotate:this.rotate(),fixedWidth:typeof e=="boolean"?e:this.config.fixedWidth,stackItemSize:this.stackItem!=null?this.stackItem.stackItemSize():void 0},r=this.transform(),i=typeof r=="string"?Jn.transform(r):r,o=this.mask(),s=o!=null?this.findIconDefinition(o):null,l={},u=this.a11yRole();u!=null&&(l.role=u);let d={};return a.rotate!=null&&!Zn(a.rotate)&&(d["--fa-rotate-angle"]=`${a.rotate}`),{title:this.title(),transform:i,classes:So(a),mask:s??void 0,symbol:this.symbol(),attributes:l,styles:d}}static \u0275fac=function(a){return new(a||t)};static \u0275cmp=gt({type:t,selectors:[["fa-icon"]],hostAttrs:[1,"ng-fa-icon"],hostVars:2,hostBindings:function(a,r){a&2&&(le("innerHTML",r.renderedIconHTML(),re),se("title",r.title()??void 0))},inputs:{icon:[1,"icon"],title:[1,"title"],animation:[1,"animation"],mask:[1,"mask"],flip:[1,"flip"],size:[1,"size"],pull:[1,"pull"],border:[1,"border"],inverse:[1,"inverse"],symbol:[1,"symbol"],rotate:[1,"rotate"],fixedWidth:[1,"fixedWidth"],transform:[1,"transform"],a11yRole:[1,"a11yRole"]},outputs:{icon:"iconChange",title:"titleChange",animation:"animationChange",mask:"maskChange",flip:"flipChange",size:"sizeChange",pull:"pullChange",border:"borderChange",inverse:"inverseChange",symbol:"symbolChange",rotate:"rotateChange",fixedWidth:"fixedWidthChange",transform:"transformChange",a11yRole:"a11yRoleChange"},decls:0,vars:0,template:function(a,r){},encapsulation:2,changeDetection:0})}return t})();var ts=(()=>{class t{static \u0275fac=function(a){return new(a||t)};static \u0275mod=ie({type:t});static \u0275inj=ne({})}return t})();export{Zo as a,ts as b};
