//Manage all the tabs by Chrome
let sentNotifications = {};

function start() {
    chrome.tabs.onUpdated.addListener((tabId) => setBadgeText("OFF", tabId));

    chrome.commands.onCommand.addListener((command, tab) => {
        if (!tab.url.startsWith("https://www.drillster.com/user"))
            return;

        if (command === "start_stop") {
            chrome.action.getBadgeText({tabId: tab.id}).then((prevState) => {
                const nextState = prevState === "ON" ? "OFF" : "ON";

                if (prevState === "RLD")
                    return;

                sendState(nextState, nextState, tab);
            });

        } else if (command === "wordlist") {
            chrome.tabs.sendMessage(tab.id, {show_wordlist: true}, () => {
                if (chrome.runtime.lastError) {
                } //Preventing "Receiving end does not exist!"
            });
        }
    });

    chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
        if (request.percentage !== undefined) {
            chrome.tabs.sendMessage(sender.tab.id, {
                drill_finished: request.drill_title,
                questions: request.questions,
                flaws: request.flaws
            }).then();
        } else if (request.drills_finished) {
            let notification = {
                type: "basic", iconUrl: "../images/icon-128.png", title: "DrillsterBot",
                message: "DrillsterBot is klaar met Drillster! Vragen: " + request.questions + ", Fouten: " + request.flaws,
                buttons: [{title: "Close Tab"}]
            }

            if (!request.auto_close) {
                notification.buttons = [{title: "Close Tab"}, {title: "Browse Tab"}];
                setBadgeText("OFF", sender.tab.id);
            } else {
                chrome.tabs.remove(sender.tab.id).then();
            }

            chrome.notifications.create("drill_finished", notification, (notificationId) => {
                if (!request.auto_close)
                    sentNotifications[notificationId] = sender.tab.id;
            });
        } else if (request.off_state !== undefined) {
            sendState("OFF", "OFF", sender.tab);
        } else if (request.on_state !== undefined) {
            sendState("ON", "ON", sender.tab);
        } else if (request.set_answer_time !== undefined) {
            chrome.storage.local.set({answer_time: request.set_answer_time}).then();
            notifyAllTabs({set_answer_time: request.set_answer_time});
        } else if (request.set_storage_enabled !== undefined) {
            chrome.storage.local.set({storage_enabled: request.set_storage_enabled}).then();
            notifyAllTabs({set_storage_enabled: request.set_storage_enabled});
        } else if (request.set_auto_close !== undefined) {
            chrome.storage.local.set({auto_close: request.set_auto_close}).then();
            notifyAllTabs({set_auto_close: request.set_auto_close});
        } else if (request.set_flaw_marge !== undefined) {
            chrome.storage.local.set({flaw_marge: request.set_flaw_marge}).then();
            notifyAllTabs({set_flaw_marge: request.set_flaw_marge});
        } else if (request.get_storage !== undefined) {
            chrome.storage.local.get(["answer_time", "storage_enabled", "auto_close", "flaw_marge"], (storage) => {
                sendResponse({
                    storage_enabled: (storage.storage_enabled === undefined ? true : storage.storage_enabled),
                    answer_time: (storage.answer_time === undefined || storage.answer_time <= 0 || storage.answer_time > 1000 ? 1 : storage.answer_time),
                    flaw_marge: (storage.flaw_marge === undefined || storage.flaw_marge >= 100 || storage.flaw_marge < 0 ? 0 : storage.flaw_marge),
                    auto_close: (storage.auto_close === undefined ? false : storage.auto_close)
                });
            });
            return true;
        }

        sendResponse();
        return true;
    });

    chrome.notifications.onButtonClicked.addListener((notificationId, buttonIndex) => {
        if (notificationId in sentNotifications) {
            chrome.tabs.get(sentNotifications[notificationId]).then((tab) => {
                chrome.notifications.clear(notificationId);
                delete sentNotifications[notificationId];

                if (tab === undefined)
                    return;

                if (buttonIndex === 0)
                    chrome.tabs.remove(tab.id).then();
                else
                    chrome.tabs.update(tab.id, {"active": true}).then();
            });
        }
    });
}

function sendState(tabState, state, preferredTab) {
    let tabId = preferredTab.id;
    let url = preferredTab.url;

    chrome.tabs.sendMessage(tabId, {run_mode: state, url: url}, () => {
        if (chrome.runtime.lastError) {
            chrome.action.setBadgeText({
                tabId: tabId,
                text: "RLD",
            }).then();
        }
    });

    setBadgeText(tabState, tabId);
}

function setBadgeText(text, tabId) {
    chrome.action.setBadgeText({
        tabId: tabId,
        text: text,
    }).then();
}

function notifyAllTabs(message) {
    chrome.tabs.query({}, (tabs) => {
        for (let index = 0; index < tabs.length; index++)
            chrome.tabs.sendMessage(tabs[index].id, message, () => {
                if (chrome.runtime.lastError) {
                } //Preventing "Receiving end does not exist!"
            });
    });
}

start();