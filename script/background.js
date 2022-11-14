chrome.runtime.onInstalled.addListener(async () => {
    await chrome.action.setBadgeText({
        text: "OFF",
    });
});

chrome.commands.onCommand.addListener(async () => {
    let [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});

    if (tab.url.startsWith("https://www.drillster.com/user")) {
        const prevState = await chrome.action.getBadgeText({tabId: tab.id});
        const nextState = prevState === "ON" ? "OFF" : "ON";

        if(prevState === "DNE" || prevState === "RLD")
            return;

        chrome.tabs.sendMessage(tab.id, {run_mode: nextState}, function () {
            if (chrome.runtime.lastError) {
                chrome.action.setBadgeText({
                    tabId: tab.id,
                    text: "RLD",
                });
            }
        });

        await chrome.action.setBadgeText({
            tabId: tab.id,
            text: nextState,
        });

        if(nextState === "ON") {
            let interval = setInterval(function () {
                chrome.tabs.sendMessage(tab.id, {progress: "GET"}, function (response) {
                    if (response.progress === 100) {
                        chrome.notifications.create("drill_finished", {
                            type: "basic",
                            iconUrl: "../images/icon-128.png",
                            title: "DrillsterBot",
                            message: "DrillsterBot is klaar met deze Drill!"
                        });

                        chrome.action.setBadgeText({
                            tabId: tab.id,
                            text: "DNE",
                        });
                    }

                    if(response.progress === -1 || response.progress === 100) {
                        clearInterval(interval);
                    }
                });
            }, 1000);
        }
    }
});