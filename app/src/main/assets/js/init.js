try {

    // avoid repeated injection
if (!window.injected){
    // Utils
    values = (key) => {
        const languages = {
            'zh': { 'loop': '循环播放', 'download': '下载', 'ok': '确定', 'video': '视频', 'cover': '封面', 'extension': '插件' },
            'en': { 'loop': 'Loop Play', 'download': 'Download', 'ok': 'OK', 'video': 'Video', 'cover': 'Cover', 'extension': 'Extension' },
            'ja': { 'loop': 'ループ再生', 'download': 'ダウンロード', 'ok': 'はい', 'video': 'ビデオ', 'cover': 'カバー', 'extension': 'プラグイン' },
            'ko': { 'loop': '반복 재생', 'download': '시모타코', 'ok': '확인', 'video': '비디오', 'cover': '커버', 'extension': '플러그인' },
            'fr': { 'loop': 'Lecture en boucle', 'download': 'Télécharger', 'ok': "D'accord", 'video': 'vidéo', 'cover': 'couverture', 'extension': 'extension' },
            }

        const lang = (document.body.lang || 'en').substring(0, 2).toLowerCase()
        return languages[lang] ? languages[lang][key] : languages['en'][key]
    }

    get_page_class = (url) => {
        url = url.toLowerCase();
        if (url.startsWith('https://m.youtube.com/shorts')) return 'shorts';
        if (url.startsWith('https://m.youtube.com/watch')) return 'watch';
        if (url.startsWith('https://m.youtube.com/feed/subscriptions')) return 'subscriptions';
        if (url.startsWith('https://m.youtube.com/feed/library')) return 'library';
        if (url.startsWith('https://m.youtube.com/channel')) return 'channel';
        if (url.startsWith('https://m.youtube.com/@')) return '@';
        if (url.startsWith('https://m.youtube.com/select_site')) return 'select_site';
        if (url.startsWith('https://m.youtube.com')) return 'home';
        else return 'unknown';
    }


    get_video_id = (url) => {
        try {
            const match = url.match(/watch\?v=([^&#]+)/)
            return match ? match[1] : null
        } catch (error) {
            console.error('Error getting video ID:', error)
            return null
        }
    }

    get_shorts_id = (url) => {
        try {
            const match = url.match(/shorts\/([^&#]+)/)
            return match ? match[1] : null
        } catch (error) {
            console.error('Error getting video ID:', error)
            return null
        }
    }

    // observe video id change

    // using onProgressChangeFinish event in android
    observe_video_id = () => {
        const current_video_id = get_video_id(location.href)
        if (current_video_id && window.video_id !== current_video_id) {
            window.video_id = current_video_id
            window.dispatchEvent(new Event('onVideoIdChange'))
        }
    }

    window.addEventListener('onProgressChangeFinish', observe_video_id)

    observe_shorts_id = () => {
        const current_shorts_id = get_shorts_id(location.href)
        if (current_shorts_id && window.shorts_id !== current_shorts_id) {
            window.shorts_id = current_shorts_id
            window.dispatchEvent(new Event('onShortsIdChange'))
        }
    }

    window.addEventListener('onProgressChangeFinish', observe_shorts_id)


    // Ads block
    if (!window.originalFetch) {
        window.originalFetch = fetch

        fetch = async (...args) => {
            const url = args[0].url
            if (url && url.includes('youtubei/v1/player')) {
                const response = await window.originalFetch(...args)
                const data = await response.json()

                // Remove specified keys from the response data
                const rules = ['playerAds', 'adPlacements', 'adBreakHeartbeatParams', 'adSlots']
                for (const rule of rules) {
                    if (data.hasOwnProperty(rule)) {
                        delete data[rule]
                    }
                }

                return new Response(JSON.stringify(data), {
                    status: response.status,
                    headers: response.headers,
                })
            } else {
                return window.originalFetch(...args)
            }
        }
    }


    // Init configuration

    // video quality
    set_quality = () => {
        const player = document.getElementById("movie_player")
        if (get_page_class(location.href) === 'watch'){
            const target_quality = localStorage.getItem('video_quality') || 'default'
            if (player.getAvailableQualityLevels().indexOf(target_quality) !== -1){
                player.setPlaybackQualityRange(target_quality)
            }
        }
    }

    save_quality = (e) => {
        const target = e.target
        if (target.id.startsWith('player-quality-dropdown') || target.classList.contains('player-quality-settings')){
            console.log('video quality setting changed')
            localStorage.setItem('video_quality', document.getElementById("movie_player").getPlaybackQuality())
        }
    }

    document.addEventListener('change', save_quality)
    window.addEventListener('onVideoIdChange', set_quality)


    // refresh
    window.addEventListener('onRefresh', () => {
        window.location.reload()
    })
    window.addEventListener('onProgressChangeFinish', () => {
        android.finishRefresh()
    })

    window.addEventListener('doUpdateVisitedHistory', () => {
        const page_class = get_page_class(location.href)
        if (page_class === 'home' || page_class === 'subscriptions' || page_class === 'library'
            || page_class === '@'
        ){
            android.setRefreshLayoutEnabled(true)
        } else{
            android.setRefreshLayoutEnabled(false)
        }
    })


    // init video player

    const observer = new MutationObserver((mutationsList) => {
        for (const mutation of mutationsList) {
            if (mutation.type === 'childList') {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType !== 1) {
                        return
                    }

                    const key = 'progress-' + get_video_id(location.href)
                    const expiredTime = Math.floor((Date.now() + 3 * 24 * 60 * 60 * 1000) / 100000).toString();
                    if (node.classList.contains('video-stream')){

                        // memory progress
                        node.addEventListener('timeupdate', () => {
                            if (node.currentTime !== 0){
                                localStorage.setItem(key, `${node.currentTime.toString()}-${expiredTime}`)
                            }
                        })
                    }

                    if (node.id === 'movie_player') {
                        window.last_player_state = -1
                        node.addEventListener('onStateChange', (data) => {
                            if([1, 3].includes(data) && window.last_player_state === -1 && get_page_class(location.href) === 'watch'){
                                // resume progress
                                const savedItem = (localStorage.getItem(key) || '0-0').split('-')
                                const saved_time = parseInt(savedItem[0])
                                const saved_expired = parseInt(savedItem[1])
                                if (saved_time > 5 && node.getDuration() - saved_time > 5) {
                                    node.seekTo(saved_time)
                                }
                                if (saved_expired > 0 && saved_expired < Math.floor(Date.now() / 100000).toString()) {
                                    localStorage.removeItem(key)
                                }

                                // show and sync playback
                                const title = document.title.replace(/\s*- YouTube$/, "");
                                const thumbnail = `https://img.youtube.com/vi/${get_video_id(location.href)}/maxresdefault.jpg`;
                                const duration = node.getDuration();
                                android.showPlayback(title, thumbnail, duration);

                                const player = node.querySelector('.video-stream');
                                player.addEventListener('timeupdate', () => {

                                    const currentTimeMs = Math.round(node.getCurrentTime() * 1000);
                                    const isActuallyPlaying = (node.getPlayerState() === 1);
                                    android.updatePlayback(
                                        currentTimeMs,
                                        node.getPlaybackRate(),
                                        isActuallyPlaying
                                    );
                                });
                                // listen to "play" event
                                window.addEventListener('play', () => node.playVideo());
                                // listen to "pause" event
                                window.addEventListener('pause', () => node.pauseVideo());
                                // listen to "skip" event
                                window.addEventListener('skipToNext', () => node.nextVideo());
                                window.addEventListener('skipToPrevious', () => node.previousVideo());
                                // listen to "seekTo" event
                                window.addEventListener('seek', e => node.seekTo(parseInt(e.detail.time)));
                            }
                            window.last_player_state = data
                        })
                    }

                    // add download button
                    let bSave = document.querySelector('.yt-spec-button-view-model.slim_video_action_bar_renderer_button')
                    if(bSave && get_page_class(location.href) === 'watch' && !document.getElementById('downloadButton')) {
                        let bDown = bSave.cloneNode(true)
                        bDown.id = 'downloadButton'
                        bDown.getElementsByClassName("yt-spec-button-shape-next__button-text-content")[0].innerText = values('download')

                        const svg = bDown.querySelector('svg')
                        if (svg) {
                            svg.setAttribute("viewBox", "0 -960 960 960")
                            const path = svg.querySelector('path')
                            if (path) {
                                path.setAttribute("d", "M480-328.46 309.23-499.23l42.16-43.38L450-444v-336h60v336l98.61-98.61 42.16 43.38L480-328.46ZM252.31-180Q222-180 201-201q-21-21-21-51.31v-108.46h60v108.46q0 4.62 3.85 8.46 3.84 3.85 8.46 3.85h455.38q4.62 0 8.46-3.85 3.85-3.84 3.85-8.46v-108.46h60v108.46Q780-222 759-201q-21 21-51.31 21H252.31Z")
                            }
                            bDown.onclick = function() {
                                android.download(location.href)
                            }
                            bSave.parentElement.insertBefore(bDown, bSave)
                        }
                    }

                  // add extension button
                  if (get_page_class(location.href) === 'select_site' && !document.getElementById('extensionButton')) {
                        let settings = document.querySelector('ytm-settings');
                        if (settings) {
                            let button = settings.firstElementChild;
                            if (button && button.querySelector('svg')) {
                                let bExtension = button.cloneNode(true);
                                bExtension.id = 'extensionButton';
                                bExtension.querySelector('.yt-core-attributed-string').innerText = values('extension');

                                const svg = bExtension.querySelector('svg');
                                if (svg) {
                                    svg.setAttribute("viewBox", "0 -960 960 960");
                                    const path = svg.querySelector('path');
                                    if (path) path.setAttribute("d", "M358.15-160H200q-16.85 0-28.42-11.58Q160-183.15 160-200v-158.15q34.15-10 57.08-37.81Q240-423.77 240-460q0-36.23-22.92-64.04-22.93-27.81-57.08-37.81V-720q0-16.85 11.58-28.42Q183.15-760 200-760h160q10.77-34.31 37.85-54.85 27.07-20.54 62.15-20.54t62.15 20.54Q549.23-794.31 560-760h160q16.85 0 28.42 11.58Q760-736.85 760-720v160q34.31 10.77 54.85 37.85 20.54 27.07 20.54 62.15t-20.54 62.15Q794.31-370.77 760-360v160q0 16.85-11.58 28.42Q736.85-160 720-160H561.85q-10.77-36.15-38.81-58.08Q495-240 460-240t-63.04 21.92q-28.04 21.93-38.81 58.08ZM200-200h131.92q17.08-39.85 52.77-59.92Q420.38-280 460-280q39.62 0 75.31 20.08Q571-239.85 588.08-200H720v-195.38h18.46q28.77-4.62 42.85-23.7 14.07-19.07 14.07-40.92t-14.07-40.92q-14.08-19.08-42.85-23.7H720V-720H524.62v-18.46q-4.62-28.77-23.7-42.85-19.07-14.07-40.92-14.07t-40.92 14.07q-19.08 14.08-23.7 42.85V-720H200v131.08q37.08 17.69 58.54 52.77Q280-501.08 280-460q0 40.85-21.46 75.92-21.46 35.08-58.54 53V-200Zm260-260Z");
                                }
                                bExtension.onclick = function () {
                                    android.extension();
                                };

                                settings.insertBefore(bExtension, button);

                            }
                        }

                  }
                })
            }
        }
    })
    observer.observe(document.documentElement, {
        childList: true,
        subtree: true
    })



    window.injected = true

}} catch (error) {
    console.error(error)
    throw error
}

