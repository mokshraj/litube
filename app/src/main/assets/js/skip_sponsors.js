// refs: https://greasyfork.org/en/scripts/505870-skip-sponsors-using-sponsorblock-api-on-m-youtube-com-for-kiwi-browser-and-others/code
// ==UserScript==
// @name         Skip sponsors - using Sponsorblock API on m.youtube.com (for Kiwi browser and others)
// @namespace    http://your-namespace.com
// @version      2.17
// @description  Skip sponsors/SelfPromo, adds a highlight.
// @author       Sp0kz
// @match        https://m.youtube.com/*
// @icon         https://www.google.com/s2/favicons?sz=64&domain=youtube.com
// @grant        none
// @run-at       document-end
// ==/UserScript==
(function() {
if (window.skip_sponsors_injected) return;
    'use strict';
 //sha256 library
	 var sha256=function a(b){function c(a,b){return a>>>b|a<<32-b}for(var d,e,f=Math.pow,g=f(2,32),h="length",i="",j=[],k=8*b[h],l=a.h=a.h||[],m=a.k=a.k||[],n=m[h],o={},p=2;64>n;p++)if(!o[p]){for(d=0;313>d;d+=p)o[d]=p;l[n]=f(p,.5)*g|0,m[n++]=f(p,1/3)*g|0}for(b+="\x80";b[h]%64-56;)b+="\x00";for(d=0;d<b[h];d++){if(e=b.charCodeAt(d),e>>8)return;j[d>>2]|=e<<(3-d)%4*8}for(j[j[h]]=k/g|0,j[j[h]]=k,e=0;e<j[h];){var q=j.slice(e,e+=16),r=l;for(l=l.slice(0,8),d=0;64>d;d++){var s=q[d-15],t=q[d-2],u=l[0],v=l[4],w=l[7]+(c(v,6)^c(v,11)^c(v,25))+(v&l[5]^~v&l[6])+m[d]+(q[d]=16>d?q[d]:q[d-16]+(c(s,7)^c(s,18)^s>>>3)+q[d-7]+(c(t,17)^c(t,19)^t>>>10)|0),x=(c(u,2)^c(u,13)^c(u,22))+(u&l[1]^u&l[2]^l[1]&l[2]);l=[w+x|0].concat(l),l[4]=l[4]+w|0}for(d=0;8>d;d++)l[d]=l[d]+r[d]|0}for(d=0;8>d;d++)for(e=3;e+1;e--){var y=l[d]>>8*e&255;i+=(16>y?0:"")+y.toString(16)}return i}; /*https://geraintluff.github.io/sha256/sha256.min.js (public domain)*/

let index3=false;
  const info_elem = document.createElement('div');  let gradientStops = [];let gradientStophighlight = [];let skipSegments = [];let intervalId;let highlightSpan = document.createElement('span');const skipButton = document.createElement('button');skipButton.style.width="50px";skipButton.style.fontSize="40px";skipButton.textContent = '>>';let cu=0;
skipButton.className=('skip-button-S');skipButton.style.zIndex='8888';skipButton.style.background='transparent';skipButton.style.display="none";skipButton.style.textShadow = ` -1px -1px 0 #000, 1px -1px 0 #000,  -1px 1px 0 #000, 1px 1px 0 #000`;
  function getYouTubeVideoID(url) { const match = url.match(/(?:v=|\/)([a-zA-Z0-9_-]{11})/);
  return match ? match[1] : null;
}
let previousUrl = window.location.href;
///////
	    setInterval(() => {
			    if ((window.location.href !== previousUrl) && !window.location.href.includes('#searching') && !window.location.href.includes('#bottom-sheet')){
										previousUrl = window.location.href;
										clearInterval(intervalId);
			 if(document.querySelector('div[role="slider"]')){ document.querySelector('div[role="slider"]').style.background="none";document.querySelector('div[role="slider"]').parentNode.style.background="none";}
					if(window.location.href.includes('watch?v=')){
gradientStops = [];gradientStophighlight = []; skipSegments = [];index3=false;highlightSpan.style.opacity='0';skipButton.style.display='none';
 }
 else {skipButton.style.display='none';} }
  /////////
  if(document.querySelector('video')){
if (document.querySelector('video').currentTime>0 && document.querySelector('div[role="slider"]') &&window.location.href.includes('watch?v=') &&!index3) {
	index3=true;
	const videoID = getYouTubeVideoID(window.location.href);
  const hash = sha256(videoID).substr(0,4);
  const video_obj = document.querySelector("video");
  let url = `https://sponsor.ajay.app/api/skipSegments/${hash}?service=YouTube&categories=%5B%22sponsor%22,%22poi_highlight%22,%22selfpromo%22%5D`;
    (async () => {
		 try {
			 if(document.querySelector('div[role="slider"]')){ document.querySelector('div[role="slider"]').style.background="none";document.querySelector('div[role="slider"]').parentNode.style.background="none";}
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`HTTP error! Sponsorblock server down! Status: ${response.status}`);
        }
        const data = await response.json();
        if (!Array.isArray(data)) {
            throw new Error("Unexpected response format");
        }
        for (const video of data) {
      if (video.videoID != videoID) continue;
      info_elem.innerText = `(${video.segments.length} segments)`;
      const cat_n = video.segments.map(e=>e.category).sort()
          .reduce((acc,e) => (acc[e]=(acc[e]||0)+1, acc), {});
      info_elem.title = Object.entries(cat_n).map(e=>e.join(': ')).join(', ');
      for (const segment of video.segments) {
        const [start, stop] = segment.segment;
		let startPosition = ((start ) / (video_obj.duration)) * 100;
            let stopPosition = ((stop) / (video_obj.duration)) * 100;
		   		  if (segment.category === "sponsor" ){
		  gradientStops.push(`transparent ${startPosition+ 0.5}%, green ${startPosition + 0.5}%, green ${stopPosition}%, transparent ${stopPosition}%, transparent ${stopPosition}%`);
             skipSegments.push({ start, stop });
		  }
		  else if (segment.category === "selfpromo" ){
		  gradientStops.push(`transparent ${startPosition}%,  yellow ${startPosition}%, yellow ${stopPosition}%, transparent ${stopPosition}%, transparent ${stopPosition}%`);
          skipSegments.push({ start, stop });
		 }
		   		else  if (segment.category === "poi_highlight"){
					        highlightSpan.textContent = 'Highlight';highlightSpan.style.position = 'absolute';highlightSpan.style.bottom = '32px';highlightSpan.style.left = `${startPosition}%`;highlightSpan.style.marginLeft="5px";highlightSpan.style.transform = 'translateX(-50%)';highlightSpan.style.backgroundColor = 'black';highlightSpan.style.zIndex="5555";highlightSpan.style.fontSize="13px";highlightSpan.style.color = 'white';highlightSpan.style.padding = '2px 5px';highlightSpan.style.display="none";highlightSpan.style.opacity='1';
		 gradientStophighlight.push(`transparent ${((start ) / (video_obj.duration)) * 100}%,  red ${((start ) / (video_obj.duration)) * 100}%, red ${((start ) / (video_obj.duration)) * 100+1}%, transparent ${((start ) / (video_obj.duration)) * 100 +1}%, transparent ${((start ) / (video_obj.duration)) * 100+1}%`);
						 skipButton.style.display="block";
			skipButton.style.display="block";
	skipButton.addEventListener('touchstart', () => {document.querySelector('video').currentTime = start;});
	skipButton.addEventListener('click', () => {document.querySelector('video').currentTime = start;});
	skipButton.addEventListener('dblclick', () => {document.querySelector('video').currentTime = start;});
		     }
       if (segment.category != "sponsor" && segment.category != "selfpromo") continue;
      }
 intervalId = setInterval(() => {
	 	  	 if(document.querySelector('div[role="slider"]')){document.querySelector('div[role="slider"]').style.backgroundSize = `100% 25%`;document.querySelector('div[role="slider"]').style.backgroundPosition = `center center`;document.querySelector('div[role="slider"]').style.backgroundRepeat = 'no-repeat';document.querySelector('div[role="slider"]').style.backgroundImage = `linear-gradient(to right, ${gradientStops.join(', ')})`;document.querySelector('div[role="slider"]').parentNode.style.backgroundPosition = `center center`;document.querySelector('div[role="slider"]').parentNode.style.backgroundRepeat = 'no-repeat';document.querySelector('div[role="slider"]').parentNode.style.backgroundSize = `100% 25%`;document.querySelector('div[role="slider"]').parentNode.style.backgroundImage = `linear-gradient(to right, ${gradientStophighlight})`;
document.querySelector('div[role="slider"]')?.parentNode.appendChild(highlightSpan);
			 			 skipButton.title="Skip To Highlight";
					if (document.querySelector('.player-controls-top'))   document.querySelector('.player-controls-top').insertBefore(skipButton,document.querySelector('.player-controls-top').firstChild);
    document.querySelector('div[role="slider"]')?.addEventListener('mousemove', function(event) {
            highlightSpan.style.display = "block";
    });
	    document.querySelector('div[role="slider"]')?.parentNode.addEventListener('mouseout', function(event) {highlightSpan.style.display = "none";});
}
	  for (const { start, stop } of skipSegments) {
    if (video_obj.currentTime >= start && video_obj.currentTime < stop - 1) {video_obj.currentTime = stop;}
  }
  	}, 1000);
       }

    } catch (error) {
        console.error("Error fetching data:", error);
    }
})();
  }
    }
  }, 1000);
  window.skip_sponsors_injected = true;
})();