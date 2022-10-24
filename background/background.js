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
        let running = true;

        chrome.tabs.sendMessage(tab.id, {run_mode: nextState}, function (response) {
        });

        if(nextState === "ON") {
            while (await chrome.action.getBadgeText({tabId: tab.id}) === "ON" && running) {
                setTimeout(function () {
                    chrome.tabs.sendMessage(tab.id, {check_running: "Check"}, function (response) {

                        if (!response.is_running) {
                            running = false;

                            chrome.action.setBadgeText({
                                tabId: tab.id,
                                text: "OFF",
                            });
                        }
                    });
                }, 5000);
            }
        }

        await chrome.action.setBadgeText({
            tabId: tab.id,
            text: nextState,
        });
    }
});