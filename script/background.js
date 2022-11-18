chrome.runtime.onInstalled.addListener(async () => {
    await chrome.action.setBadgeText({
        text: "OFF",
    });
});

chrome.runtime.onMessage.addListener(async (request, sender, sendResponse) => {
    if (request.percentage !== undefined) {
        chrome.notifications.create("drill_finished", {
            type: "basic",
            iconUrl: "../images/icon-128.png",
            title: "DrillsterBot",
            message: "DrillsterBot is klaar met deze Drill! Vragen: " + request.questions + ", Fouten: " + request.flaws
        });

        await chrome.action.setBadgeText({
            tabId: request.tabId,
            text: "DNE",
        });

        return true;
    }

    sendResponse();
});

chrome.commands.onCommand.addListener(async () => {
    let [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});

    if (tab.url.startsWith("https://www.drillster.com/user")) {
        const prevState = await chrome.action.getBadgeText({tabId: tab.id});
        const nextState = prevState === "ON" ? "OFF" : "ON";

        if (prevState === "DNE" || prevState === "RLD")
            return;

        chrome.tabs.sendMessage(tab.id, {run_mode: nextState, tabId: tab.id}, async () => {
            if (chrome.runtime.lastError) {
                await chrome.action.setBadgeText({
                    tabId: tab.id,
                    text: "RLD",
                });
            }
        });

        await chrome.action.setBadgeText({
            tabId: tab.id,
            text: nextState,
        });
    }
});