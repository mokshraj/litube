// refers: https://greasyfork.org/en/scripts/519614-live-chat-on-youtube-mobile
(function () {
    'use strict';

    setInterval(() => {
        const isLive = document.querySelector('#movie_player')?.getPlayerResponse()?.playabilityStatus?.liveStreamability && location.href.toLowerCase().startsWith('https://m.youtube.com/watch');
        show_live_chat(isLive);
    }, 1000);

    function values(key) {
        const languages = {
            'zh': { "Show Live Chat": "显示直播聊天", "Hide Live Chat": "隐藏直播聊天" },
            'en': { "Show Live Chat": "Show Live Chat", "Hide Live Chat": "Hide Live Chat" },
            'ja': { "Show Live Chat": "ライブチャットを表示", "Hide Live Chat": "ライブチャットを隠す" },
            'ko': { "Show Live Chat": "라이브 채팅 표시", "Hide Live Chat": "라이브 채팅 숨기기" },
            'fr': { "Show Live Chat": "Afficher le chat en direct", "Hide Live Chat": "Masquer le chat en direct" }
        };
        const lang = (document.body.lang || 'en').substring(0, 2).toLowerCase();
        return languages[lang]?.[key] || languages['en'][key];
    }

    function show_live_chat(isLive) {
        let button = document.querySelector('#live-chat-button');
        let chatContainer = document.querySelector('#live-chat-container');

        if (!button) {
            button = document.createElement('button');
            button.id = 'live-chat-button';
            button.style.display = 'none';
            document.body.appendChild(button);
        }

        if (!chatContainer) {
            chatContainer = document.createElement('div');
            chatContainer.id = 'live-chat-container';
            chatContainer.style.display = 'none';
            document.body.appendChild(chatContainer);
        }

        if (isLive) {
            button.innerText = values("Show Live Chat");
            button.style.position = 'fixed';
            button.style.bottom = '50px';
            button.style.right = '20px';
            button.style.zIndex = '9999';
            button.style.padding = '10px';
            button.style.backgroundColor = '#ff0000';
            button.style.color = '#fff';
            button.style.border = 'none';
            button.style.borderRadius = '5px';
            button.style.cursor = 'pointer';
            button.style.display = 'inline-block';

            chatContainer.style.position = 'absolute';
            chatContainer.style.width = 'calc(100% - 4px)';
            chatContainer.style.height = '420px';
            chatContainer.style.border = '1px solid #ccc';
            chatContainer.style.backgroundColor = '#fff';
            chatContainer.style.zIndex = '1000';
            chatContainer.style.overflow = 'auto';
            chatContainer.style.marginLeft = '2px';
            chatContainer.style.marginRight = '2px';
            chatContainer.style.setProperty("border-radius", "10px");

            if (!chatContainer.dataset.iframeAttached) {
                const chatIframe = document.createElement('iframe');
                chatIframe.style.width = '100%';
                chatIframe.style.height = '100%';
                chatIframe.style.border = 'none';
                chatIframe.style.maxWidth = '100%';

                button.onclick = function () {
                    if (chatContainer.style.display === 'none') {
                        const videoIdMatch = window.location.search.match(/v=([^&]+)/);
                        if (videoIdMatch) {
                            const videoId = videoIdMatch[1];
                            chatIframe.src = `https://www.youtube.com/live_chat?v=${videoId}&embed_domain=${window.location.hostname}`;
                            chatContainer.appendChild(chatIframe);
                            document.body.appendChild(chatContainer);
                            chatContainer.style.display = 'block';
                            button.innerText = values("Hide Live Chat");

                            const videoPlayer = document.querySelector('video');
                            if (videoPlayer) {
                                const videoRect = videoPlayer.getBoundingClientRect();
                                chatContainer.style.top = `${videoRect.bottom + window.scrollY}px`;
                            }

                            chatIframe.onload = function () {
                                try {
                                    const iframeDocument = chatIframe.contentWindow.document;
                                    const style = iframeDocument.createElement('style');
                                    style.textContent = `
                                        .style-scope.yt-live-chat-text-message-renderer {
                                            font-size: 12px !important;
                                        }
                                        .style-scope.yt-live-chat-text-message-renderer #author-name {
                                            font-size: 12px !important;
                                        }
                                    `;
                                    iframeDocument.head.appendChild(style);
                                } catch (e) {
                                    console.warn('Chat iframe CSS inject failed:', e);
                                }
                            };
                            chatContainer.dataset.iframeAttached = 'true';
                        } else {
                            alert('Could not find the video ID.');
                        }
                    } else {
                        chatContainer.style.display = 'none';
                        button.innerText = values("Show Live Chat");
                    }
                };
            }
        } else {
            button.style.display = 'none';
            chatContainer.style.display = 'none';
        }
    }

})();
